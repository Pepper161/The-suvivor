package com.survivor.game;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class MapManager {
    private TiledMap map;
    private Array<Rectangle> obstacles;

    public MapManager(String mapPath) {
        // Load the Tiled map
        map = new TmxMapLoader().load(mapPath);
        
        // Load obstacles
        obstacles = new Array<>();
        loadObstacles();
    }

    private void loadObstacles() {
        // The layer containing objects (either an ObjectLayer or a TileLayer)
        MapLayer objectLayer = map.getLayers().get("Objects"); // Try the standard object layer name
        
        if (objectLayer != null) {
            for (MapObject object : objectLayer.getObjects()) {
                // Get object name (may be null if not set)
                String objectName = object.getName();
                
                // Check if this object should be an obstacle based on its name
                boolean isObstacle = false;
                if (objectName != null) {
                    // Make obstacles from objects with specific names (customize this list)
                    isObstacle = objectName.contains("Wall") || 
                                 objectName.contains("Obstacle");
                }
                
                // You can also check the object's type property
                String objectType = object.getProperties().get("type", String.class);
                if (objectType != null && objectType.equals("obstacle")) {
                    isObstacle = true;
                }
                
                // If we couldn't determine by name/type, assume all objects are obstacles
                if (objectName == null && objectType == null) {
                    isObstacle = true;
                }
                
                if (isObstacle) {
                    float x = object.getProperties().get("x", Float.class);
                    float y = object.getProperties().get("y", Float.class);
                    float width = object.getProperties().get("width", Float.class);
                    float height = object.getProperties().get("height", Float.class);
    
                    obstacles.add(new Rectangle(x, y, width, height));
                    
                    // System.out.println("Added obstacle: " + 
                    //     (objectName != null ? objectName : "unnamed") + 
                    //     " at " + x + "," + y);
                }
            }
        } else {
            System.out.println("No object layers found for obstacles");
        }
        
        System.out.println("Total obstacles loaded: " + obstacles.size);
    }
    
    /**
     * Checks if a rectangle (player) collides with any obstacle
     * @param rectangle The rectangle representing the player's bounds
     * @return true if collision detected, false otherwise
     */
    public boolean collidesWithObstacle(Rectangle rectangle) {
        for (Rectangle obstacle : obstacles) {
            if (rectangle.overlaps(obstacle)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a move to a new position would cause a collision
     * @param x The potential new x position
     * @param y The potential new y position
     * @param width The width of the player
     * @param height The height of the player
     * @return true if the move is valid (no collision), false otherwise
     */
    public boolean isValidMove(float x, float y, float width, float height) {
        Rectangle playerBounds = new Rectangle(x, y, width, height);
        return !collidesWithObstacle(playerBounds);
    }

    public TiledMap getMap() {
        return map;
    }

    public Array<Rectangle> getObstacles() {
        return obstacles;
    }
}
