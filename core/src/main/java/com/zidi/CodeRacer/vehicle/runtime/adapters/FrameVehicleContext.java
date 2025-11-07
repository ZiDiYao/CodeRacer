package com.zidi.CodeRacer.vehicle.runtime.adapters;

import com.zidi.CodeRacer.vehicle.components.frame.Frame;
import com.zidi.CodeRacer.vehicle.components.frame.Pose;
import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;

/**
 * 极简适配器：
 * - steer 视为“本帧转角（弧度）”
 * - throttle 视为“本帧前进距离”
 * - brake 先忽略（之后接入动力/阻力即可）
 */
public class FrameVehicleContext implements VehicleContext {
    private final Frame frame;
    private final Pose pose;

    public FrameVehicleContext(Frame frame) {
        this.frame = frame;
        this.pose  = frame.pose();
    }

    @Override
    public void apply(float steer, float throttle, float brake) {
        if (steer != 0f)    pose.rotate(steer);
        if (throttle != 0f) pose.translate(throttle);
    }

    @Override public float getSpeed()   { return pose.getSpeed(); }
    @Override public float getHeading() { return pose.getHeadingRad(); }
    @Override public float getX()       { return pose.getX(); }
    @Override public float getY()       { return pose.getY(); }

    @Override public void setPosition(float x, float y) { pose.set(x, y, pose.getHeadingRad(), pose.getSpeed()); }
    @Override public void setHeading(float headingRad)  { pose.setHeadingRad(headingRad); }
}
