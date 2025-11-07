// TurnByAngleCommand.java
package com.zidi.CodeRacer.vehicle.commands.Impl;

import com.zidi.CodeRacer.vehicle.commands.VehicleCommand;
import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;
import com.badlogic.gdx.math.MathUtils;

/**
 * 平滑转弯命令，可配置：
 * - 延迟前行时间 delayBeforeTurn（单位秒）
 * - 每帧转角 yawStepRad
 * - 每帧前进距离 stepDist
 * - 目标角 targetDelta
 * - steerPolarity：决定 apply() 里正负号方向
 */
public class TurnByAngleCommand implements VehicleCommand {

    private final float targetDelta;     // 目标累计角度（左正右负）
    private final float yawStep;         // 每帧旋转角（正数）
    private final float stepDist;        // 每帧前进距离
    private final float snapTol;         // 角度贴合阈值
    private final int steerPolarity;     // +1 或 -1
    private final float delayBeforeTurn; // 延迟时间（秒）

    private float turned = 0f;
    private float startHeading;
    private boolean started = false;
    private float delayElapsed = 0f;

    public TurnByAngleCommand(float targetDeltaRad,
                              float yawStepRad,
                              float stepDist,
                              float snapTolRad,
                              int steerPolarity,
                              float delayBeforeTurnSec) {
        this.targetDelta = targetDeltaRad;
        this.yawStep = Math.abs(yawStepRad);
        this.stepDist = stepDist;
        this.snapTol = snapTolRad;
        this.steerPolarity = steerPolarity >= 0 ? +1 : -1;
        this.delayBeforeTurn = delayBeforeTurnSec;
    }

    @Override
    public void onStart(VehicleContext ctx) {
        started = true;
        turned = 0f;
        delayElapsed = 0f;
        startHeading = ctx.getHeading();
    }

    @Override
    public boolean execute(float dt, VehicleContext ctx) {
        if (!started) onStart(ctx);

        // -------- 阶段 1：延迟前行 -------- //
        if (delayElapsed < delayBeforeTurn) {
            delayElapsed += dt;
            // 🚗 直行，不转动方向
            ctx.apply(0f, stepDist, 0f);
            return false;
        }

        // -------- 阶段 2：开始转弯 -------- //
        float remain = targetDelta - turned;
        if (Math.abs(remain) <= snapTol) {
            ctx.setHeading(norm(startHeading + targetDelta * steerPolarity));
            return true;
        }

        float sign = Math.signum(remain == 0 ? targetDelta : remain);
        float yawNow = sign * yawStep;
        float steerNow = yawNow * steerPolarity;

        ctx.apply(steerNow, stepDist, 0f);
        turned += yawNow;
        return false;
    }

    @Override public void onEnd(VehicleContext ctx) {}

    private static float norm(float a) {
        float r = (a + MathUtils.PI) % MathUtils.PI2;
        if (r < 0) r += MathUtils.PI2;
        return r - MathUtils.PI;
    }
}
