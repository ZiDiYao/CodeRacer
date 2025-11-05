package com.zidi.CodeRacer.Commons.utils;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * 极简路口检测器：
 *  - 启动时从 TiledMap 读取路口矩形（像素→世界坐标 + Y 翻转）
 *  - 每帧调用 update(x,y) 返回 IntersectionHit（并缓存为当前状态）
 *  - 也提供 isAtIntersection() / justEntered() / justExited() 等布尔便捷方法
 */
public class IntersectionDetector {

    /** 内部保存的世界坐标矩形 + 可选 id */
    public static class RectRef {
        public final Rectangle rect;
        public final String id;
        public RectRef(Rectangle rect, String id) { this.rect = rect; this.id = id; }
    }

    private final List<RectRef> rects = new ArrayList<>();
    private int lastIndex = -1;                 // 上一帧所在矩形索引；-1=不在任何路口
    private IntersectionHit lastHit = IntersectionHit.NONE;

    private IntersectionDetector() {}

    /** 从 TiledMap 构造（layerName 对应你的路口图层名；unitScale=1/tileWidth；mapHeightPx=tilesH*tileHpx） */
    public static IntersectionDetector fromMap(TiledMap map, String layerName,
                                               float unitScale, int mapHeightPx) {
        IntersectionDetector det = new IntersectionDetector();
        if (map == null) return det;

        MapLayer layer = map.getLayers().get(layerName);
        if (layer == null) return det;

        for (MapObject o : layer.getObjects()) {
            if (o instanceof RectangleMapObject rmo) {
                Rectangle rp = rmo.getRectangle(); // 像素坐标（左上为原点）
                float wx = rp.x * unitScale;
                float wy = (mapHeightPx - rp.y - rp.height) * unitScale; // 翻转 Y 并减去高
                float ww = rp.width * unitScale;
                float wh = rp.height * unitScale;

                String id = null;
                Object pid = rmo.getProperties().get("id");
                if (pid != null) id = String.valueOf(pid);

                det.rects.add(new RectRef(new Rectangle(wx, wy, ww, wh), id));
            }
        }
        return det;
    }

    /**【核心】更新并返回当前命中状态；同时缓存为 lastHit，供便捷方法读取 */
    public IntersectionHit update(float x, float y) {
        int nowIndex = -1;
        RectRef hitRef = null;

        for (int i = 0; i < rects.size(); i++) {
            RectRef rr = rects.get(i);
            if (rr.rect.contains(x, y)) {
                nowIndex = i;
                hitRef = rr;
                break;
            }
        }

        boolean wasInside = (lastIndex != -1);
        boolean inside    = (nowIndex != -1);
        boolean entered   = (!wasInside && inside);
        boolean exited    = (wasInside && !inside);

        lastIndex = nowIndex;

        if (!inside && !entered && !exited) {
            lastHit = IntersectionHit.NONE;
            return lastHit;
        }

        Vector2 center = (hitRef == null) ? null
            : new Vector2(hitRef.rect.x + hitRef.rect.width * 0.5f,
            hitRef.rect.y + hitRef.rect.height * 0.5f);

        lastHit = new IntersectionHit(entered, inside, exited,
            hitRef == null ? null : hitRef.id,
            hitRef == null ? null : hitRef.rect,
            center);
        return lastHit;
    }

    // ———— 便捷布尔接口（供“玩家写 if”直接用） ————

    /** 是否正在路口内（等价于 lastHit.inside） */
    public boolean isAtIntersection() { return lastHit != null && lastHit.inside; }

    /** 本帧是否刚进入路口 */
    public boolean justEntered() { return lastHit != null && lastHit.entered; }

    /** 本帧是否刚离开路口 */
    public boolean justExited() { return lastHit != null && lastHit.exited; }

    // ———— 可选：取当前命中的一些信息（若需要的话） ————

    /** 当前命中的路口 id（可能为 null） */
    public String currentId() { return (lastHit == null) ? null : lastHit.id; }

    /** 当前命中的路口中心点（可能为 null） */
    public Vector2 currentCenter() { return (lastHit == null) ? null : lastHit.center; }

    /** 供调试或统计：返回不可变的所有路口矩形拷贝 */
    public List<RectRef> getAll() { return new ArrayList<>(rects); }
}
