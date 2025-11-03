package com.zidi.CodeRacer.vehicle.commands.Impl;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.zidi.CodeRacer.vehicle.commands.VehicleCommand;
import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;

public class LaneChangeCommandImpl implements VehicleCommand {
    private final boolean left;
    private final float laneWidth;       // 一条车道的宽度：你这里传 1f
    private final float length;          // 变道过程中向前走的距离（建议 6~12）
    private final float durationSeconds; // 变道时长（建议 1.2~2.0）

    private Vector2 start, dir, n;
    private float t;

    public LaneChangeCommandImpl(boolean left, float laneWidth, float length, float durationSeconds) {
        this.left = left;
        this.laneWidth = laneWidth;
        this.length = length;
        this.durationSeconds = durationSeconds;
    }

    @Override
    public void onStart(VehicleContext ctx) {
        start = new Vector2(ctx.getX(), ctx.getY());
        float heading = ctx.getHeading();
        dir = new Vector2(MathUtils.cos(heading), MathUtils.sin(heading)); // 单位前向
        n   = new Vector2(-dir.y, dir.x);                                   // 左法向
        if (!left) n.scl(-1f);                                              // 右变道
        t = 0f;
    }

    @Override
    public boolean execute(float dt, VehicleContext ctx) {
        t = Math.min(1f, t + dt / durationSeconds);

        // 纵向：线性推进
        float s = t;
        float forward = length * s;

        // 横向：SmoothStep（3s^2-2s^3），单调且端点导数为 0，绝不超过 laneWidth
        float sSmooth = s * s * (3f - 2f * s);
        float lateral = laneWidth * sSmooth;

        // 新位置
        float x = start.x + dir.x * forward + n.x * lateral;
        float y = start.y + dir.y * forward + n.y * lateral;
        ctx.setPosition(x, y);

        // 朝向：用微小前向差分近似切线方向
        float eps = 1e-3f;
        float s2 = Math.min(1f, s + eps);
        float f2 = length * s2;
        float l2 = laneWidth * (s2 * s2 * (3f - 2f * s2));
        float x2 = start.x + dir.x * f2 + n.x * l2;
        float y2 = start.y + dir.y * f2 + n.y * l2;
        ctx.setHeading((float) Math.atan2(y2 - y, x2 - x));

        return t >= 1f;
    }
}
