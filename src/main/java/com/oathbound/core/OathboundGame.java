package com.oathbound.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.ArrayList;
import java.util.List;

public class OathboundGame extends ApplicationAdapter {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera; 
    
    // Background Layers
    private Texture bgLayer1;
    private Texture bgLayer2;
    private Texture bgLayer3;
    
    // Game Entities
    private Player player;
    private HUD hud;
    private TileMapLoader mapLoader;
    private VowStone vowStone;
    private List<Enemy> enemies;
    private List<Projectile> projectiles; // Added for Mage and Archer

    // Level & Character Management
    private int currentLevel = 1;
    private final int MAX_LEVELS = 9;
    private int currentCharacterIndex = 0; // 0=Samurai, 1=Beastman, 2=Mage, 3=Archer

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        
        // SETUP CAMERA (true = Y points DOWN like Java Swing)
        camera = new OrthographicCamera();
        camera.setToOrtho(true, 1280, 736); 
        
        // Load Background Layers
        bgLayer1 = new Texture(Gdx.files.internal("sprites/layer_1.png"));
        bgLayer2 = new Texture(Gdx.files.internal("sprites/layer_2.png"));
        bgLayer3 = new Texture(Gdx.files.internal("sprites/layer_3.png"));
        
        hud = new HUD();
        mapLoader = new TileMapLoader();
        enemies = new ArrayList<>();
        projectiles = new ArrayList<>();
        
