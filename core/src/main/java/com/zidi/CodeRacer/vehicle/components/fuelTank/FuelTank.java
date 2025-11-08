package com.zidi.CodeRacer.vehicle.components.fuelTank;

/**
 * FuelTank - 车辆燃料储存接口
 *
 * 由不同材质或容量的具体实现类（如 WoodenFuelTank, MetalFuelTank 等）实现。
 */
public interface FuelTank {

    /** 当前剩余燃料量（升或任意单位） */
    float getFuelLevel();

    /** 最大容量 */
    float getCapacity();

    /**
     * 消耗指定数量的燃料。
     * 例如：每移动一格，或每帧根据发动机效率减少。
     *
     * @param amount 要消耗的燃料量（必须 >= 0）
     */
    void consume(float amount);

    /**
     * 为油箱加油。
     * 若传入量使得超过最大容量，则自动加满。
     *
     * @param amount 要加的燃料量（若 <= 0 则忽略）
     */
    void refuel(float amount);

    /**
     * 将油箱加满。
     */
    void refuel();

    /**
     * 是否没油。
     * @return true 若油量为 0 或以下
     */
    boolean isEmpty();
}
