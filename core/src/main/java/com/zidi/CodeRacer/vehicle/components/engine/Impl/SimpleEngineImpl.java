package com.zidi.CodeRacer.vehicle.components.engine.Impl;

import com.zidi.CodeRacer.vehicle.components.engine.EngineSpec;

/**
 * SimpleEngineImpl
 * 一个基础的汽油机模型：
 * - 线性上升至峰值扭矩，再逐步下降至红线
 * - 达到红线自动断油
 */
public class SimpleEngineImpl extends DefaultEngine {

    public SimpleEngineImpl(String id, String name, String desc, int mass, int cost, EngineSpec spec) {
        super(id, name, desc, mass, cost, spec);
    }

    @Override
    protected float torqueAtRpm(float rpm) {
        // 取出基础规格参数
        float idle = spec.idleRpm();
        float peakR = spec.peakTorqueRpm();
        float red  = spec.redlineRpm();
        float peakT = spec.peakTorqueNm();

        // -----------------------------
        // 简化的三段式扭矩曲线：
        // 怠速 → 峰值 → 红线
        // -----------------------------
        if (rpm <= idle) {
            // 怠速以下：扭矩极小（防熄火）
            return 0.5f * peakT * (rpm / Math.max(1f, idle));
        }
        else if (rpm <= peakR) {
            // 怠速~峰值：扭矩上升
            float t = (rpm - idle) / Math.max(1f, peakR - idle);
            return lerp(0.6f * peakT, peakT, t);
        }
        else if (rpm <= red) {
            // 峰值~红线：扭矩下降
            float t = (rpm - peakR) / Math.max(1f, red - peakR);
            return lerp(peakT, 0.4f * peakT, t);
        }
        else {
            // 超红线：强制衰减
            return 0f;
        }
    }

    @Override
    protected boolean redlineCutoff() {
        // 达到红线时自动断油
        return true;
    }

    // ===== 工具函数 =====
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp(t, 0f, 1f);
    }

}
