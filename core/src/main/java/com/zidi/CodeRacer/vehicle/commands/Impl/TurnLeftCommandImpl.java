package com.zidi.CodeRacer.vehicle.commands.Impl;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.zidi.CodeRacer.vehicle.commands.VehicleCommand;
import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;
import com.zidi.CodeRacer.vehicle.runtime.Impl.VehicleContextImpl;

public class TurnLeftCommandImpl implements VehicleCommand {

    // —— 输入 —— //
    private final Rectangle interRect;
    private final float laneCenterX;
    private final float laneCenterY;

    // —— 圆弧几何 —— //
    private Vector2 C;      // 圆心
    private float   r;      // 半径
    private float   ang0;   // 弧起始角
    private float   ang1;   // 弧终止角
    private float   tArc;   // 弧进度 [0,1]
    private Vector2 T0, T1; // 调试点

    // —— 状态机 —— //
    // -1: 预滚动直行；0:趋近T0（保留但基本跳过）；1:跑弧；2:出口缓对齐；3:完成
    private int phase = -1;
    private boolean lockedArcStart = false;

    // —— 预滚动（“晚点再左转”） —— //
    private boolean preRoll = true;
    private float   preRollRemain;        // 剩余需要直行的距离
    private float   preRollStep   = 0.12f;
    private float   preRollScale  = 0.45f; // 预滚动长度 ~ 0.35 * min(w,h)
    private float   preRollMin    = 0.6f;  // 至少直行这么多

    // —— 柔和对齐（弧末逐帧贴中线） —— //
    private boolean alignUseX = true;   // true: 对齐x；false: 对齐y
    private float   alignTarget;        // 目标中线坐标
    private float   alignAlpha  = 0.22f;// LERP 系数（0~1）
    private float   alignDist   = 0.8f; // 对齐过程前进距离

    // —— 其它参数 —— //
    private float radiusScale     = 0.35f;
    private float marginScale     = 0.10f;
    private float approachStop    = 0.25f;
    private float stepToT0        = 0.14f;
    private float dThetaCoef      = 1.6f;
    private float dThetaMin       = 0.02f;
    private float dThetaMax       = 0.10f;
    private float exitStepBase    = 0.12f;
    private float exitDist        = 0.8f;
    private float exited          = 0f;

    public TurnLeftCommandImpl(Rectangle interRect, float laneCenterX, float laneCenterY) {
        this.interRect   = new Rectangle(interRect);
        this.laneCenterX = laneCenterX;
        this.laneCenterY = laneCenterY;
        float w = interRect.width, h = interRect.height;
        this.preRollRemain = Math.max(preRollMin, preRollScale * Math.min(w, h)); // 晚一点再左转
    }

