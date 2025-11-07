package com.zidi.CodeRacer.vehicle.commands.Impl;
import com.badlogic.gdx.math.MathUtils;
import com.zidi.CodeRacer.vehicle.commands.VehicleCommand;


import com.badlogic.gdx.math.MathUtils;

public class TurnRight90Command extends TurnByAngleCommand {
    public TurnRight90Command(int steerPolarity) {
        super(
            -MathUtils.PI / 2f,                 // 右转90°
            MathUtils.degreesToRadians * 1.0f,
            0.05f,
            MathUtils.degreesToRadians * 4f,
            steerPolarity,
            1.0f
        );
    }
}
