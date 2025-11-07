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
import com.zidi.CodeRacer.vehicle.commands.CommandRunner;
import com.zidi.CodeRacer.vehicle.commands.Impl.MoveForwardCommentImpl;
import com.zidi.CodeRacer.vehicle.commands.Impl.TurnLeft90Command;
import com.zidi.CodeRacer.vehicle.commands.Impl.TurnRight90Command;
import com.zidi.CodeRacer.vehicle.components.carBrain.StickyTurnPolicy;
import com.zidi.CodeRacer.vehicle.components.frame.Frame;
import com.zidi.CodeRacer.vehicle.components.frame.Pose;
import com.zidi.CodeRacer.vehicle.components.frame.Impl.WoodenFrame;
import com.zidi.CodeRacer.vehicle.components.sensor.Impl.DefaultSensor;
import com.zidi.CodeRacer.vehicle.components.sensor.Impl.SectorSweepSensor;
import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;
import com.zidi.CodeRacer.vehicle.runtime.adapters.FrameVehicleContext;

public class Main extends ApplicationAdapter {

    private static final int   STEER_POLARITY = -1; // 你的转向极性
    private static final float VIEW_W = 24f, VIEW_H = 14f;
    private static final String TMX_PATH = "Maps/circuit_04.tmx";
    private static final String LAYER_SPAWNS = "Spawns";
    private static final float CRUISE_SPEED = 0.3f; // tile/s，别太快

    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer sr;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private float unitScale = 1f / 16f;
    private float mapTilesW = 1, mapTilesH = 1;
    private int tileWpx = 16, tileHpx = 16, mapHeightPx = 1;

    private TiledWorldUtils world;

    private Frame frame;
    private Pose pose;
    private VehicleContext ctx;

    private DefaultSensor sFront, sLeft, sRight;

    // 成员
    private final StickyTurnPolicy policy = new StickyTurnPolicy();
    private boolean lastWasTurning = false;

    private CommandRunner runner;

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
        world = new TiledWorldUtils(map, unitScale, mapHeightPx);

        // 出生
        Vector2 spawn = readFirstSpawnPointWorldNoFlip();
        float headingRad = readSpawnHeadingRad();

        // 车体
        frame = new WoodenFrame("frame-wood", "Wooden Frame", "Basic frame", 5, 10);
        pose  = frame.pose();
        pose.set(spawn.x, spawn.y, headingRad, CRUISE_SPEED);

        // 适配器 + 命令执行器
        ctx = new FrameVehicleContext(frame);
        runner = new CommandRunner();

        // 三个传感器（按你现有挂点）
// ==================== 前向扫描 ====================
// ±30° 扇形，探测 25 tile，31 根射线，5 帧扫完
        sFront = new SectorSweepSensor(
            "s-front","Front","front",1,1,
            world, new Pose(),
            (float)Math.toRadians(30f),  // fovHalfRad：±30°
            25f,                         // rMax：最远距离
            31,                          // nTotal：总射线数
            5,                           // kFrames：几帧扫完
            0.25f,                       // stepLen：步长
            0.3f                         // emaAlpha：指数平滑系数
        );

// ==================== 左肩扫描（邻道） ====================
// ±10° 扇形，探测 8 tile，9 根射线，4 帧扫完
        sLeft = new SectorSweepSensor(
            "s-left","Left","left",1,1,
            world, new Pose(),
            (float)Math.toRadians(10f),
            8f,
            9, 4,
            0.25f, 0.3f
        );

// ==================== 右肩扫描（邻道） ====================
        sRight = new SectorSweepSensor(
            "s-right","Right","right",1,1,
            world, new Pose(),
            (float)Math.toRadians(10f),
            8f,
            9, 4,
            0.25f, 0.3f
        );


        frame.frontSite().add(sFront);
        frame.leftSite().add(sLeft);
        frame.rightSite().add(sRight);

