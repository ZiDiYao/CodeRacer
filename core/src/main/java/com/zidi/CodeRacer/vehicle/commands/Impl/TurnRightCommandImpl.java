package com.zidi.CodeRacer.vehicle.commands.Impl;

import com.badlogic.gdx.math.Rectangle;
import com.zidi.CodeRacer.vehicle.commands.VehicleCommand;
import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;
import com.zidi.CodeRacer.vehicle.runtime.Impl.VehicleContextImpl; // 用于安全转换
import com.badlogic.gdx.math.MathUtils;

/**
 * TurnRightCommentImpl — 微步右转：
 * 每帧仅移动一个很小的距离，并顺时针旋转一个很小角度，直到累计右转 ~90°。
 * 约定：heading 用弧度，0 指 +X，逆时针为正；右转=顺时针=负角。
 */
public class TurnRightCommandImpl implements VehicleCommand {


    private final float targetX, targetY;

    // 微步参数（按你 1 格车道调的很小）
    private final float stepDist;        // 每帧前进
    private final float yawStepRad;      // 每帧右转角（负值 = 顺时针）
    private final float completeTurnRad; // 90°
    private final float snapPosTol;      // 吸附距离阈值
    private final float snapYawTol;      // 吸附角度阈值（弧度）

    private float startHeading = 0f;
    private float turnedRight = 0f;
    private boolean started = false;

    public TurnRightCommandImpl(Rectangle interRect, float exitX, float exitY) {
        this(interRect, exitX, exitY,
            0.05f,                          // stepDist：更小，避免越界
            -MathUtils.degreesToRadians*1.2f, // yawStep：更小角度右转
            MathUtils.PI/2f,                // 90°
            0.12f,                          // 位置吸附阈值（~1/8 格）
            MathUtils.degreesToRadians*4f   // 朝向吸附阈值（~4°）
        );
    }

    public TurnRightCommandImpl(Rectangle interRect, float exitX, float exitY,
                                     float stepDist, float yawStepRad, float completeTurnRad,
                                     float snapPosTol, float snapYawTol) {
        this.targetX = exitX;
        this.targetY = exitY;
        this.stepDist = stepDist;
        this.yawStepRad = yawStepRad;
        this.completeTurnRad = completeTurnRad;
        this.snapPosTol = snapPosTol;
        this.snapYawTol = snapYawTol;
    }

    @Override
    public void onStart(VehicleContext ctx) {
        started = true;
        startHeading = norm(ctx.getHeading());
        turnedRight = 0f;
    }

    @Override
    public boolean execute(float dt, VehicleContext ctx) {
        if (!started) onStart(ctx);
        if (!(ctx instanceof VehicleContextImpl v)) return true; // 仅支持微步实现

        // 当还未达到 90°，持续小步右转；最后几步自动缩小角度避免超转
        float remain = completeTurnRad - turnedRight;
        float yawThis = Math.max(-remain, yawStepRad); // yawStepRad < 0
        float h = norm(v.getHeading() + yawThis);
        v.setHeading(h);

        // 前进微步
        float nx = v.getX() + stepDist * MathUtils.cos(h);
        float ny = v.getY() + stepDist * MathUtils.sin(h);
        v.setPosition(nx, ny);

        // 累计右转量
        float inc = ((yawThis + MathUtils.PI) % MathUtils.PI2) - MathUtils.PI; // (-π,π]
        if (inc <= 0) turnedRight += -inc;

        // —— 收尾吸附：接近 90° 且到达目标点附近就“吸附”到车道中心 —— //
        float dx = targetX - v.getX();
        float dy = targetY - v.getY();
        boolean nearPos = dx*dx + dy*dy <= snapPosTol*snapPosTol;
        boolean nearYaw = Math.abs(angleToRightOf(startHeading) - h) <= snapYawTol;

        if ((turnedRight >= completeTurnRad * 0.98f && nearPos) || (turnedRight >= completeTurnRad)) {
            // 最后一步吸附，确保不落到虚线
            v.setPosition(targetX, targetY);
            v.setHeading(angleToRightOf(startHeading));
            return true;
        }
        return false;
    }

    @Override public void onEnd(VehicleContext context) {}

    // === 工具 ===
    private static float norm(float a) {
        float r = (a + MathUtils.PI) % MathUtils.PI2;
        if (r < 0) r += MathUtils.PI2;
        return r - MathUtils.PI;
    }

    /** 起始朝向右转 90° 的目标朝向 */
    private static float angleToRightOf(float startHeading) {
        return norm(startHeading - MathUtils.PI/2f); // 顺时针=减
    }
}