        // Start the game at level 1 with 5 health
        loadLevel(currentLevel, 5);
    }

    /**
     * Dynamically loads a level file and spawns all entities based on the map data.
     * @param level The level number to load (1-9)
     * @param startingHealth The health the player should have when spawning in
     */
    private void loadLevel(int level, int startingHealth) {
        enemies.clear();
        projectiles.clear(); // Clear old arrows/magic when changing rooms
        
        // LibGDX automatically looks in src/main/resources/ for internal files!
        String levelPath = "levels/level_" + level + ".txt";
        System.out.println("Loading Level: " + levelPath);
        mapLoader.load(levelPath);
        
        // Spawn Player at the 'P' tile
        int[] spawn = mapLoader.getPlayerSpawn();
        spawnCurrentCharacter(spawn[0], spawn[1], startingHealth); 
        
        // Spawn VowStone at the 'V' tile
        List<int[]> vowPositions = mapLoader.getVowStonePositions();
        if (!vowPositions.isEmpty()) {
            vowStone = new VowStone(vowPositions.get(0)[0], vowPositions.get(0)[1]);
        } else {
            vowStone = new VowStone(1100, 200); // Fallback position
        }

        // Spawn Enemies at all 'E' tiles
        for (int[] ePos : mapLoader.getEnemyPositions()) {
            enemies.add(new Enemy(ePos[0], ePos[1]));
        }
    }

    /**
     * Helper to spawn the currently selected character class.
     */
    private void spawnCurrentCharacter(int x, int y, int health) {
        if (player != null) player.dispose(); // Free up texture memory
        
        switch (currentCharacterIndex) {
            case 0: player = new Samurai(x, y); break;
            case 1: player = new Beastman(x, y); break;
            case 2: player = new Mage(x, y, projectiles); break;
            case 3: player = new Archer(x, y, projectiles); break;
            default: player = new Samurai(x, y); break;
        }
        player.setHealth(health);
    }

    /**
     * Cycles through playable character classes dynamically.
     */
    private void switchCharacter() {
        if (player == null) return;
        
        int px = (int) player.getBounds().x;
        int py = (int) player.getBounds().y;
        int currentHealth = player.getHealth();
        
        currentCharacterIndex++;
        if (currentCharacterIndex > 3) currentCharacterIndex = 0;
        
        spawnCurrentCharacter(px, py, currentHealth);
        System.out.println("Switched Character Class -> Index: " + currentCharacterIndex);
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        
        // --- 1. INPUT PROCESSING ---
        // Tab Character Switching
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            switchCharacter();
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) player.setLeft(true);
        else player.setLeft(false);

        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) player.setRight(true);
        else player.setRight(false);
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) player.jump();
        if (Gdx.input.isKeyJustPressed(Input.Keys.J) || Gdx.input.isKeyJustPressed(Input.Keys.Z)) player.attack();

        // --- 2. LOGIC UPDATE ---
        player.update(dt, mapLoader.getSolidTiles());
        
        // Update all active enemies
        for (Enemy enemy : enemies) {
            enemy.update(dt, mapLoader.getSolidTiles());
        }

        // Update Projectiles (Arrows/Magic)
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update(dt, mapLoader.getSolidTiles());
            if (!p.isActive()) {
                projectiles.remove(i);
            }
        }

        // Pit Detection (Death Plane) - Checks if player falls off the bottom of the screen
        if (player.getBounds().y > 800) {
            int remainingHealth = player.getHealth() - 1;
            if (remainingHealth > 0) {
                System.out.println("Fell into a pit! Lives remaining: " + remainingHealth);
                loadLevel(currentLevel, remainingHealth); // Restart current room with 1 less life
            } else {
                System.out.println("Game Over! Restarting from Trial 1.");
                currentLevel = 1;
                loadLevel(currentLevel, 5); // Full reset
            }
            return; // Skip rendering this frame to prevent graphical glitches during load
        }

        // Level Transition Logic
        if (vowStone.checkCollision(player.getBounds())) {
            currentLevel++;
            int nextHealth = player.getHealth(); // Preserve health going into the next level
            
            if (currentLevel > MAX_LEVELS) {
                System.out.println("You beat the Ten Trials!");
                currentLevel = 1; // Loop back to start
                nextHealth = 5; // Reset health upon beating the game
            }
            loadLevel(currentLevel, nextHealth);
            return; // Skip rendering this frame to prevent graphical glitches during load
        }

        // --- 3. RENDERING ---
        // Clear the screen with a dark sky color
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        
        // CRITICAL FIX: Tell the renderers to actually use the Camera!
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        
        // DRAW BACKGROUNDS
        batch.begin();
        drawBackground(batch);
        batch.end();

        // Enable blending so semi-transparent HUD colors and anti-aliasing work
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // A) Draw Shapes (Map, VowStone, UI, Projectiles)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        mapLoader.render(shapeRenderer);
        vowStone.render(shapeRenderer);
        hud.render(shapeRenderer, player);
        
        // Render enemies
        for (Enemy enemy : enemies) {
            enemy.render(shapeRenderer);
        }
        
        // Render Projectiles
        for (Projectile p : projectiles) {
            p.render(shapeRenderer);
        }
        shapeRenderer.end();

        // B) Draw Textures (Player)
        batch.begin();
        player.render(batch);
        batch.end();
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /**
     * Ported PB-022 Parallax Background drawing logic.
     */
    private void drawBackground(SpriteBatch batch) {
        int w = 1280;
        int h = 736;
        float camX = (player != null) ? player.getBounds().x : 0;

        // Layer 1: Sky/moon — stretch to fill entire screen, NO tiling, NO scroll
        if (bgLayer1 != null) {
            batch.draw(bgLayer1, 0, 0, w, h, 0, 0, bgLayer1.getWidth(), bgLayer1.getHeight(), false, true);
        }

        // Layer 2: Castle buildings — tile horizontally, sits at the BOTTOM of screen
        // slow parallax scroll
        if (bgLayer2 != null) {
            int imgW = bgLayer2.getWidth();
            int imgH = bgLayer2.getHeight();
            
            float scale = 2.5f; // increase this to make buildings bigger
            int drawW = (int)(imgW * scale);
            int drawH = (int)(imgH * scale);
            int drawY = h - drawH; // still pinned to bottom
            
            int offsetX = -(int)(camX * 0.2f) % drawW;
            if (offsetX > 0) offsetX -= drawW;
            for (int x = offsetX; x < w; x += drawW) {
                batch.draw(bgLayer2, x, drawY, drawW, drawH, 0, 0, imgW, imgH, false, true);
            }
        }

        // Layer 3: Dirt/ground strip — tile horizontally, sits just above tile ground
        // faster parallax scroll
        if (bgLayer3 != null) {
            int imgW = bgLayer3.getWidth();
            int imgH = bgLayer3.getHeight();
            int drawY = h - imgH; // also pins to bottom, overlaps castle base
            int offsetX = -(int)(camX * 0.5f) % imgW;
            if (offsetX > 0) offsetX -= imgW;
            for (int x = offsetX; x < w; x += imgW) {
                batch.draw(bgLayer3, x, drawY, imgW, imgH, 0, 0, imgW, imgH, false, true);
            }
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        
        if (bgLayer1 != null) bgLayer1.dispose();
        if (bgLayer2 != null) bgLayer2.dispose();
        if (bgLayer3 != null) bgLayer3.dispose();
        
        if (player != null) player.dispose();
    }
}