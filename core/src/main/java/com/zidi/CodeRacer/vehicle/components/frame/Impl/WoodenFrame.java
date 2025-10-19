package com.zidi.CodeRacer.vehicle.components.frame.Impl;

import com.zidi.CodeRacer.Commons.Enum.InstallSite;

import java.util.List;

public class WoodenFrame extends DefaultFrame {

    private static final int MAX_DUR = 2;

    public WoodenFrame(String id, String name, String desc, int mass, int cost) {
        super(id, name, desc, mass, cost,
            1,                                // maxMountCount
            List.of(InstallSite.FRONT));      // 允许的挂点：只有 FRONT
        reset();
    }

    @Override public int getMaxDurability() { return MAX_DUR; }
    @Override public void onClick() {}
    @Override public void reset() {
        durability = MAX_DUR;
        mountCount = 0;
        mounts.replaceAll((k, v) -> null);
    }
}
