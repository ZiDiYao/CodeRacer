package com.zidi.CodeRacer.vehicle.runtime.Impl;
import com.zidi.CodeRacer.vehicle.components.engine.Engine;
import com.zidi.CodeRacer.vehicle.components.frame.Pose;
import com.zidi.CodeRacer.vehicle.components.powertrain.SimpleGeartrain;
import com.zidi.CodeRacer.vehicle.components.wheel.Wheel;
import com.zidi.CodeRacer.vehicle.controller.ControlBus;

import java.util.List;

/** 最小闭环帧更新：Control → Engine → Wheels → 合力推进 Pose */
public class VehicleUpdater {
    private static final float G = 9.81f;
    private static final float BRAKE_TORQUE_MAX = 3000f; // 先给个常数，后面换成每轮规格

    private final Pose pose;
    private final float massKg;
    private final Engine engine;
    private final List<Wheel> wheels;
    private final ControlBus control;
    private final SimpleGeartrain geartrain;

    public VehicleUpdater(Pose pose, float massKg, Engine engine, List<Wheel> wheels,
                          ControlBus control, SimpleGeartrain geartrain) {
        this.pose = pose;
        this.massKg = Math.max(1f, massKg);
        this.engine = engine;
        this.wheels = wheels;
        this.control = control;
        this.geartrain = geartrain;
    }

    public void step(float dt){
        // 0) 大脑把指令写到总线（外部先调用；若没大脑，这里也可以什么都不做）

        // 1) 应用控制到部件
        engine.setThrottle(control.throttle01());
        float brakeNm = control.brake01() * BRAKE_TORQUE_MAX;
        float steerDeg = control.steerDeg();
        for (Wheel w : wheels){
            w.setBrakeTorque(brakeNm);
            w.setTargetSteerDeg(steerDeg); // 第一版：四轮同角；以后前轮转向
        }

        // 2) 估算负载角速度，更新发动机
        float loadOmega = geartrain.estimateLoadOmega(pose);
        float engineTorque = engine.update(dt, loadOmega);

        // 3) 扭矩分配到驱动轮
        geartrain.distributeTorque(engineTorque);

        // 4) 给每个轮子做 preStep，喂入载荷/地面/运动学
        float perWheelLoad = (massKg * G) / Math.max(1, wheels.size());
        float v = Math.max(0f, pose.getSpeed());
        for (Wheel w : wheels){
            float R = Math.max(1e-4f, w.spec().radius());
            float omega = v / R;
            float mu = w.spec().muDry(); // 先用干地
            w.preStep(dt, perWheelLoad, mu, omega, v, 0f);
        }

        // 5) 让轮子结算受力
        float sumFx = 0f;
        for (Wheel w : wheels){
            w.step(dt);
            sumFx += w.getFx();
        }

        // 6) 纵向合力推进车速与位置（极简 1D）
        float ax = sumFx / massKg;
        float newSpeed = Math.max(0f, v + ax * dt);
        pose.setSpeed(newSpeed);
        pose.step(dt); // 用 Pose 内置的 “按速度沿朝向前进”
    }
}
