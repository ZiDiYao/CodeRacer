package com.zidi.CodeRacer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Main extends ApplicationAdapter {

    private SpriteBatch batch;
    private Texture image;

    private TiledMap map;
    private OrthographicCamera camera;
    private OrthogonalTiledMapRenderer renderer;
    private Viewport viewport;

    private float carX, carY;
    private float carSpeed;

    private static final float TILE_SIZE = 16f;          // pixels per tile
    private static final float UNIT_SCALE = 1f / TILE_SIZE; // world units per pixel (so 16px=1 world unit)

    // World size (in world units) shown by the camera. Tweak to taste.
    private static final float WORLD_WIDTH = 20f;
    private static final float WORLD_HEIGHT = 12f;

    private Rectangle carBounds;
    private float carSizeWU;  // car size in world units (here = 1.0)
    private float carOffset;  // half size = 0.5

    // Pre-scaled collision rectangles (in world units)
    private final Array<Rectangle> collisionRectsWU = new Array<>();

    @Override
    public void create() {
        // 1) Graphics / camera / viewport
        batch = new SpriteBatch();
        image = new Texture(Gdx.files.internal("tile_0483.png")); // make sure this path exists
        carSpeed = 6.0f; // world units per second (6 tiles/sec)

        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply(true);

        // center camera initially
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0f);
        camera.update();

        // 2) Map + renderer
        map = new TmxMapLoader().load("Maps/MVP_Map01.tmx");
        renderer = new OrthogonalTiledMapRenderer(map, UNIT_SCALE); // renders in world units

        // 3) Car spawn (defaults)
        int tilesW = map.getProperties().get("width", Integer.class);
        int tilesH = map.getProperties().get("height", Integer.class);
        float mapWidthWU = tilesW * (TILE_SIZE * UNIT_SCALE);   // = tilesW * 1
        float mapHeightWU = tilesH * (TILE_SIZE * UNIT_SCALE);  // = tilesH * 1

        carX = mapWidthWU / 2f;
        carY = mapHeightWU / 2f;

        MapLayer spawnLayer = map.getLayers().get("SpawnPoints");
        if (spawnLayer != null && spawnLayer.getObjects().getCount() > 0) {
            // Use the first object’s x/y if present
            MapObject o = spawnLayer.getObjects().get(0);
            Float px = o.getProperties().get("x", Float.class);
            Float py = o.getProperties().get("y", Float.class);
            if (px != null && py != null) {
                // convert pixels -> world units
                carX = px * UNIT_SCALE;
                carY = py * UNIT_SCALE;
            }
        }

        // 4) Car bounds (use car center semantics)
        carSizeWU = TILE_SIZE * UNIT_SCALE; // = 1.0
        carOffset = carSizeWU / 2f;         // = 0.5
        carBounds = new Rectangle(carX - carOffset, carY - carOffset, carSizeWU, carSizeWU);

        // 5) Pre-scale collision rectangles to world units (so they match renderer + carBounds)
        MapLayer collisionLayer = map.getLayers().get("Collision");
        if (collisionLayer != null) {
            for (MapObject obj : collisionLayer.getObjects()) {
                if (obj instanceof RectangleMapObject rectObj) {
                    Rectangle rPx = rectObj.getRectangle(); // pixel space
                    // scale to world units
                    Rectangle rWU = new Rectangle(
                        rPx.x * UNIT_SCALE,
                        rPx.y * UNIT_SCALE,
                        rPx.width * UNIT_SCALE,
                        rPx.height * UNIT_SCALE
                    );
                    collisionRectsWU.add(rWU);
                }
            }
        }

        // move camera to spawn
        camera.position.set(carX, carY, 0f);
        camera.update();
    }

    private boolean collidesAt(float newX, float newY, float widthWU, float heightWU) {
        // temp bounds at target location
        carBounds.set(newX, newY, widthWU, heightWU);
        for (Rectangle r : collisionRectsWU) {
            if (carBounds.overlaps(r)) return true;
        }
        return false;
    }

    private void handleInput(float delta) {
        float move = carSpeed * delta;

        float newX = carX;
        float newY = carY;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) newX -= move;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) newX += move;

        // try X
        if (!collidesAt(newX - carOffset, carY - carOffset, carSizeWU, carSizeWU)) {
            carX = newX;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.W)) newY += move;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) newY -= move;

        // try Y (note we use updated carX)
        if (!collidesAt(carX - carOffset, newY - carOffset, carSizeWU, carSizeWU)) {
            carY = newY;
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        handleInput(delta);

        // keep carBounds in sync (left-bottom from center)
        carBounds.setPosition(carX - carOffset, carY - carOffset);

        // camera follow
        camera.position.set(carX, carY, 0f);
        camera.update();

        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1f);

        // draw map
        renderer.setView(camera);
        renderer.render();

        // draw car (left-bottom from center)
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(image, carX - carOffset, carY - carOffset, carSizeWU, carSizeWU);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (image != null) image.dispose();
        if (renderer != null) renderer.dispose();
        if (map != null) map.dispose();
    }
}
