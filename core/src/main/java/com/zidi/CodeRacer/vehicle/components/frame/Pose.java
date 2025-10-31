package com.zidi.CodeRacer.vehicle.components.frame;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import static com.zidi.CodeRacer.world.coordinate.WorldCoordinateSystem.wrapAngleRad;

/**
 * Pose 表示实体在世界坐标系下的位姿（Position + Orientation + Speed）。
 *
 * 适用于车辆、行人、传感器等具有空间位置与朝向的对象。
 * 在 CodeRacer 中，Pose 是 LocalCoordinateSystem 的核心状态数据来源。
 */
public class Pose {

    /** 世界坐标下的位置 */
    private final Vector2 pos = new Vector2();

    /** 朝向角（弧度，逆时针为正） */
    private float headingRad = 0f;

    /** 当前速度（标量，m/s） */
    private float speed = 0f;

    // 缓存用临时向量（减少 GC）
    private final Vector2 forwardCache = new Vector2();

    // =========================================
    // 构造与基础操作
    // =========================================

    public Pose() {}

    public Pose(float x, float y, float headingRad, float speed) {
        this.pos.set(x, y);
        this.headingRad = headingRad;
        this.speed = speed;
    }

    /** 设置位姿 */
    public Pose set(float x, float y, float headingRad, float speed) {
        this.pos.set(x, y);
        this.headingRad = headingRad;
        this.speed = speed;
        return this;
    }

    /** 从另一 Pose 复制 */
    public Pose set(Pose other) {
        this.pos.set(other.pos);
        this.headingRad = other.headingRad;
        this.speed = other.speed;
        return this;
    }

    // =========================================
    // 位姿更新相关
    // =========================================

    /** 沿当前朝向前进指定距离 */
    public void translate(float distance) {
        pos.x += distance * MathUtils.cos(headingRad);
        pos.y += distance * MathUtils.sin(headingRad);
    }

    /** 按速度 * 时间步长 更新位置 */
    public void step(float dt) {
        pos.x += speed * MathUtils.cos(headingRad) * dt;
        pos.y += speed * MathUtils.sin(headingRad) * dt;
    }

    /** 改变朝向（弧度），会自动 wrap 到 [-π, π] */
    public void rotate(float deltaRad) {
        headingRad = wrapAngleRad(headingRad + deltaRad);
    }

    /** 清零速度 */
    public void stop() {
        speed = 0f;
    }

    // =========================================
    // 工具方法
    // =========================================

    /** 返回当前朝向的单位方向向量（复用缓存避免创建新对象） */
    public Vector2 forward() {
        forwardCache.set(MathUtils.cos(headingRad), MathUtils.sin(headingRad));
        return forwardCache;
    }

    /** 复制一份 Pose（独立副本） */
    public Pose cpy() {
        return new Pose(pos.x, pos.y, headingRad, speed);
    }

    // =========================================
    // Getter / Setter
    // =========================================

    public Vector2 getPos() { return pos; }
    public float getX() { return pos.x; }
    public float getY() { return pos.y; }
    public float getHeadingRad() { return headingRad; }
    public float getSpeed() { return speed; }

    public void setHeadingRad(float headingRad) {
        this.headingRad = wrapAngleRad(headingRad);
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    @Override
    public String toString() {
        return String.format(
            "Pose[x=%.2f, y=%.2f, heading=%.1f°, speed=%.2f m/s]",
            pos.x, pos.y, headingRad * MathUtils.radiansToDegrees, speed
        );
    }
}
