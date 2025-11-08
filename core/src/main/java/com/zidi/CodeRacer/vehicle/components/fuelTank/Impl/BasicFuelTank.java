package com.zidi.CodeRacer.vehicle.components.fuelTank.Impl;

import com.zidi.CodeRacer.vehicle.components.fuelTank.FuelTank;
import com.zidi.CodeRacer.vehicle.components.Part;

/**
 * 抽象燃料箱基类，提供通用油量逻辑。
 */
public abstract class BasicFuelTank extends Part implements FuelTank {

    private final float capacity;
    private float fuelLevel;

    protected BasicFuelTank(String partID, String partName, String description,
                            int mass, int cost, float capacity) {
        super(partID, partName, description, mass, cost);
        this.capacity = capacity;
        this.fuelLevel = capacity; // 默认加满
    }

    @Override
    public float getFuelLevel() {
        return fuelLevel;
    }

    @Override
    public float getCapacity() {
        return capacity;
    }

    @Override
    public void consume(float amount) {
        if (amount <= 0) return;
        fuelLevel = Math.max(0, fuelLevel - amount);
    }

    @Override
    public void refuel(float amount) {
        if (amount <= 0) return;
        fuelLevel = Math.min(capacity, fuelLevel + amount);
    }

    @Override
    public void refuel() {
        fuelLevel = capacity;
    }

    @Override
    public boolean isEmpty() {
        return fuelLevel <= 0.0001f;
    }

    @Override
    public String toString() {
        return String.format("%s [%.1f / %.1f]", getPartName(), fuelLevel, capacity);
    }
}
