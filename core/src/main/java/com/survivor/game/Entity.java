package com.survivor.game;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Base class for all game entities (Player, Enemy, etc.)
 */
public abstract class Entity {
    protected Vector2 position;
    protected Rectangle bounds;
    protected float speed;
    protected int health;
    
    public Entity(float x, float y, float width, float height) {
        position = new Vector2(x, y);
        bounds = new Rectangle(x, y, width, height);
        speed = 100; // Default speed
        health = 100; // Default health
    }
    
    public abstract void update(float delta);
    
    public abstract void render(SpriteBatch batch);
    
    public Vector2 getPosition() {
        return position;
    }
    
    public void setPosition(Vector2 position) {
        this.position = position;
        // Add this line to update bounds
        this.bounds.setPosition(position.x, position.y);
    }
    
    public void setPosition(float x, float y) {
        this.position.x = x;
        this.position.y = y;
        // Add this line to update bounds
        this.bounds.setPosition(x, y);
    }
    
    public Rectangle getBounds() {
        return bounds;
    }
    
    public int getHealth() {
        return health;
    }
    
    public boolean isAlive() {
        return health > 0;
    }
    
    protected void updateBounds() {
        bounds.setPosition(position);
    }
    
    public abstract void dispose();
}
