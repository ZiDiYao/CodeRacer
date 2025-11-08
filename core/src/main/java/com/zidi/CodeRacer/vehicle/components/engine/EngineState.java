package com.zidi.CodeRacer.vehicle.components.engine;

public interface EngineState {
    float rpm();
    float throttle01();
    float torqueNm();
    boolean stalled();
}
