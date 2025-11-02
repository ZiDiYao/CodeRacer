package com.zidi.CodeRacer.vehicle.runtime;


/**
 * VehicleContext 是命令执行的上下文环境。
 * 它定义命令可用的控制接口与可观测信息。
 */
public interface VehicleContext {
    /** 应用一帧的底层控制信号（你底层物理层实现） */
    void apply(float steer, float throttle, float brake);

    /** 获取当前车速，用于距离型命令计算 */
    float getSpeed();

    /** 获取朝向、位置等可观测信息 */
    float getHeading();
    float getX();
    float getY();

    void setPosition(float x, float y);
    void setHeading(float heading);
}
