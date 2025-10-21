package com.zidi.CodeRacer.vehicle.components.engine;

public interface Engine {

    int getCurrentSpeed();

    int getMaxSpeed();

    int getFuelConsumptionRate();

    void accelerate(float deltaTime);

    void decelerate(float deltaTime);






}
