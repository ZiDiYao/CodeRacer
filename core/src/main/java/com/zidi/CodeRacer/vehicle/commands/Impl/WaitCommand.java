package com.zidi.CodeRacer.vehicle.commands.Impl;

import com.zidi.CodeRacer.vehicle.commands.VehicleCommand;
import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;

public class WaitCommand implements VehicleCommand {
    private final float waitTime;
    private float elapsed = 0f;

    public WaitCommand(float seconds) {
        this.waitTime = seconds;
    }

    @Override
    public void onStart(VehicleContext ctx) {
        elapsed = 0f;
    }

    @Override
    public boolean execute(float dt, VehicleContext ctx) {
        elapsed += dt;
        // 可以选择让车辆保持静止
        ctx.apply(0f, 0f, 0f);
        return elapsed >= waitTime;
    }

    @Override
    public void onEnd(VehicleContext ctx) {
        ctx.apply(0f, 0f, 0f);
    }
}
