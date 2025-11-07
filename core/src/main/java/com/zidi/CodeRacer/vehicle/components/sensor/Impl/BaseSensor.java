package com.zidi.CodeRacer.vehicle.components.sensor.Impl;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.zidi.CodeRacer.Commons.utils.TiledWorldUtils;
import com.zidi.CodeRacer.vehicle.components.frame.Pose;
import com.zidi.CodeRacer.vehicle.components.sensor.SensorReading;

/**
 * 最小侵入：保持 detect() 只负责检测并返回结果，
 * 同时记录本次检测用到的射线（供渲染层可视化）。
 */
public class BaseSensor extends DefaultSensor {

    // —— 调参项（必要时在构造器里改，或提供 setter）——
    private float warnDist = 5f;     // 预警距离（tile）
    private float maxDist  = 50f;    // 光线最远探测距离（tile）
    private float step     = 0.25f;  // 采样步长（tile）

    // —— 调试可视化：只读 ——
    private final Vector2 rayStart = new Vector2();
    private final Vector2 rayEnd   = new Vector2();
    private final Array<Vector2> samples = new Array<>(false, 64); // 可选：采样点（画小圆点用）

    public BaseSensor(String id, String name, String desc, int mass, int cost,
                      TiledWorldUtils world, Pose pose) {
        super(
            id, name, desc, mass, cost,
            /*range*/        2,
            /*sampleRateHz*/ 10,
            /*fovDegrees*/   1,
            /*powerCost*/    0,
            world, pose
        );
    }

    @Override
    public void onClick() { /* 可做测试/自检 */ }

    /**
     * 目前：单射线前向探测 Collision。
     * 返回：是否触发 + 最近障碍物距离。
     * 同时：记录 rayStart/rayEnd/samples 供渲染层画线。
     */
    @Override
    public SensorReading detect() {
        // 清理上次采样点（可选）
        samples.clear();

        final float x = pose.getX();
        final float y = pose.getY();
        final float h = pose.getHeadingRad();

        float d = 0f;
        // 射线采样前向推进
        while (d <= maxDist) {
            float sx = x + d * MathUtils.cos(h);
            float sy = y + d * MathUtils.sin(h);

            // 记录采样点（若你不想画点，可以注释掉）
            samples.add(new Vector2(sx, sy));

            if (world.isCollisionAt(sx, sy)) {
                break; // 命中
            }
            d += step;
        }

        // 更新调试射线端点
        rayStart.set(x, y);
        float hitX = x + d * MathUtils.cos(h);
        float hitY = y + d * MathUtils.sin(h);
        rayEnd.set(hitX, hitY);

        boolean alert = (d <= warnDist);
        return new SensorReading(alert, d, 0f, alert ? "CollisionAhead" : "Clear");
    }

    // ====== 渲染层可用的只读 Getter（不影响逻辑层） ======
    public Vector2 getRayStart() { return rayStart; }
    public Vector2 getRayEnd()   { return rayEnd;   }
    public Array<Vector2> getSamples() { return samples; }

    // ====== （可选）参数调节 ======
    public void setWarnDist(float warnDist) { this.warnDist = Math.max(0f, warnDist); }
    public void setMaxDist(float maxDist)   { this.maxDist  = Math.max(step, maxDist); }
    public void setStep(float step)         { this.step     = Math.max(0.01f, step); }

    public Pose pose() { return this.pose; }

}
