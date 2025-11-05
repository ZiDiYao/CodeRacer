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
import com.zidi.CodeRacer.vehicle.commands.Impl.TurnRightCommandImpl;
import com.zidi.CodeRacer.vehicle.runtime.Impl.VehicleContextImpl;

// 世界/地图
import com.zidi.CodeRacer.Commons.utils.TiledWorldUtils;

// 路口检测
import com.zidi.CodeRacer.Commons.utils.IntersectionDetector;
import com.zidi.CodeRacer.Commons.utils.IntersectionHit;

public class Main extends ApplicationAdapter {

    // ===== 基本配置 =====
    private static final float VIEW_W = 24f, VIEW_H = 14f;
    private static final String TMX_PATH = "Maps/Map04_withRoadSide.tmx";
    private static final String LAYER_SPAWN = "SpawnPoints";
    private static final String LAYER_INTER = "Intersection";
    private static final float LANE_WIDTH = 1f;
    private static float CRUISE_SPEED = 4f;

    // 渲染/相机
    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer sr;

    // 地图
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private float unitScale = 1f / 16f;
    private int mapHeightPx = 1;
    private float mapTilesW = 1, mapTilesH = 1;

    // 车辆与命令
    private VehicleContextImpl car;
    private CommandRunner runner;

    // 路口检测
    private IntersectionDetector interDetector;

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

        // 世界/碰撞工具（实验：只保留坐标/单位换算用途，不做碰撞）
        // 世界
        TiledWorldUtils world = new TiledWorldUtils(map, unitScale, mapHeightPx);

        // 出生点（像素→世界，翻转Y，再下移 1 格）
        Vector2 spawn = readFirstSpawnPointWorld();
        spawn.y -= 1f;

        // 车辆与命令
        car = new VehicleContextImpl(spawn.x, spawn.y, 0f);
        runner = new CommandRunner();

        // 路口检测器
        interDetector = IntersectionDetector.fromMap(map, LAYER_INTER, unitScale, mapHeightPx);

        camera.position.set(car.getX(), car.getY(), 0f);
        camera.update();
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();

        // 调速 & 手动清空队列（开发调试）
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) CRUISE_SPEED = Math.max(1f, CRUISE_SPEED - 1f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)) CRUISE_SPEED += 1f;
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            runner.clear();
            System.out.println("[Runner] cleared");
        }

        // 变道热键（和右转互不影响）
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) startLaneChange(true);
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) startLaneChange(false);

        // 执行动作 / 巡航（无碰撞，纯运动学）
        runner.update(dt, car);
        if (runner.isIdle()) {
            cruiseForward(dt);
        }

        // —— 路口检测：只要“进入”就排一个右转命令 —— //
        IntersectionHit interHit = interDetector.update(car.getX(), car.getY());
        if (interHit.entered) {
            System.out.println("[Intersection] entered"
                + (interHit.id != null ? (" #" + interHit.id) : "")
                + " at (" + car.getX() + ", " + car.getY() + ")");

            runner.clear();

            Rectangle r = interHit.rect; // IntersectionDetector 需填充 rect
            Vector2 laneCenter = computeExitLaneCenterForRightTurn(r, car.getHeading());

            // 右转：微步右转 + 收尾吸附到靠边车道中心
            runner.addCommand(new TurnRightCommandImpl(r, laneCenter.x, laneCenter.y));
        }

        // 相机与渲染
        smoothFollowCamera();

        Gdx.gl.glClearColor(0.08f, 0.1f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        mapRenderer.setView(camera);
        mapRenderer.render();

        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Line);
        drawCarTriangle(sr, car.getX(), car.getY(), car.getHeading(), 0.45f);
        sr.end();
    }

    private void startLaneChange(boolean left) {
        runner.clear();
        runner.addCommand(new LaneChangeCommandImpl(left, LANE_WIDTH + 1, 8f, 0.8f));
    }

    @Override
    public void resize(int w, int h) { viewport.update(w, h, true); }

    @Override
    public void dispose() {
        if (mapRenderer != null) mapRenderer.dispose();
        if (map != null) map.dispose();
        if (sr != null) sr.dispose();
    }

    // ====== 右转：计算“右转后的出口车道中心”（一格车道｜一格虚线｜一格逆向） ======
    private Vector2 computeExitLaneCenterForRightTurn(Rectangle interRect, float headingRad) {
        String dir = headingNESW(headingRad);
        float cx = interRect.x + interRect.width  * 0.5f;
        float cy = interRect.y + interRect.height * 0.5f;

        // 车道中心距离边缘 0.5 格（你的 LANE_WIDTH=1f）
        float inset = 0.5f;

        // 右转映射：E->S, S->W, W->N, N->E
        return switch (dir) {
            case "E" -> // 右转后朝南：取南向靠右车道中心（路口下边缘往内0.5）
                    new Vector2(cx, interRect.y + inset);
            case "S" -> // 右转后朝西：取西向靠右车道中心（左边缘往内0.5）
                    new Vector2(interRect.x + inset, cy);
            case "W" -> // 右转后朝北：取北向靠右车道中心（上边缘往内0.5）
                    new Vector2(cx, interRect.y + interRect.height - inset); // 右转后朝东：取东向靠右车道中心（右边缘往内0.5）
            default -> new Vector2(interRect.x + interRect.width - inset, cy);
        };
    }

    // ===== 基础运动 =====
    private void cruiseForward(float dt) {
        float d = CRUISE_SPEED * dt;
        float h = car.getHeading();
        car.setPosition(
            car.getX() + d * MathUtils.cos(h),
            car.getY() + d * MathUtils.sin(h)
        );
    }

    // ===== 出生点（像素→世界；翻转Y）=====
    private Vector2 readFirstSpawnPointWorld() {
        MapLayer sp = map.getLayers().get(LAYER_SPAWN);
        if (sp == null || sp.getObjects().getCount() == 0) return new Vector2(0, 0);
        MapObject o = sp.getObjects().get(0);
        float xt = toFloat(o.getProperties().get("x"), 0f);
        float yt = toFloat(o.getProperties().get("y"), 0f);
        return new Vector2(xt * unitScale, (mapHeightPx - yt) * unitScale);
    }

    // ===== 相机跟随 =====
    private void smoothFollowCamera() {
        float lerp = 0.12f;
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

    // ===== 小工具 =====
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

    private static String headingNESW(float rad) {
        float deg = (float)Math.toDegrees(rad);
        deg = (deg % 360 + 360) % 360;
        if (deg >= 45 && deg < 135)  return "N";
        if (deg >= 135 && deg < 225) return "W";
        if (deg >= 225 && deg < 315) return "S";
        return "E";
    }
}
