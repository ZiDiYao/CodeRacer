package com.zidi.CodeRacer.vehicle.commands.Impl;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.zidi.CodeRacer.vehicle.commands.VehicleCommand;
import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;

public class LaneChangeCommandImpl implements VehicleCommand {
    private final boolean left;
    private final float laneWidth;       // 车道宽度（世界单位）
    private final float length;          // 变道前进距离
    private final float durationSeconds; // 持续时间（控制快慢）

    private Vector2 p0, p1, p2, p3; // 曲线控制点
    private float t = 0f;

    public LaneChangeCommandImpl(boolean left, float laneWidth, float length, float durationSeconds) {
        this.left = left;
        this.laneWidth = laneWidth;
        this.length = length;
        this.durationSeconds = durationSeconds;
    }

    @Override
    public void onStart(VehicleContext ctx) {
        Vector2 start = new Vector2(ctx.getX(), ctx.getY());
        float heading = ctx.getHeading();
        Vector2 dir = new Vector2(MathUtils.cos(heading), MathUtils.sin(heading));
        Vector2 n = new Vector2(-dir.y, dir.x); // 左法向
        if (!left) n.scl(-1f); // 如果右变道则取反

        // 生成 3 次贝塞尔控制点（S 型曲线）
        p0 = start;
        p3 = new Vector2(start).mulAdd(dir, length).mulAdd(n, laneWidth);
        p1 = new Vector2(start).mulAdd(dir, length * 0.33f);
        p2 = new Vector2(p3).mulAdd(dir, -length * 0.33f);
        t = 0f;
    }

    @Override
    public boolean execute(float dt, VehicleContext ctx) {
        t = Math.min(1f, t + dt / durationSeconds);
        Vector2 pos = bezier(p0, p1, p2, p3, t);
        ctx.setPosition(pos.x, pos.y);

        // 方向随曲线切线变化
        Vector2 tan = bezierTangent(p0, p1, p2, p3, t);
        ctx.setHeading((float)Math.atan2(tan.y, tan.x));

        return t >= 1f;
    }

    private static Vector2 bezier(Vector2 a, Vector2 b, Vector2 c, Vector2 d, float t) {
        float u = 1f - t;
        float x = u*u*u*a.x + 3*u*u*t*b.x + 3*u*t*t*c.x + t*t*t*d.x;
        float y = u*u*u*a.y + 3*u*u*t*b.y + 3*u*t*t*c.y + t*t*t*d.y;
        return new Vector2(x, y);
    }

    private static Vector2 bezierTangent(Vector2 a, Vector2 b, Vector2 c, Vector2 d, float t) {
        float u = 1f - t;
        float x = 3*u*u*(b.x - a.x) + 6*u*t*(c.x - b.x) + 3*t*t*(d.x - c.x);
        float y = 3*u*u*(b.y - a.y) + 6*u*t*(c.y - b.y) + 3*t*t*(d.y - c.y);
        return new Vector2(x, y);
    }
}
