package com.zidi.CodeRacer.vehicle.commands.Impl;

import com.zidi.CodeRacer.vehicle.commands.VehicleCommand;
import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;

public class MoveForwardCommentImpl implements VehicleCommand {
    private float remain;           // 还要走的距离
    private final float maxStep;    // 每帧最大步长（防止冲过头）

    public MoveForwardCommentImpl() {
        this(0.30f, 0.10f);         // 默认：总共前进 0.30，单帧最多 0.10
    }
    public MoveForwardCommentImpl(float distance, float maxStep) {
        this.remain = Math.max(0f, distance);
        this.maxStep = Math.max(0.02f, maxStep);
    }

    @Override public void onStart(VehicleContext ctx) {}

    @Override
    public boolean execute(float dt, VehicleContext ctx) {
        if (remain <= 0f) return true;
        float step = Math.min(remain, maxStep);
        // 这里把 throttle 当“本帧前进距离”
        ctx.apply(0f, step, 0f);
        remain -= step;
        return remain <= 0f;
    }

    @Override public void onEnd(VehicleContext ctx) { ctx.apply(0f,0f,0f); }
}
