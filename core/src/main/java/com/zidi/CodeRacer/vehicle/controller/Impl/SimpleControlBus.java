package com.zidi.CodeRacer.vehicle.controller.Impl;

import com.zidi.CodeRacer.vehicle.controller.ControlBus;

/** 最简单的控制总线实现：提供 setter，谁当大脑谁来改数值 */
public class SimpleControlBus implements ControlBus {
    private float throttle01, brake01, steerDeg;
    @Override public float throttle01() { return throttle01; }
    @Override public float brake01()    { return brake01; }
    @Override public float steerDeg()    { return steerDeg; }
    public void setThrottle01(float v){ this.throttle01 = clamp01(v); }
    public void setBrake01(float v){ this.brake01 = clamp01(v); }
    public void setSteerDeg(float v){ this.steerDeg = v; }
    private static float clamp01(float v){ return v < 0 ? 0 : (v > 1 ? 1 : v); }
}
