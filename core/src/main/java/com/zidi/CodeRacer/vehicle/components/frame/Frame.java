package com.zidi.CodeRacer.vehicle.components.frame;

import com.zidi.CodeRacer.Commons.Enum.InstallSite;
import com.zidi.CodeRacer.Commons.Enum.MountResult;
import com.zidi.CodeRacer.vehicle.components.Part;
import com.zidi.CodeRacer.world.coordinate.LocalCoordinateSystem;

public interface Frame {
    // 固定挂点访问器
    MountSite frontSite();
    MountSite leftSite();
    MountSite rightSite();

    // 由挂点调 Frame 做登记（只接受 FRONT/LEFT/RIGHT）
    MountResult mountPart(InstallSite site, Part part);
    MountResult unmountPart(InstallSite site);

    int getMountCount();
    int getMaxMountCount();

    Pose pose();
    LocalCoordinateSystem localCS();
}
