// com.zidi.CodeRacer.Commons.utils.TiledWorldUtils
package com.zidi.CodeRacer.Commons.utils;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.MathUtils;
import java.util.ArrayList;
import java.util.List;

public final class TiledWorldUtils {

    private static final String LAYER_COLLISION = "Collision";

    private final float unitScale;
    private final int mapHeightPx;

    private final List<Rectangle> collisionsWorld = new ArrayList<>();

    public TiledWorldUtils(TiledMap map, float unitScale, int mapHeightPx) {
        this.unitScale   = unitScale;
        this.mapHeightPx = mapHeightPx;
        cacheCollisionRects(map);
    }

    private void cacheCollisionRects(TiledMap map) {
        collisionsWorld.clear();
        MapLayer col = map.getLayers().get(LAYER_COLLISION);
        if (col == null) return;

        for (MapObject o : col.getObjects()) {
            if (o instanceof RectangleMapObject rmo) {
                Rectangle rp = rmo.getRectangle();               // 像素（左上原点）
                float wx = rp.x * unitScale;
                float wy = (mapHeightPx - rp.y - rp.height) * unitScale; // 翻转Y并减去高
                float ww = rp.width  * unitScale;
                float wh = rp.height * unitScale;
                collisionsWorld.add(new Rectangle(wx, wy, ww, wh));
            }
        }
    }

    /** 点 (x,y) 是否在任一 Collision 矩形内 */
    public boolean isCollisionAt(float x, float y) {
        for (Rectangle r : collisionsWorld) {
            if (r.contains(x, y)) return true;
        }
        return false;
    }

    /**
     * 从 (x0,y0,heading) 沿朝向做“射线”，返回到最近 Collision 的**距离**；若没撞到，返回 maxDist。
     * step 越小越精细（建议 0.25f）。
     */
    public float distanceToCollisionForward(float x0, float y0, float headingRad, float maxDist, float step) {
        float d = 0f;
        while (d <= maxDist) {
            float sx = x0 + d * MathUtils.cos(headingRad);
            float sy = y0 + d * MathUtils.sin(headingRad);
            if (isCollisionAt(sx, sy)) return d;
            d += step;
        }
        return maxDist;
    }
}
