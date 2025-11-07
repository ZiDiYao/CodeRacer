package com.zidi.CodeRacer.vehicle.components.carBrain;

import static java.lang.Math.*;

public class SimpleTurnPolicy {

    public enum Decision { STRAIGHT, TURN_LEFT, TURN_RIGHT }

    // === 可调参数（tile 为单位）===
    private static final float DEG = (float)toRadians(35);  // 侧前斜角（和你的挂点一致）
    private static final float COS = (float)cos(DEG);

    // 直行“护城河”：只要前方够远，就坚决直走
    private static final float STRAIGHT_MIN = 1.2f;   // 前方 >= 1.2 tiles 必直走

    // 侧向提前判别：Δ 的阈值/趋势
    private static final float DELTA_THRESH  = 0.15f; // Δ 绝对值超过即偏向某侧（越大越迟钝）
    private static final float TREND_THRESH  = 0.08f; // Δ 的 EMA 斜率阈值（提前量）
    private static final float HYST          = 0.10f; // 迟滞，避免边缘抖动

    // EMA 平滑
    private static final float EMA_ALPHA     = 0.35f; // 0.2~0.5 都可
    // 弯中/刚转完的冷却
    private static final float LOCK_TIME_S   = 0.50f; // 触发后锁定 0.7s（配合你转 90° 的耗时）

    private float emaDelta = 0f;   // Δ 的 EMA
    private float lastEma  = 0f;   // 上帧 EMA（估趋势）
    private float lockLeft = 0f;   // 剩余锁定时间（秒）

    // 给 main 每帧传 dt 进来（如果你不想改签名，也可在 main 内部扣减锁定）
    public Decision decide(float dF, float dL, float dR, float dt) {
        // 冷却
        if (lockLeft > 0f) lockLeft = max(0f, lockLeft - dt);

        // 投影修正：把侧斜射线映射为横向有效距离
        float dLe = dL * COS;
        float dRe = dR * COS;

        // 1) 前向优先：只要前方安全，就直走（强约束）
        if (dF >= STRAIGHT_MIN && lockLeft <= 0f) {
            updateEma(dRe - dLe); // 仍然更新 EMA，用于“快到弯了”的提前判别
            return Decision.STRAIGHT;
        }

        // 2) 用 Δ 的 EMA + 趋势做提前量
        float delta = dRe - dLe;     // >0 表示右更空，倾向右转；<0 则左更空
        float prev  = emaDelta;
        updateEma(delta);
        float slope = emaDelta - prev; // 粗略趋势

        // 2.1 若未锁定：满足趋势或幅度，就触发
        if (lockLeft <= 0f) {
            if (emaDelta >  (DELTA_THRESH + HYST) || slope > TREND_THRESH) {
                lockLeft = LOCK_TIME_S;
                return Decision.TURN_RIGHT;
            }
            if (emaDelta < -(DELTA_THRESH + HYST) || slope < -TREND_THRESH) {
                lockLeft = LOCK_TIME_S;
                return Decision.TURN_LEFT;
            }
        }

        // 3) 走到这里：要么正在锁定（不改向），要么还不够格 → 直走/按原向
        return Decision.STRAIGHT;
    }

    // 兼容你原来的签名：如果 main 还在调用 decide(dF,dL,dR)，可以默认 dt=1/60f
    public Decision decide(float dF, float dL, float dR) {
        return decide(dF, dL, dR, 1/60f);
    }

    public void onTurnStarted() { lockLeft = LOCK_TIME_S; }
    public void onTurnFinished() { lockLeft = 0.15f; } // 小缓冲，防止立刻反判（可要可不要）

    private void updateEma(float x){
        if (Float.isNaN(emaDelta)) emaDelta = 0f;
        emaDelta = EMA_ALPHA * x + (1f - EMA_ALPHA) * emaDelta;
    }


    public void onTurnCommitted(float dL, float dR) { /* no-op */ }
}
