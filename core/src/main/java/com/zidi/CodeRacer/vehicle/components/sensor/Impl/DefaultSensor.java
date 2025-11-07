package com.zidi.CodeRacer.vehicle.components.sensor.Impl;

import com.badlogic.gdx.math.MathUtils;
import com.zidi.CodeRacer.Commons.Enum.TileType;
import com.zidi.CodeRacer.Commons.utils.TiledWorldUtils;
import com.zidi.CodeRacer.vehicle.components.Part;
import com.zidi.CodeRacer.vehicle.components.frame.Pose;
import com.zidi.CodeRacer.vehicle.components.sensor.Sensor;
import com.zidi.CodeRacer.vehicle.components.sensor.SensorReading;

public abstract class DefaultSensor extends Part implements Sensor {
    private final int range;
    private final int sampleRateHz;
    private final int fovDegrees;
    private final int powerCost;

    protected final TiledWorldUtils world; // ← 注入
    protected final Pose pose;             // ← 注入（车辆的实时 Pose 引用）

    protected DefaultSensor(
        String id, String name, String desc, int mass, int cost,
        int range, int sampleRateHz, int fovDegrees, int powerCost,
        TiledWorldUtils world, Pose pose
    ) {
        super(id, name, desc, mass, cost);
        this.range = range;
        this.sampleRateHz = sampleRateHz;
        this.fovDegrees = fovDegrees;
        this.powerCost = powerCost;

        this.world = world; // ← 赋值
        this.pose  = pose;  // ← 赋值
    }

    @Override public int getRange()         { return range; }
    @Override public float getSampleRateHz(){ return sampleRateHz; }
    @Override public int getFovDegrees()    { return fovDegrees; }
    @Override public int getPowerCost()     { return powerCost; }

    public Pose pose() {
        return this.pose;
    }
}
