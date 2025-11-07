package com.zidi.CodeRacer;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.zidi.CodeRacer.Commons.utils.TiledWorldUtils;
import com.zidi.CodeRacer.vehicle.components.frame.Frame;
import com.zidi.CodeRacer.vehicle.components.frame.Pose;
import com.zidi.CodeRacer.vehicle.components.frame.Impl.WoodenFrame;
import com.zidi.CodeRacer.vehicle.components.sensor.Impl.BaseSensor;
import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;
import com.zidi.CodeRacer.vehicle.runtime.adapters.FrameVehicleContext;

public class Main extends ApplicationAdapter {

    private static final float VIEW_W = 24f, VIEW_H = 14f;
    private static final String TMX_PATH = "Maps/circuit_01.tmx";
    private static final String LAYER_SPAWNS = "Spawns";
    private static final float CRUISE_SPEED = 4f;

    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer sr;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private float unitScale = 1f / 16f;
    private float mapTilesW = 1, mapTilesH = 1;
    private int tileWpx = 16, tileHpx = 16, mapHeightPx = 1;

    // 世界/碰撞工具（你项目里已有）
    private TiledWorldUtils world;

    // 车辆：Frame + 适配器
    private Frame frame;
    private Pose pose;
    private VehicleContext ctx;

    // 三个传感器（作为部件挂在 Frame 上）
    private BaseSensor sFront, sLeft, sRight;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_W, VIEW_H, camera);
        viewport.apply(true);
        sr = new ShapeRenderer();

        map = new TmxMapLoader().load(TMX_PATH);
        MapProperties p = map.getProperties();
        tileWpx = p.get("tilewidth", Integer.class);
        tileHpx = p.get("tileheight", Integer.class);
        int tilesW  = p.get("width", Integer.class);
        int tilesH  = p.get("height", Integer.class);
        unitScale   = 1f / tileWpx;
        mapTilesW   = tilesW;
        mapTilesH   = tilesH;
        mapHeightPx = tilesH * tileHpx;

        mapRenderer = new OrthogonalTiledMapRenderer(map, unitScale);

        // 世界/碰撞
        world = new TiledWorldUtils(map, unitScale, mapHeightPx);

        // 出生点（沿用你“不翻转”的版本）
        Vector2 spawn = readFirstSpawnPointWorldNoFlip();
        float headingRad = readSpawnHeadingRad();

        // 用 Frame 作为实体
        frame = new WoodenFrame("frame-wood", "Wooden Frame", "Basic frame", 5, 10);
        pose  = frame.pose();
        pose.set(spawn.x, spawn.y, headingRad, CRUISE_SPEED);

        // 适配器：命令层只面向 VehicleContext
        ctx = new FrameVehicleContext(frame);

        // 创建并安装传感器（使用你已有的 BaseSensor，Pose 单独给）
        sFront = new BaseSensor("s-front", "Front Sensor", "front", 1, 1, world, new Pose());
        sLeft  = new BaseSensor("s-left",  "Left Sensor",  "left",  1, 1, world, new Pose());
        sRight = new BaseSensor("s-right", "Right Sensor", "right", 1, 1, world, new Pose());
        frame.frontSite().add(sFront);
        frame.leftSite().add(sLeft);
        frame.rightSite().add(sRight);
        camera.position.set(pose.getX(), pose.getY(), 0f);
        camera.update();
        Gdx.app.log("Spawn", "world=(" + spawn.x + "," + spawn.y + ")");
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();

        // 直行：把 throttle 当成“本帧前进距离”
        ctx.apply(0f, CRUISE_SPEED * dt, 0f);

        // 相机跟随 + 限界
        float lerp = 0.12f;
        camera.position.x += (pose.getX() - camera.position.x) * lerp;
        camera.position.y += (pose.getY() - camera.position.y) * lerp;
        float halfW = camera.viewportWidth * 0.5f;
        float halfH = camera.viewportHeight * 0.5f;
        camera.position.x = MathUtils.clamp(camera.position.x, halfW, Math.max(halfW, mapTilesW - halfW));
        camera.position.y = MathUtils.clamp(camera.position.y, halfH, Math.max(halfH, mapTilesH - halfH));
        camera.update();

        // 渲染地图
        Gdx.gl.glClearColor(0.08f, 0.1f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        mapRenderer.setView(camera);
        mapRenderer.render();

        // === 画车 + 三个传感器射线 ===
        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Line);

        drawCarTriangle(sr, pose.getX(), pose.getY(), pose.getHeadingRad(), 0.45f);

        // 根据挂点把传感器位姿更新到世界坐标
        updateSensorPoseFromSite(sFront, frame.frontSite());
        updateSensorPoseFromSite(sLeft,  frame.leftSite());
        updateSensorPoseFromSite(sRight, frame.rightSite());

        // 画射线（使用 world 最近碰撞距离）
        drawSensorRay(sr, sFront, 50f);
        drawSensorRay(sr, sLeft,  50f);
        drawSensorRay(sr, sRight, 50f);

        sr.end();
    }

    @Override
    public void resize(int w, int h) { viewport.update(w, h, true); }
    @Override
    public void dispose() { if (mapRenderer!=null) mapRenderer.dispose(); if (map!=null) map.dispose(); if (sr!=null) sr.dispose(); }

    // -------------------- 你的原版：不翻转的 spawn 读取 --------------------
    private Vector2 readFirstSpawnPointWorldNoFlip() {
        MapLayer layer = map.getLayers().get(LAYER_SPAWNS);
        if (layer == null || layer.getObjects().getCount() == 0) return new Vector2(0,0);
        MapObject o = layer.getObjects().get(0);
        float xpx = toFloat(o.getProperties().get("x"), 0f);
        float ypx = toFloat(o.getProperties().get("y"), 0f);
        return new Vector2(xpx * unitScale, ypx * unitScale);
    }

    private float readSpawnHeadingRad() {
        MapLayer layer = map.getLayers().get(LAYER_SPAWNS);
        if (layer == null || layer.getObjects().getCount() == 0) return 0f;
        MapObject o = layer.getObjects().get(0);
        float deg = toFloat(o.getProperties().get("heading_deg"), 0f);
        return (float)Math.toRadians(deg);
    }

    // -------------------- 传感器位姿 & 射线可视化 --------------------
    private void updateSensorPoseFromSite(BaseSensor sensor,
                                          com.zidi.CodeRacer.vehicle.components.frame.MountSite site) {
        Pose sp = sensor.pose();
        float hx = pose.getHeadingRad();

        // 局部 -> 世界
        float wx = localToWorldX(site.getLocalX(), site.getLocalY(), pose.getX(), pose.getY(), hx);
        float wy = localToWorldY(site.getLocalX(), site.getLocalY(), pose.getX(), pose.getY(), hx);

        sp.set(wx, wy, wrapAngle(hx + site.getRelAngleRad()), 0f);
    }

    private void drawSensorRay(ShapeRenderer sr, BaseSensor s, float maxDist) {
        Pose sp = s.pose();
        float sx = sp.getX(), sy = sp.getY(), sh = sp.getHeadingRad();

        float d = world.distanceToCollisionForward(sx, sy, sh, maxDist, 0.25f);

        float ex = sx + d * MathUtils.cos(sh);
        float ey = sy + d * MathUtils.sin(sh);

        sr.line(sx, sy, ex, ey);     // 射线
        float r = 0.08f;             // 端点小十字
        sr.line(ex - r, ey, ex + r, ey);
        sr.line(ex, ey - r, ex, ey + r);

    }

    // -------------------- 工具 --------------------
    private static void drawCarTriangle(ShapeRenderer sr, float cx, float cy, float rad, float r) {
        Vector2 tip   = new Vector2(cx + r * MathUtils.cos(rad), cy + r * MathUtils.sin(rad));
        Vector2 left  = new Vector2(cx + 0.6f * MathUtils.cos(rad + 2.6f), cy + 0.6f * MathUtils.sin(rad + 2.6f));
        Vector2 right = new Vector2(cx + 0.6f * MathUtils.cos(rad - 2.6f), cy + 0.6f * MathUtils.sin(rad - 2.6f));
        sr.line(left, tip); sr.line(tip, right); sr.line(right, left);
    }
    private static float localToWorldX(float lx, float ly, float px, float py, float h){
        float c = MathUtils.cos(h), s = MathUtils.sin(h);
        return px + (lx * c - ly * s);
    }
    private static float localToWorldY(float lx, float ly, float px, float py, float h){
        float c = MathUtils.cos(h), s = MathUtils.sin(h);
        return py + (lx * s + ly * c);
    }
    private static float wrapAngle(float rad){
        rad = (rad + MathUtils.PI) % MathUtils.PI2;
        if (rad < 0) rad += MathUtils.PI2;
        return rad - MathUtils.PI;
    }
    private static float toFloat(Object o, float def) {
        if (o instanceof Float f) return f;
        if (o instanceof Number n) return n.floatValue();
        if (o instanceof String s) { try { return Float.parseFloat(s); } catch (Exception ignored) {} }
        return def;
    }
}
