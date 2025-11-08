package com.zidi.CodeRacer.vehicle.components.wheel;

import com.zidi.CodeRacer.vehicle.components.Part;

/** 轮子本体：外部只通过这层驱动/刹车/设转角，并读取受力 */
public interface Wheel {
    WheelSpec spec();
    WheelState state();

    // 控制输入（由引擎/制动/转向系统写入）
    void setTargetSteerDeg(float targetDeg);
    void setDriveTorque(float Nm);     // +驱动 / -发动机制动
    void setBrakeTorque(float Nm);     // 机械刹车

    // 物理步进（由 Physics/VehicleContext 每帧调用）
    void preStep(float dt, float normalLoadN, float groundMu, float wheelAngularVel, float wheelForwardSpeed, float wheelLateralSpeed);
    void step(float dt);

    // 受力输出（施加到车体的力）
    float getFx();   // 纵向力 N（推进/制动）
    float getFy();   // 侧向力 N（转弯）
    float getMz();   // 车轮自对准力矩（可选，用于更真实转向手感）
}
