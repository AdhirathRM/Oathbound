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
import java.awt.Rectangle;
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
    private List<Projectile> projectiles;

    // Level & Character Management
    private int currentLevel = 1;
    private final int MAX_LEVELS = 9;
    private int currentCharacterIndex = 0; 
    
    // Store spawn point for soft-resets on pit falls or spikes
    private int spawnX;
    private int spawnY;
    
    private float playerDamageTimer = 0f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        
        camera = new OrthographicCamera();
        camera.setToOrtho(true, 1280, 736); 
        
        bgLayer1 = new Texture(Gdx.files.internal("sprites/layer_1.png"));
        bgLayer2 = new Texture(Gdx.files.internal("sprites/layer_2.png"));
        bgLayer3 = new Texture(Gdx.files.internal("sprites/layer_3.png"));
        
        hud = new HUD();
        mapLoader = new TileMapLoader();
        enemies = new ArrayList<>();
        projectiles = new ArrayList<>();
        
        loadLevel(currentLevel, 5);
    }

    private void loadLevel(int level, int startingHealth) {
        enemies.clear();
        projectiles.clear(); 
        
        String levelPath = "levels/level_" + level + ".txt";
        System.out.println("Loading Level: " + levelPath);
        mapLoader.load(levelPath);
        
        int[] spawn = mapLoader.getPlayerSpawn();
        spawnX = spawn[0];
        spawnY = spawn[1];
        spawnCurrentCharacter(spawnX, spawnY, startingHealth); 
        
        List<int[]> vowPositions = mapLoader.getVowStonePositions();
        if (!vowPositions.isEmpty()) {
            vowStone = new VowStone(vowPositions.get(0)[0], vowPositions.get(0)[1]);
        } else {
            vowStone = new VowStone(1100, 200); 
        }

        for (int[] ePos : mapLoader.getEnemyPositions()) {
            enemies.add(new Enemy(ePos[0], ePos[1]));
        }
    }

    private void spawnCurrentCharacter(int x, int y, int health) {
        if (player != null) player.dispose(); 
        
        switch (currentCharacterIndex) {
            case 0: player = new Player(x, y); break; 
            case 1: player = new Samurai(x, y); break;
            case 2: player = new Beastman(x, y); break;
            case 3: player = new Mage(x, y, projectiles); break;
            case 4: player = new Archer(x, y, projectiles); break;
            default: player = new Player(x, y); break;
        }
        player.setHealth(health);
        playerDamageTimer = 1.0f;
    }

    private void switchCharacter() {
        if (player == null) return;
        
        int px = (int) player.getBounds().x;
        int py = (int) player.getBounds().y;
        int currentHealth = player.getHealth();
        
        currentCharacterIndex++;
        if (currentCharacterIndex > 4) currentCharacterIndex = 0; 
        
        spawnCurrentCharacter(px, py, currentHealth);
        System.out.println("Switched Character Class -> Index: " + currentCharacterIndex);
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        
        // --- 1. INPUT PROCESSING ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) switchCharacter();
        
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) player.setLeft(true);
        else player.setLeft(false);

        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) player.setRight(true);
        else player.setRight(false);
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) player.jump();
        if (Gdx.input.isKeyJustPressed(Input.Keys.J) || Gdx.input.isKeyJustPressed(Input.Keys.Z)) player.attack();

        // --- 2. LOGIC UPDATE ---
        player.update(dt, mapLoader.getSolidTiles());
        
        for (Enemy enemy : enemies) {
            enemy.update(dt, mapLoader.getSolidTiles());
        }

        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update(dt, mapLoader.getSolidTiles());
            if (!p.isActive()) {
                projectiles.remove(i);
            }
        }
        
        // --- 3. COLLISION & COMBAT SYSTEM ---
        if (playerDamageTimer > 0) playerDamageTimer -= dt;

        for (Enemy enemy : enemies) {
            if (!enemy.isActive()) continue;

            float px = player.getBounds().x + player.getBounds().width / 2f;
            float py = player.getBounds().y + player.getBounds().height / 2f;
            float ex = enemy.getBounds().x + enemy.getBounds().width / 2f;
            float ey = enemy.getBounds().y + enemy.getBounds().height / 2f;

            float distX = Math.abs(px - ex);
            float distY = Math.abs(py - ey);

            if (distX < 100 && distY < 60) {
                if (px < ex) enemy.setDirection(-1); 
                else enemy.setDirection(1);          
                
                enemy.triggerAttack();
            }

            if (enemy.getAttackHitbox() != null && enemy.getAttackHitbox().width > 0) {
                if (playerDamageTimer <= 0 && player.getBounds().intersects(enemy.getAttackHitbox())) {
                    player.takeDamage(1);
                    playerDamageTimer = 1.0f;
                    System.out.println("Enemy slashed player! Health: " + player.getHealth());
                }
            }
            
            if (player.getAttackHitbox().width > 0) { 
                if (player.getAttackHitbox().intersects(enemy.getBounds())) {
                    enemy.takeDamage(1); 
                }
            }
        }
        
        for (Projectile p : projectiles) {
            if (!p.isActive()) continue;
            
            if (p.isAOE()) {
                boolean hitEnemyDirectly = false;
                
                if (!p.isExploding()) {
                    for (Enemy enemy : enemies) {
                        if (enemy.isActive() && p.getBounds().intersects(enemy.getBounds())) {
                            hitEnemyDirectly = true;
                            break;
                        }
                    }
                }
                
                if ((hitEnemyDirectly || p.isExploding()) && !p.isDamageApplied()) {
                    p.triggerExplosion(); 
                    p.setDamageApplied(true); 
                    
                    float cx = p.getBounds().x + p.getBounds().width / 2f;
                    float cy = p.getBounds().y + p.getBounds().height / 2f;
                    
                    for (Enemy e : enemies) {
                        if (!e.isActive()) continue;
                        float ex = e.getBounds().x + e.getBounds().width / 2f;
                        float ey = e.getBounds().y + e.getBounds().height / 2f;
                        
                        float dist = (float) Math.hypot(cx - ex, cy - ey);
                        if (dist <= p.getExplosionRadius()) {
                            e.takeDamage(2); 
                        }
                    }
                }
            } else {
                for (Enemy enemy : enemies) {
                    if (!enemy.isActive()) continue;
                    if (p.getBounds().intersects(enemy.getBounds())) {
                        enemy.takeDamage(1); 
                        p.deactivate(); 
                        break; 
                    }
                }
            }
        }

        // --- 4. MAP TRANSITIONS, PITS & TRAPS ---
        
        // NEW: Check if player touches any Spike Traps
        boolean hitSpikes = false;
        // Only trigger spikes if the player isn't currently invincible from another attack
        if (playerDamageTimer <= 0) { 
            for (Rectangle trap : mapLoader.getTrapTiles()) {
                if (player.getBounds().intersects(trap)) {
                    hitSpikes = true;
                    break;
                }
            }
        }

        // Trigger Soft-Respawn on Pit Fall OR Spike Trap collision
        if (player.getBounds().y > 800 || hitSpikes) {
            int remainingHealth = player.getHealth() - 1;
            
            if (remainingHealth > 0) {
                System.out.println(hitSpikes ? "Hit a spike trap! Lives remaining: " + remainingHealth : "Fell into a pit! Lives remaining: " + remainingHealth);
                player.setHealth(remainingHealth);
                player.resetPosition(spawnX, spawnY); // Teleport to current room spawn
                playerDamageTimer = 1.0f; // Give brief safety window so you don't instantly die again
            } else {
                System.out.println("Game Over! Restarting from Trial 1.");
                currentLevel = 1;
                loadLevel(currentLevel, 5); // Full Game Over resets everything
            }
            return; 
        }

        if (vowStone.checkCollision(player.getBounds())) {
            currentLevel++;
            int nextHealth = player.getHealth(); 
            
            if (currentLevel > MAX_LEVELS) {
                System.out.println("You beat the Ten Trials!");
                currentLevel = 1; 
                nextHealth = 5; 
            }
            loadLevel(currentLevel, nextHealth);
            return; 
        }

        // --- 5. RENDERING ---
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        
        batch.begin();
        drawBackground(batch);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        mapLoader.render(shapeRenderer);
        vowStone.render(shapeRenderer);
        hud.render(shapeRenderer, player);
        
        for (Projectile p : projectiles) {
            p.render(shapeRenderer);
        }
        shapeRenderer.end();

        batch.begin();
        for (Enemy enemy : enemies) {
            enemy.render(batch);
        }
        player.render(batch);
        batch.end();
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawBackground(SpriteBatch batch) {
        int w = 1280;
        int h = 736;
        float camX = (player != null) ? player.getBounds().x : 0;

        if (bgLayer1 != null) {
            batch.draw(bgLayer1, 0, 0, w, h, 0, 0, bgLayer1.getWidth(), bgLayer1.getHeight(), false, true);
        }

        if (bgLayer2 != null) {
            int imgW = bgLayer2.getWidth();
            int imgH = bgLayer2.getHeight();
            float scale = 2.5f; 
            int drawW = (int)(imgW * scale);
            int drawH = (int)(imgH * scale);
            int drawY = h - drawH; 
            
            int offsetX = -(int)(camX * 0.2f) % drawW;
            if (offsetX > 0) offsetX -= drawW;
            for (int x = offsetX; x < w; x += drawW) {
                batch.draw(bgLayer2, x, drawY, drawW, drawH, 0, 0, imgW, imgH, false, true);
            }
        }

        if (bgLayer3 != null) {
            int imgW = bgLayer3.getWidth();
            int imgH = bgLayer3.getHeight();
            int drawY = h - imgH; 
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