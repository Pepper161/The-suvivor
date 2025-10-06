package com.survivor.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class MainMenuScreen implements Screen {
    private final Main game; // Reference the Main class
    private OrthographicCamera camera;
    private Viewport viewport;
    private Stage stage;
    private SpriteBatch batch;
    private Texture backgroundTexture;
    private Texture titleTexture; // Add a texture for the title

    public MainMenuScreen(Main game) {
        this.game = game;

        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camera);
        stage = new Stage(viewport);
        batch = new SpriteBatch();

        Gdx.input.setInputProcessor(stage);

        backgroundTexture = new Texture(Gdx.files.internal("background.png"));
        titleTexture = new Texture(Gdx.files.internal("The_Survivor.png")); // Load the title texture

        createUI();
    }

    private void createUI() {
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        table.left().padLeft(50);

        Texture playTexture = new Texture(Gdx.files.internal("play_button.png"));
        Texture quitTexture = new Texture(Gdx.files.internal("quit_button.png"));

        ImageButton playButton = new ImageButton(new TextureRegionDrawable(playTexture));
        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game)); // Pass the Main instance to GameScreen
                dispose();
            }
        });

        ImageButton quitButton = new ImageButton(new TextureRegionDrawable(quitTexture));
        quitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });

        table.add(playButton).padBottom(20).row();
        table.add(quitButton);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined); // Ensure the batch uses the camera's projection
        batch.begin();

        // Draw the background centered and scaled to fit the viewport
        float backgroundX = 0;
        float backgroundY = 0;
        float backgroundWidth = viewport.getWorldWidth();
        float backgroundHeight = viewport.getWorldHeight();
        batch.draw(backgroundTexture, backgroundX, backgroundY, backgroundWidth, backgroundHeight);

        // Draw the title at the top-right corner
        float titleWidth = 400; // Adjust width as needed
        float titleHeight = 300; // Adjust height as needed
        float titleX = viewport.getWorldWidth() - titleWidth - 20; // 20px padding from the right
        float titleY = viewport.getWorldHeight() - titleHeight + 60; // 20px padding from the top
        batch.draw(titleTexture, titleX, titleY, titleWidth, titleHeight);

        batch.end();

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true); // Ensure the viewport is updated and centered
        camera.position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0); // Center the camera
        camera.update();
    }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        backgroundTexture.dispose();
        titleTexture.dispose(); // Dispose of the title texture
    }

    @Override
    public void show() {
        // Set fullscreen mode
        Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());

        // Update the camera and viewport to ensure the background is positioned correctly
        camera.position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0);
        camera.update();
        viewport.apply();
        Gdx.input.setInputProcessor(stage); // Ensure input is set to the stage
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}
