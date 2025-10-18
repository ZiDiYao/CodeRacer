package com.zidi.CodeRacer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;

    private TiledMap map;
    private OrthographicCamera camera;
    private OrthogonalTiledMapRenderer renderer;
    private Viewport viewport;

    // --- 车辆位置和运动参数 ---
    private float carX, carY;
    private float carSpeed;
    private final float TILE_SIZE = 16f;
    private final float UNIT_SCALE = 1 / TILE_SIZE;
    // 调试：暂时放大视口，确保地图可见
    private final float WORLD_WIDTH = 50f;

    @Override
    public void create() {
        // --- 1. 地图和渲染器初始化 ---
        map = new TmxMapLoader().load("Maps/MVP_Map01.tmx");
        renderer = new OrthogonalTiledMapRenderer(map, UNIT_SCALE);

        // --- 2. 相机和视口初始化 ---
        camera = new OrthographicCamera();
        // 设置一个较大的视口（50个世界单位），更容易看到地图
        viewport = new FitViewport(WORLD_WIDTH, WORLD_WIDTH * Gdx.graphics.getHeight() / Gdx.graphics.getWidth(), camera);

        // 默认将相机位置设置在地图中央
        float mapWidth = map.getProperties().get("width", Integer.class) * TILE_SIZE * UNIT_SCALE;
        float mapHeight = map.getProperties().get("height", Integer.class) * TILE_SIZE * UNIT_SCALE;

        // --- 3. 游戏资源和初始位置 ---
        batch = new SpriteBatch();
        image = new Texture("tile_0483.png");
        carSpeed = 10f; // 车辆移动速度（世界单位/秒）

        // 车辆初始位置设在地图中央
        carX = mapWidth / 2;
        carY = mapHeight / 2;
    }

    /** 处理车辆移动的逻辑 (模拟 go_straight, turn_left 等) */
    private void handleInput(float delta) {
        float moveDistance = carSpeed * delta;

        // W/S 控制前进后退 (Y轴)
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            carY += moveDistance;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            carY -= moveDistance;
        }

        // A/D 控制左右移动 (X轴)
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            carX -= moveDistance;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            carX += moveDistance;
        }
    }

    @Override
    public void render() {
        // 获取自上次渲染以来的时间，用于帧率独立移动
        float delta = Gdx.graphics.getDeltaTime();

        // 处理键盘输入，移动车辆
        handleInput(delta);

        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1);

        // --- 1. 相机跟随车辆 ---
        camera.position.set(carX, carY, 0);
        camera.update();

        // --- 2. 渲染地图 ---
        renderer.setView(camera);
        renderer.render();

        // --- 3. 渲染车辆 ---
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        // 绘制车辆，尺寸转换成世界单位
        batch.draw(image, carX - TILE_SIZE * UNIT_SCALE / 2, carY - TILE_SIZE * UNIT_SCALE / 2, TILE_SIZE * UNIT_SCALE, TILE_SIZE * UNIT_SCALE);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        // 处理窗口大小变化时的屏幕适配
        viewport.update(width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
        map.dispose();
        renderer.dispose();
    }
}
