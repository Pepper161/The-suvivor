package com.survivor.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/**
 * Utility class for drawing hitboxes and collision boundaries
 */
public class HitboxRenderer {
    private static ShapeRenderer shapeRenderer;

    /**
     * Begins rendering in line mode
     */
    public static void begin(OrthographicCamera camera) {
        if (shapeRenderer == null) {
            shapeRenderer = new ShapeRenderer();
        }
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
    }

    /**
     * Draws a rectangle outline with the specified color
     * 
     * @param rect The rectangle to draw
     * @param color The color to use
     */
    public static void drawRect(Rectangle rect, Color color) {
        if (shapeRenderer != null) {
            shapeRenderer.setColor(color);
            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
        }
    }

    /**
     * Ends rendering
     */
    public static void end() {
        if (shapeRenderer != null) {
            shapeRenderer.end();
        }
    }

    /**
     * Disposes of the shape renderer
     */
    public static void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }
    }
}
