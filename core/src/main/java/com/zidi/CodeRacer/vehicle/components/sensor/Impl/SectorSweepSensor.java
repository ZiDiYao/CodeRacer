package com.zidi.CodeRacer.vehicle.components.sensor.Impl;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.zidi.CodeRacer.Commons.utils.TiledWorldUtils;
import com.zidi.CodeRacer.vehicle.components.frame.Pose;
import com.zidi.CodeRacer.vehicle.components.sensor.SensorReading;

public class SectorSweepSensor extends DefaultSensor {
    // 扇形参数
    private final float fovHalfRad;   // 扇形半宽（弧度）
    private final float rMax;         // 最大量程（世界单位）
    private final int   nTotal;       // 总射线数（扫完整个扇区）
    private final int   kFrames;      // 扫完整个扇区的帧数
    private final float stepLen;      // world.distanceToCollisionForward 的步长
    private final float emaAlpha;     // EMA 融合系数（0..1）

    // 告警/输出
    private float warnDist = 5f;      // 预警距离（世界单位，可 set）
    private int   phase = 0;          // 0..kFrames-1
    private float dFiltered = Float.POSITIVE_INFINITY;
    private float lastBatchMin = Float.POSITIVE_INFINITY;

    // 可视化
    private final Array<Vector2> lastRayEnds = new Array<>(false, 64);

    public SectorSweepSensor(
        String id, String name, String desc, int mass, int cost,
        TiledWorldUtils world, Pose pose,
        float fovHalfRad, float rMax,
        int nTotal, int kFrames,
        float stepLen, float emaAlpha
    ) {
        super(id, name, desc, mass, cost,
            /*range*/ 0, /*sampleRateHz*/ 0, /*fovDegrees*/ 0, /*powerCost*/ 0,
            world, pose);
        this.fovHalfRad = Math.max(0.01f, fovHalfRad);
        this.rMax       = Math.max(stepLen, rMax);
        this.nTotal     = Math.max(3, nTotal);
        this.kFrames    = Math.max(1, kFrames);
        this.stepLen    = Math.max(0.02f, stepLen);
        this.emaAlpha   = MathUtils.clamp(emaAlpha, 0.05f, 0.9f);
    }

    /* --- 关键：每次 detect() 做一批扫描，并返回融合后的读数 --- */
    @Override
    public SensorReading detect() {
        updateSweepOnce(); // 打本帧的那一小束

        final boolean alert = (dFiltered <= warnDist);
        // angle 用不到先给 0；type 做个简单标签
        return new SensorReading(alert, dFiltered, 0f, alert ? "Obstacle" : "Clear");
    }

    /* 分帧扫描（原来的 updateSweep 改名为内部私有） */
    private void updateSweepOnce() {
        lastRayEnds.clear();

        final float cx = pose.getX();
        final float cy = pose.getY();
        final float ch = pose.getHeadingRad();

        final int nStep   = Math.max(1, MathUtils.ceil((float) nTotal / kFrames));
        final int startIx = phase * nStep;
        final int endIx   = Math.min(nTotal, startIx + nStep);

        float batchMin = Float.POSITIVE_INFINITY;

        // i ∈ [0, nTotal-1] 映射到 [-fovHalf, +fovHalf]
        for (int i = startIx; i < endIx; i++) {
            final float t   = (nTotal == 1) ? 0f : (i / (float)(nTotal - 1)); // 0..1
            final float off = -fovHalfRad + (2f * fovHalfRad) * t;            // -half..+half
            final float a   = ch + off;

            final float d = world.distanceToCollisionForward(cx, cy, a, rMax, stepLen);
            if (d < batchMin) batchMin = d;

            // 可视化端点
            final float ex = cx + d * MathUtils.cos(a);
            final float ey = cy + d * MathUtils.sin(a);
            lastRayEnds.add(new Vector2(ex, ey));
        }

        if (!Float.isFinite(batchMin)) batchMin = rMax;
        lastBatchMin = batchMin;

        // 指数平滑，抖动更小
        dFiltered = (dFiltered == Float.POSITIVE_INFINITY)
            ? batchMin
            : MathUtils.lerp(dFiltered, batchMin, emaAlpha);

        // 推进相位
        phase = (phase + 1) % kFrames;
    }

    /* —— 可视化 / 参数 —— */
    public Array<Vector2> getLastRayEnds() { return lastRayEnds; }
    public float getDistanceFiltered()     { return dFiltered; }
    public float getLastBatchMin()         { return lastBatchMin; }
    public void  setWarnDist(float w)      { this.warnDist = Math.max(0f, w); }

    /* 覆盖这些 getter，让外部用统一接口也拿到真实值 */
    @Override public int  getRange()      { return (int)Math.ceil(rMax); }
    @Override public int  getFovDegrees() { return Math.round((float)Math.toDegrees(2f * fovHalfRad)); }

    @Override
    public void onClick() {

    }
}
