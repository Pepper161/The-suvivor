package com.survivor.game;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;


public class BasicEnemy extends Enemy {

    private static final float DEFAULT_SPEED = 100f;
    private static final int DEFAULT_DAMAGE = 5;
    private static final float ATTACK_RADIUS = 50f;
    private static final float ATTACK_COOLDOWN = 1.0f;

    private float lastAttackTime = 0f;
    private Animation<TextureRegion> deathAnimation, attackAnimation;
    private boolean isDying = false;
    private float deathTimer = 0f;
    private float attackAnimTimer = 0f;
    private boolean isAttacking = false;
    private static final float DEATH_DURATION = 0.6f; // Duration of death animation
    private final List<Obstacle> obstacles;

    public BasicEnemy(float x, float y, List<Obstacle> obstacles) {
        super(new Texture(Gdx.files.internal("Mushroom-Run.png")), x, y, DEFAULT_SPEED, DEFAULT_DAMAGE);
        this.obstacles = obstacles; // 追加
        initAnimation();
    }

    private void initAnimation() {
        try {
            TextureRegion[][] tempFrames = TextureRegion.split(spriteSheet, 80, 64);

            // Movement animation (row 0)
            if (tempFrames.length > 0 && tempFrames[0].length >= 8) {
                animation = new Animation<>(0.1f, tempFrames[0][0], tempFrames[0][1], tempFrames[0][2], tempFrames[0][3],
                    tempFrames[0][4], tempFrames[0][5], tempFrames[0][6], tempFrames[0][7]);
                currentFrame = tempFrames[0][0];
            } else {
                currentFrame = new TextureRegion(spriteSheet);
            }

            // Death animation (row 1)
            if (tempFrames.length > 1 && tempFrames[1].length >= 4) {
                deathAnimation = new Animation<>(0.15f, tempFrames[1][0], tempFrames[1][1], tempFrames[1][2], tempFrames[1][3]);
            }// Load attack sprite sheet separately
            Texture attackSheet = new Texture(Gdx.files.internal("Mushroom-Attack.png"));
            TextureRegion[][] attackFrames = TextureRegion.split(attackSheet, 80, 64); // adjust if necessary

            Array<TextureRegion> attackRegions = new Array<>();
            for (int i = 0; i < attackFrames[0].length; i++) {
                attackRegions.add(attackFrames[0][i]);
            }

            attackAnimation = new Animation<>(0.02f, attackRegions);

            bounds.setSize(30, 30); // Adjust collision size
            bounds.setPosition(position.x - bounds.getWidth() / 2, position.y - bounds.getHeight() / 2); // Center the hitbox

        } catch (Exception e) {
            Gdx.app.error("BasicEnemy", "Error setting up animation", e);
            currentFrame = new TextureRegion(spriteSheet);
        }
    }


    private boolean isObstacleAt(Vector2 newPos) {
        Rectangle futureBounds = new Rectangle(newPos.x, newPos.y, bounds.getWidth(), bounds.getHeight());

        for (Obstacle obs : obstacles) {
            if (obs.isCollidable() && obs.getBounds().overlaps(futureBounds)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isObstacle(int x, int y) {
        return false;
    }

    public void update(float delta, Player player, List<Enemy> allEnemies) {
        stateTime += delta;

        if (!isAlive) {
            if (deathAnimation != null) {
                deathTimer += delta;
                if (deathTimer >= DEATH_DURATION) {
                    // Animation over
                }
                currentFrame = deathAnimation.getKeyFrame(deathTimer, false);
            }
            return;
        }

        // Center the hitbox around the enemy's position
        bounds.setPosition(position.x - bounds.getWidth() / 2, position.y - bounds.getHeight() / 2);

        if (detectPlayer(player)) {
            navigateTo(player, delta, allEnemies);
        }

        updateAnimation(delta);

    }

    private boolean detectPlayer(Player player) {
        return true; // Always track the player (no line-of-sight logic yet)
    }

    private void navigateTo(Player player, float delta, List<Enemy> allEnemies) {
        Vector2 playerPos = player.getPosition();
        Vector2 direction = new Vector2(playerPos).sub(position).nor();

        Vector2 newPos = new Vector2(position).add(direction.x * speed * delta, direction.y * speed * delta);

        for (Enemy other : allEnemies) {
            if (other != this && other.isAlive() && newPos.dst(other.getPosition()) < 20f) {
                // Adjust direction to avoid collision and surround the player
                Vector2 avoidanceDirection = new Vector2(other.getPosition()).sub(position).nor().scl(-1);
                direction.add(avoidanceDirection).nor();
                newPos = new Vector2(position).add(direction.x * speed * delta, direction.y * speed * delta);
                break;
            }
        }

        position.set(newPos);

        if (position.dst(playerPos) <= ATTACK_RADIUS && canAttack(delta)) {
            attack(player);
        }
    }

    private boolean canAttack(float delta) {
        lastAttackTime += delta;
        if (lastAttackTime >= ATTACK_COOLDOWN) {
            lastAttackTime = 0f;
            return true;
        }
        return false;
    }

    private void attack(Player player) {
        player.takeDamage(DEFAULT_DAMAGE);
        isAttacking = true;
        attackAnimTimer = 0f;
    }

    private void updateAnimation(float delta) {
        if (isAttacking) {
            attackAnimTimer += delta;
            currentFrame = attackAnimation.getKeyFrame(attackAnimTimer, false);

            // When animation finishes, return to idle/run
            if (attackAnimation.isAnimationFinished(attackAnimTimer)) {
                isAttacking = false;
                stateTime = 0;
            }
        } else {
            if (animation != null) {
                stateTime += delta;
                currentFrame = animation.getKeyFrame(stateTime, true);
            }
        }
    }

    @Override
    public void update(float delta) {
        // Center the hitbox around the enemy's position
        bounds.setPosition(position.x - bounds.getWidth() / 2, position.y - bounds.getHeight() / 2);
    }

    @Override
    public void takeDamage(int amount) {
        if (!isAlive) return;

        health -= amount;
        if (health <= 0) {
            health = 0;
            isAlive = false;
            isDying = true;
            deathTimer = 0f;
            stateTime = 0f;
        }
    }

    @Override
    public void checkCollisionWithPlayerAttack(Rectangle playerAttackBounds) {
        if (bounds.overlaps(playerAttackBounds)) {
            takeDamage(15);
        }
    }


    public boolean isDying() {
        return isDying;
    }

    public float getDeathTimer() {
        return deathTimer;
    }

    public float getDeathDuration() {
        return DEATH_DURATION;
    }
}
