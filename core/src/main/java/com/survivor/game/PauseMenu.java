package com.survivor.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class PauseMenu {
    private final Main game; // Reference to the game instance
    private final Texture backgroundTexture;
    private final Texture resumeButtonTexture;
    private final Texture quitButtonTexture;
    private final SpriteBatch batch;

    private boolean resumeSelected = false;
    private boolean quitSelected = false;

    public PauseMenu(Main game) {
        this.game = game; // Initialize the game instance
        batch = new SpriteBatch();
        backgroundTexture = new Texture(Gdx.files.internal("pause_menu.png"));
        resumeButtonTexture = new Texture(Gdx.files.internal("resume_button.png"));
        quitButtonTexture = new Texture(Gdx.files.internal("return_to_main_menu.png"));
    }

    public void render() {
        batch.begin();

        // Draw background
        float backgroundWidth = Gdx.graphics.getWidth() * 0.2f; // 60% of screen width
        float backgroundHeight = Gdx.graphics.getHeight() * 0.4f; // 60% of screen height
        float backgroundX = (Gdx.graphics.getWidth() - backgroundWidth) / 2f; // Center horizontally
        float backgroundY = (Gdx.graphics.getHeight() - backgroundHeight) / 2f; // Center vertically
        batch.draw(backgroundTexture, backgroundX, backgroundY, backgroundWidth, backgroundHeight);

        // Draw buttons
        float buttonWidth = 400;
        float buttonHeight = 160;

        // Adjust button positions relative to the background
        float resumeX = backgroundX + (backgroundWidth - buttonWidth) / 2f;
        float resumeY = backgroundY + backgroundHeight - buttonHeight - 190; // 190px padding from top
        float quitX = backgroundX + (backgroundWidth - buttonWidth) / 2f;
        float quitY = backgroundY + 30; // 30px padding from bottom

        batch.draw(resumeButtonTexture, resumeX, resumeY, buttonWidth, buttonHeight);
        batch.draw(quitButtonTexture, quitX, quitY, buttonWidth, buttonHeight);

        batch.end();

        // Handle button selection
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY(); // Flip Y-axis

        boolean isMouseOverResume = mouseX >= resumeX && mouseX <= resumeX + buttonWidth &&
            mouseY >= resumeY && mouseY <= resumeY + buttonHeight;

        boolean isMouseOverQuit = mouseX >= quitX && mouseX <= quitX + buttonWidth &&
            mouseY >= quitY && mouseY <= quitY + buttonHeight;

        if (Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.LEFT)) {
            if (isMouseOverResume) {
                Gdx.app.log("PauseMenu", "Resume selected");
                resumeSelected = true; // Set flag to resume the game
            } else if (isMouseOverQuit) {
                Gdx.app.log("PauseMenu", "Quit selected");
                quitSelected = true; // Set flag to quit to the main menu
            }
        }
    }

    public boolean isResumeSelected() {
        return resumeSelected;
    }

    public boolean isQuitSelected() {
        return quitSelected;
    }

    public void dispose() {
        batch.dispose();
        backgroundTexture.dispose();
        resumeButtonTexture.dispose();
        quitButtonTexture.dispose();
    }

    public boolean handleSelection() {
        if (resumeSelected) {
            resumeSelected = false; // Reset flag
            return true; // Indicate resume
        } else if (quitSelected) {
            quitSelected = false; // Reset flag
            game.setScreen(new MainMenuScreen(game)); // Return to main menu
        }
        return false; // No action
    }
}
