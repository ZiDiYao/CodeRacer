package com.zidi.CodeRacer.world.coordinate;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;

/**
 * 世界坐标系统 (右手系)
 * 这是一个单例类：全局只存在一份。
 *
 * 作用：
 *   - 定义世界原点、朝向、比例
 *   - 提供常用数学函数
 */
public final class WorldCoordinateSystem {

    /** ------------------ 单例实现 ------------------ */
    private static final WorldCoordinateSystem INSTANCE = new WorldCoordinateSystem();

    /** 私有构造函数，外部不能 new */
    private WorldCoordinateSystem() {}

    /** 获取唯一实例 */
    public static WorldCoordinateSystem getInstance() {
        return INSTANCE;
    }

    /** ------------------ 世界定义 ------------------ */
    public static final Vector2 ORIGIN = new Vector2(0, 0);
    public static final float WORLD_SCALE = 1.0f; // 1 单位 = 1 米

    /** 世界朝向 0 弧度 = X 轴正方向 */

    public float headingRad() {
        return 0f;
    }


    public Vector2 origin() {
        return ORIGIN;
    }

    /** 世界系的 toWorld 和 toLocal 恒等（自身） */

    public Vector2 toWorld(Vector2 local) {
        return new Vector2(local);
    }


    public Vector2 toLocal(Vector2 world) {
        return new Vector2(world);
    }

    /** ------------------ 工具函数 ------------------ */

    /** 将角度(弧度)wrap到[-π, π] */
    public static float wrapAngleRad(float rad) {
        rad = (rad + MathUtils.PI) % MathUtils.PI2;
        if (rad < 0) rad += MathUtils.PI2;
        return rad - MathUtils.PI;
    }

    /** 世界坐标平移 */
    public static Vector2 translate(Vector2 pos, float distance, float rad) {
        pos.x += distance * MathUtils.cos(rad);
        pos.y += distance * MathUtils.sin(rad);
        return pos;
    }

    /** 计算从 A 指向 B 的方向角（弧度） */
    public static float headingFromTo(Vector2 a, Vector2 b) {
        return MathUtils.atan2(b.y - a.y, b.x - a.x);
    }

    /** 计算距离 */
    public static float distance(Vector2 a, Vector2 b) {
        return a.dst(b);
    }
}
