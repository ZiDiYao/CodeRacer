package com.zidi.CodeRacer.world.coordinate;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.zidi.CodeRacer.vehicle.components.frame.Pose;

public class LocalCoordinateSystem {
    private final Pose pose; // 绑定实体位姿

    public LocalCoordinateSystem(Pose pose) {
        this.pose = pose;
    }

    /** 局部 -> 世界 */
    public Vector2 toWorld(Vector2 local) {
        float c = MathUtils.cos(pose.getHeadingRad());
        float s = MathUtils.sin(pose.getHeadingRad());
        Vector2 origin = pose.getPos();
        return new Vector2(
            origin.x + local.x * c - local.y * s,
            origin.y + local.x * s + local.y * c
        );
    }

    /** 世界 -> 局部 */
    public Vector2 toLocal(Vector2 world) {
        Vector2 origin = pose.getPos();
        float dx = world.x - origin.x;
        float dy = world.y - origin.y;
        float c = MathUtils.cos(-pose.getHeadingRad());
        float s = MathUtils.sin(-pose.getHeadingRad());
        return new Vector2(
            dx * c - dy * s,
            dx * s + dy * c
        );
    }
}
