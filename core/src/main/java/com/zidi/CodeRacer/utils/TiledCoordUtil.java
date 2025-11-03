package com.zidi.CodeRacer.utils;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public final class TiledCoordUtil {
    private final float unitScale;
    private final int mapHpx;

    public TiledCoordUtil(TiledMap map, float unitScale) {
        this.unitScale = unitScale;
        int h = map.getProperties().get("height", Integer.class);
        int th = map.getProperties().get("tileheight", Integer.class);
        this.mapHpx = h * th;
    }

    /** Tiled point(x,y) -> world(x,y), flip Y */
    public Vector2 toWorldPoint(float xt, float yt) {
        return new Vector2(xt * unitScale, (mapHpx - yt) * unitScale);
    }

    /** Tiled rect(x,y,w,h) -> world(x,y,w,h), flip Y and anchor bottom-left */
    public Rectangle toWorldRect(float x, float y, float w, float h) {
        float wx = x * unitScale;
        float wy = (mapHpx - y - h) * unitScale;
        return new Rectangle(wx, wy, w * unitScale, h * unitScale);
    }
}
