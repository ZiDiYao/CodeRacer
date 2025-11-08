package com.zidi.CodeRacer.vehicle.components.engine.Impl;

import com.zidi.CodeRacer.vehicle.components.Part;
import com.zidi.CodeRacer.vehicle.components.engine.Engine;
import com.zidi.CodeRacer.vehicle.components.engine.EngineSpec;
import com.zidi.CodeRacer.vehicle.components.engine.EngineState;
import com.zidi.CodeRacer.vehicle.components.fuelTank.FuelTank;

/**
 * DefaultEngine
 * 抽象发动机基类：实现 Engine 的通用逻辑（油门、油耗、转速/扭矩更新、熄火判定、状态导出）。
 * 具体机型只需实现：扭矩曲线、燃油曲线等抽象方法即可。
 *
 * 建议你先继承它做一个 SimpleEngine（填一条扭矩-转速曲线与油耗曲线），
 * 再把该发动机接到 VehicleContext 的动力链即可。
 */
public abstract class DefaultEngine extends Part implements Engine {

    // ===== 固定规格 =====
    protected final EngineSpec spec;

    // ===== 依赖（可选）=====
    protected FuelTank tank;            // 外部可 attach，没油则熄火

    // ===== 运行状态 =====
    protected float rpm;                // 曲轴转速
    protected float omega;              // 角速度 rad/s（内部积分）
    protected float throttle01;         // 油门 0..1
    protected float torqueNm;           // 本步输出扭矩
    protected boolean stalled;          // 是否熄火（没油或故障）

    // ===== 数值参数 / 可调 =====
    protected float syncK = 5f;         // 与负载角速度的同步强度
    protected float selfDampingK = 0.02f; // 自身阻尼（防止无限升速）

    // ===== 燃油累加（把 float 单位转成 FuelTank 的 int 单位）=====
    private float pendingFuelUnits = 0f;     // 累积的“燃油单位”
    protected float fuelUnit = 1.0f;         // 1 个单位 = FuelTank.consume(1) 的粒度（可改为 0.5 / 0.1）

    protected DefaultEngine(String partID, String partName, String description,
                            int mass, int cost, EngineSpec spec) {
        super(partID, partName, description, mass, cost);
        this.spec = spec;
        this.rpm = clamp(spec.idleRpm(), 0f, spec.redlineRpm());
        this.omega = rpmToOmega(this.rpm);
        this.throttle01 = 0f;
        this.torqueNm = 0f;
        this.stalled = false;
    }

    // ============== Engine 接口实现 ==============

    @Override
    public EngineSpec spec() { return spec; }

    @Override
    public EngineState state() {
        final float r = rpm, t = throttle01, tq = torqueNm;
        final boolean s = stalled;
        return new EngineState() {
            @Override public float rpm()        { return r; }
            @Override public float throttle01() { return t; }
            @Override public float torqueNm()   { return tq; }
            @Override public boolean stalled()  { return s; }
        };
    }

    @Override
    public void setThrottle(float throttle) {
        this.throttle01 = clamp(throttle, 0f, 1f);
        // 熄火状态下给油门无效（如需“点火”逻辑，可在子类里覆写）
    }

    @Override
    public void attachFuelTank(FuelTank tank) {
        this.tank = tank;
    }

    /**
     * 模板方法：一次物理步更新。
     * @param dt        步长（秒）
     * @param loadOmega 负载角速度（rad/s），由传动系/车轮等效回发动机
     * @return 本步输出扭矩（Nm）
     */
    @Override
    public float update(float dt, float loadOmega) {
        // 1) 燃油消耗与熄火判定
        handleFuel(dt);
        if (tank != null && tank.isEmpty()) {
            stalled = true;
        }

        // 2) 熄火：无扭矩输出，角速度逐步衰减
        if (stalled) {
            torqueNm = 0f;
            rpm = Math.max(0f, rpm - 2000f * dt);
            omega = rpmToOmega(rpm);
            return torqueNm;
        }

        // 3) 同步到负载角速度（简化耦合）
        omega += (loadOmega - omega) * syncK * dt;

        // 4) 计算目标扭矩：由子类给出扭矩-转速曲线 * 油门
        float baseTorque = clamp(torqueAtRpm(rpm), 0f, Float.MAX_VALUE);
        float targetTorque = baseTorque * throttle01;

        // 5) 自阻尼（与角速度成正比）
        float selfBrakeNm = selfDampingK * omega;
        torqueNm = Math.max(0f, targetTorque - selfBrakeNm);

        // 6) ω 与 rpm 的积分与钳制
        float J = Math.max(1e-4f, spec.inertia());
        omega += (torqueNm / J) * dt;
        rpm = clamp(omegaToRpm(omega), 0f, spec.redlineRpm());

        // 达到红线附近时可触发“切断”（可选：子类里覆写 redlineCutoff()）
        if (rpm >= spec.redlineRpm() && redlineCutoff()) {
            torqueNm = 0f; // 强制切断
        }

        return torqueNm;
    }

    @Override public float getTorqueNm() { return torqueNm; }
    @Override public float getRpm()      { return rpm; }
    @Override public boolean isStalled() { return stalled; }

    @Override
    public void onClick() {
        // 可用于 UI 检查/调试
    }

    // ============== 模板钩子（交给子类实现/覆写） ==============

    /**
     * 扭矩-转速曲线（满油门基线），单位 Nm。
     * 子类必须实现：给定 rpm 返回“满油门时”的基础扭矩。
     * 外层会乘以 throttle01 并做阻尼/红线等处理。
     */
    protected abstract float torqueAtRpm(float rpm);

    /**
     * 当前油门下的“燃油单位”消耗率（单位/秒）。
     * 默认实现：满油门 rate * throttle（线性），子类可覆写成更真实的映射。
     */
    protected float fuelUnitsPerSecond(float throttle01) {
        return spec.fullThrottleFuelUnitsPerSec() * clamp(throttle01, 0f, 1f);
    }

    /**
     * 红线切断：到达红线时是否进行扭矩切断。
     * 返回 true 表示切断（默认 true）；如需赛车风格“轻切”，可覆写返回 false。
     */
    protected boolean redlineCutoff() { return true; }

    // ============== 内部：燃油处理 ==============

    protected void handleFuel(float dt) {
        if (tank == null || stalled) return;
        float rate = Math.max(0f, fuelUnitsPerSecond(throttle01)); // 单位/秒
        pendingFuelUnits += rate * dt;

        // 将浮点燃油单位“攒够 1”后，交给 FuelTank.consume(1)
        while (pendingFuelUnits >= fuelUnit) {
            if (tank.isEmpty()) {
                stalled = true;
                pendingFuelUnits = 0f;
                return;
            }
            tank.consume(1);
            pendingFuelUnits -= fuelUnit;
        }
    }

    // ============== 工具 ==============

    protected static float clamp(float v, float lo, float hi) {
        return (v < lo) ? lo : (Math.min(v, hi));
    }

    protected static float rpmToOmega(float rpm) {
        return rpm * (float)(2.0 * Math.PI / 60.0);
    }

    protected static float omegaToRpm(float omega) {
        return omega * (float)(60.0 / (2.0 * Math.PI));
    }
}
