package com.zidi.CodeRacer.vehicle.components.frame.Impl;

import com.zidi.CodeRacer.Commons.Enum.InstallSite;
import com.zidi.CodeRacer.Commons.Enum.MountResult;
import com.zidi.CodeRacer.vehicle.components.Damageable;
import com.zidi.CodeRacer.vehicle.components.Part;
import com.zidi.CodeRacer.vehicle.components.frame.Frame;
import com.zidi.CodeRacer.vehicle.components.frame.Pose;
import com.zidi.CodeRacer.world.coordinate.LocalCoordinateSystem;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class DefaultFrame extends Part implements Frame, Damageable {

    protected int durability;                 // 当前耐久度
    protected int mountCount;
    protected final int maxMountCount;
    protected final int maxDurability;        // 最大耐久（常量）

    protected final Map<InstallSite, Part> mounts = new HashMap<>();

    /** 位姿（与 LocalCS 绑定的数据源） */
    protected final Pose pose = new Pose();

    /** 车体局部坐标系（绑定 Pose；懒加载一次即可） */
    private LocalCoordinateSystem localCS;

    protected DefaultFrame(String id, String name, String desc, int mass, int cost,
                           int maxDurability,
                           int maxMountCount,
                           Collection<InstallSite> allowedSites) {
        super(id, name, desc, mass, cost);
        this.maxDurability = maxDurability;
        this.maxMountCount = maxMountCount;
        this.durability = maxDurability;         // ✅ 初始化
        for (InstallSite s : allowedSites) mounts.put(s, null);
    }

    // ---- Frame 扩展（位姿 & 坐标） ----
    @Override public Pose pose() { return pose; }

    /** 提供 LocalCS（内部共用一个实例；它读取 pose 的实时值，不需要每帧重建） */
    @Override
    public LocalCoordinateSystem localCS() {
        if (localCS == null) localCS = new LocalCoordinateSystem(pose);
        return localCS;
    }

    // ---- Damageable ----
    @Override public int getDurability() { return durability; }
    @Override public int getMaxDurability() { return maxDurability; }
    @Override public boolean isBroken() { return durability <= 0; }

    @Override
    public void takeDamage(int amount) {
        if (amount <= 0) return;
        int before = durability;
        durability = Math.max(0, durability - amount);
        onDamaged(amount, durability);
        if (before > 0 && durability == 0) onDestroyed();
    }

    // ---- 挂载/卸载 ----
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
        mounts.put(site, null);
        current.setInstalled(false);
        current.onAfterUninstall();
        mountCount = Math.max(0, mountCount - 1);
        return MountResult.UNMOUNT_SUCCESS;
    }
}
