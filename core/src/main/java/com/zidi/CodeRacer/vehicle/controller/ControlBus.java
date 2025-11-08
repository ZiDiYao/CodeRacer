package com.zidi.CodeRacer.vehicle.controller;

/** 大脑给车辆的统一控制总线（单位约定见注释） */
public interface ControlBus {
    /** 0..1 油门 */
    float throttle01();
    /** 0..1 刹车 */
    float brake01();
    /** 方向：角度（deg），左负右正，或你喜欢就约定 -1..1 也行（下方示例用 deg） */
    float steerDeg();
}
