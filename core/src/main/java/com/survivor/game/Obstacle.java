package com.survivor.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Obstacle {
    private Vector2 position;
    private Rectangle bounds;
    private Texture texture;
    private boolean collidable;
    
    public Obstacle(float x, float y, float width, float height, Texture texture, boolean collidable) {
        this.position = new Vector2(x, y);
        this.bounds = new Rectangle(x, y, width, height);
        this.texture = texture;
        this.collidable = collidable;
    }
    
    public void render(SpriteBatch batch) {
        if (texture != null) {
            batch.draw(texture, position.x, position.y, bounds.width, bounds.height);
        }
    }
    
    public Texture getTexture() {
        return texture;
    }
    
    public Rectangle getBounds() {
        return bounds;
    }
    
    public boolean isCollidable() {
        return collidable;
    }
    
    public void dispose() {
        if (texture != null) {
            texture.dispose();
        }
    }
}
