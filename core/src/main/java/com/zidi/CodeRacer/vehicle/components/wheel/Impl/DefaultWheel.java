package com.zidi.CodeRacer.vehicle.components.wheel.Impl;

import com.zidi.CodeRacer.vehicle.components.Part;
import com.zidi.CodeRacer.vehicle.components.wheel.Wheel;
import com.zidi.CodeRacer.vehicle.components.wheel.WheelSpec;
import com.zidi.CodeRacer.vehicle.components.wheel.WheelState;

/**
 * DefaultWheel
 * 抽象轮子基类：封装通用的控制输入、状态维护与简化受力模型。
 * 具体型号（如 WoodenWheel/RacingWheel）只需提供 WheelSpec 即可。
 */
public abstract class DefaultWheel extends Part implements Wheel {

    // ---- 固定规格 ----
    protected final WheelSpec spec;

    // ---- 控制输入（由上层写入） ----
    protected float targetSteerDeg = 0f; // 目标转角
    protected float driveTorqueNm  = 0f; // + 驱动 / - 发动机制动
    protected float brakeTorqueNm  = 0f; // 机械刹车（≥0）

    // ---- 物理步输入缓存（preStep 提供） ----
    protected float dt;
    protected float normalLoadN;      // Fz
    protected float groundMu;         // 地面 μ（已由上层综合干湿/材质）
    protected float wheelOmega;       // 轮角速度 rad/s
    protected float wheelVx;          // 车轮局部前向速度 m/s
    protected float wheelVy;          // 车轮局部侧向速度 m/s

    // ---- 输出受力（step 计算） ----
    protected float Fx;               // 纵向力 N
    protected float Fy;               // 侧向力 N
    protected float Mz;               // 自对准力矩（此处简化为 0）

    // ---- 状态（提供给外部只读） ----
    protected float steerDeg;         // 当前实际转角
    protected float slipRatio;        // 纵向滑移 κ
    protected float wear01;           // 磨损 0..1
    protected float tempC = 20f;      // 温度 ℃（初始常温）

    // ---- 可调参数（响应/模型简化） ----
    protected float steerSpeedDegPerSec = 720f; // 转向响应速率
    protected float longStiffBx = 8f,  longShapeCx = 1.3f; // 纵向 “tanh” 参数
    protected float latStiffBy  = 6f,  latShapeCy  = 1.2f; // 侧向 “tanh” 参数
    protected float wearHeatK   = 1e-7f; // 磨损增温系数
    protected float coolRate    = 0.05f; // 冷却速率（℃/s）
    protected float eps         = 1e-4f;

    protected DefaultWheel(String partID, String partName, String description,
                           int mass, int cost, WheelSpec spec) {
        super(partID, partName, description, mass, cost);
        this.spec = spec;
    }

    // ================= Wheel 接口实现 =================

    @Override public WheelSpec spec() { return spec; }

    @Override
    public WheelState state() {
        final float s = steerDeg, k = slipRatio, w = wear01, t = tempC;
        return new WheelState() {
            @Override public float steerDeg()  { return s; }
            @Override public float slipRatio() { return k; }
            @Override public float wear01()    { return w; }
            @Override public float tempC()     { return t; }
        };
    }

    @Override
    public void setTargetSteerDeg(float targetDeg) {
        // 限幅到规格允许的最大转角
        float max = Math.max(0f, spec.maxSteerDeg());
        if (targetDeg >  max) targetDeg =  max;
        if (targetDeg < -max) targetDeg = -max;
        this.targetSteerDeg = targetDeg;
    }

    @Override
    public void setDriveTorque(float Nm) {
        this.driveTorqueNm = spec.driven() ? Nm : 0f;
    }

    @Override
    public void setBrakeTorque(float Nm) {
        this.brakeTorqueNm = spec.braked() ? Math.max(0f, Nm) : 0f;
    }

