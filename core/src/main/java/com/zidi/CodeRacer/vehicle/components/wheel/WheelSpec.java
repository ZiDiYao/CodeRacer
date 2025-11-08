package com.zidi.CodeRacer.vehicle.components.wheel;


/** 只读的轮胎参数（不会随时间变化） */
public interface WheelSpec {
    float radius();            // 半径 (m)
    float width();             // 胎宽 (m)
    float maxSteerDeg();       // 最大转向角 (deg)
    boolean driven();          // 是否驱动轮
    boolean braked();          // 是否带刹车
    float muDry();             // 干地摩擦系数
    float muWet();             // 湿地摩擦系数
    float cRolling();          // 滚阻系数
}
