package com.zidi.CodeRacer.vehicle.commands.Impl;


import com.zidi.CodeRacer.vehicle.commands.VehicleCommand;
import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;

public class MoveForwardCommentImpl implements VehicleCommand {
    private static final float STEP = 1f;     // 前进 1 单位
    private static final float EPS  = 0.02f;  // 到达阈值

    private float targetX, targetY;

    @Override
    public void onStart(VehicleContext ctx) {
        float h = ctx.getHeading();
        targetX = ctx.getX() + STEP * (float) Math.cos(h);
        targetY = ctx.getY() + STEP * (float) Math.sin(h);
    }

    @Override
    public boolean execute(float dt, VehicleContext ctx) {
        // 让底层位移一步（你引擎内部决定每次 move 多远）
        ctx.apply(0f, 1f, 0f);

        // 距离目标点是否足够近
        float dx = targetX - ctx.getX();
        float dy = targetY - ctx.getY();
        return dx*dx + dy*dy <= EPS*EPS;
    }

    @Override
    public void onEnd(VehicleContext ctx) {
        // 可选：收油或轻刹
        ctx.apply(0f, 0f, 0f);
    }
}
