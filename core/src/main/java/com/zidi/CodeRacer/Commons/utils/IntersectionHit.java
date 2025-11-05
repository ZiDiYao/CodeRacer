package com.zidi.CodeRacer.Commons.utils;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

/** 返回一次“路口检测”的状态（可直接当成 true/false 使用：hit.inside） */
public class IntersectionHit {
    public final boolean entered;   // 本帧刚进入
    public final boolean inside;    // 当前在里面
    public final boolean exited;    // 本帧刚离开
    public final String  id;        // 路口 id（若 Tiled 矩形上有 id 属性）
    public final Rectangle rect;    // 世界坐标矩形
    public final Vector2 center;    // 中心点（世界坐标）

    public static final IntersectionHit NONE = new IntersectionHit(false,false,false,null,null,null);

    public IntersectionHit(boolean entered, boolean inside, boolean exited,
                           String id, Rectangle rect, Vector2 center) {
        this.entered = entered; this.inside = inside; this.exited = exited;
        this.id = id; this.rect = rect; this.center = center;
    }
}

