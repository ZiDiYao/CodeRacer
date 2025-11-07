package com.zidi.CodeRacer.Commons.utils;

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

    // 点
    public Vector2 toWorldPoint(float xt, float yt) {
        return new Vector2(xt * unitScale, yt * unitScale); // 不翻转
    }
    // 矩形
    public Rectangle toWorldRect(float x, float y, float w, float h) {
        return new Rectangle(x * unitScale, y * unitScale, w * unitScale, h * unitScale); // 不翻转
    }

}
