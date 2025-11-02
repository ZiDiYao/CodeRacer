package com.zidi.CodeRacer.vehicle.runtime.Impl;


import com.badlogic.gdx.math.MathUtils;
import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;

import com.badlogic.gdx.math.MathUtils;
import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;

public class VehicleContextImpl implements VehicleContext {

    private float x, y, heading;

    public VehicleContextImpl(float x, float y, float heading) {
        this.x = x;
        this.y = y;
        this.heading = heading;
    }

    @Override
    public void apply(float steer, float throttle, float brake) {
        // 简化物理：每次调用前进 1 单位
        heading += steer;
        x += MathUtils.cos(heading);
        y += MathUtils.sin(heading);
    }

    /** 手动旋转（调试用，例如按 A/D 键时） */
    public void rotate(float dHeading) {
        heading += dHeading;
    }

    /** ✅ 新增：允许命令类直接修改位置 */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /** ✅ 新增：允许命令类直接修改朝向 */
    public void setHeading(float heading) {
        this.heading = heading;
    }

    @Override public float getSpeed()   { return 1f; }
    @Override public float getHeading() { return heading; }
    @Override public float getX()       { return x; }
    @Override public float getY()       { return y; }
}