        camera.position.set(pose.getX(), pose.getY(), 0f);
        camera.update();
    }

    @Override
    public void render() {

        float dt = Gdx.graphics.getDeltaTime();

        // --- 1) 更新三传感器位姿并测距（只用现有 front/left/right 三根射线） ---
        updateSensorPoseFromSite(sFront, frame.frontSite());
        updateSensorPoseFromSite(sLeft, frame.leftSite());
        updateSensorPoseFromSite(sRight, frame.rightSite());

        float dF = raycast(sFront, 50f);
        float dL = raycast(sLeft, 50f);
        float dR = raycast(sRight, 50f);

        // --- 2) 识别当前是否“处于转弯命令中” ---
        boolean turningNow = !(runner.getCurrent() == null
            || runner.getCurrent() instanceof MoveForwardCommentImpl);

        // 若上一帧在转、这一帧不转 => 刚刚“转完”，立刻把左右比例锁为新直线基线
        if (!turningNow && lastWasTurning) {
            policy.onTurnCommitted(dL, dR);
        }
        lastWasTurning = turningNow;

        // --- 3) 仅在空闲时进行决策并下发下一条命令 ---
        if (runner.isIdle()) {
            StickyTurnPolicy.Decision dec = policy.decide(dF, dL, dR);
            switch (dec) {
                case TURN_LEFT -> runner.addCommand(new TurnLeft90Command(STEER_POLARITY));
                case TURN_RIGHT -> runner.addCommand(new TurnRight90Command(STEER_POLARITY));
                default -> runner.addCommand(new MoveForwardCommentImpl());
            }
        }

        // --- 4) 执行当前命令（可能是刚刚入队的） ---
        runner.update(dt, ctx);

        // --- 5) 渲染（你可以选择在转弯时不画射线；这里一直画便于调参） ---
        smoothFollowCamera();

        Gdx.gl.glClearColor(0.08f, 0.1f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        mapRenderer.setView(camera);
        mapRenderer.render();

        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Line);
        drawCarTriangle(sr, pose.getX(), pose.getY(), pose.getHeadingRad(), 0.45f);

        // 若你想转弯时不画射线，可以用 if (!turningNow) 包起来
        drawSensorRay(sr, sFront, dF);
        drawSensorRay(sr, sLeft, dL);
        drawSensorRay(sr, sRight, dR);
        sr.end();
    }


    // ---------- 渲染 ----------
    private void drawSceneOnlyCar() {
        smoothFollowCamera();
        Gdx.gl.glClearColor(0.08f, 0.1f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        mapRenderer.setView(camera);
        mapRenderer.render();

        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Line);
        drawCarTriangle(sr, pose.getX(), pose.getY(), pose.getHeadingRad(), 0.45f);
        sr.end();
    }

    private void drawSceneWithRays(float dF, float dL, float dR) {
        smoothFollowCamera();
        Gdx.gl.glClearColor(0.08f, 0.1f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        mapRenderer.setView(camera);
        mapRenderer.render();

        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Line);
        drawCarTriangle(sr, pose.getX(), pose.getY(), pose.getHeadingRad(), 0.45f);
        drawSensorRay(sr, sFront, dF);
        drawSensorRay(sr, sLeft,  dL);
        drawSensorRay(sr, sRight, dR);
        sr.end();
    }

    // ---------- 传感器/测距 ----------
    private void updateSensorPoseFromSite(DefaultSensor sensor,
                                          com.zidi.CodeRacer.vehicle.components.frame.MountSite site) {
        Pose sp = sensor.pose();
        float hx = pose.getHeadingRad();
        float wx = localToWorldX(site.getLocalX(), site.getLocalY(), pose.getX(), pose.getY(), hx);
        float wy = localToWorldY(site.getLocalX(), site.getLocalY(), pose.getX(), pose.getY(), hx);
        sp.set(wx, wy, wrapAngle(hx + site.getRelAngleRad()), 0f);
    }

    private float raycast(DefaultSensor s, float maxDist) {
        Pose sp = s.pose();
        return world.distanceToCollisionForward(sp.getX(), sp.getY(), sp.getHeadingRad(), maxDist, 0.25f);
    }

    private void drawSensorRay(ShapeRenderer sr, DefaultSensor s, float d) {
        Pose sp = s.pose();
        float sx = sp.getX(), sy = sp.getY(), sh = sp.getHeadingRad();
        float ex = sx + d * MathUtils.cos(sh);
        float ey = sy + d * MathUtils.sin(sh);
        sr.line(sx, sy, ex, ey);
        float r = 0.08f;
        sr.line(ex - r, ey, ex + r, ey);
        sr.line(ex, ey - r, ex, ey + r);
    }

    // ---------- Camera & Utils ----------
    private void smoothFollowCamera() {
        float lerp = 0.12f;
        camera.position.x += (pose.getX() - camera.position.x) * lerp;
        camera.position.y += (pose.getY() - camera.position.y) * lerp;
        float halfW = camera.viewportWidth * 0.5f;
        float halfH = camera.viewportHeight * 0.5f;
        camera.position.x = MathUtils.clamp(camera.position.x, halfW, Math.max(halfW, mapTilesW - halfW));
        camera.position.y = MathUtils.clamp(camera.position.y, halfH, Math.max(halfH, mapTilesH - halfH));
        camera.update();
    }

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
        return (float) Math.toRadians(deg);
    }

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
