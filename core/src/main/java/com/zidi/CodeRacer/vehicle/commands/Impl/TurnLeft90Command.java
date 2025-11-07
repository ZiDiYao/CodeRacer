package com.zidi.CodeRacer.vehicle.commands.Impl;

import com.badlogic.gdx.math.MathUtils;
import com.zidi.CodeRacer.vehicle.commands.VehicleCommand;

public class TurnLeft90Command extends TurnByAngleCommand {
    public TurnLeft90Command(int steerPolarity) {
        super(
            +MathUtils.PI / 2f,                 // 左转90°
            MathUtils.degreesToRadians * 1.0f,  // 每帧转1°
            0.05f,                              // 每帧前进
            MathUtils.degreesToRadians * 4f,    // 容差4°
            steerPolarity,
            1.0f                                // 延迟1秒再转
        );
    }
}
