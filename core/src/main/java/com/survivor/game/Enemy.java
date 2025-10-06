package com.survivor.game;

import java.util.List;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public abstract class Enemy extends Entity{
    protected Texture spriteSheet;
    protected Animation<TextureRegion> animation;
    protected TextureRegion currentFrame;
    protected Vector2 position;
    protected Vector2 target;
    protected float stateTime;
    protected float speed;
    protected int damage;
    protected Rectangle bounds;

    // Direction vector for movement
    protected Vector2 direction;

    // Attack radius and cooldown properties
    protected float attackRadius = 20f; // Distance at which enemy stops and attacks
    protected float attackCooldown = 1.0f; // Time between attacks in seconds
    protected float lastAttackTime = 0f;

    public Enemy(Texture spriteSheet, float x, float y, float speed, int damage) {
        super(x, y, 32, 32); // Call the appropriate Entity constructor with required arguments
        this.spriteSheet = spriteSheet;
        this.position = new Vector2(x, y);
        this.speed = speed;
        this.damage = damage;
        this.stateTime = 0f;
        this.direction = new Vector2(0, 0);
        float width = 32;
        float height = 32;
        this.bounds = new Rectangle(x - width / 2f, y - height / 2f, width, height);
    }

    protected abstract boolean isObstacle(int x, int y);

    public void update(float delta, Player player, List<Enemy> enemies) {
        stateTime += delta;

        // Calculate distance to player
        float distanceToPlayer = position.dst(player.getPosition());

        if (distanceToPlayer > attackRadius) {
            // Move toward player if outside attack radius
            moveTowardPlayer(player, delta);
        } else {
            // Attack player if within radius
            attackPlayer(player, delta);
        }

        // Update bounds position
        bounds.setPosition(position.x - bounds.getWidth() / 2f, position.y - bounds.getHeight() / 2f);


        // Update animation frame
        if (animation != null) {
            currentFrame = animation.getKeyFrame(stateTime, true);
        }
    }

    private void moveTowardPlayer(Player player, float delta) {
        // Get player position
        Vector2 playerPos = player.getPosition();
        target = new Vector2(playerPos);

        // Calculate direction vector to player
        direction.set(playerPos).sub(position).nor();

        // Apply simple obstacle avoidance if needed
        if (isObstacleInPath()) {
            avoidObstacle();
        }

        // Move toward player
        position.add(direction.x * speed * delta, direction.y * speed * delta);
    }

    protected void attackPlayer(Player player, float delta) {
        // Attack once per cooldown period
        if (stateTime - lastAttackTime >= attackCooldown) {
            player.takeDamage(damage);
            lastAttackTime = stateTime;
        }
    }

    protected boolean isObstacleInPath() {
        // Override in subclasses to implement obstacle detection
        return false;
    }

    // Enemy.java（変更追加）
    protected int health = 30; // 敵の初期HP
    protected boolean isAlive = true;

    public void takeDamage(int amount) {
        if (!isAlive) return;

        health -= amount;
        if (health <= 0) {
            health = 0;
            isAlive = false;
        }
    }

    public boolean isAlive() {
        return isAlive;
    }

    protected void avoidObstacle() {
        // Simple obstacle avoidance - can be implemented in subclasses
        // This could adjust the direction vector to steer around obstacles
    }

    public abstract void checkCollisionWithPlayerAttack(Rectangle playerAttackBounds);

    @Override
    public void render(SpriteBatch batch) {
        // Check if texture is null before drawing
        if (currentFrame != null) {
            batch.draw(currentFrame,
                position.x - bounds.getWidth() / 2f,
                position.y - bounds.getHeight() / 2f,
                bounds.getWidth(),
                bounds.getHeight());
        } else if (spriteSheet != null) {
            // Fallback to spriteSheet if animation frame is null
            batch.draw(spriteSheet, position.x, position.y);
        } else {
            // Don't attempt to draw if no texture is available
            System.out.println("Warning: Attempted to render enemy with null texture");
        }
    }

    public void dispose() {
        if (spriteSheet != null) {
            spriteSheet.dispose();
        }
    }

    public Vector2 getPosition() {
        return position;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void setAttackRadius(float attackRadius) {
        this.attackRadius = attackRadius;
    }

    public float getAttackRadius() {
        return attackRadius;
    }

    public void setAttackCooldown(float cooldown) {
        this.attackCooldown = cooldown;
    }
}
