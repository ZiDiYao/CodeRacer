package com.zidi.CodeRacer.vehicle.components.sensor;

import com.zidi.CodeRacer.vehicle.components.Part;

public interface Sensor {

    int getRange();

    float getSampleRateHz();

    int getFovDegrees();

    default int getPowerCost(){return 0;}

    // Call this method to start detect

    String detect();


}
