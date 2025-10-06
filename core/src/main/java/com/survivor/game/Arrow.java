package com.survivor.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Arrow {
    private static final float SPEED = 250f;
    private static final int WIDTH = 32;
    private static final int HEIGHT = 32;

    private Vector2 position;
    private Vector2 velocity;
    private Rectangle bounds;
    private boolean active = true;
    private static Texture texture;

    public Arrow(Vector2 startPosition, Vector2 target) {
        this.position = new Vector2(startPosition);
        this.velocity = new Vector2(target).sub(startPosition).nor().scl(SPEED);
        this.bounds = new Rectangle(position.x, position.y, WIDTH, HEIGHT);

        if (texture == null) {
            texture = new Texture(Gdx.files.internal("Arrow01(32x32).png"));
        }
    }

    public Arrow(float x, float y, float vx, float vy) {
        this.position = new Vector2(x, y);
        this.velocity = new Vector2(vx, vy);
        this.bounds = new Rectangle(position.x, position.y, WIDTH, HEIGHT);

        if (texture == null) {
            texture = new Texture(Gdx.files.internal("Arrow01(32x32).png"));
        }
    }

    public void update(float delta) {
        if (!active) return;

        position.mulAdd(velocity, delta);
        bounds.setPosition(position.x, position.y);

        // 範囲外で無効化
        if (position.x < 0 || position.y < 0 || position.x > 2000 || position.y > 2000) {
            active = false;
        }
    }

    public void render(SpriteBatch batch) {
        if (active && texture != null) {
            batch.draw(texture, position.x, position.y, WIDTH, HEIGHT);
        }
    }

    public boolean isActive() {
        return active;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void checkCollision(Player player) {
        if (active && bounds.overlaps(player.getBounds())) {
            player.takeDamage(10);
            active = false;
        }
    }

    public static void disposeTexture() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
    }
}
