package com.zidi.CodeRacer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.zidi.CodeRacer.vehicle.commands.CommandRunner;
import com.zidi.CodeRacer.vehicle.commands.Impl.MoveForwardCommentImpl;   // 你项目里已经有
import com.zidi.CodeRacer.vehicle.commands.Impl.LaneChangeCommandImpl;    // 新增的变道命令
import com.zidi.CodeRacer.vehicle.runtime.Impl.VehicleContextImpl;

public class Main extends ApplicationAdapter {

    // 视口大小（世界单位 = tile）
    private static final float WORLD_W = 24f;
    private static final float WORLD_H = 14f;

    // “每秒一步”的节奏
    private static final float STEP_INTERVAL = 1f;

    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer sr;

    // 地图
    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer mapRenderer;
    private float unitScale = 1f;
    private float mapW = 100, mapH = 100; // 地图尺寸（tile = world unit）

    // 车辆与命令
    private VehicleContextImpl ctx;
    private CommandRunner runner;

    // 定时器
    private float timer = 0f;

    @Override
    public void create() {
        // 相机/视口
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_W, WORLD_H, camera);
        viewport.apply(true);
        camera.position.set(0f, 0f, 0f);   // 出生点(0,0)
        camera.update();

        sr = new ShapeRenderer();

        // === 加载 TMX 地图 ===
        tiledMap = new TmxMapLoader().load("Maps/MVP_Map01.tmx");

        MapProperties p = tiledMap.getProperties();
        int tileWpx = getInt(p.get("tilewidth"), 16);
        int tilesW  = getInt(p.get("width"),  100);
        int tilesH  = getInt(p.get("height"), 100);

        unitScale = 1f / Math.max(1, tileWpx);
        mapW = tilesW;   // 1 tile = 1 world unit
        mapH = tilesH;

        mapRenderer = new OrthogonalTiledMapRenderer(tiledMap, unitScale);

        // === 车辆：出生在(0,0)，朝向0（向右） ===
        ctx = new VehicleContextImpl(0f, 0f, 0f);

        // === 命令执行器 ===
        runner = new CommandRunner();
        // 不先塞前进命令；等待 1s 计时或按键
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();

        // --- 变道触发（优先级最高） ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {  // 左变道
            runner.clear();
            // laneWidth=3f（侧向偏移），length=8f（前向推进），duration=2s（变道时长）
            runner.addCommand(new LaneChangeCommandImpl(true, 3f, 8f, 2f));
            timer = 0f; // 避免变道刚结束就立刻插一次直行
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {  // 右变道
            runner.clear();
            runner.addCommand(new LaneChangeCommandImpl(false, 3f, 8f, 2f));
            timer = 0f;
        }

        // --- “每秒一步直行”：仅在空闲时入队，避免与变道抢占 ---
        if (runner.isIdle()) {
            timer += dt;
            if (timer >= STEP_INTERVAL) {
                runner.addCommand(new MoveForwardCommentImpl());
                timer = 0f;
            }
        }

        // 执行队列
        runner.update(dt, ctx);

        // === 相机平滑跟随 + 夹紧到地图范围 ===
        float lerp = 0.18f;
        camera.position.x += (ctx.getX() - camera.position.x) * lerp;
        camera.position.y += (ctx.getY() - camera.position.y) * lerp;

        float halfW = camera.viewportWidth * 0.5f;
        float halfH = camera.viewportHeight * 0.5f;
        float cx = MathUtils.clamp(camera.position.x, halfW, Math.max(halfW, mapW - halfW));
        float cy = MathUtils.clamp(camera.position.y, halfH, Math.max(halfH, mapH - halfH));
        camera.position.set(cx, cy, 0f);
        camera.update();

        // === 渲染 ===
        Gdx.gl.glClearColor(0.08f, 0.1f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        mapRenderer.setView(camera);
        mapRenderer.render();

        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Line);
        drawCarTriangle(sr, ctx.getX(), ctx.getY(), ctx.getHeading(), 0.7f);
        sr.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (mapRenderer != null) mapRenderer.dispose();
        if (tiledMap != null)   tiledMap.dispose();
        if (sr != null)         sr.dispose();
    }

    // ===== 小工具 =====
    private static int getInt(Object o, int def){
        if (o instanceof Integer i) return i;
        if (o instanceof String s) { try { return Integer.parseInt(s); } catch(Exception ignore){} }
        return def;
    }

    private static void drawCarTriangle(ShapeRenderer sr, float cx, float cy, float rad, float r) {
        Vector2 tip   = new Vector2(cx + r * MathUtils.cos(rad), cy + r * MathUtils.sin(rad));
        Vector2 left  = new Vector2(cx + 0.6f * MathUtils.cos(rad + 2.6f), cy + 0.6f * MathUtils.sin(rad + 2.6f));
        Vector2 right = new Vector2(cx + 0.6f * MathUtils.cos(rad - 2.6f), cy + 0.6f * MathUtils.sin(rad - 2.6f));
        sr.line(left, tip); sr.line(tip, right); sr.line(right, left);
    }
}