    @Override
    public boolean execute(float dt, VehicleContext ctx) {
        if (phase == 3) return true;

        // —— Phase -1: 预滚动 —— //
        if (preRoll) {
            if (ctx instanceof VehicleContextImpl v) {
                float h = v.getHeading();
                float step = Math.min(preRollRemain, preRollStep);
                v.setPosition(v.getX() + step * MathUtils.cos(h),
                    v.getY() + step * MathUtils.sin(h));
                preRollRemain -= step;
                if (preRollRemain <= 0f) {
                    preRoll = false;
                    // 此时用“当前姿态”构造局部弧，直接进入跑弧（跳过趋近T0）
                    buildQuarterArc(ctx);
                    phase = 1;
                }
            } else {
                // 兜底：轻微油门直行
                ctx.apply(0f, 0.5f, 0f);
            }
            return false;
        }

        if (C == null) buildQuarterArc(ctx);

        switch (phase) {
            case 0 -> { // 保留：一般不会走到这里
                Vector2 pos = new Vector2(ctx.getX(), ctx.getY());
                Vector2 to  = new Vector2(T0).sub(pos);
                float d = to.len();
                float desired = (float) Math.atan2(to.y, to.x);

                if (ctx instanceof VehicleContextImpl v) {
                    v.setHeading(desired);
                    float step = Math.min(d, stepToT0);
                    v.setPosition(v.getX() + step * MathUtils.cos(desired),
                        v.getY() + step * MathUtils.sin(desired));
                } else {
                    float err = norm(desired - ctx.getHeading());
                    ctx.apply(MathUtils.clamp(err, -0.12f, 0.12f), 0.45f, 0f);
                }
                if (d < approachStop) {
                    if (ctx instanceof VehicleContextImpl v) v.setHeading(ang0 + MathUtils.PI * 0.5f);
                    tArc = 0f; lockedArcStart = false; phase = 1;
                }
            }

            case 1 -> { // —— 跑弧（纯几何硬覆盖） —— //
                if (ctx instanceof VehicleContextImpl v) {
                    if (!lockedArcStart) {
                        Vector2 startOn = new Vector2(C.x + r * MathUtils.cos(ang0),
                            C.y + r * MathUtils.sin(ang0));
                        v.setPosition(startOn.x, startOn.y);
                        v.setHeading(ang0 + MathUtils.PI * 0.5f);
                        lockedArcStart = true;
                    }
                    float dTheta = MathUtils.PI * 0.5f *
                        MathUtils.clamp(dt * dThetaCoef, dThetaMin, dThetaMax);
                    tArc = Math.min(1f, tArc + dTheta / (MathUtils.PI * 0.5f));
                    float ang = MathUtils.lerp(ang0, ang1, tArc);

                    Vector2 on = new Vector2(C.x + r * MathUtils.cos(ang),
                        C.y + r * MathUtils.sin(ang));
                    v.setPosition(on.x, on.y);
                    v.setHeading(ang + MathUtils.PI * 0.5f);

                    if (tArc >= 1f) {
                        lockedArcStart = false;

                        // 确定对齐轴 & 目标（右侧通行修正）
                        String dir = headingNESW(v.getHeading());
                        Vector2 fixedLC = ensureRightHandLane(interRect, dir, laneCenterX, laneCenterY);
                        switch (dir) {
                            case "N", "S" -> { alignUseX = true;  alignTarget = fixedLC.x; }
                            case "E", "W" -> { alignUseX = false; alignTarget = fixedLC.y; }
                        }
                        exited = 0f;
                        phase  = 2; // 进入柔和对齐阶段
                    }
                } else {
                    // 兜底近似
                    float total = MathUtils.PI * 0.5f;
                    float per   = MathUtils.clamp(dt * 0.08f, 0.01f, 0.04f);
                    int steps = Math.max(1, Math.round((total * (1f - tArc)) / per / 12f));
                    for (int i = 0; i < steps; i++) {
                        ctx.apply(MathUtils.clamp(per, -0.08f, 0.08f), 0.6f, 0f);
                        tArc = Math.min(1f, tArc + (per / total));
                        if (tArc >= 1f) break;
                    }
                    if (tArc >= 1f) phase = 2;
                }
            }

            case 2 -> { // —— 柔和对齐 + 直行缓冲 —— //
                if (ctx instanceof VehicleContextImpl v) {
                    float h = v.getHeading();
                    float step = exitStepBase;

                    float nx = v.getX(), ny = v.getY();
                    if (alignUseX) {
                        nx = MathUtils.lerp(nx, alignTarget, alignAlpha); // 逐帧贴近中线
                    } else {
                        ny = MathUtils.lerp(ny, alignTarget, alignAlpha);
                    }
                    // 前进
                    nx += step * MathUtils.cos(h);
                    ny += step * MathUtils.sin(h);

                    v.setPosition(nx, ny);
                    exited += step;
                } else {
                    ctx.apply(0f, 0.5f, 0f);
                    exited += Math.max(0.6f, ctx.getSpeed());
                }
                if (exited >= Math.max(exitDist, alignDist)) phase = 3;
            }
        }

        return phase == 3;
    }

    // ========= 局部四分之一圆：与当前姿态严格相切 =========
    private void buildQuarterArc(VehicleContext ctx) {
        float wRect = interRect.width, hRect = interRect.height;
        float margin = marginScale * Math.min(wRect, hRect);
        r = Math.max(0.25f, radiusScale * Math.min(wRect, hRect) - margin);

        float hx = ctx.getX(), hy = ctx.getY();
        float heading = ctx.getHeading();

        Vector2 left = new Vector2(-MathUtils.sin(heading), MathUtils.cos(heading)); // 左法向
        C = new Vector2(hx, hy).mulAdd(left, r);

        ang0 = (float) Math.atan2(hy - C.y, hx - C.x);
        ang1 = ang0 + MathUtils.PI * 0.5f;

        T0 = new Vector2(hx, hy);
        T1 = new Vector2(C.x + r * MathUtils.cos(ang1),
            C.y + r * MathUtils.sin(ang1));

        tArc = 0f;
    }

    // ========= 右侧通行矫正：把 laneCenter 修正到“本向正确一侧” =========
    private static Vector2 ensureRightHandLane(Rectangle interRect, String dir,
                                               float laneCenterX, float laneCenterY) {
        float cx = interRect.x + interRect.width  * 0.5f;
        float cy = interRect.y + interRect.height * 0.5f;
        float x = laneCenterX, y = laneCenterY;

        switch (dir) {
            case "N" -> { if (x < cx) x = cx + (cx - x); } // 北行：右侧在东边
            case "S" -> { if (x > cx) x = cx - (x - cx); } // 南行：右侧在西边
            case "E" -> { if (y > cy) y = cy - (y - cy); } // 东行：右侧在南边
            case "W" -> { if (y < cy) y = cy + (cy - y); } // 西行：右侧在北边
        }
        return new Vector2(x, y);
    }

    // ========= 工具 =========
    private static String headingNESW(float rad) {
        float deg = (float) Math.toDegrees(rad);
        deg = (deg % 360 + 360) % 360;
        if (deg >= 45 && deg < 135)  return "N";
        if (deg >= 135 && deg < 225) return "W";
        if (deg >= 225 && deg < 315) return "S";
        return "E";
    }

    private static float norm(float a) {
        while (a >  Math.PI) a -= MathUtils.PI2;
        while (a < -Math.PI) a += MathUtils.PI2;
        return a;
    }
}