    @Override
    public void preStep(float dt, float normalLoadN, float groundMu,
                        float wheelAngularVel, float wheelForwardSpeed, float wheelLateralSpeed) {
        this.dt           = Math.max(0f, dt);
        this.normalLoadN  = Math.max(0f, normalLoadN);
        this.groundMu     = Math.max(0f, groundMu);
        this.wheelOmega   = wheelAngularVel;
        this.wheelVx      = wheelForwardSpeed;
        this.wheelVy      = wheelLateralSpeed;

        // 一阶限速转向追踪
        float maxDelta = steerSpeedDegPerSec * this.dt;
        float d = clamp(targetSteerDeg - steerDeg, -maxDelta, maxDelta);
        steerDeg += d;
    }

    @Override
    public void step(float dtIgnored) {
        // ---- 1) 计算滑移 ----
        float R = Math.max(spec.radius(), eps);
        float wheelLinear = wheelOmega * R; // 轮面切向线速
        float vxAbs = Math.max(Math.abs(wheelVx), eps);

        // 纵向滑移 κ ≈ (轮面线速 - 前向速度) / |前向速度|
        slipRatio = (wheelLinear - wheelVx) / vxAbs;

        // ---- 2) 摩擦极限 ----
        float Fz   = normalLoadN;
        float mu   = effectiveMu();         // μ 依据地面传入，可叠加磨损/温度影响
        float Fmax = mu * Fz;               // 摩擦极限

        // ---- 3) 纵/侧向力（简化 tanh 曲线）----
        float FxLong = Fmax * tanh(longStiffBx * longShapeCx * slipRatio);

        // 简化侧向：依据侧向速度产生抗滑力（用 “等效侧偏” 近似）
        float alphaRad = (float) Math.atan2(-wheelVy, vxAbs); // 负号：Vy>0 产生向负方向的侧向力
        float FyLat    = -Fmax * tanh(latStiffBy * latShapeCy * alphaRad);

        // 滚阻（与行驶方向反向）
        float rollSign = (wheelVx >= 0f ? 1f : -1f);
        float Froll    = spec.cRolling() * Fz * rollSign;
        FxLong -= Froll;

        // ---- 4) 扭矩→力（τ/R），受摩擦极限约束）----
        float torqueFx = (driveTorqueNm - brakeTorqueNm) / R;
        FxLong = clamp(FxLong + torqueFx, -Fmax, Fmax);

        // ---- 5) 输出/缓存 ----
        this.Fx = FxLong;
        this.Fy = FyLat;
        this.Mz = 0f; // 简化为 0；需要时可做自对准力矩模型

        // ---- 6) 磨损/温度（简化功耗积分）----
        float work = (Math.abs(Fx) * Math.abs(wheelVx) + Math.abs(Fy) * Math.abs(wheelVy)) * dt;
        wear01 = clamp(wear01 + wearHeatK * work, 0f, 1f);
        tempC  = clamp(tempC  + wearHeatK * 8f * work - coolRate * dt, -20f, 200f);
    }

    @Override public float getFx() { return Fx; }
    @Override public float getFy() { return Fy; }
    @Override public float getMz() { return Mz; }

    // ================= 工具方法 =================
    protected float effectiveMu() {
        // 基本用地面 μ；可按温度/磨损稍作折减
        float muBase = groundMu;
        float wearLoss = 0.3f * wear01;        // 磨损降低抓地
        float tempLoss = (tempC > 120f) ? 0.15f : 0f; // 过热降 μ
        float mu = muBase * (1f - wearLoss - tempLoss);
        return clamp(mu, 0f, 5f);
    }

    protected static float tanh(float x) {
        // 数值稳定的简单 tanh
        if (x > 10f) return 1f;
        if (x < -10f) return -1f;
        double ex = Math.exp(2.0 * x);
        return (float) ((ex - 1.0) / (ex + 1.0));
    }

    protected static float clamp(float v, float lo, float hi) {
        return (v < lo) ? lo : (v > hi ? hi : v);
    }
}
