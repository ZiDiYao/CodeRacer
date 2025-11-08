package com.zidi.CodeRacer.vehicle.components.engine;

public interface EngineSpec {
    float idleRpm();             // 怠速
    float redlineRpm();          // 红线
    float peakTorqueNm();        // 峰值扭矩（在某个中等转速）
    float peakTorqueRpm();       // 峰值扭矩对应的转速
    float inertia();             // 发动机等效转动惯量(kg·m^2) —— 简化

    /** 满油门时的燃油消耗率（单位/秒） */
    float fullThrottleFuelUnitsPerSec();
}
