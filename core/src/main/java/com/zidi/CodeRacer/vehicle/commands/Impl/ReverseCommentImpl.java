package com.zidi.CodeRacer.vehicle.commands.Impl;

import com.zidi.CodeRacer.vehicle.commands.VehicleCommand;
import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;

public class ReverseCommentImpl implements VehicleCommand {

    @Override
    public boolean execute(float deltaTime, VehicleContext context) {
        return false;
    }

    @Override
    public void onStart(VehicleContext context) {
        VehicleCommand.super.onStart(context);
    }

    @Override
    public void onEnd(VehicleContext context) {
        VehicleCommand.super.onEnd(context);
    }
}
