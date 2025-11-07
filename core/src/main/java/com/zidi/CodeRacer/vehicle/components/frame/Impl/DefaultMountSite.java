package com.zidi.CodeRacer.vehicle.components.frame.Impl;
import com.zidi.CodeRacer.Commons.Enum.InstallSite;
import com.zidi.CodeRacer.Commons.Enum.MountResult;
import com.zidi.CodeRacer.vehicle.components.Part;
import com.zidi.CodeRacer.vehicle.components.frame.BindableToFrame;
import com.zidi.CodeRacer.vehicle.components.frame.Frame;
import com.zidi.CodeRacer.vehicle.components.frame.MountSite;

public class DefaultMountSite implements MountSite {
    private final Frame frame;
    private final InstallSite site;
    private final float localX, localY, relAngleRad;
    private Part mounted;

    public DefaultMountSite(Frame frame, InstallSite site, float localX, float localY, float relAngleRad) {
        this.frame = frame;
        this.site = site;
        this.localX = localX;
        this.localY = localY;
        this.relAngleRad = relAngleRad;
    }

    @Override public Frame getFrame() { return frame; }
    @Override public InstallSite getSite() { return site; }
    @Override public boolean isEmpty() { return mounted == null; }
    @Override public Part getMountedPart() { return mounted; }
    @Override public float getLocalX() { return localX; }
    @Override public float getLocalY() { return localY; }
    @Override public float getRelAngleRad() { return relAngleRad; }

    @Override
    public MountResult add(Part part) {
        if (part == null) return MountResult.FAILED_NULL;
        if (mounted != null) return MountResult.FAILED_ALREADY_OCCUPIED;

        // 交给 Frame 做容量/登记校验（只允许固定集合）
        MountResult res = frame.mountPart(site, part);
        if (res != MountResult.SUCCESS) return res;

        mounted = part;
        if (part instanceof BindableToFrame bindable) {
            bindable.bindTo(frame.localCS(), localX, localY, relAngleRad);
        }
        return MountResult.SUCCESS;
    }

    @Override
    public MountResult remove() {
        if (mounted == null) return MountResult.UNMOUNT_FAILED_NO_PART;
        if (mounted instanceof BindableToFrame bindable) bindable.unbindFromFrame();
        MountResult res = frame.unmountPart(site);
        if (res == MountResult.UNMOUNT_SUCCESS) mounted = null;
        return res;
    }
}
