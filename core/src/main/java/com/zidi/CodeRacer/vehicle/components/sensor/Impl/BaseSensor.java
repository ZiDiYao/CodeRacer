package com.zidi.CodeRacer.vehicle.components.sensor.Impl;

import com.badlogic.gdx.math.MathUtils;
import com.zidi.CodeRacer.Commons.Enum.TileType;
import com.zidi.CodeRacer.Commons.utils.TiledWorldUtils;
import com.zidi.CodeRacer.vehicle.components.frame.Pose;
import com.zidi.CodeRacer.vehicle.components.sensor.SensorReading;

public class BaseSensor extends DefaultSensor {


    public BaseSensor(String id, String name, String desc, int mass, int cost, TiledWorldUtils world, Pose pose) {
        super(
            id, name, desc, mass, cost,
            /*range*/        2,
            /*sampleRateHz*/ 1,
            /*fovDegrees*/   1,
            /*powerCost*/    0,
            world, pose
        );
    }

    @Override
    public void onClick() {

    }


    /**
     *
     * 这个目前只能识别 Collection
     * @return
     */
    @Override
    public SensorReading detect() {
        final float warnDist = 5f;        // 提前 5 格预警
        final float maxDist  = 50f;       // 最大射线距离（够大即可）
        final float step     = 0.25f;     // 采样步长

        float x = pose.getX();
        float y = pose.getY();
        float h = pose.getHeadingRad();

        // 算到最近碰撞体的距离
        float d = world.distanceToCollisionForward(x, y, h, maxDist, step);

        if (d <= warnDist) {
            return new SensorReading(true, d, 0f, "CollisionAhead");
        }
        return new SensorReading(false, d, 0f, "Clear");
    }


}
