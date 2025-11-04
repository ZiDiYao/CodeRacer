package com.zidi.CodeRacer.vehicle.components.sensor;

public final class SensorReading {
    private final boolean alert;   // 是否触发预警/碰撞
    private final float distance;  // 前方最近障碍物距离
    private final float angle;     // 相对角度（可选）
    private final String label;    // 标签/原因

    public SensorReading(boolean alert, float distance, float angle, String label) {
        this.alert = alert;
        this.distance = distance;
        this.angle = angle;
        this.label = label;
    }

    // —— 兼容多种命名 ——
    public boolean triggered() { return alert; }   // 示例代码用到的
    public boolean isAlert()   { return alert; }

    public float distance()    { return distance; } // 示例代码用到的
    public float getDistance() { return distance; }

    public float angle()       { return angle; }
    public float getAngle()    { return angle; }

    public String label()      { return label; }
    public String getLabel()   { return label; }
}

