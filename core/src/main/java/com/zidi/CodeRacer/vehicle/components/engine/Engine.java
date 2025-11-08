package com.zidi.CodeRacer.vehicle.components.engine;

public interface Engine {
    EngineSpec  spec();
    EngineState state();

    /** 0..1 油门开度 */
    void setThrottle(float throttle);

    /** 连接油箱（可选先空） */
    void attachFuelTank(com.zidi.CodeRacer.vehicle.components.fuelTank.FuelTank tank);

    /**
     * 物理步：dt 秒
     * @param loadOmega  负载角速度(rad/s)，简单可传车轮等效到发动机的ω；暂时可传当前内部ω
     * @return 本步输出扭矩（Nm）
     */
    float update(float dt, float loadOmega);

    /** 直接取当前可用扭矩（Nm），等同 update 后的输出 */
    float getTorqueNm();

    /** 当前曲轴转速（rpm） */
    float getRpm();

    /** 是否熄火/没油 */
    boolean isStalled();
}
