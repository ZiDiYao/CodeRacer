package com.zidi.CodeRacer.vehicle.components.fuelTank;

public interface FuelTank {

    int getFuelLevel();

    int getCapacity();

    /**
     * 根据不通的发动机 + 车身有不同的油耗，比如走一格掉多少油
     * @param rate
     */

    void consume(int rate);

    void refuel();

    boolean isEmpty();




}
