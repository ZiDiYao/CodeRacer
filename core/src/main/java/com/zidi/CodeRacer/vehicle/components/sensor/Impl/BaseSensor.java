package com.zidi.CodeRacer.vehicle.components.sensor.Impl;

public class BaseSensor extends DefaultSensor {


    public BaseSensor(String id, String name, String desc, int mass, int cost) {
        super(id, name, desc, mass, cost,
            2, 1, 1, 0);
    }

    @Override
    public void onClick() {

    }



    @Override
    public String detect() {
        return null;
    }

}
