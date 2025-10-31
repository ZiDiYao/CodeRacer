package com.zidi.CodeRacer.vehicle.components.frame;

import com.zidi.CodeRacer.Commons.Enum.InstallSite;
import com.zidi.CodeRacer.Commons.Enum.MountResult;
import com.zidi.CodeRacer.vehicle.components.Part;
import com.zidi.CodeRacer.world.coordinate.LocalCoordinateSystem;

import java.util.Map;

/** 车辆车架：挂载零件 + 暴露位姿与局部坐标系 */
public interface Frame {
    Map<InstallSite, Part> getMountedParts();

    MountResult mountPart(InstallSite site, Part part);
    MountResult unmountPart(InstallSite site);

    int getMountCount();
    int getMaxMountCount();

    /** 位姿（位置/朝向/速度） */
    Pose pose();

    /** 车辆局部坐标系（基于 pose） */
    LocalCoordinateSystem localCS();
}
