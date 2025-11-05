package com.zidi.CodeRacer.vehicle.runtime.Impl;

import com.badlogic.gdx.math.MathUtils;
import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;

public class VehicleContextImpl implements VehicleContext {

    private float x, y;
    private float headingRad; // 弧度

    public VehicleContextImpl(float x, float y, float headingRad) {
        this.x = x;
        this.y = y;
        this.headingRad = headingRad; // 传入即为弧度
    }

    @Override
    public void apply(float steer, float throttle, float brake) {
        // 简化物理：每次调用向前推进 1 单位，并且把 steer 直接当作“本帧转角（弧度增量）”
        headingRad = normalizeRad(headingRad + steer);
        x += MathUtils.cos(headingRad); // 弧度
        y += MathUtils.sin(headingRad);
    }

    public void rotate(float dHeadingRad) {
        headingRad = normalizeRad(headingRad + dHeadingRad);
    }

    public void setPosition(float x, float y) {
        this.x = x; this.y = y;
    }

    public void setHeading(float headingRad) {
        this.headingRad = normalizeRad(headingRad);
    }

    @Override public float getSpeed()   { return 1f; }            // demo：恒定1
    @Override public float getHeading() { return headingRad; }    // 弧度
    @Override public float getX()       { return x; }
    @Override public float getY()       { return y; }

    private static float normalizeRad(float a) {
        float r = (a + MathUtils.PI) % MathUtils.PI2;
        if (r < 0) r += MathUtils.PI2;
        return r - MathUtils.PI;
    }
}
