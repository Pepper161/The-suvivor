package com.survivor.game;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;

public class MiniMap {
    private static final float MINIMAP_SIZE = 150f; // Size of the minimap in pixels
    private static final Color PLAYER_DOT_COLOR = Color.BLUE;
    private static final Color ENEMY_DOT_COLOR = Color.RED;
    private static final Color BORDER_COLOR = Color.WHITE;
    private static final float DOT_SIZE = 4f;
    
    private final OrthographicCamera minimapCamera;
    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final TiledMap map;
    private final Texture minimapTexture;
    private final float mapWidth;
    private final float mapHeight;
    
    public MiniMap(TiledMap map, float mapWidth, float mapHeight) {
        this.map = map;
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        
        minimapCamera = new OrthographicCamera(MINIMAP_SIZE, MINIMAP_SIZE);
        minimapCamera.position.set(mapWidth / 2, mapHeight / 2, 0);
        minimapCamera.update();
        
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        
        // Generate simplified minimap texture from the map
        minimapTexture = generateMinimapTexture();
    }
    
    private Texture generateMinimapTexture() {
        // Create a simple representation of the map
        // In a real implementation, you might want to render the actual map
        // to a FrameBuffer and use that as a texture
        Pixmap pixmap = new Pixmap((int)MINIMAP_SIZE, (int)MINIMAP_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.2f, 0.2f, 0.2f, 0.8f)); // Dark gray background
        pixmap.fill();
        
        // Add some detail to represent the map structure
        // This is just a placeholder - for a real implementation, you would 
        // need to extract this from your actual map data
        pixmap.setColor(new Color(0.3f, 0.3f, 0.3f, 0.8f));
        int padding = 10;
        pixmap.fillRectangle(padding, padding, (int)MINIMAP_SIZE - padding*2, (int)MINIMAP_SIZE - padding*2);
        
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
    
    public void render(SpriteBatch gameBatch, Vector2 playerPosition, List<Enemy> enemies, float screenWidth, float screenHeight) {
        // Position in the top-left corner with some padding
        float mapX = 10; // Left corner
        float mapY = screenHeight - MINIMAP_SIZE - 10; // Top corner
        
        // Use a separate projection matrix for the minimap UI
        OrthographicCamera uiCamera = new OrthographicCamera(screenWidth, screenHeight);
        uiCamera.position.set(screenWidth / 2, screenHeight / 2, 0);
        uiCamera.update();
        
        // Draw minimap background with SpriteBatch
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        batch.draw(minimapTexture, mapX, mapY);
        batch.end();
        
        // Calculate scaling factor between world coordinates and minimap coordinates
        float scaleX = MINIMAP_SIZE / mapWidth;
        float scaleY = MINIMAP_SIZE / mapHeight;
        
        // Draw dots with ShapeRenderer - ensure we're not already in a batch
        try {
            shapeRenderer.setProjectionMatrix(uiCamera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            
            // Draw player dot (blue)
            shapeRenderer.setColor(PLAYER_DOT_COLOR);
            float playerX = mapX + playerPosition.x * scaleX - DOT_SIZE/2;
            float playerY = mapY + playerPosition.y * scaleY - DOT_SIZE/2;
            shapeRenderer.rect(playerX, playerY, DOT_SIZE, DOT_SIZE);
            
            // Draw enemy dots (red)
            shapeRenderer.setColor(ENEMY_DOT_COLOR);
            for (Enemy enemy : enemies) {
                Vector2 enemyPos = enemy.getPosition();
                float enemyX = mapX + enemyPos.x * scaleX - DOT_SIZE/2;
                float enemyY = mapY + enemyPos.y * scaleY - DOT_SIZE/2;
                shapeRenderer.rect(enemyX, enemyY, DOT_SIZE, DOT_SIZE);
            }
            
            shapeRenderer.end();
            
            // Draw border around minimap
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(BORDER_COLOR);
            shapeRenderer.rect(mapX, mapY, MINIMAP_SIZE, MINIMAP_SIZE);
            shapeRenderer.end();
        } catch (IllegalStateException e) {
            // Just in case we're interrupting another shape rendering
            System.err.println("ShapeRenderer error in MiniMap: " + e.getMessage());
        }
    }
    
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        minimapTexture.dispose();
    }
}
