package com.zidi.CodeRacer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Main extends ApplicationAdapter {

    // ===== 画面 / 地图 =====
    private OrthographicCamera camera;
    private Viewport viewport;
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private ShapeRenderer sr;
    private Texture carTex;

    // ===== 单位换算（从 TMX 读取） =====
    private float unitScale = 1f; // = 1f / tilewidth(px)

    // ===== 视口尺寸（世界单位：1 tile = 1 world unit）=====
    private static final float WORLD_WIDTH  = 20f;
    private static final float WORLD_HEIGHT = 12f;

    // ===== 碰撞矩形（世界单位）=====
    private final Array<Rectangle> collisionRectsWU = new Array<>();

    // ===== 车辆状态（世界单位）=====
    private float carX, carY;          // 中心点
    private float headingRad = 0f;     // 初始朝右（0），向上（PI/2）
    private float speed = 6f;          // world units/s（= tiles/s）
    private float carSizeWU = 1.0f;    // 车宽高 1 tile
    private float carHalf   = 0.5f;
    private Rectangle carBounds;

    // ===== 调试开关 =====
    private boolean debugDrawCollision = true;
    private boolean debugDrawSCurve    = true;
    private boolean enableCollision    = true;

    // ===== S 形机动（两段三次贝塞尔）=====
    private boolean sManeuver = true;   // 默认一启动就跑 S；按空格切换
    private float sProgress = 0f;       // [0,2) —— 每 1.0 表示一段曲线
    private Vector2 s_p0, s_p1, s_p2, s_p3; // 段1控制点
    private Vector2 s_q0, s_q1, s_q2, s_q3; // 段2控制点
    private float   s_totalLen = 1f;         // 近似总长度（速度→参数）

    @Override
    public void create() {
        // 摄像机 / 视口
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply(true);
        camera.position.set(WORLD_WIDTH/2f, WORLD_HEIGHT/2f, 0f);
        camera.update();

        // 加载地图
        map = new TmxMapLoader().load("Maps/MVP_Map01.tmx");

        // 动态计算 unitScale（像素→世界）：1/tilewidth
        int tileWpx = map.getProperties().get("tilewidth", Integer.class);
        if (tileWpx <= 0) tileWpx = 16; // 兜底
        unitScale = 1f / (float) tileWpx;

        // 渲染器 & 画笔
        renderer = new OrthogonalTiledMapRenderer(map, unitScale);
        sr = new ShapeRenderer();
        carTex = new Texture(Gdx.files.internal("tile_0483.png"));

        // 地图尺寸（世界单位：每 tile = 1）
        int tilesW = map.getProperties().get("width", Integer.class);
        int tilesH = map.getProperties().get("height", Integer.class);
        float mapW_WU = tilesW;
        float mapH_WU = tilesH;

        // 默认出生在地图中心
        carX = mapW_WU / 2f;
        carY = mapH_WU / 2f;

        // 读 SpawnPoints（像素→世界）
        MapLayer spawn = map.getLayers().get("SpawnPoints");
        if (spawn != null && spawn.getObjects().getCount() > 0) {
            MapObject o = spawn.getObjects().get(0);
            Float px = o.getProperties().get("x", Float.class);
            Float py = o.getProperties().get("y", Float.class);
            if (px != null && py != null) {
                carX = px * unitScale;
                carY = py * unitScale;
            }
        }

        // 读 Collision（像素→世界）
        MapLayer col = map.getLayers().get("Collision");
        if (col != null) {
            for (MapObject obj : col.getObjects()) {
                if (obj instanceof RectangleMapObject rmo) {
                    Rectangle rpx = rmo.getRectangle();
                    collisionRectsWU.add(new Rectangle(
                        rpx.x * unitScale,
                        rpx.y * unitScale,
                        rpx.width * unitScale,
                        rpx.height * unitScale
                    ));
                }
            }
        }

        // AABB
        carBounds = new Rectangle(carX - carHalf, carY - carHalf, carSizeWU, carSizeWU);

        // 摄像机移到出生点
        camera.position.set(carX, carY, 0f);
        camera.update();

        // 可选：一开始朝上
        // headingRad = MathUtils.PI / 2f;

        // 生成一条以当前位姿为起点的 S 曲线
        buildSShape(carX, carY, headingRad, /*len*/ 24f, /*amp*/ 6f);
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();

        // 轻量按键：空格切 S / 直行；F1 碰撞开关；F2 显示切换
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) sManeuver = !sManeuver;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1))    enableCollision = !enableCollision;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            debugDrawCollision = !debugDrawCollision;
            debugDrawSCurve    = !debugDrawSCurve;
        }

        // ===== 移动 =====
        if (sManeuver) {
            // S 曲线推进：速度（世界单位/秒）→ 参数增量
            float dv = speed * dt;
            float dtCurve = dv / Math.max(0.1f, s_totalLen);
            sProgress += dtCurve; // [0,2)
            while (sProgress >= 2f) sProgress -= 2f;

            Vector2 pos = new Vector2();
            Vector2 tan = new Vector2();

            if (sProgress < 1f) {
                float t = sProgress;
                bezierPoint(s_p0, s_p1, s_p2, s_p3, t, pos);
                bezierTangent(s_p0, s_p1, s_p2, s_p3, t, tan);
            } else {
                float t = sProgress - 1f;
                bezierPoint(s_q0, s_q1, s_q2, s_q3, t, pos);
                bezierTangent(s_q0, s_q1, s_q2, s_q3, t, tan);
            }

            float nextX = pos.x, nextY = pos.y;
            // 可选：S 模式也做一次兜底碰撞（简单回退）
            if (enableCollision &&
                collidesAt(nextX - carHalf, nextY - carHalf, carSizeWU, carSizeWU)) {
                // 撞上就停住当前 sProgress 增量（也可 sManeuver=false）
            } else {
                carX = nextX; carY = nextY;
                headingRad = (float)Math.atan2(tan.y, tan.x);
            }

        } else {
            // 恒速直行 + 分轴碰撞
            float dx = speed * MathUtils.cos(headingRad) * dt;
            float dy = speed * MathUtils.sin(headingRad) * dt;

            if (!enableCollision) {
                carX += dx; carY += dy;
            } else {
                float tryX = carX + dx, tryY = carY + dy;
                if (!collidesAt(tryX - carHalf, carY - carHalf, carSizeWU, carSizeWU)) carX = tryX;
                if (!collidesAt(carX - carHalf, tryY - carHalf, carSizeWU, carSizeWU)) carY = tryY;
            }
        }

        // 同步 AABB & 相机
        carBounds.setPosition(carX - carHalf, carY - carHalf);
        camera.position.set(carX, carY, 0f);
        camera.update();

        // ===== 渲染 =====
        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1f);

        renderer.setView(camera);
        renderer.render(); // 地图

        // 车精灵（世界单位）
        var batch = renderer.getBatch();
        batch.begin();
        if (carTex != null) batch.draw(carTex, carX - carHalf, carY - carHalf, carSizeWU, carSizeWU);
        batch.end();

        // 调试绘制：车头三角、碰撞框、S 曲线
        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Line);
        drawCarTriangle(sr, carX, carY, headingRad, 0.6f);

        if (debugDrawCollision) {
            for (Rectangle r : collisionRectsWU) sr.rect(r.x, r.y, r.width, r.height);
        }
        if (debugDrawSCurve) {
            drawBezier(sr, s_p0, s_p1, s_p2, s_p3, 40);
            drawBezier(sr, s_q0, s_q1, s_q2, s_q3, 40);
        }
        sr.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (renderer != null) renderer.dispose();
        if (map != null) map.dispose();
        if (sr != null) sr.dispose();
        if (carTex != null) carTex.dispose();
    }

    // ===== 碰撞判定（世界单位）=====
    private boolean collidesAt(float left, float bottom, float w, float h) {
        carBounds.set(left, bottom, w, h);
        for (Rectangle r : collisionRectsWU) {
            if (carBounds.overlaps(r)) return true;
        }
        return false;
    }

    // ===== 画一个指向 heading 的小三角（调试车头）=====
    private static void drawCarTriangle(ShapeRenderer sr, float x, float y, float heading, float r) {
        Vector2 tip   = new Vector2(x + r * MathUtils.cos(heading), y + r * MathUtils.sin(heading));
        Vector2 left  = new Vector2(x + 0.6f * MathUtils.cos(heading + 2.6f), y + 0.6f * MathUtils.sin(heading + 2.6f));
        Vector2 right = new Vector2(x + 0.6f * MathUtils.cos(heading - 2.6f), y + 0.6f * MathUtils.sin(heading - 2.6f));
        sr.line(left, tip);
        sr.line(tip, right);
        sr.line(right, left);
    }

    // ===== S 曲线：生成、取点、切向、估长、绘制 =====
    private void buildSShape(float x, float y, float heading, float len, float amp) {
        Vector2 dir  = new Vector2(MathUtils.cos(heading), MathUtils.sin(heading)); // 前向
        Vector2 left = new Vector2(-dir.y, dir.x);                                  // 左法向

        // 段1：向左偏再回中
        s_p0 = new Vector2(x, y);
        s_p3 = new Vector2(x, y).mulAdd(dir, 0.5f * len).mulAdd(left, amp);
        s_p1 = new Vector2(x, y).mulAdd(dir, 0.20f * len);      // 起点切线
        s_p2 = new Vector2(s_p3).mulAdd(dir, -0.20f * len);     // 终点切线

        // 段2：继续向前，向右偏再回中
        s_q0 = new Vector2(s_p3);
        s_q3 = new Vector2(x, y).mulAdd(dir, 1.0f * len);       // 回到中线
        s_q1 = new Vector2(s_q0).mulAdd(dir, 0.20f * len);
        s_q2 = new Vector2(s_q3).mulAdd(dir, -0.20f * len).mulAdd(left, -amp * 0.8f);

        s_totalLen = estimateLength(s_p0, s_p1, s_p2, s_p3, 50)
            + estimateLength(s_q0, s_q1, s_q2, s_q3, 50);
        sProgress = 0f;
    }

    private static void bezierPoint(Vector2 a, Vector2 b, Vector2 c, Vector2 d, float t, Vector2 out) {
        float u = 1f - t;
        float x = u*u*u*a.x + 3*u*u*t*b.x + 3*u*t*t*c.x + t*t*t*d.x;
        float y = u*u*u*a.y + 3*u*u*t*b.y + 3*u*t*t*c.y + t*t*t*d.y;
        out.set(x, y);
    }

    private static void bezierTangent(Vector2 a, Vector2 b, Vector2 c, Vector2 d, float t, Vector2 out) {
        float u = 1f - t; // 三次贝塞尔一阶导
        float x = 3*u*u*(b.x - a.x) + 6*u*t*(c.x - b.x) + 3*t*t*(d.x - c.x);
        float y = 3*u*u*(b.y - a.y) + 6*u*t*(c.y - b.y) + 3*t*t*(d.y - c.y);
        out.set(x, y);
    }

    private static float estimateLength(Vector2 a, Vector2 b, Vector2 c, Vector2 d, int samples) {
        float len = 0f;
        Vector2 prev = new Vector2(a);
        Vector2 cur  = new Vector2();
        for (int i = 1; i <= samples; i++) {
            float t = i / (float) samples;
            bezierPoint(a, b, c, d, t, cur);
            len += prev.dst(cur);
            prev.set(cur);
        }
        return len;
    }

    private static void drawBezier(ShapeRenderer sr, Vector2 a, Vector2 b, Vector2 c, Vector2 d, int samples) {
        Vector2 prev = new Vector2(a);
        Vector2 cur  = new Vector2();
        for (int i = 1; i <= samples; i++) {
            float t = i / (float) samples;
            bezierPoint(a, b, c, d, t, cur);
            sr.line(prev, cur);
            prev.set(cur);
        }
    }
}
