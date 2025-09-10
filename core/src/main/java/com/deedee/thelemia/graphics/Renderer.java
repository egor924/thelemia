package com.deedee.thelemia.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.deedee.thelemia.event.EventBus;
import com.deedee.thelemia.event.common.*;
import com.deedee.thelemia.scene.GameSystem;
import com.deedee.thelemia.scene.component.*;

import java.util.ArrayList;
import java.util.List;

public class Renderer extends GameSystem {
    private final RenderListener listener = new RenderListener(this);
    private final Color defaultBackground;

    private final Stage stage;
    private final Table root = new Table();

    private final Camera camera;
    private final SpriteBatch batch = new SpriteBatch();

    private final ShaderManager shaderManager = new ShaderManager();
    private OrthogonalTiledMapRenderer mapRenderer;

    private final List<AnimatedSpriteComponent> spriteComponents = new ArrayList<>();
    private final List<ParticlesComponent> particlesComponents = new ArrayList<>();
    private Transition nextTransition;

    public Renderer(Color backgroundColor) {
        this.defaultBackground = backgroundColor;
        camera = new Camera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage = new Stage(camera.getViewport(), batch);
        subscribeListener();

        batch.setProjectionMatrix(camera.getProjectionMatrix());
        root.setFillParent(true);
        stage.addActor(root);

        EventBus.getInstance().post(new AssignStageEvent(stage));
    }
    public Renderer() {
        this.defaultBackground = Color.WHITE;
        camera = new Camera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage = new Stage(camera.getViewport(), batch);
        subscribeListener();

        batch.setProjectionMatrix(camera.getProjectionMatrix());
        root.setFillParent(true);
        stage.addActor(root);

        EventBus.getInstance().post(new AssignStageEvent(stage));
    }

    @Override
    public void subscribeListener() {
        EventBus.getInstance().subscribe(RenderFragmentEvent.class, listener);
        EventBus.getInstance().subscribe(RenderAnimatedSpriteEvent.class, listener);
        EventBus.getInstance().subscribe(ChangeMapEvent.class, listener);
        EventBus.getInstance().subscribe(RenderParticlesEvent.class, listener);
        EventBus.getInstance().subscribe(ChangeTransitionEvent.class, listener);
        EventBus.getInstance().subscribe(FinishTransitionEvent.class, listener);
        EventBus.getInstance().subscribe(ApplyShaderEvent.class, listener);
        EventBus.getInstance().subscribe(ResetShaderEvent.class, listener);
    }
    @Override
    public void update(float delta) {
        clearScreen(null);

        camera.update(delta);
        batch.setProjectionMatrix(camera.getProjectionMatrix());

        if (mapRenderer != null) {
            mapRenderer.setView(camera.getInternalCamera());
            mapRenderer.render();
        }

        // Flush the sprite batch
        batch.begin();

        for (AnimatedSpriteComponent spriteComponent : spriteComponents) {
            AnimatedSprite sprite = spriteComponent.getRenderableObject();
            if (!sprite.isLoaded()) continue;

            sprite.update(delta);
            drawAnimatedSprite(spriteComponent);
        }
        for (ParticlesComponent particlesComponent : particlesComponents) {
            Particles particles = particlesComponent.getRenderableObject();
            if (!particles.isLoaded()) continue;

            particles.update(delta);
            drawParticles(particlesComponent);
        }

        if (nextTransition != null && !nextTransition.isFinished()) {
            nextTransition.draw(batch);
        }

        batch.end();

        stage.act(delta);
        stage.draw();
    }
    @Override
    public void dispose() {
        batch.dispose();
        camera.dispose();
        stage.dispose();
        shaderManager.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
    }
    @Override
    public RenderListener getListener() {
        return listener;
    }

    public void addWidget(WidgetComponent widgetComponent) {
        root.add(widgetComponent.getRenderableObject().getWidget());
    }
    public void addAnimatedSprite(AnimatedSpriteComponent spriteComponent) {
        spriteComponents.add(spriteComponent);
    }
    public void addParticles(ParticlesComponent particlesComponent) {
        particlesComponents.add(particlesComponent);
    }
    public void changeTileMap(TileMapComponent tileMapComponent, float unitScale) {
        if (mapRenderer != null) mapRenderer.dispose();

        TileMap tileMap = tileMapComponent.getRenderableObject();
        mapRenderer = new OrthogonalTiledMapRenderer(tileMap.getTiledMap(), unitScale, batch);
    }

    public void drawAnimatedSprite(AnimatedSpriteComponent spriteComponent) {
        AnimatedSprite sprite = spriteComponent.getRenderableObject();
        TransformComponent transform = spriteComponent.getOwner().getComponentByType(TransformComponent.class);

        Vector2 position = transform.getPosition();
        Vector2 origin = transform.getOrigin();
        Vector2 scale = transform.getScale();
        float rotation = transform.getRotation();

        sprite.draw(batch, position.x, position.y, origin.x, origin.y, scale.x, scale.y, rotation);
    }
    public void drawParticles(ParticlesComponent particlesComponent) {
        particlesComponent.getRenderableObject().draw(batch);
    }

    public void changeNextTransition(Transition nextTransition) {
        this.nextTransition = nextTransition;
    }
    public void finishCurrentTransition() {
        if (nextTransition != null) nextTransition.finish();
    }

    public void loadShader(String name, String vertexPath, String fragmentPath) {
        shaderManager.loadShader(name, vertexPath, fragmentPath);
    }
    public void applyShader(String name) {
        ShaderProgram currentShader = shaderManager.applyShader(name);
        batch.setShader(currentShader);
    }
    public void resetShader() {
        batch.setShader(null);
    }

    public void clearScreen(Color color) {
        if (color == null) {
            Gdx.gl.glClearColor(defaultBackground.r, defaultBackground.g, defaultBackground.b, defaultBackground.a);
        } else {
            Gdx.gl.glClearColor(color.r, color.g, color.b, color.a);
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    public Stage getStage() {
        return stage;
    }
    public Table getRoot() {
        return root;
    }
    public Camera getCamera() {
        return camera;
    }
    public SpriteBatch getBatch() {
        return batch;
    }
    public OrthogonalTiledMapRenderer getMapRenderer() {
        return mapRenderer;
    }

}
