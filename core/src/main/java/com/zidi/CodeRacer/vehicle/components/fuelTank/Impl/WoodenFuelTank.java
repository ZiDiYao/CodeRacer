package com.zidi.CodeRacer.vehicle.components.fuelTank.Impl;

/**
 * WoodenFuelTank - 木质燃料箱（容量小、便宜、易燃）
 */
public class WoodenFuelTank extends BasicFuelTank {

    private static final float CAPACITY = 100f;

    public WoodenFuelTank(String partID, String partName, String description, int mass, int cost) {
        super(partID, partName, description, mass, cost, CAPACITY);
    }

    @Override
    public void onClick() {
        System.out.println("[WoodenFuelTank] clicked — current fuel: " + getFuelLevel());
    }
}
