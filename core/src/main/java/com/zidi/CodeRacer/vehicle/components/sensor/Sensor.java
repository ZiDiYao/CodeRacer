package com.zidi.CodeRacer.vehicle.components.sensor;

public interface Sensor {

    int getRange();             // 探测距离（tile）
    float getSampleRateHz();    // 采样频率
    int getFovDegrees();        // 视场角

    default int getPowerCost() { return 0; }

    /**
     * 启动检测，返回结构化结果
     * 例如：
     *   - 对障碍物传感器 => distance, blocked
     *   - 对车道传感器 => 偏移角、合法性
     */
    SensorReading detect();

}
