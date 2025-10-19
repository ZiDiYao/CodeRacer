package com.zidi.CodeRacer.vehicle.components.frame.Impl;

import com.zidi.CodeRacer.Commons.Enum.InstallSite;

import java.util.List;

public class WoodenFrame extends DefaultFrame {

    public WoodenFrame(String id, String name, String desc, int mass, int cost) {
        super(id, name, desc, mass, cost,
            2,                              // maxDurability
            1,                              // maxMountCount
            List.of(InstallSite.FRONT));    // 允许挂点
        reset();
    }

    @Override public void onClick() {}

    @Override
    public void reset() {
        durability = getMaxDurability();
        mountCount = 0;
        mounts.replaceAll((k, v) -> null);
    }
}
