package com.zidi.CodeRacer.vehicle.components.carBrain;

import com.badlogic.gdx.Gdx;

/** 粘性比例基线策略：转弯后保存 dL/(dL+dR) 为基线；仅在稳定偏离时才触发新弯。 */
public class StickyTurnPolicy {

    public enum Decision { STRAIGHT, TURN_LEFT, TURN_RIGHT }

    // —— 前向阈值（单位：世界单位 / tiles）——
    private final float FRONT_HARD;    // 非常近：必须立刻拐
    private final float FRONT_SAFE;    // 安全：>=此值时按“基线粘性”规则直走/拐

    // —— 比例迟滞 ——
    private final float BAND_LOW;      // 低阈值：|ratio - baseline| <= 低阈 → 视为“没变”
    private final float BAND_HIGH;     // 高阈值：超过才可能触发拐弯
    private final int   K_STABLE;      // 需要连续 K 帧“同一侧偏离”来确认

    // 运行时状态
    private float baseline = Float.NaN;  // 上一次转弯结束后锁定的比例
    private float lastRatio = Float.NaN;
    private int   driftCount = 0;        // 同向偏离计数（防抖）
    private int   driftSign  = 0;        // -1: 比 baseline 小（左趋近），+1: 大（右趋近）

    public StickyTurnPolicy() {
        this(/*FRONT_HARD*/ 1.0f,
            /*FRONT_SAFE*/ 0.01f,
            /*BAND_LOW*/   0.02f,
            /*BAND_HIGH*/  0.02f,
            /*K_STABLE*/   4);
    }

    public StickyTurnPolicy(float FRONT_HARD, float FRONT_SAFE,
                            float BAND_LOW, float BAND_HIGH, int K_STABLE) {
        this.FRONT_HARD = FRONT_HARD;
        this.FRONT_SAFE = FRONT_SAFE;
        this.BAND_LOW   = BAND_LOW;
        this.BAND_HIGH  = BAND_HIGH;
        this.K_STABLE   = K_STABLE;
    }

    /** 在“确认完成一次转弯”后立刻调用，刷新新的直线基线。 */
    public void onTurnCommitted(float dL, float dR) {
        float ratio = ratio(dL, dR);
        if (!Float.isNaN(ratio)) {
            baseline   = ratio;
            lastRatio  = ratio;
            driftCount = 0;
            driftSign  = 0;
            Gdx.app.debug("Sticky", String.format("Baseline set = %.3f", baseline));
        }
    }

    /** 首帧/冷启动时，如果 baseline 还没初始化，就先锁一次。 */
    public void maybeInitBaseline(float dL, float dR) {
        if (Float.isNaN(baseline)) onTurnCommitted(dL, dR);
    }

    /** 返回：在本帧应当直走/左转/右转。 */
    public Decision decide(float dF, float dL, float dR) {
        maybeInitBaseline(dL, dR);

        // 1) 前方非常近：立刻往空的一侧拐
        if (dF <= FRONT_HARD) {
            return (dL >= dR) ? Decision.TURN_LEFT : Decision.TURN_RIGHT;
        }

        float r = ratio(dL, dR);
        if (Float.isNaN(r) || Float.isNaN(baseline)) {
            return Decision.STRAIGHT;
        }

        // 2) 前方足够安全：按“粘性基线”来看左右趋势
        if (dF >= FRONT_SAFE) {
            float diff = r - baseline;
            float ad   = Math.abs(diff);

            // 在低阈值内 → 认为没有变化，清计数
            if (ad <= BAND_LOW) {
                driftCount = 0;
                driftSign  = 0;
                lastRatio  = r;
                return Decision.STRAIGHT;
            }

            // 介于低/高阈之间 → 观察期
            int sign = (diff < 0f) ? -1 : +1;
            if (driftSign == sign) driftCount++; else { driftSign = sign; driftCount = 1; }
            lastRatio = r;

            // 超过高阈且稳定漂移 K 帧 → 触发拐弯
            if (ad >= BAND_HIGH && driftCount >= K_STABLE) {
                return (sign < 0) ? Decision.TURN_LEFT : Decision.TURN_RIGHT;
            }

            return Decision.STRAIGHT;
        }

        // 3) 前方介于 HARD/SAFE：半风险区，先看趋势，必要时也可转
        //   （这里保持保守，只有稳定偏离到高阈才转；否则直走）
        float rdiff = r - baseline;
        int sign = (rdiff < 0f) ? -1 : +1;
        if (Math.abs(rdiff) >= BAND_HIGH) {
            if (driftSign == sign) driftCount++; else { driftSign = sign; driftCount = 1; }
            if (driftCount >= K_STABLE) {
                return (sign < 0) ? Decision.TURN_LEFT : Decision.TURN_RIGHT;
            }
        } else {
            driftCount = 0; driftSign = 0;
        }
        lastRatio = r;
        return Decision.STRAIGHT;
    }

    private static float ratio(float dL, float dR) {
        float sum = dL + dR;
        if (sum <= 1e-6f) return Float.NaN;
        return dL / sum; // 0..1
    }
}
