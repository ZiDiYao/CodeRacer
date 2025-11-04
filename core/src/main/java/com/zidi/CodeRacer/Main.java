package com.zidi.CodeRacer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.zidi.CodeRacer.vehicle.commands.CommandRunner;
import com.zidi.CodeRacer.vehicle.commands.Impl.LaneChangeCommandImpl;
import com.zidi.CodeRacer.vehicle.runtime.Impl.VehicleContextImpl;

// === 传感器相关 ===
import com.zidi.CodeRacer.vehicle.components.frame.Pose;
import com.zidi.CodeRacer.vehicle.components.sensor.SensorReading;
import com.zidi.CodeRacer.vehicle.components.sensor.Impl.BaseSensor;
import com.zidi.CodeRacer.Commons.utils.TiledWorldUtils;

import java.util.ArrayList;
import java.util.List;

public class Main extends ApplicationAdapter {

    // ===== 基本配置 =====
    private static final float VIEW_W = 24f, VIEW_H = 14f;              // 视口（世界单位）
    private static final String TMX_PATH = "Maps/Map04_withRoadSide.tmx";
    private static final String LAYER_SPAWN = "SpawnPoints";
    private static final String LAYER_INTER = "Intersection";
    private static final float LANE_WIDTH = 1f;                          // 每条车道=1 格
    private static float CRUISE_SPEED = 4f;                              // 巡航速度（格/秒）

    // —— 安全/防抖参数 —— //
    private static final float EMERGENCY_DIST = 2.0f;        // 变道/宽容期内的硬急停阈值（格）
    private static final float GRACE_AFTER_LC = 0.25f;       // 变道结束后的宽容期（秒）
    private static final float HEADING_DEADBAND_DEG = 5f;    // 死区：与冻结朝向小于 5° 视为已对齐
    private static final int   REQUIRED_TRIGGER_FRAMES = 3;  // 变道/宽容期内，需连续命中 N 帧才急停

    // 渲染/相机
    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer sr;

    // 地图
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private float unitScale = 1f / 16f; // 1 tile(=16px) = 1 世界单位
    private int mapHeightPx = 1;
    private float mapTilesW = 1, mapTilesH = 1;

    // 车辆与命令
    private VehicleContextImpl car;
    private CommandRunner runner;

    // 路口世界矩形缓存
    private final List<Rectangle> intersectionsWorld = new ArrayList<>();
    private boolean wasInsideIntersection = false;

    // ===== 传感器/世界/姿态 =====
    private TiledWorldUtils world;
    private Pose pose;                 // 传感器姿态（位置 + “传感器用朝向”）
    private BaseSensor sensor;         // 碰撞预警

    // 刹停 / 变道状态
    private boolean emergencyBrake = false;
    private boolean laneChanging = false;
    private float frozenSenseHeading = 0f; // 变道/宽容期内冻结的“传感器朝向”
    private float graceTimer = 0f;         // 宽容期计时
    private int triggerFrames = 0;         // 连续命中计数（仅变道/宽容期内使用）

    @Override
    public void create() {
        // 相机
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_W, VIEW_H, camera);
        viewport.apply(true);
        sr = new ShapeRenderer();

        // 地图
        map = new TmxMapLoader().load(TMX_PATH);
        MapProperties p = map.getProperties();
        int tileWpx = getInt(p.get("tilewidth"), 16);
        int tileHpx = getInt(p.get("tileheight"), 16);
        int tilesW  = getInt(p.get("width"), 60);
        int tilesH  = getInt(p.get("height"), 40);
        mapTilesW   = tilesW;
        mapTilesH   = tilesH;
        mapHeightPx = tilesH * tileHpx;
        unitScale   = 1f / tileWpx;

        mapRenderer = new OrthogonalTiledMapRenderer(map, unitScale);

        // 世界/碰撞工具（按你项目的签名调整）
        world = new TiledWorldUtils(map, unitScale, mapHeightPx);

        // 出生点（像素→世界，翻转Y，再下移 1 格）
        Vector2 spawn = readFirstSpawnPointWorld();
        spawn.y -= 1f;

        // 车辆与命令
        car = new VehicleContextImpl(spawn.x, spawn.y, 0f); // 初始朝向向右
        runner = new CommandRunner();

        // 姿态与传感器
        pose = new Pose(car.getX(), car.getY(), car.getHeading(), 0f);
        sensor = new BaseSensor(
            "SENSOR_FRONT", "Front Collision Warning",
            "Warns when obstacle ahead within safe distance",
            0, 0, world, pose
        );

