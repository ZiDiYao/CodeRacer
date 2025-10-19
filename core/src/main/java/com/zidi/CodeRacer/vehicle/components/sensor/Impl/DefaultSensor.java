package com.zidi.CodeRacer.vehicle.components.sensor.Impl;

import com.zidi.CodeRacer.vehicle.components.Part;
import com.zidi.CodeRacer.vehicle.components.sensor.Sensor;
public abstract class DefaultSensor extends Part implements Sensor {
    private final int range;
    private final int sampleRateHz;
    private final int fovDegrees;
    private final int powerCost;

    protected DefaultSensor(String id, String name, String desc, int mass, int cost,
                            int range, int sampleRateHz, int fovDegrees, int powerCost) {
        super(id, name, desc, mass, cost);
        this.range = range;
        this.sampleRateHz = sampleRateHz;
        this.fovDegrees = fovDegrees;
        this.powerCost = powerCost;
    }

    @Override public int getRange()        { return range; }
    @Override public float getSampleRateHz(){ return sampleRateHz; }
    @Override public int getFovDegrees()   { return fovDegrees; }
    @Override public int getPowerCost()    { return powerCost; }
}
