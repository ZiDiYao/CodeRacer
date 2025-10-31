package com.zidi.CodeRacer.vehicle.runtime;
import com.badlogic.gdx.math.MathUtils;
import com.zidi.CodeRacer.vehicle.components.frame.Pose;
import com.zidi.CodeRacer.world.nav.Path;
import com.zidi.CodeRacer.world.nav.PathSampler;

public class VehicleUpdater {
    public float wheelbase = 2.6f; // 轴距
    public float kpSpeed   = 2.0f; // 速度P增益
    public float maxAccel  = 5.0f; // 加速度限幅
    public int   lookaheadSteps = 6;

    private int nearestHint = 0;

    /** 基于 Path 的最小 pure-pursuit 横向 + 简单纵向控制 */
    public void step(Pose pose, Path path, float targetSpeed, float dt) {
        if (path == null || path.size() == 0) return;

        // 1) 找最近点 + 前视点
        nearestHint = PathSampler.nearestIndex(path, pose.getPos(), nearestHint);
        int la = PathSampler.lookaheadIndex(path, nearestHint, lookaheadSteps);
        var target = path.get(la);

        // 世界→车体局部
        float dx = target.x - pose.getX();
        float dy = target.y - pose.getY();
        float c = MathUtils.cos(-pose.getHeadingRad());
        float s = MathUtils.sin(-pose.getHeadingRad());
        float xL = dx * c - dy * s;             // 车前
        float yL = dx * s + dy * c;             // 车左

        // 2) 转角（简化 pure-pursuit）：steer ≈ atan2(2L*y / Ld^2)
        float steer = MathUtils.atan2(2f * yL, Math.max(0.001f, xL * xL));
        steer = MathUtils.clamp(steer, -0.65f, 0.65f);

        // 3) 航向更新：自行车模型 yawRate = v * tan(steer)/L
        float yawRate = pose.getSpeed() * MathUtils.tan(steer) / Math.max(0.1f, wheelbase);
        pose.setHeadingRad(pose.getHeadingRad() + yawRate * dt);

        // 4) 纵向速度：P 控制 + 限幅
        float accel = MathUtils.clamp(kpSpeed * (targetSpeed - pose.getSpeed()), -maxAccel, maxAccel);
        pose.setSpeed(Math.max(0f, pose.getSpeed() + accel * dt));

        // 5) 位移
        pose.step(dt);
    }
}
