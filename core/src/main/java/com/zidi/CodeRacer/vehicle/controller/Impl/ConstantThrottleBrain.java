package com.zidi.CodeRacer.vehicle.controller.Impl;

/** 一个最简单的“恒定控制”大脑：先让车能动起来 */
public class ConstantThrottleBrain {
    private final SimpleControlBus bus;
    private final float throttle, brake, steerDeg;
    public ConstantThrottleBrain(SimpleControlBus bus, float throttle01, float brake01, float steerDeg){
        this.bus = bus;
        this.throttle = throttle01;
        this.brake = brake01;
        this.steerDeg = steerDeg;
    }
    /** 每帧调用一次，把指令写进总线 */
    public void update(){
        bus.setThrottle01(throttle);
        bus.setBrake01(brake);
        bus.setSteerDeg(steerDeg);
    }
}
