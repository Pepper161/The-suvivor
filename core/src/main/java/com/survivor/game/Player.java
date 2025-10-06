package com.survivor.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Logger;

public class Player extends Entity {
    // Constants
    private static final Logger logger = new Logger("Player", Logger.DEBUG);
    private static final int TILE_WIDTH = 32;
    private static final int TILE_HEIGHT = 32;
    private static final int FRAMES_PER_ANIMATION = 3;
    private static final float FRAME_DURATION = 0.15f;
    private static final float ATTACK_DURATION = FRAME_DURATION * FRAMES_PER_ANIMATION;
    private static final float PPM = 100f; // Pixels per meter for physics calculations
    private Vector2 velocity = new Vector2(0, 0);
    private float maxSpeed = 100f;
    private float friction = 0.85f;
    private boolean showCollisionMessage = false;
    private String collisionMessage = "";
    private float messageTimer = 0f;
    private static final float MESSAGE_DURATION = 3f; // Show message for 3 seconds
    private boolean isHit = false;
    private float hitTimer = 0f;
    private static final float HIT_FLASH_DURATION = 1f;


    // Map collision detection
    private boolean collidingWithObject = false;
    private String collidingObjectName = "";

    // Enum for player states
    public enum State {
        IDLE_BACK, IDLE_RIGHT, IDLE_LEFT, IDLE_FRONT,
        WALK_BACK, WALK_RIGHT, WALK_LEFT, WALK_FRONT,
        ATTACK_BACK, ATTACK_RIGHT, ATTACK_LEFT, ATTACK_FRONT,
        DIE
    }

    // Resources
    private static Texture spriteSheet;
    private TextureRegion[][] animations;

    // Physics
    private Body body;
    private World world;
    private Rectangle bounds;

    // Player properties
    private Vector2 position;
    private float speed = 100f;
    private int health = 100;
    private boolean alive = true;

    // State tracking
    private State currentState = State.IDLE_FRONT;
    private State lastDirection = State.IDLE_FRONT;
    private State lastMovementState = State.WALK_FRONT;
    private float stateTime = 0;

    // Action flags
    private boolean isAttacking = false;
    private float attackTimer = 0f;

    // Movement flags
    private boolean isMovingUp = false;
    private boolean isMovingDown = false;
    private boolean isMovingLeft = false;
    private boolean isMovingRight = false;

    // Define map boundaries
    private static final float MAP_WIDTH = 1600f; // Replace with your map's width
    private static final float MAP_HEIGHT = 1600f; // Replace with your map's height

    // Constructors
    public Player() {
        super(0, 0, TILE_WIDTH, TILE_HEIGHT);
        float centerX = Gdx.graphics.getWidth() / 2f - TILE_WIDTH / 2f;
        float centerY = Gdx.graphics.getHeight() / 2f - TILE_HEIGHT / 2f;

        position = new Vector2(centerX, centerY);
        initializePlayer();
    }

    public Player(float x, float y) {
        super(x, y, TILE_WIDTH, TILE_HEIGHT);
        position = new Vector2(x, y);
        Gdx.app.debug("Player", "Created at position: (" + x + ", " + y + ")");
        initializePlayer();
    }

    public Player(World world, float x, float y) {
        super(x, y, TILE_WIDTH, TILE_HEIGHT);
        this.world = world;
        position = new Vector2(x, y);
        Gdx.app.debug("Player", "Created at position: (" + x + ", " + y + ")");

        initializePlayer();
        createPhysicsBody();
    }

    // Initialization methods
    private void initializePlayer() {
        loadTextures();
        loadAnimations();
        bounds = new Rectangle(position.x - TILE_WIDTH / 2f, position.y - TILE_HEIGHT / 2f, TILE_WIDTH, TILE_HEIGHT);

    }

    private void createPhysicsBody() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(position.x / PPM, position.y / PPM);
        bodyDef.fixedRotation = true;

        body = world.createBody(bodyDef);

        CircleShape shape = new CircleShape();
        shape.setRadius(16 / PPM);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0.4f;

