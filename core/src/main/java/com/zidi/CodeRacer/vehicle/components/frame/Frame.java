package com.zidi.CodeRacer.vehicle.components.frame;

import com.zidi.CodeRacer.Commons.Enum.InstallSite;
import com.zidi.CodeRacer.Commons.Enum.MountResult;
import com.zidi.CodeRacer.vehicle.components.Part;
import java.util.Map;

/**
 * Interface for the vehicle frame, responsible for mounting and removing parts.
 */
public interface Frame {
    Map<InstallSite, Part> getMountedParts();

    MountResult mountPart(InstallSite site, Part part);
    MountResult unmountPart(InstallSite site);

    /** 当前已装数量 */
    int getMountCount();

    /** 此 Frame 所允许的最大挂载数（由具体实现决定） */
    int getMaxMountCount();
}
