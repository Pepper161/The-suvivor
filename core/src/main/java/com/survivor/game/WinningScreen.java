package com.survivor.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class WinningScreen implements Screen {
    private final Main game;
    private final GameScreen gameScreen; // Reference to the GameScreen
    private final Stage stage;
    private final SpriteBatch batch;
    private final Texture congratulationImage;

    public WinningScreen(Main game, GameScreen gameScreen) {
        this.game = game;
        this.gameScreen = gameScreen; // Initialize the GameScreen
        this.stage = new Stage(new FitViewport(800, 600));
        this.batch = new SpriteBatch();
        this.congratulationImage = new Texture(Gdx.files.internal("congratulation.png"));

        Gdx.input.setInputProcessor(stage);
        createUI();
    }

    private void createUI() {
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Texture playAgainTexture = new Texture(Gdx.files.internal("Play_Again.png"));
        Texture returnTexture = new Texture(Gdx.files.internal("return_to_main_menu.png"));

        ImageButton playAgainButton = new ImageButton(new TextureRegionDrawable(playAgainTexture));
        playAgainButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game)); // Properly initialize a new GameScreen
                dispose(); // Dispose of the WinningScreen resources
            }
        });
        playAgainButton.setSize(150, 50); // Set button size

        ImageButton returnButton = new ImageButton(new TextureRegionDrawable(returnTexture));
        returnButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });
        returnButton.setSize(150, 50); // Set button size

        // Add buttons to the table with padding
        table.add(playAgainButton).size(playAgainButton.getWidth(), playAgainButton.getHeight()).padTop(20).padBottom(20).padRight(20);
        table.add(returnButton).size(returnButton.getWidth(), returnButton.getHeight()).padTop(20).padBottom(20).padLeft(20);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1); // Clear the screen with a black background
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        // Set the size and vertical padding for the congratulation image
        float imageWidth = 1900; // Set desired width
        float imageHeight = 1600; // Set desired height
        float verticalPadding = -180; // Vertical padding
        float horizontalPadding = -50;
        float imageX = (Gdx.graphics.getWidth() - imageWidth) / 2f - horizontalPadding;
        float imageY = (Gdx.graphics.getHeight() - imageHeight) / 2f - verticalPadding;
        batch.draw(congratulationImage, imageX, imageY, imageWidth, imageHeight);
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        congratulationImage.dispose();
    }

    @Override
    public void show() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}
