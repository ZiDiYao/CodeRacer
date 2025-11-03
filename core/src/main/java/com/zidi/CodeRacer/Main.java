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
import java.util.ArrayList;
import java.util.List;

public class Main extends ApplicationAdapter {

    // ===== 基本配置 =====
    private static final float VIEW_W = 24f, VIEW_H = 14f;              // 视口（世界单位）
    private static final String TMX_PATH = "Maps/Map03_withIntersection_Lanes.tmx";
    private static final String LAYER_SPAWN = "SpawnPoints";
    private static final String LAYER_INTER = "Intersection";
    private static final float LANE_WIDTH = 1f;                          // 每条车道=1 格
    private static float CRUISE_SPEED = 4f;                              // 巡航速度（格/秒）

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
        unitScale   = 1f / tileWpx; // 保证 1 tile = 1 世界单位

        mapRenderer = new OrthogonalTiledMapRenderer(map, unitScale);

        // 出生点（像素→世界，翻转Y，再下移 1 格）
        Vector2 spawn = readFirstSpawnPointWorld();
        spawn.y -= 1f; // 往下 1 tile
        car = new VehicleContextImpl(spawn.x, spawn.y, 0f); // 初始朝向向右
        runner = new CommandRunner();

        // 缓存路口矩形
        cacheIntersectionWorldRects();

        // 相机
        camera.position.set(car.getX(), car.getY(), 0f);
        camera.update();
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();

        // —— 速度热键（可选）——
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) CRUISE_SPEED = Math.max(1f, CRUISE_SPEED - 1f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)) CRUISE_SPEED += 1f;

        // —— 变道：Q 左 / E 右（侧向偏移=1格）——
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            runner.clear();
            runner.addCommand(new LaneChangeCommandImpl(true, LANE_WIDTH+1, 8f, 0.8f));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            runner.clear();
            runner.addCommand(new LaneChangeCommandImpl(false, LANE_WIDTH+1, 8f, 0.8f));
        }

        // 执行命令（变道时由命令控制位置与朝向）
        runner.update(dt, car);

        // 空闲就持续巡航（顺滑前进）
        if (runner.isIdle()) cruiseForward(dt);

        // 进入路口检测（边沿打印）
        boolean insideNow = isInsideAnyIntersection(car.getX(), car.getY());
        if (insideNow && !wasInsideIntersection) {
            System.out.println("[Intersection] entered at (" + car.getX() + ", " + car.getY() + ")");
        }
        wasInsideIntersection = insideNow;

        // 相机
        smoothFollowCamera();

        // 渲染
        Gdx.gl.glClearColor(0.08f, 0.1f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        mapRenderer.setView(camera);
        mapRenderer.render();

        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Line);
        drawCarTriangle(sr, car.getX(), car.getY(), car.getHeading(), 0.7f);
        sr.end();
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
        float lerp = 0.12f; // 稍微更稳一点
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
}
