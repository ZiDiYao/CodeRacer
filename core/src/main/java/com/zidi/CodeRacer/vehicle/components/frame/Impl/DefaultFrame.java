package com.zidi.CodeRacer.vehicle.components.frame.Impl;

import com.zidi.CodeRacer.Commons.Enum.InstallSite;
import com.zidi.CodeRacer.Commons.Enum.MountResult;
import com.zidi.CodeRacer.vehicle.components.Damageable;
import com.zidi.CodeRacer.vehicle.components.Part;
import com.zidi.CodeRacer.vehicle.components.frame.Frame;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class DefaultFrame extends Part implements Frame, Damageable {

    protected int durability;
    protected int mountCount;
    protected final int maxMountCount;

    /** 允许的挂点集合，由子类在构造时传入；value 为已装零件（未装为 null） */
    protected final Map<InstallSite, Part> mounts = new HashMap<>();

    protected DefaultFrame(String id, String name, String desc, int mass, int cost,
                           int maxMountCount,
                           Collection<InstallSite> allowedSites) {
        super(id, name, desc, mass, cost);
        this.maxMountCount = maxMountCount;
        // 子类决定有哪些挂点，这里只建立 key，初始为 null
        for (InstallSite s : allowedSites) mounts.put(s, null);
    }

    // ---- Damageable ----
    @Override public int getDurability() { return durability; }
    @Override public boolean isBroken() { return durability <= 0; }
    @Override public void takeDamage(int amount) {
        if (amount <= 0) return;
        int before = durability;
        durability = Math.max(0, durability - amount);
        onDamaged(amount, durability);
        if (before > 0 && durability == 0) onDestroyed();
    }

    // ---- Frame ----
    @Override public Map<InstallSite, Part> getMountedParts() { return Collections.unmodifiableMap(mounts); }
    @Override public int getMountCount() { return mountCount; }
    @Override public int getMaxMountCount() { return maxMountCount; }

    @Override
    public MountResult mountPart(InstallSite site, Part part) {
        if (site == null || part == null)     return MountResult.FAILED_NULL;
        if (!mounts.containsKey(site))        return MountResult.FAILED_INVALID_SITE;
        if (mounts.get(site) != null)         return MountResult.FAILED_ALREADY_OCCUPIED;
        if (mountCount >= maxMountCount)      return MountResult.FAILED_CAPACITY_EXCEEDED;

        part.onBeforeInstall(site);
        mounts.put(site, part);
        part.setInstalled(true);
        part.onAfterInstall(site);
        mountCount++;
        return MountResult.SUCCESS;
    }

    @Override
    public MountResult unmountPart(InstallSite site) {
        if (site == null || !mounts.containsKey(site)) return MountResult.FAILED_INVALID_SITE;
        Part current = mounts.get(site);
        if (current == null) return MountResult.UNMOUNT_FAILED_NO_PART;

        current.onBeforeUninstall();
        mounts.put(site, null);           // 保留挂点，只清空
        current.setInstalled(false);
        current.onAfterUninstall();
        mountCount = Math.max(0, mountCount - 1);
        return MountResult.UNMOUNT_SUCCESS;
    }
}
