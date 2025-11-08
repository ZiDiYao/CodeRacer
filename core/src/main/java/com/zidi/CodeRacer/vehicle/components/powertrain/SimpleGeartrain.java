package com.zidi.CodeRacer.vehicle.components.powertrain;

import com.zidi.CodeRacer.vehicle.components.frame.Pose;
import com.zidi.CodeRacer.vehicle.components.wheel.Wheel;
import com.zidi.CodeRacer.vehicle.components.wheel.WheelSpec;

import java.util.ArrayList;
import java.util.List;

/** 最小可用的直驱传动：发动机扭矩按驱动轮平均分配，负载角速度用 v/R 估算 */
public class SimpleGeartrain {
    private final List<Wheel> drivenWheels = new ArrayList<>();
    /** 估算负载角速度（rad/s）：用车辆速度与平均半径 */
    public float estimateLoadOmega(Pose pose){
        if (drivenWheels.isEmpty()) return 0f;
        float v = Math.max(0f, pose.getSpeed()); // m/s
        float rAvg = 0f;
        for (Wheel w : drivenWheels) rAvg += Math.max(1e-4f, w.spec().radius());
        rAvg /= drivenWheels.size();
        return v / Math.max(1e-4f, rAvg);
    }
    /** 把发动机扭矩平均分给所有驱动轮（单位 Nm） */
    public void distributeTorque(float engineTorque){
        if (drivenWheels.isEmpty()) return;
        float each = engineTorque / drivenWheels.size();
        for (Wheel w : drivenWheels){
            w.setDriveTorque(each);
        }
    }
    /** 注册可能的驱动轮（会自动过滤 spec.driven()==true 的） */
    public void setCandidateWheels(List<Wheel> wheels){
        drivenWheels.clear();
        for (Wheel w : wheels){
            WheelSpec s = w.spec();
            if (s != null && s.driven()) drivenWheels.add(w);
        }
    }
    public int drivenCount(){ return drivenWheels.size(); }
}
