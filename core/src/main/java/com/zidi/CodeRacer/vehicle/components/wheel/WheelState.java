package com.zidi.CodeRacer.vehicle.components.wheel;

/** 轮胎的瞬时可变状态（物理步进中更新） */
public interface WheelState {
    float steerDeg();          // 当前转角
    float slipRatio();         // 纵向滑移 (κ)
    float wear01();            // 磨损 0..1
    float tempC();             // 温度 ℃
}
