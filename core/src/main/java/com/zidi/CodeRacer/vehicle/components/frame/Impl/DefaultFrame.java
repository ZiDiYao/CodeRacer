package com.zidi.CodeRacer.vehicle.components.frame.Impl;
import com.zidi.CodeRacer.Commons.Enum.InstallSite;
import com.zidi.CodeRacer.Commons.Enum.MountResult;
import com.zidi.CodeRacer.vehicle.components.Damageable;
import com.zidi.CodeRacer.vehicle.components.Part;
import com.zidi.CodeRacer.vehicle.components.frame.Frame;
import com.zidi.CodeRacer.vehicle.components.frame.Impl.DefaultMountSite;
import com.zidi.CodeRacer.vehicle.components.frame.MountSite;
import com.zidi.CodeRacer.vehicle.components.frame.Pose;
import com.zidi.CodeRacer.world.coordinate.LocalCoordinateSystem;

import java.util.EnumMap;
import java.util.Map;

public abstract class DefaultFrame extends com.zidi.CodeRacer.vehicle.components.Part
    implements Frame, Damageable {

    protected final Pose pose = new Pose();
    private LocalCoordinateSystem localCS;

    private final Map<InstallSite, Part> mounts = new EnumMap<>(InstallSite.class);
    private final int maxDurability;
    private int durability;
    private int mountCount;

    // 固定挂点对象
    protected final MountSite frontSite;
    protected final MountSite leftSite;
    protected final MountSite rightSite;

    protected DefaultFrame(String id, String name, String desc, int mass, int cost,
                           int maxDurability,
                           float frontX, float frontY, float frontAngle,
                           float leftX,  float leftY,  float leftAngle,
                           float rightX, float rightY, float rightAngle) {
        super(id, name, desc, mass, cost);
        this.maxDurability = maxDurability;
        this.durability = maxDurability;

        // 初始化固定键
        mounts.put(InstallSite.FRONT, null);
        mounts.put(InstallSite.LEFT,  null);
        mounts.put(InstallSite.RIGHT, null);

        // 构造固定挂点（位置+相对角）
        this.frontSite = new DefaultMountSite(this, InstallSite.FRONT, frontX, frontY, frontAngle);
        this.leftSite  = new DefaultMountSite(this, InstallSite.LEFT,  leftX,  leftY,  leftAngle);
        this.rightSite = new DefaultMountSite(this, InstallSite.RIGHT, rightX, rightY, rightAngle);
    }

    // Frame 接口
    @Override public MountSite frontSite() { return frontSite; }
    @Override public MountSite leftSite()  { return leftSite; }
    @Override public MountSite rightSite() { return rightSite; }

    @Override
    public MountResult mountPart(InstallSite site, Part part) {
        if (site == null || part == null) return MountResult.FAILED_NULL;
        if (!mounts.containsKey(site)) return MountResult.FAILED_INVALID_SITE;
        if (mounts.get(site) != null) return MountResult.FAILED_ALREADY_OCCUPIED;
        if (mountCount >= getMaxMountCount()) return MountResult.FAILED_CAPACITY_EXCEEDED;

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
        Part cur = mounts.get(site);
        if (cur == null) return MountResult.UNMOUNT_FAILED_NO_PART;

        cur.onBeforeUninstall();
        mounts.put(site, null);
        cur.setInstalled(false);
        cur.onAfterUninstall();
        mountCount = Math.max(0, mountCount - 1);
        return MountResult.UNMOUNT_SUCCESS;
    }

    @Override public int getMountCount()     { return mountCount; }
    @Override public int getMaxMountCount()  { return 3; }  // 固定三个挂点
    @Override public Pose pose()             { return pose; }
    @Override public LocalCoordinateSystem localCS() {
        if (localCS == null) localCS = new LocalCoordinateSystem(pose);
        return localCS;
    }

    // Damageable
    @Override public int getDurability()     { return durability; }
    @Override public int getMaxDurability()  { return maxDurability; }
    @Override public boolean isBroken()      { return durability <= 0; }
    @Override public void takeDamage(int amount) {
        if (amount <= 0) return;
        int before = durability;
        durability = Math.max(0, durability - amount);
        onDamaged(amount, durability);
        if (before > 0 && durability == 0) onDestroyed();
    }
}
