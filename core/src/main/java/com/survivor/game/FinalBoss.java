package com.survivor.game;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class FinalBoss extends Enemy {
    private static final float DEFAULT_SPEED = 30f;
    private static final int DEFAULT_DAMAGE = 30;
    private static final float BOSS_SCALE = 2f; // Half the previous size

    private int health = 500;
    private final int maxHealth = 500;

    private Animation<TextureRegion> walkAnimation, attackAnimation, deathAnimation, hitAnimation;
    private float stateTimer = 0f;
    private float attackTimer = 0f;
    private float attackCooldown = 0f;
    private static final float ATTACK_INTERVAL = 4f;
    private boolean isAttacking = false;

    private static Texture pixelTexture;
    private BitmapFont font = new BitmapFont();
    private boolean isDying = false;
    private float deathTimer = 0f;
    private static final float DEATH_DURATION = 2.0f; // Adjust duration as needed

    private Vector2 direction;

    public FinalBoss(float x, float y) {
        super(new Texture(Gdx.files.internal("FinalBoss-Run.png")), x, y, DEFAULT_SPEED, DEFAULT_DAMAGE);
        loadAnimations();
        setSpawnPosition(611, 1094); // Set default spawn position
        bounds.setSize(150 * BOSS_SCALE, 150 * BOSS_SCALE);

        // Initialize direction vector to avoid null issues
        direction = new Vector2(0, 0);

        if (pixelTexture == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(1, 1, 1, 1);
            pixmap.fill();
            pixelTexture = new Texture(pixmap);
            pixmap.dispose();
        }
    }

    private void loadAnimations() {
        TextureRegion[][] runFrames = TextureRegion.split(new Texture(Gdx.files.internal("FinalBoss-Run.png")), 150, 150);
        TextureRegion[][] attackFrames = TextureRegion.split(new Texture(Gdx.files.internal("FinalBoss-Attack.png")), 150, 150);
        TextureRegion[][] deathFrames = TextureRegion.split(new Texture(Gdx.files.internal("FinalBoss-Death.png")), 150, 150);
        TextureRegion[][] hitFrames = TextureRegion.split(new Texture(Gdx.files.internal("FinalBoss-Take-Hit.png")), 150, 150);

        walkAnimation = new Animation<>(0.3f, flatten(runFrames));
        attackAnimation = new Animation<>(0.2f, flatten(attackFrames));
        deathAnimation = new Animation<>(0.2f, flatten(deathFrames));
        hitAnimation = new Animation<>(0.1f, flatten(hitFrames));

        currentFrame = walkAnimation.getKeyFrame(0);
    }

    private Array<TextureRegion> flatten(TextureRegion[][] frames) {
        Array<TextureRegion> result = new Array<>();
        for (TextureRegion[] row : frames) {
            for (TextureRegion frame : row) {
                result.add(frame);
            }
        }
        return result;
    }

    @Override
    public void update(float delta, Player player, List<Enemy> enemies) {
        stateTimer += delta;
        attackCooldown -= delta;

        // Calculate direction towards player
        Vector2 playerPos = player.getPosition();
        direction.set(playerPos).sub(position).nor(); // Always update direction

        if (isDying) {
            deathTimer += delta;
            currentFrame = deathAnimation.getKeyFrame(deathTimer, false);

            if (deathTimer >= DEATH_DURATION) {
                isDying = false; // Optional: avoid looping
                isAlive = false; // Set this flag to ensure removal by GameScreen
            }
            return;
        }

        // Movement logic: Always move towards the player
        if (!isAttacking) {
            position.add(direction.x * speed * delta, direction.y * speed * delta);
        }

        // Attack logic
        float distance = position.dst(playerPos);
        if (isAttacking) {
            attackTimer += delta;
            currentFrame = attackAnimation.getKeyFrame(attackTimer, false);

            // Check if attack animation is at the last frame and player is within range
            if (attackAnimation.isAnimationFinished(attackTimer) && distance <= 60) {
                player.takeDamage(damage);
            }

            if (attackAnimation.isAnimationFinished(attackTimer)) {
                isAttacking = false;
                attackTimer = 0;
            }
        } else if (distance <= 60 && attackCooldown <= 0f) {
            isAttacking = true;
            attackCooldown = ATTACK_INTERVAL;
        } else {
            currentFrame = walkAnimation.getKeyFrame(stateTimer, true);
        }

        bounds.setPosition(position.x - bounds.getWidth() / 2, position.y - bounds.getHeight() / 2);
    }

    @Override
    public void render(SpriteBatch batch) {
        if (currentFrame != null) {
            batch.draw(currentFrame,
                position.x - bounds.getWidth() / 2f,
                position.y - bounds.getHeight() / 2f,
                bounds.getWidth(), bounds.getHeight());


            Matrix4 originalMatrix = batch.getProjectionMatrix().cpy();
            Matrix4 uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.setProjectionMatrix(uiMatrix);

            float healthPercent = (float) health / maxHealth;
            float barWidth = 600;
            float barHeight = 20;
            float barX = (Gdx.graphics.getWidth() - barWidth) / 2f;
            float barY = Gdx.graphics.getHeight() - 150;

            batch.setColor(1, 0, 0, 1);
            batch.draw(pixelTexture, barX, barY, barWidth, barHeight);
            batch.setColor(0, 1, 0, 1);
            batch.draw(pixelTexture, barX, barY, barWidth * healthPercent, barHeight);
            batch.setColor(1, 1, 1, 1);

            font.getData().setScale(1.5f);
            font.draw(batch, "Final Boss HP: " + health, barX, barY + 45);

            batch.setProjectionMatrix(originalMatrix);
        }
    }

    @Override
    public void checkCollisionWithPlayerAttack(Rectangle playerAttackBounds) {
        if (bounds.overlaps(playerAttackBounds) && GameScreen.getPlayer().isAlive()) {
            takeDamage(50); // Deduct 50 HP when attacked by the player
        }
    }

    public void takeDamage(int damage) {
        if (isDying || health <= 0) return;

        health -= damage;
        if (health <= 0) {
            health = 0;
            isDying = true;
            deathTimer = 0f;
            stateTimer = 0f;
        }
    }

    public int getHealth() {
        return health;
    }

    public boolean isAlive() {
        return health > 0 || isDying;
    }

    public void setSpawnPosition(float x, float y) {
        position.set(x, y);
        bounds.setPosition(x - bounds.getWidth() / 2f, y - bounds.getHeight() / 2f);
    }

    @Override
    protected boolean isObstacle(int x, int y) {
        return false; // Allow the boss to walk through obstacles
    }

    @Override
    public void update(float delta) {
        // Not used
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    public static void disposeStaticResources() {
        if (pixelTexture != null) {
            pixelTexture.dispose();
            pixelTexture = null;
        }
    }
}
