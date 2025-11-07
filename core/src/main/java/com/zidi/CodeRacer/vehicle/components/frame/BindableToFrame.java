package com.zidi.CodeRacer.vehicle.components.frame;

import com.zidi.CodeRacer.world.coordinate.LocalCoordinateSystem;

/** 能挂在车架上的部件可实现该接口，以接收局部位姿绑定信息 */
public interface BindableToFrame {
    /** 将部件绑定到车架的局部坐标系（局部x/y + 相对朝向） */
    void bindTo(LocalCoordinateSystem localCS, float localX, float localY, float relAngleRad);

    /** 从车架解绑（卸载时调用） */
    void unbindFromFrame();
}