        cacheIntersectionWorldRects();

        camera.position.set(car.getX(), car.getY(), 0f);
        camera.update();
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();

        // —— 调速 & 手动复位 —— //
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) CRUISE_SPEED = Math.max(1f, CRUISE_SPEED - 1f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)) CRUISE_SPEED += 1f;
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            emergencyBrake = false;
            triggerFrames = 0;
            graceTimer = 0f;
            laneChanging = false;
            System.out.println("[Brake] manual reset");
        }

        // —— 变道 —— //
        if (!emergencyBrake) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) startLaneChange(true);
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) startLaneChange(false);
        } else {
            runner.clear();
        }

        // —— 执行动作 / 巡航 —— //
        if (!emergencyBrake) {
            runner.update(dt, car);
            if (runner.isIdle()) {
                if (laneChanging) {
                    laneChanging = false;
                    graceTimer = GRACE_AFTER_LC; // 进入宽容期：继续冻结传感器朝向
                    // System.out.println("[LaneChange] finished; grace start");
                }
                cruiseForward(dt);
            }
        }

        // 宽容期计时递减
        if (graceTimer > 0f) graceTimer -= dt;

        // 如果在宽容期内且车头已回正到死区内，则提前结束宽容期
        if (graceTimer > 0f) {
            float d = wrapAngleDelta(car.getHeading(), frozenSenseHeading);
            if (Math.abs(d) <= MathUtils.degreesToRadians * HEADING_DEADBAND_DEG) {
                graceTimer = 0f;
            }
        }

        // —— 同步传感器姿态（位置总是跟车，朝向取决于是否冻结） —— //
        float headingForSensor = (laneChanging || graceTimer > 0f) ? frozenSenseHeading : car.getHeading();
        if (pose == null) pose = new Pose(car.getX(), car.getY(), headingForSensor, 0f);
        else pose.set(car.getX(), car.getY(), headingForSensor, 0f);

        // —— 传感器检测 —— //
        if (sensor != null) {
            SensorReading reading = sensor.detect();
            if (reading != null && reading.triggered()) {
                boolean inTransient = laneChanging || graceTimer > 0f;

                if (inTransient) {
                    // 变道/宽容期：必须足够近 + 连续 N 帧
                    if (reading.distance() <= EMERGENCY_DIST) {
                        triggerFrames++;
                    } else {
                        triggerFrames = 0;
                    }
                    if (triggerFrames >= REQUIRED_TRIGGER_FRAMES && !emergencyBrake) {
                        emergencyBrake = true;
                        runner.clear();
                        System.out.println("[EMERGENCY] (transient) dist=" + reading.distance()
                            + " at (" + car.getX() + ", " + car.getY() + ")");
                    }
                } else {
                    // 正常直行：立即刹停
                    if (!emergencyBrake) {
                        emergencyBrake = true;
                        runner.clear();
                        System.out.println("[EMERGENCY] dist=" + reading.distance()
                            + " at (" + car.getX() + ", " + car.getY() + ")");
                    }
                }
            } else {
                // 未命中则清零连续帧计数
                triggerFrames = 0;
            }
        }

        // —— 路口进入提示 —— //
        boolean insideNow = isInsideAnyIntersection(car.getX(), car.getY());
        if (insideNow && !wasInsideIntersection) {
            System.out.println("[Intersection] entered at (" + car.getX() + ", " + car.getY() + ")");
        }
        wasInsideIntersection = insideNow;

        // —— 相机与渲染 —— //
        smoothFollowCamera();

        Gdx.gl.glClearColor(0.08f, 0.1f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        mapRenderer.setView(camera);
        mapRenderer.render();

        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Line);
        drawCarTriangle(sr, car.getX(), car.getY(), car.getHeading(), 0.7f);
        if (emergencyBrake) {
            float r = 1.0f;
            sr.rect(car.getX() - r * 0.5f, car.getY() - r * 0.5f, r, r);
        }
        sr.end();
    }

    private void startLaneChange(boolean left) {
        laneChanging = true;
        frozenSenseHeading = car.getHeading(); // 冻结当前车头（即车道切线）作为传感器朝向
        triggerFrames = 0;                      // 进入变道时清零防抖计数
        runner.clear();
        runner.addCommand(new LaneChangeCommandImpl(left, LANE_WIDTH + 1, 8f, 0.8f));
        // System.out.println("[LaneChange] start " + (left ? "LEFT" : "RIGHT"));
    }

    @Override
    public void resize(int w, int h) { viewport.update(w, h, true); }

    @Override
    public void dispose() {
        if (mapRenderer != null) mapRenderer.dispose();
        if (map != null) map.dispose();
        if (sr != null) sr.dispose();
    }

    // ====== 运动：持续巡航 ======
    private void cruiseForward(float dt) {
        float d = CRUISE_SPEED * dt;
        float h = car.getHeading();
        car.setPosition(
            car.getX() + d * MathUtils.cos(h),
            car.getY() + d * MathUtils.sin(h)
        );
    }

    // ====== 读取出生点（像素→世界；翻转Y） ======
    private Vector2 readFirstSpawnPointWorld() {
        MapLayer sp = map.getLayers().get(LAYER_SPAWN);
        if (sp == null || sp.getObjects().getCount() == 0) return new Vector2(0, 0);
        MapObject o = sp.getObjects().get(0);
        float xt = toFloat(o.getProperties().get("x"), 0f);
        float yt = toFloat(o.getProperties().get("y"), 0f);
        return new Vector2(xt * unitScale, (mapHeightPx - yt) * unitScale);
    }

    // ====== 路口世界矩形缓存 ======
    private void cacheIntersectionWorldRects() {
        intersectionsWorld.clear();
        MapLayer inter = map.getLayers().get(LAYER_INTER);
        if (inter == null) return;
        for (MapObject o : inter.getObjects()) {
            if (o instanceof RectangleMapObject rmo) {
                Rectangle rp = rmo.getRectangle(); // 像素矩形（左上原点）
                float wx = rp.x * unitScale;
                float wy = (mapHeightPx - rp.y - rp.height) * unitScale; // 翻转Y并减去高
                float ww = rp.width * unitScale;
                float wh = rp.height * unitScale;
                intersectionsWorld.add(new Rectangle(wx, wy, ww, wh));
            }
        }
    }

    private boolean isInsideAnyIntersection(float x, float y) {
        for (Rectangle r : intersectionsWorld) {
            if (r.contains(x, y)) return true;
        }
        return false;
    }

    // ====== 相机跟随 ======
    private void smoothFollowCamera() {
        float lerp = 0.12f; // 更稳一点
        camera.position.x += (car.getX() - camera.position.x) * lerp;
        camera.position.y += (car.getY() - camera.position.y) * lerp;

        float halfW = camera.viewportWidth * 0.5f;
        float halfH = camera.viewportHeight * 0.5f;
        float mapW = mapTilesW, mapH = mapTilesH;
        float cx = MathUtils.clamp(camera.position.x, halfW, Math.max(halfW, mapW - halfW));
        float cy = MathUtils.clamp(camera.position.y, halfH, Math.max(halfH, mapH - halfH));
        camera.position.set(cx, cy, 0f);
        camera.update();
    }

    // ====== 小工具 ======
    private static void drawCarTriangle(ShapeRenderer sr, float cx, float cy, float rad, float r) {
        Vector2 tip   = new Vector2(cx + r * MathUtils.cos(rad), cy + r * MathUtils.sin(rad));
        Vector2 left  = new Vector2(cx + 0.6f * MathUtils.cos(rad + 2.6f), cy + 0.6f * MathUtils.sin(rad + 2.6f));
        Vector2 right = new Vector2(cx + 0.6f * MathUtils.cos(rad - 2.6f), cy + 0.6f * MathUtils.sin(rad - 2.6f));
        sr.line(left, tip); sr.line(tip, right); sr.line(right, left);
    }

    private static int getInt(Object o, int def) {
        if (o instanceof Integer i) return i;
        if (o instanceof String s) { try { return Integer.parseInt(s); } catch (Exception ignored) {} }
        return def;
    }

    private static float toFloat(Object o, float def) {
        if (o instanceof Float f) return f;
        if (o instanceof Number n) return n.floatValue();
        if (o instanceof String s) { try { return Float.parseFloat(s); } catch (Exception ignored) {} }
        return def;
    }

    // 返回 a 相对 b 的最小弧度差（范围 [-π, π]）
    private static float wrapAngleDelta(float a, float b) {
        float d = a - b;
        while (d > MathUtils.PI)  d -= MathUtils.PI2;
        while (d < -MathUtils.PI) d += MathUtils.PI2;
        return d;
    }
}