        body.createFixture(fixtureDef);
        shape.dispose();
    }

    private void loadTextures() {
        try {
            if (spriteSheet == null || !spriteSheet.getTextureData().isPrepared()) {
                String fileName = "player_spritesheet.png";
                if (Gdx.files.internal(fileName).exists()) {
                    spriteSheet = new Texture(Gdx.files.internal(fileName));
                    logger.debug("Loaded spritesheet from: " + fileName);
                } else if (Gdx.files.internal("assets/" + fileName).exists()) {
                    spriteSheet = new Texture(Gdx.files.internal("assets/" + fileName));
                    logger.debug("Loaded spritesheet from: assets/" + fileName);
                } else {
                    logger.error("Player spritesheet not found! Creating placeholder texture.");
                    spriteSheet = new Texture(2, 2, com.badlogic.gdx.graphics.Pixmap.Format.RGB888);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load player spritesheet: " + e.getMessage());
            spriteSheet = new Texture(2, 2, com.badlogic.gdx.graphics.Pixmap.Format.RGB888);
        }
    }

    private void loadAnimations() {
        try {
            animations = new TextureRegion[13][FRAMES_PER_ANIMATION];

            // Idle animations
            for (int i = 0; i < FRAMES_PER_ANIMATION; i++) {
                animations[State.IDLE_BACK.ordinal()][i] = new TextureRegion(spriteSheet, i * TILE_WIDTH, 0, TILE_WIDTH, TILE_HEIGHT);
                animations[State.IDLE_RIGHT.ordinal()][i] = new TextureRegion(spriteSheet, i * TILE_WIDTH, TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
                animations[State.IDLE_LEFT.ordinal()][i] = new TextureRegion(spriteSheet, i * TILE_WIDTH, TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
                animations[State.IDLE_LEFT.ordinal()][i].flip(true, false);
                animations[State.IDLE_FRONT.ordinal()][i] = new TextureRegion(spriteSheet, i * TILE_WIDTH, 2 * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
            }

            // Walking animations
            for (int i = 0; i < FRAMES_PER_ANIMATION; i++) {
                animations[State.WALK_BACK.ordinal()][i] = new TextureRegion(spriteSheet, i * TILE_WIDTH, 5 * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
                animations[State.WALK_RIGHT.ordinal()][i] = new TextureRegion(spriteSheet, i * TILE_WIDTH, 4 * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
                animations[State.WALK_LEFT.ordinal()][i] = new TextureRegion(spriteSheet, i * TILE_WIDTH, 4 * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
                animations[State.WALK_LEFT.ordinal()][i].flip(true, false);
                animations[State.WALK_FRONT.ordinal()][i] = new TextureRegion(spriteSheet, i * TILE_WIDTH, 3 * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
            }

            // Attack animations
            for (int i = 0; i < FRAMES_PER_ANIMATION; i++) {
                animations[State.ATTACK_FRONT.ordinal()][i] = new TextureRegion(spriteSheet, i * TILE_WIDTH, 6 * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
                animations[State.ATTACK_RIGHT.ordinal()][i] = new TextureRegion(spriteSheet, i * TILE_WIDTH, 7 * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
                animations[State.ATTACK_LEFT.ordinal()][i] = new TextureRegion(spriteSheet, i * TILE_WIDTH, 7 * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
                animations[State.ATTACK_LEFT.ordinal()][i].flip(true, false);
                animations[State.ATTACK_BACK.ordinal()][i] = new TextureRegion(spriteSheet, i * TILE_WIDTH, 8 * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
            }

            // Die animation
            for (int i = 0; i < FRAMES_PER_ANIMATION; i++) {
                animations[State.DIE.ordinal()][i] = new TextureRegion(spriteSheet, i * TILE_WIDTH, 9 * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
            }

            logger.debug("Player animations loaded successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize animations: " + e.getMessage());
        }
    }

    // Core update method
    public void update(float deltaTime) {
        stateTime += deltaTime;


        // Check if the player should die
        if (health <= 0 && currentState != State.DIE) {
            die();
        }

        // If the player is in the DIE state, update the death animation and stop further updates
        if (currentState == State.DIE) {
            // Ensure the death animation continues playing
            return;
        }

        if (isHit) {
            hitTimer += deltaTime;
            if (hitTimer >= HIT_FLASH_DURATION) {
                isHit = false;
                hitTimer = 0f;
            }
        }


        updatePosition(deltaTime);
        updateAttackState(deltaTime);
        updateBounds();
        bounds.setPosition(position.x - TILE_WIDTH / 2f, position.y - TILE_HEIGHT / 2f);


        // Reset collision flag each update
        collidingWithObject = false;
        collidingObjectName = "";
    }

    private void updatePosition(float deltaTime) {
        // Calculate potential new position based on velocity
        float newX = position.x + velocity.x * deltaTime;
        float newY = position.y + velocity.y * deltaTime;

        // Apply the movement only if it doesn't cause a collision
        position.x = newX;
        position.y = newY;

        // Apply friction
        if (!isMovingUp && !isMovingDown && !isMovingLeft && !isMovingRight) {
            velocity.x *= friction;
            velocity.y *= friction;
            if (Math.abs(velocity.x) < 1) velocity.x = 0;
            if (Math.abs(velocity.y) < 1) velocity.y = 0;
        }

        // Clamp position to map boundaries
        position.x = Math.max(TILE_WIDTH / 2, Math.min(position.x, MAP_WIDTH - TILE_WIDTH / 2));
        position.y = Math.max(TILE_HEIGHT / 2, Math.min(position.y, MAP_HEIGHT - TILE_HEIGHT / 2));
    }

    private void updateAttackState(float deltaTime) {
        if (isAttacking) {
            attackTimer += deltaTime;
            if (attackTimer >= ATTACK_DURATION) {
                isAttacking = false;
                attackTimer = 0f;
                updateAnimationState();
            }
        }
    }

    @Override
    protected void updateBounds() {
        bounds.setPosition(position.x - TILE_WIDTH/2, position.y - TILE_HEIGHT/2);
    }

    private void updateAnimationState() {
        if (isAttacking) return;

        if (isMovingUp) {
            currentState = State.WALK_BACK;
            lastDirection = State.IDLE_BACK;
            lastMovementState = State.WALK_BACK;
        } else if (isMovingDown) {
            currentState = State.WALK_FRONT;
            lastDirection = State.IDLE_FRONT;
            lastMovementState = State.WALK_FRONT;
        } else if (isMovingLeft) {
            currentState = State.WALK_LEFT;
            lastDirection = State.IDLE_LEFT;
            lastMovementState = State.WALK_LEFT;
        } else if (isMovingRight) {
            currentState = State.WALK_RIGHT;
            lastDirection = State.IDLE_RIGHT;
            lastMovementState = State.WALK_RIGHT;
        } else {
            // Set idle state based on last direction
            switch (lastDirection) {
                case IDLE_BACK: currentState = State.IDLE_FRONT; break;
                case IDLE_FRONT: currentState = State.IDLE_BACK; break;
                case IDLE_LEFT: currentState = State.IDLE_LEFT; break;
                case IDLE_RIGHT: currentState = State.IDLE_RIGHT; break;
                default: currentState = State.IDLE_FRONT; break;
            }
        }
    }

    // Rendering methods
    public void render(SpriteBatch batch) {
        if (batch == null) {
            logger.error("SpriteBatch is null in render method");
            return;
        }

        try {
            TextureRegion currentFrame = getCurrentAnimationFrame();
            if (currentFrame != null) {
                if (isHit && ((int)(hitTimer * 10) % 2 == 0)) {
                    batch.setColor(1, 1, 1, 0.3f); // 点滅状態
                }

                batch.draw(currentFrame, position.x - TILE_WIDTH/2, position.y - TILE_HEIGHT/2);
                batch.setColor(1, 1, 1, 1f); // 描画後は元に戻す
            } else {
                logger.error("Current frame is null. State: " + currentState);
            }
        } catch (Exception e) {
            logger.error("Error in render: " + e.getMessage());
        }
    }

    public void renderAtPosition(SpriteBatch batch, float x, float y) {
        if (batch == null) {
            logger.error("SpriteBatch is null in render method");
            return;
        }

        try {
            TextureRegion currentFrame = getCurrentAnimationFrame();
            if (currentFrame != null) {
                batch.draw(currentFrame, x, y);
            } else {
                logger.error("Current frame is null. State: " + currentState);
            }
        } catch (Exception e) {
            logger.error("Error in render: " + e.getMessage());
        }
    }

    private TextureRegion getCurrentAnimationFrame() {
        int frameIndex;

        if (currentState == State.DIE) {
            // Ensure the death animation plays correctly
            frameIndex = Math.min((int)(stateTime / FRAME_DURATION), FRAMES_PER_ANIMATION - 1);
        } else if (isAttacking) {
            frameIndex = (int)(attackTimer / FRAME_DURATION);
            if (frameIndex >= FRAMES_PER_ANIMATION) frameIndex = FRAMES_PER_ANIMATION - 1;
        } else {
            frameIndex = (int)(stateTime / FRAME_DURATION) % FRAMES_PER_ANIMATION;
        }

        return animations[currentState.ordinal()][frameIndex];
    }

    // Player actions
    public void attack() {
        if (health <= 0 || isAttacking) return;

        isAttacking = true;
        attackTimer = 0f;

        // Determine attack direction based on movement state
        if (isMovingUp) {
            currentState = State.ATTACK_BACK;  // Attack matches walking direction
        } else if (isMovingDown) {
            currentState = State.ATTACK_FRONT; // Attack matches walking direction
        } else if (isMovingLeft) {
            currentState = State.ATTACK_LEFT;  // Attack matches walking direction
        } else if (isMovingRight) {
            currentState = State.ATTACK_RIGHT; // Attack matches walking direction
        } else {
            // Not currently moving, use last movement direction
            switch (lastMovementState) {
                case WALK_BACK:
                    currentState = State.ATTACK_BACK;
                    break;
                case WALK_FRONT:
                    currentState = State.ATTACK_FRONT;
                    break;
                case WALK_LEFT:
                    currentState = State.ATTACK_LEFT;
                    break;
                case WALK_RIGHT:
                    currentState = State.ATTACK_RIGHT;
                    break;
                default:
                    // If no last movement, use idle direction
                    switch (lastDirection) {
                        case IDLE_BACK:
                            currentState = State.ATTACK_BACK;
                            break;
                        case IDLE_FRONT:
                            currentState = State.ATTACK_FRONT;
                            break;
                        case IDLE_LEFT:
                            currentState = State.ATTACK_LEFT;
                            break;
                        case IDLE_RIGHT:
                            currentState = State.ATTACK_RIGHT;
                            break;
                        default:
                            currentState = State.ATTACK_FRONT;
                            break;
                    }
            }
        }

        logger.debug("Attack direction: " + currentState);
    }

    public void takeDamage(int amount) {
        if (isHit || health <= 0) return;

        health -= amount;
        Gdx.app.log("Player", "Took " + amount + " damage. Health now: " + health);

        isHit = true;
        hitTimer = 0f;

        if (health <= 0) {
            health = 0;
            die();
        }
    }

    private void die() {
        // Stop all movement
        isMovingUp = isMovingDown = isMovingLeft = isMovingRight = isAttacking = false;

        // Set death state and reset animation timer
        currentState = State.DIE;
        stateTime = 0;

        logger.debug("Player has died. State set to DIE.");
    }

    // Movement methods
    public void moveUp(float deltaTime) {
        if (health <= 0 || currentState == State.DIE) return;

        isMovingUp = true;
        isMovingDown = false;
        velocity.y = Math.min(velocity.y + speed * 5 * deltaTime, maxSpeed);

        if (body != null) {
            body.setLinearVelocity(body.getLinearVelocity().x, speed / PPM);
        }

        if (!isAttacking) {
            currentState = State.WALK_BACK;
            lastDirection = State.IDLE_BACK;
            lastMovementState = State.WALK_BACK;
        }
    }

    public void moveDown(float deltaTime) {
        if (health <= 0 || currentState == State.DIE) return;

        isMovingDown = true;
        isMovingUp = false;
        velocity.y = Math.max(velocity.y - speed * 5 * deltaTime, -maxSpeed);

        if (body != null) {
            body.setLinearVelocity(body.getLinearVelocity().x, -speed / PPM);
        }

        if (!isAttacking) {
            currentState = State.WALK_FRONT;
            lastDirection = State.IDLE_FRONT;
            lastMovementState = State.WALK_FRONT;
        }
    }

    public void moveLeft(float deltaTime) {
        if (health <= 0 || currentState == State.DIE) return;

        isMovingLeft = true;
        isMovingRight = false;
        velocity.x = Math.max(velocity.x - speed * 5 * deltaTime, -maxSpeed);

        if (body != null) {
            body.setLinearVelocity(-speed / PPM, body.getLinearVelocity().y);
        }

        if (!isAttacking) {
            currentState = State.WALK_LEFT;
            lastDirection = State.IDLE_LEFT;
            lastMovementState = State.WALK_LEFT;
        }
    }

    public void moveRight(float deltaTime) {
        if (health <= 0 || currentState == State.DIE) return;

        isMovingRight = true;
        isMovingLeft = false;
        velocity.x = Math.min(velocity.x + speed * 5 * deltaTime, maxSpeed);

        if (body != null) {
            body.setLinearVelocity(speed / PPM, body.getLinearVelocity().y);
        }

        if (!isAttacking) {
            currentState = State.WALK_RIGHT;
            lastDirection = State.IDLE_RIGHT;
            lastMovementState = State.WALK_RIGHT;
        }
    }

    public void moveUpRaw(float deltaTime) {
        if (health <= 0 || currentState == State.DIE || isAttacking) return;

        position.y += speed * deltaTime;
    }

    public void moveDownRaw(float deltaTime) {
        if (health <= 0 || currentState == State.DIE || isAttacking) return;

        position.y -= speed * deltaTime;
    }

    public void moveLeftRaw(float deltaTime) {
        if (health <= 0 || currentState == State.DIE || isAttacking) return;

        position.x -= speed * deltaTime;
    }

    public void moveRightRaw(float deltaTime) {
        if (health <= 0 || currentState == State.DIE || isAttacking) return;

        position.x += speed * deltaTime;
    }

    public void stopMovingUp() {
        if (health <= 0 || currentState == State.DIE) return;

        isMovingUp = false;
        updateAnimationState();
    }

    public void stopMovingDown() {
        if (health <= 0 || currentState == State.DIE) return;

        isMovingDown = false;
        updateAnimationState();
    }

    public void stopMovingLeft() {
        if (health <= 0 || currentState == State.DIE) return;

        isMovingLeft = false;
        updateAnimationState();
    }

    public void stopMovingRight() {
        if (health <= 0 || currentState == State.DIE) return;

        isMovingRight = false;
        updateAnimationState();
    }

    public void stopMoving() {
        if (health <= 0 || currentState == State.DIE) return;

        isMovingUp = isMovingDown = isMovingLeft = isMovingRight = false;

        if (!isAttacking) {
            updateAnimationState();
        }
    }

    public void resetMovement() {
        if (health <= 0 || currentState == State.DIE) return;

        if (!isAttacking) {
            switch (currentState) {
                case WALK_BACK:
                    currentState = State.IDLE_BACK;
                    lastDirection = State.IDLE_BACK;
                    break;
                case WALK_FRONT:
                    currentState = State.IDLE_FRONT;
                    lastDirection = State.IDLE_FRONT;
                    break;
                case WALK_LEFT:
                    currentState = State.IDLE_LEFT;
                    lastDirection = State.IDLE_LEFT;
                    break;
                case WALK_RIGHT:
                    currentState = State.IDLE_RIGHT;
                    lastDirection = State.IDLE_RIGHT;
                    break;
            }
        }
    }

    // Getters and setters
    public Vector2 getPosition() {
        return position;
    }

    public void setPosition(Vector2 position) {
        this.position = position;
        updateBounds(); // Use the entity-specific bounds update
    }

    public void setPosition(float x, float y) {
        position.x = x;
        position.y = y;
        updateBounds();
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public boolean isAttacking() {
        return isAttacking;
    }

    public State getCurrentState() {
        return currentState;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public Body getBody() {
        return body;
    }

    /**
     * Check collision with a map object
     * @param objectBounds Rectangle representing the object's bounds
     * @param objectName Name of the object (for identification in logs)
     * @return true if collision detected, false otherwise
     */
    public boolean checkCollisionWithObject(Rectangle objectBounds, String objectName) {
        if (bounds.overlaps(objectBounds)) {
            if (!collidingWithObject || !collidingObjectName.equals(objectName)) {
                logger.debug("Player collided with object: " + objectName);
                System.out.println("Player collided with: " + objectName);
            }
            collidingWithObject = true;
            collidingObjectName = objectName;
            return true;
        }
        return false;
    }

    /**
     * Returns whether the player is currently colliding with an object
     * @return true if player is colliding, false otherwise
     */
    public boolean isCollidingWithObject() {
        return collidingWithObject;
    }

    /**
     * Gets the name of the object player is colliding with
     * @return Name of colliding object or empty string if none
     */
    public String getCollidingObjectName() {
        return collidingObjectName;
    }

    /**
     * Stops velocity in the direction of collision
     * @param isHorizontal true if collision occurred horizontally, false if vertically
     * @param isNegative true if collision is in negative direction (left/down), false otherwise (right/up)
     */
    public void stopVelocityInCollisionDirection(boolean isHorizontal, boolean isNegative) {
        if (isHorizontal) {
            if ((isNegative && velocity.x < 0) || (!isNegative && velocity.x > 0)) {
                velocity.x = 0;
            }
        } else {
            if ((isNegative && velocity.y < 0) || (!isNegative && velocity.y > 0)) {
                velocity.y = 0;
            }
        }
    }

    // Utility methods
    public String getPositionInfo() {
        return String.format("Position: (%.1f, %.1f), State: %s",
                position.x, position.y, currentState.toString());
    }

    public void logPosition() {
        logger.debug(getPositionInfo());
    }

    public static TextureRegion[] getTextureRegionsForPlayer() {
        return null; // Placeholder for backward compatibility
    }

    // Resource management
    public void dispose() {
        if (spriteSheet != null) {
            spriteSheet.dispose();
            spriteSheet = null;
        }
        // Note: Box2D bodies should be disposed by the World
    }
}
