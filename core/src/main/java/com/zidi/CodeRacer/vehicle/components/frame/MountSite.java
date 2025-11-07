package com.zidi.CodeRacer.vehicle.components.frame;

import com.zidi.CodeRacer.Commons.Enum.InstallSite;
import com.zidi.CodeRacer.Commons.Enum.MountResult;
import com.zidi.CodeRacer.vehicle.components.Part;

/**
 * MountSite 接口：定义挂点的通用行为。
 * 每个 Frame 可以包含多个挂点（传感器、轮胎、引擎等）。
 */
public interface MountSite {

    /** 返回该挂点所属的车架 */
    Frame getFrame();

    /** 返回该挂点类型（FRONT_LEFT、FRONT_RIGHT 等） */
    InstallSite getSite();

    /** 当前挂载的部件（可能为 null） */
    Part getMountedPart();

    /** 安装一个部件 */
    MountResult add(Part part);

    /** 卸载当前部件 */
    MountResult remove();

    /** 当前是否空闲 */
    boolean isEmpty();

    /** 局部坐标（相对车架） */
    float getLocalX();
    float getLocalY();

    /** 相对车头角度（弧度） */
    float getRelAngleRad();
}
