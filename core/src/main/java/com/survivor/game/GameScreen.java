package com.survivor.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen implements Screen {
    private final Main game; // Add this field to store the game instance
    private final SpriteBatch batch;
    private final Player player;
    private final List<Enemy> enemies;
    private FinalBoss boss;
    private final BitmapFont font;
    private final Random random;
    private float enemySpawnTimer = 0;
    private static final float ENEMY_SPAWN_INTERVAL = 3f;
    private boolean bossSpawned = false;
    private ShapeRenderer shapeRenderer; // New field for drawing shapes
    private Sound swordSound; // New field for sword sound
    private int killedBasicEnemyCount = 0;

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final float worldWidth;
    private final float worldHeight;
    private boolean showFPS = false;

    private final TiledMap map;
    private final OrthogonalTiledMapRenderer mapRenderer;
    private final MapManager mapManager;

    private boolean wPressed = false;
    private boolean aPressed = false;
    private boolean sPressed = false;
    private boolean dPressed = false;

    private String lastMovementDirection = "none";

    private final List<Obstacle> obstacles;

    private boolean showCoordinates = true;
    private boolean logCoordinatesWhileMoving = false;
    private float coordinateLogTimer = 0;
    private static final float COORDINATE_LOG_INTERVAL = 0.5f;

    private boolean showHitboxes = false;
    private static final Color PLAYER_HITBOX_COLOR = Color.GREEN;
    private static final Color ENEMY_HITBOX_COLOR = Color.RED;
    private static final Color OBSTACLE_HITBOX_COLOR = Color.BLUE;

    // Add a TAG constant for logging purposes
    private static final String TAG = "GameScreen";
    private List<Arrow> arrows = new ArrayList<>();
    private PauseMenu pauseMenu;
    private boolean isPaused = false;

    private MiniMap miniMap;
    private Sound runningSound; // Add a field for the running sound
    private long runningSoundId = -1; // Track the sound instance ID

    private float gameOverTimer = 0; // Timer to track delay for game over screens
    private float objectiveProgress = 0; // Progress for the objective bar
    private static final float MAX_BASIC_ENEMY_KILLS = 20; // Total basic enemies to kill for progression
    private boolean finalBossObjective = false; // Track if the objective is for the final boss
    private float attackCooldownTimer = 0; // Timer to track attack cooldown
    private static final float ATTACK_COOLDOWN = 0.5f; // Cooldown duration in seconds

    public GameScreen(Main game) {
        this.game = game; // Initialize the game instance
        GameScreenHolder.instance = this;
        // Set fullscreen mode
        Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer(); // Initialize shape renderer

        // Load the sword sound
        swordSound = Gdx.audio.newSound(Gdx.files.internal("sword-sound.wav"));
        runningSound = Gdx.audio.newSound(Gdx.files.internal("running_sound.mp3")); // Load the running sound

        worldWidth = Gdx.graphics.getWidth();
        worldHeight = Gdx.graphics.getHeight();
        mapManager = new MapManager("map.tmx");
        map = mapManager.getMap();
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        float mapWidth = map.getProperties().get("width", Integer.class) *
            map.getProperties().get("tilewidth", Integer.class);
        float mapHeight = map.getProperties().get("height", Integer.class) *
            map.getProperties().get("tileheight", Integer.class);
        float mapCenterX = mapWidth / 2;
        float mapCenterY = mapHeight / 2;

        // Update viewport to better handle fullscreen
        camera = new OrthographicCamera();
        viewport = new FitViewport(worldWidth / 2, worldHeight / 2, camera);
        camera.position.set(mapCenterX, mapCenterY, 0);
        camera.update();
        viewport.apply();

        // Initialize player at map center
        player = new Player(mapCenterX, mapCenterY);
        enemies = new ArrayList<>();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        random = new Random();

        obstacles = new ArrayList<>();
        Array<Rectangle> mapObstacles = mapManager.getObstacles();
        for (Rectangle rect : mapObstacles) {
            obstacles.add(new Obstacle(
                rect.x, rect.y,
                rect.width, rect.height,
                null, true
            ));
        }

        for (int i = 0; i < 5; i++) {
            spawnEnemy();
        }

        // Initialize mini map
        miniMap = new MiniMap(map, mapWidth, mapHeight);
        pauseMenu = new PauseMenu(game); // Pass the game instance to PauseMenu
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        // Toggle pause state with ESC
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            isPaused = !isPaused;
            Gdx.app.log(TAG, "Pause toggled: " + isPaused);
        }

        if (isPaused) {
            // Render the pause menu
            pauseMenu.render();

            // Handle resume or quit actions
            if (pauseMenu.handleSelection()) {
                isPaused = false; // Resume the game
            }
            return; // Skip the rest of the game rendering and updates
        }

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        // Update
        player.update(delta);

        // Update camera to follow player position
        camera.position.set(player.getPosition().x, player.getPosition().y, 0);
        camera.update();

        // Render the tiled map
        mapRenderer.setView(camera);
        mapRenderer.render();

        // Update the attack cooldown timer
        if (attackCooldownTimer > 0) {
            attackCooldownTimer -= delta;
        }

        // Set camera for batch
        batch.setProjectionMatrix(camera.combined);

        // Handle input for player movement with collision detection
        handlePlayerInputWithCollision(delta);

        // Log coordinates periodically if enabled and moving
        updateCoordinateTracking(delta);

        // Update enemies and check for dead ones
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.update(delta, player, enemies);

            // If enemy is dead and it is a BasicEnemy, increment the kill count
            if (!enemy.isAlive()) {
                if (enemy instanceof BasicEnemy) {
                    killedBasicEnemyCount++;  // Increment kill count when BasicEnemy is killed
                }
                enemies.remove(i).dispose();
                continue;
            }

            resolveObstacleCollisionsForEntity(enemy);
        }

        // Update boss if spawned
        if (boss != null && boss.isAlive()) {
            boss.update(delta, player, enemies);

        } else if (boss != null) {
            // Boss is dead, you win!
            // Could transition to a win screen
        }

        // Spawn enemies periodically
        enemySpawnTimer += delta;
        if (enemySpawnTimer >= ENEMY_SPAWN_INTERVAL && enemies.size() < 20) { // Limit max enemies
            spawnEnemy();
            enemySpawnTimer = 0;
        }

        // Spawn boss after killing 20 BasicEnemies
        if (!bossSpawned && killedBasicEnemyCount >= 20) {
            spawnBoss();  // Spawn the final boss after 20 kills
        }

        // Prevent spawning BasicEnemy after 20 kills
        if (killedBasicEnemyCount < 20) {
            enemySpawnTimer += delta;
            if (enemySpawnTimer >= ENEMY_SPAWN_INTERVAL && enemies.size() < 20) {
                spawnBasicEnemy();  // Spawn BasicEnemy if less than 20 kills
                enemySpawnTimer = 0;
            }
        }

        for (int i = arrows.size() - 1; i >= 0; i--) {
            Arrow arrow = arrows.get(i);
            arrow.update(delta);
            arrow.checkCollision(player);

            if (!arrow.isActive()) {
                arrows.remove(i);
            }
        }

        // Update objective progress
        if (!finalBossObjective) {
            objectiveProgress = Math.min(killedBasicEnemyCount / MAX_BASIC_ENEMY_KILLS, 1f);
        } else if (boss != null && !boss.isAlive()) {
            objectiveProgress = 1f; // Final boss defeated
        }

        // Draw the objective bar
        renderObjectiveBar();

        // Draw
        try {
            batch.begin();

            // Draw obstacles (if they have textures)
            for (Obstacle obstacle : obstacles) {
                if (obstacle != null) {
                    try {
                        obstacle.render(batch);
                    } catch (Exception e) {
                        Gdx.app.error(TAG, "Error rendering obstacle: " + e.getMessage());
                    }
                }
            }

            // Draw enemies - they move relative to player
            for (Enemy enemy : enemies) {
                if (enemy != null) {
                    try {
                        enemy.render(batch);
                    } catch (Exception e) {
                        Gdx.app.error(TAG, "Error rendering enemy: " + e.getMessage());
                    }
                }
            }

            // Draw boss if spawned
            if (boss != null) {
                try {
                    boss.render(batch);
                } catch (Exception e) {
                    Gdx.app.error(TAG, "Error rendering boss: " + e.getMessage());
                }
            }

            for (Arrow arrow : arrows) {
                arrow.render(batch);
            }

            // Draw player at its actual position, not centered at (0,0)
            if (player != null) {
                try {
                    player.render(batch);
                } catch (Exception e) {
                    Gdx.app.error(TAG, "Error rendering player: " + e.getMessage());
                }
            }

            batch.end();

            // Draw hitboxes if enabled (must be drawn after batch.end() and before next batch.begin())
            if (showHitboxes) {
                renderHitboxes();
            }

            // Draw health bar (after hitboxes but before mini map)
            renderHealthBar();

            // Render the mini map (after main rendering but before any UI text)
            miniMap.render(batch, player.getPosition(), enemies, worldWidth/2, worldHeight/2);

            // Draw UI text on top of everything
            batch.begin();

            // Position UI elements in the top right corner
            float uiX = camera.position.x + worldWidth / 4;  // Changed from - to + to move to right side
            float uiY = camera.position.y + worldHeight / 4;

            // Calculate text widths for right alignment - removed health text
            String enemiesText = "Enemies: " + enemies.size();

            // Right-align the text (subtract text width from position) - removed health text line
            font.draw(batch, enemiesText, uiX - font.getCache().addText(enemiesText, 0, 0).width - 20, uiY - 20);

            // Display player coordinates if enabled - also right-aligned
            if (showCoordinates) {
                Vector2 pos = player.getPosition();
                String posText = String.format("Position: (%.1f, %.1f)", pos.x, pos.y);
                String stateText = "State: " + player.getCurrentState();

                font.draw(batch, posText, uiX - font.getCache().addText(posText, 0, 0).width - 20, uiY - 40);
                font.draw(batch, stateText, uiX - font.getCache().addText(stateText, 0, 0).width - 20, uiY - 60);
            }

            // Show hitbox debug info if enabled - also right-aligned
            if (showHitboxes) {
                String hitboxText = "HITBOXES VISIBLE (F5 to toggle)";
                font.draw(batch, hitboxText, uiX - font.getCache().addText(hitboxText, 0, 0).width - 20, uiY - 80);
            }

            String killCountText = "Basic Enemies Defeated: " + killedBasicEnemyCount;
            font.draw(batch, killCountText, uiX - font.getCache().addText(killCountText, 0, 0).width - 20, uiY - 100);

            if (showFPS) {
                String fpsText = "FPS: " + Gdx.graphics.getFramesPerSecond();
                float fpsX = camera.position.x - camera.viewportWidth / 2 + 10; // Position near the top-left corner
                float fpsY = camera.position.y + camera.viewportHeight / 2 - 10;
                font.draw(batch, fpsText, fpsX, fpsY);
            }

            batch.end();

        } catch (Exception e) {
            // If the batch is still active but an error occurred, make sure to end it
            if (batch.isDrawing()) {
                try {
                    batch.end();
                } catch (Exception ex) {
                    // Ignore errors from ending the batch
                }
            }
            Gdx.app.error(TAG, "Error during rendering", e);
        }

        // Debug info
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F1)) {
            Gdx.app.log(TAG, "Player position: " + player.getPosition());
            Gdx.app.log(TAG, "Player state: " + player.getCurrentState());
            Gdx.app.log(TAG, "Player health: " + player.getHealth());
        }

        // Toggle coordinate display with F3
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F3)) {
            showCoordinates = !showCoordinates;
            Gdx.app.log(TAG, "Coordinate display " + (showCoordinates ? "enabled" : "disabled"));
        }

        // Toggle coordinate logging while moving with F4
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F4)) {
            logCoordinatesWhileMoving = !logCoordinatesWhileMoving;
            Gdx.app.log(TAG, "Coordinate logging while moving " +
                (logCoordinatesWhileMoving ? "enabled" : "disabled"));
        }

        // Toggle hitbox display with F5
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F5)) {
            showHitboxes = !showHitboxes;
            Gdx.app.log(TAG, "Hitbox display " + (showHitboxes ? "enabled" : "disabled"));
        }

        //Toggle FPS display with F6
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F6)) {
            showFPS = !showFPS;
            Gdx.app.log(TAG, "FPS display " + (showFPS ? "enabled" : "disabled"));
        }

        // Check game over
        if (!player.isAlive()) {
            if (runningSoundId != -1) {
                runningSound.stop(runningSoundId); // Stop the walking sound
                runningSoundId = -1;
            }
            gameOverTimer += delta;
            if (gameOverTimer >= 3) { // Delay of 3 seconds
                Gdx.app.postRunnable(() -> {
                    game.setScreen(new LosingScreen(game, this)); // Pass the current GameScreen instance
                });
            }
            return; // Ensure no further rendering occurs
        }

        // Check if the boss is defeated
        if (boss != null && !boss.isAlive()) {
            if (runningSoundId != -1) {
                runningSound.stop(runningSoundId); // Stop the walking sound
                runningSoundId = -1;
            }
            gameOverTimer += delta;
            if (gameOverTimer >= 3) { // Delay of 3 seconds
                Gdx.app.postRunnable(() -> {
                    game.setScreen(new WinningScreen(game, this)); // Pass the current GameScreen instance
                });
            }
            return; // Ensure no further rendering occurs
        }

        // Reset gameOverTimer if no game over condition is met
        gameOverTimer = 0;

    }

    // Method to spawn BasicEnemy
    private void spawnBasicEnemy() {
        float x, y;
        float spawnDistance = 150;
        float halfWidth = worldWidth / 4;
        float halfHeight = worldHeight / 4;
        int side = random.nextInt(4);

        switch (side) {
            case 0:
                x = -halfWidth + random.nextFloat() * worldWidth / 2;
                y = halfHeight + spawnDistance;
                break;
            case 1:
                x = halfWidth + spawnDistance;
                y = -halfHeight + random.nextFloat() * worldHeight / 2;
                break;
            case 2:
                x = -halfWidth + random.nextFloat() * worldWidth / 2;
                y = -halfHeight - spawnDistance;
                break;
            case 3:
                x = -halfWidth - spawnDistance;
                y = -halfHeight + random.nextFloat() * worldHeight / 2;
                break;
            default:
                x = 0;
                y = 0;
        }

        enemies.add(new BasicEnemy(x, y, obstacles));
    }

    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(1280, 720);
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
        // Update viewport after mode switch
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void updateCoordinateTracking(float delta) {
        if (logCoordinatesWhileMoving) {
            boolean isMoving = wPressed || aPressed || sPressed || dPressed;

            if (isMoving) {
                coordinateLogTimer += delta;
                if (coordinateLogTimer >= COORDINATE_LOG_INTERVAL) {
                    Vector2 pos = player.getPosition();
                    Gdx.app.log(TAG, String.format("Player moving - Position: (%.1f, %.1f)", pos.x, pos.y));
                    coordinateLogTimer = 0;
                }
            } else {
                // If player just stopped moving, log the final position
                if (coordinateLogTimer > 0) {
                    Vector2 pos = player.getPosition();
                    Gdx.app.log(TAG, String.format("Player stopped - Position: (%.1f, %.1f)", pos.x, pos.y));
                    coordinateLogTimer = 0;
                }
            }
        }
    }

    private void handlePlayerInputWithCollision(float delta) {
        // Track previous key states
        boolean prevW = wPressed;
        boolean prevA = aPressed;
        boolean prevS = sPressed;
        boolean prevD = dPressed;
        // Store original position for collision handling
        Vector2 originalPosition = new Vector2(player.getPosition());

        // Update key states
        wPressed = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W);
        aPressed = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A);
        sPressed = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.S);
        dPressed = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D);
        // Process movement based on pressed keys
        boolean anyKeyPressed = wPressed || aPressed || sPressed || dPressed;

        if (anyKeyPressed) {
            // First, check if the player is already inside an object and push them out if necessary
            checkAndResolveExistingCollisions();

            // Apply movement one direction at a time and check for collisions
            // This helps prevent walking through corners
            if (wPressed) {
                player.moveUp(delta);
                if (resolveObstacleCollisionsForEntity(player)) {
                    player.stopMovingUp();
                }
            }

            if (sPressed) {
                player.moveDown(delta);
                if (resolveObstacleCollisionsForEntity(player)) {
                    player.stopMovingDown();
                }
            }

            if (aPressed) {
                player.moveLeft(delta);
                if (resolveObstacleCollisionsForEntity(player)) {
                    player.stopMovingLeft();
                }
            }

            if (dPressed) {
                player.moveRight(delta);
                if (resolveObstacleCollisionsForEntity(player)) {
                    player.stopMovingRight();
                }
            }

            // Final collision check and resolution
            resolveObstacleCollisionsForEntity(player);
        } else {
            // If no keys are pressed, handle key releases
            if (prevW) player.stopMovingUp();
            if (prevS) player.stopMovingDown();
            if (prevA) player.stopMovingLeft();
            if (prevD) player.stopMovingRight();
            player.stopMoving();
        }

        // Track the last movement direction for attack orientation
        String currentMovement = "none";
        if (wPressed) currentMovement = "up";
        else if (sPressed) currentMovement = "down";
        if (aPressed) currentMovement = "left";
        else if (dPressed) currentMovement = "right";

        if (!currentMovement.equals("none")) {
            lastMovementDirection = currentMovement;
        }

        // Handle mouse click for attack (can be done while moving)
        if (Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.LEFT) && attackCooldownTimer <= 0) {
            player.attack();
            swordSound.play(0.5f);
            attackCooldownTimer = ATTACK_COOLDOWN; // Reset the cooldown timer

            for (Enemy enemy : enemies) {
                if (!enemy.isAlive()) continue;

                float attackRange = 40f;
                Vector2 enemyPos = enemy.getPosition();
                Vector2 playerPos = player.getPosition();
                float distance = playerPos.dst(enemyPos);
                if (distance <= attackRange) {
                    enemy.takeDamage(50); //Final boss deduct 50 hp 
                }
            }
        }

        if (boss != null && boss.isAlive() && player.isAttacking()) {
            float attackRange = 40f;
            Vector2 bossPos = boss.getPosition();
            Vector2 playerPos = player.getPosition();
            float distance = playerPos.dst(bossPos);
            if (distance <= attackRange) {
                // Remove this line to avoid double-deducting HP
                // boss.takeDamage(15);
            }
        }

        // Debug - show movement direction when pressing F2
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F2)) {
            Gdx.app.log(TAG, "Last movement direction: " + lastMovementDirection);
            Gdx.app.log(TAG, "Player state: " + player.getCurrentState());
        }

        // Play running sound when any movement key is pressed
        if (wPressed || aPressed || sPressed || dPressed) {
            if (runningSoundId == -1) {
                runningSoundId = runningSound.loop(0.4f); // Loop the sound at 50% volume
            }
        } else {
            if (runningSoundId != -1) {
                runningSound.stop(runningSoundId); // Stop the sound if no movement keys are pressed
                runningSoundId = -1;
            }
        }
    }

    private void checkAndResolveExistingCollisions() {
        Rectangle playerBounds = player.getBounds();
        Vector2 pushVector = new Vector2(0, 0);
        boolean needsPush = false;

        for (Obstacle obstacle : obstacles) {
            if (obstacle.isCollidable() && obstacle.getBounds().overlaps(playerBounds)) {
                // Player is inside an object - calculate push direction
                Rectangle obstacleBounds = obstacle.getBounds();

                // Find shortest way out
                float leftPush = playerBounds.x + playerBounds.width - obstacleBounds.x;
                float rightPush = obstacleBounds.x + obstacleBounds.width - playerBounds.x;
                float topPush = obstacleBounds.y + obstacleBounds.height - playerBounds.y;
                float bottomPush = playerBounds.y + playerBounds.height - obstacleBounds.y;

                // Find minimum push distance
                float minPush = Math.min(Math.min(leftPush, rightPush), Math.min(topPush, bottomPush));

                if (minPush == leftPush) {
                    pushVector.x -= leftPush;
                } else if (minPush == rightPush) {
                    pushVector.x += rightPush;
                } else if (minPush == bottomPush) {
                    pushVector.y -= bottomPush;
                } else if (minPush == topPush) {
                    pushVector.y += topPush;
                }
                needsPush = true;
                break; // Handle one collision at a time
            }
        }

        // Apply push if needed
        if (needsPush) {
            Vector2 currentPos = player.getPosition();
            player.setPosition(currentPos.x + pushVector.x, currentPos.y + pushVector.y);
            Gdx.app.debug(TAG, "Pushed player out of object by: " + pushVector);
        }
    }

    private boolean resolveObstacleCollisionsForEntity(Entity entity) {
        boolean collisionOccurred = false;
        // Get the entity's collision box
        Rectangle entityBounds = entity.getBounds();

        // Store the entity's initial position if it's the player
        Vector2 initialPosition = null;
        if (entity instanceof Player) {
            initialPosition = new Vector2(((Player)entity).getPosition());
        }

        for (Obstacle obstacle : obstacles) {
            if (obstacle.isCollidable() && obstacle.getBounds().overlaps(entityBounds)) {
                collisionOccurred = true;
                if (entity instanceof Player) {
                    // If it's the player, handle collision properly to prevent passing through
                    Player player = (Player)entity;
                    // Calculate the penetration depth in both axes
                    Rectangle obstacleBounds = obstacle.getBounds();
                    float centerX = player.getPosition().x;
                    float centerY = player.getPosition().y;
                    // Calculate overlapX and overlapY with a small buffer for better detection
                    float overlapX, overlapY;
                    if (centerX < obstacleBounds.x) {
                        overlapX = player.getBounds().x + player.getBounds().width - obstacleBounds.x;
                    } else {
                        overlapX = obstacleBounds.x + obstacleBounds.width - player.getBounds().x;
                    }
                    if (centerY < obstacleBounds.y) {
                        overlapY = player.getBounds().y + player.getBounds().height - obstacleBounds.y;
                    } else {
                        overlapY = obstacleBounds.y + obstacleBounds.height - player.getBounds().y;
                    }
                    // Add a small buffer to prevent getting stuck on edges
                    overlapX += 0.5f;
                    overlapY += 0.5f;

                    // Resolve collision by moving back the minimum distance needed
                    if (overlapX < overlapY) {
                        // Resolve horizontally
                        if (centerX < obstacleBounds.x) {
                            player.setPosition(player.getPosition().x - overlapX, player.getPosition().y);
                        } else {
                            player.setPosition(player.getPosition().x + overlapX, player.getPosition().y);
                        }
                    } else {
                        // Resolve vertically
                        if (centerY < obstacleBounds.y) {
                            player.setPosition(player.getPosition().x, player.getPosition().y - overlapY);
                        } else {
                            player.setPosition(player.getPosition().x, player.getPosition().y + overlapY);
                        }
                    }

                    // Make sure player's velocity components are zeroed appropriately
                    // to prevent continued movement in collision direction
                    player.stopVelocityInCollisionDirection(overlapX < overlapY,
                        centerX < obstacleBounds.x || centerY < obstacleBounds.y);
                }
                // Handle one collision per frame to prevent jitter
                break;
            }
        }

        return collisionOccurred;
    }

    private void renderHitboxes() {
        HitboxRenderer.begin(camera);
        // Draw player hitbox
        HitboxRenderer.drawRect(player.getBounds(), PLAYER_HITBOX_COLOR);

        // Draw enemy hitboxes
        for (Enemy enemy : enemies) {
            HitboxRenderer.drawRect(enemy.getBounds(), ENEMY_HITBOX_COLOR);
        }

        // Draw boss hitbox if spawned
        if (boss != null && boss.isAlive()) {
            HitboxRenderer.drawRect(boss.getBounds(), ENEMY_HITBOX_COLOR);
        }

        // Draw obstacle hitboxes
        for (Obstacle obstacle : obstacles) {
            HitboxRenderer.drawRect(obstacle.getBounds(), OBSTACLE_HITBOX_COLOR);
        }

        HitboxRenderer.end();
    }

    private void renderHealthBar() {
        // Make sure we're using proper OpenGL settings for transparency
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Set projection matrix to match camera for consistent positioning
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Use screen coordinates instead of world coordinates
        float barWidth = 150;
        float barHeight = 20;
        // Position health bar in absolute screen position rather than world position
        // This ensures it's always in the same place on screen regardless of camera movement
        float barX = camera.position.x + (camera.viewportWidth/2) - barWidth - 20;
        float barY = camera.position.y + (camera.viewportHeight/2) - 20;

        // Debug output to verify positioning
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F6)) {
            Gdx.app.log(TAG, "Camera position: " + camera.position);
            Gdx.app.log(TAG, "Health bar position: " + barX + ", " + barY);
            Gdx.app.log(TAG, "Viewport size: " + camera.viewportWidth + "x" + camera.viewportHeight);
        }

        // Calculate health percentage - use 100 as default max health
        int maxHealth = 100; // Assume 100 is max health
        int currentHealth = player.getHealth();
        float healthPercent = (float)currentHealth / maxHealth;
        healthPercent = Math.max(0, Math.min(1, healthPercent)); // Clamp between 0 and 1

        // Draw health bar with stronger colors for better visibility
        shapeRenderer.begin(ShapeType.Filled);
        // Draw health bar background (darker and more opaque)
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);

        // Draw health bar fill with more saturated colors
        Color healthColor = getHealthBarColor(healthPercent);
        shapeRenderer.setColor(healthColor);
        shapeRenderer.rect(barX, barY, barWidth * healthPercent, barHeight);
        shapeRenderer.end();

        // Draw border with thicker line for better visibility
        shapeRenderer.begin(ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);
        shapeRenderer.end();

        // Draw health number inside the bar
        batch.begin();
        // Format health display text
        String healthText = currentHealth + "/" + maxHealth;
        // Get text dimensions for centering
        float textWidth = font.getCache().addText(healthText, 0, 0).width;
        float textHeight = font.getLineHeight();
        // Calculate position to center text in health bar
        float textX = barX + (barWidth - textWidth) / 2;
        float textY = barY + (barHeight + textHeight) / 2;
        // Draw text with contrasting color (black or white depending on health)
        font.setColor(healthPercent > 0.5f ? Color.BLACK : Color.WHITE);
        font.draw(batch, healthText, textX, textY);
        // Reset font color for other UI elements
        font.setColor(Color.WHITE);

        batch.end();

        // Reset line width
        Gdx.gl.glLineWidth(1f);
        // Reset blend function
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
    }

    private Color getHealthBarColor(float healthPercent) {
        if (healthPercent > 0.6f) {
            return new Color(0f, 0.8f, 0f, 1f); // Brighter green
        } else if (healthPercent > 0.3f) {
            return new Color(1f, 0.6f, 0f, 1f); // Brighter orange
        } else {
            return new Color(1f, 0f, 0f, 1f); // Pure red
        }
    }

    private void renderObjectiveBar() {
        // Set projection matrix to match camera for consistent positioning
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Bar dimensions and position
        float barWidth = 300;
        float barHeight = 20;
        float barX = camera.position.x - barWidth / 2; // Centered horizontally
        float barY = camera.position.y + camera.viewportHeight / 2 - 50; // Near the top of the screen

        // Draw the bar background
        shapeRenderer.begin(ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f); // Dark background
        shapeRenderer.rect(barX, barY, barWidth, barHeight);

        // Draw the progress fill
        shapeRenderer.setColor(1f, 0.5f, 0f, 1f); // Green for progress
        shapeRenderer.rect(barX, barY, barWidth * objectiveProgress, barHeight);
        shapeRenderer.end();

        // Draw the border
        shapeRenderer.begin(ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);
        shapeRenderer.end();

        // Draw the objective text
        batch.begin();
        String objectiveText = finalBossObjective ? "Defeat the Final Boss" : "Defeat Basic Enemies: " + killedBasicEnemyCount + "/" + (int) MAX_BASIC_ENEMY_KILLS;
        float textWidth = font.getCache().addText(objectiveText, 0, 0).width;
        font.draw(batch, objectiveText, barX + (barWidth - textWidth) / 2, barY + barHeight + 15);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        // Improved resize handling for fullscreen
        viewport.update(width, height, true);
        camera.update();
        // Make sure we're showing a good portion of the world
        float viewportWidth = worldWidth / 2;
        float viewportHeight = worldHeight / 2;
        // Ensure we maintain a consistent zoom level
        camera.viewportWidth = viewportWidth;
        camera.viewportHeight = viewportHeight;
        camera.update();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        player.dispose();
        font.dispose();
        shapeRenderer.dispose(); // Dispose the shape renderer
        swordSound.dispose(); // Dispose the sword sound
        runningSound.dispose(); // Dispose of the running sound
        for (Enemy enemy : enemies) {
            enemy.dispose();
        }
        if (boss != null) {
            boss.dispose();
        }
        for (Obstacle obstacle : obstacles) {
            obstacle.dispose();
        }
        // Dispose TiledMap resources
        map.dispose();
        mapRenderer.dispose();

        // Clean up static resources correctly
        try {
            FinalBoss.disposeStaticResources();
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Error disposing resources", e);
        }
        // Dispose the hitbox renderer
        HitboxRenderer.dispose();
        // Dispose mini map
        miniMap.dispose();
    }

    private void spawnEnemy() {
        float x, y;
        float spawnDistance = 150;
        float halfWidth = worldWidth / 4;
        float halfHeight = worldHeight / 4;
        int side = random.nextInt(4);

        switch (side) {
            case 0: x = -halfWidth + random.nextFloat() * worldWidth / 2; y = halfHeight + spawnDistance; break;
            case 1: x = halfWidth + spawnDistance; y = -halfHeight + random.nextFloat() * worldHeight / 2; break;
            case 2: x = -halfWidth + random.nextFloat() * worldWidth / 2; y = -halfHeight - spawnDistance; break;
            case 3: x = -halfWidth - spawnDistance; y = -halfHeight + random.nextFloat() * worldHeight / 2; break;
            default: x = 0; y = 0;
        }
        enemies.add(new BasicEnemy(x, y, obstacles)); // obstacles を渡す
    }

    // Method to spawn the Final Boss
    private void spawnBoss() {
        float spawnDistance = 200;
        Vector2 playerPos = player.getPosition();
        int attempts = 10;

        for (int i = 0; i < attempts; i++) {
            float angle = (float) (Math.random() * Math.PI * 2);
            float x = playerPos.x + spawnDistance * (float) Math.cos(angle);
            float y = playerPos.y + spawnDistance * (float) Math.sin(angle);
            Rectangle futureBounds = new Rectangle(x - 75, y - 75, 150, 150); // Adjusted for center-based bounds

            boolean collides = false;
            for (Obstacle obs : obstacles) {
                if (obs.getBounds().overlaps(futureBounds)) {
                    collides = true;
                    break;
                }
            }

            if (!collides) {
                boss = new FinalBoss(x, y);
                enemies.add(boss);
                bossSpawned = true;
                finalBossObjective = true; // Switch to final boss objective
                objectiveProgress = 0; // Reset the progression bar
                return;
            }
        }

        // Fallback: place near player with no check (not recommended long-term)
        boss = new FinalBoss(playerPos.x + 150, playerPos.y + 150);
        enemies.add(boss);
        bossSpawned = true;
        finalBossObjective = true; // Switch to final boss objective
        objectiveProgress = 0; // Reset the progression bar
    }

    public static List<Obstacle> getStaticObstacles() {
        return GameScreenHolder.instance.obstacles;
    }

    private static class GameScreenHolder {
        static GameScreen instance;
    }

    private void checkMapObjectCollisions() {
        // Get the objects layer - replace "Objects" with your layer name
        MapLayer objectsLayer = map.getLayers().get("Objects");
        if (objectsLayer == null) {
            return; // No objects layer found
        }

        // Check collision with each object in the layer
        for (MapObject mapObject : objectsLayer.getObjects()) {
            if (mapObject instanceof RectangleMapObject) {
                String objectName = mapObject.getName();
                if (objectName == null) objectName = "Unnamed Object";

                // Get object type (optional)
                String objectType = mapObject.getProperties().get("type", String.class);
                if (objectType != null) {
                    objectName = objectType + ": " + objectName;
                }

                // Get object bounds
                Rectangle objectBounds = ((RectangleMapObject) mapObject).getRectangle();

                // Check collision with player
                if (player.checkCollisionWithObject(objectBounds, objectName)) {
                    // Handle collision based on object type
                    handleObjectCollision(mapObject);
                }
            }
        }
    }

    private void handleObjectCollision(MapObject mapObject) {
        // Get object properties
        String objectType = mapObject.getProperties().get("type", String.class);

        // Handle different object types
        if ("door".equalsIgnoreCase(objectType)) {
            // Handle door interaction
            System.out.println("Player is at a door. Press 'E' to enter.");
        } else if ("chest".equalsIgnoreCase(objectType)) {
            // Handle chest interaction
            System.out.println("Player found a chest! Press 'E' to open.");
        } else if ("enemy_spawn".equalsIgnoreCase(objectType)) {
            // Handle enemy spawn point
            System.out.println("Player entered an enemy spawn area!");
        }
        // Add more object types as needed
    }

    public static Player getPlayer() {
        return GameScreenHolder.instance.player;
    }
}
