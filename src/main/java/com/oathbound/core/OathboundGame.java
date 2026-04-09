package com.oathbound.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class OathboundGame extends ApplicationAdapter {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    
    // Font for UI overlays
    private BitmapFont uiFont;
    private BitmapFont buttonFont; 
    private BitmapFont largeTitleFont; 
    private BitmapFont smallButtonFont; 
    
    // --- GAME STATES ---
    public enum GameState { TITLE, PLAYING, LEVEL_COMPLETE, GAME_OVER }
    private GameState gameState = GameState.TITLE;
    private boolean showingRules = false;
    
    // UI & Background Layers
    private Texture titleScreenTex;
    private Texture buttonTex; 
    private Texture button2Tex; 
    private TextureRegion btn2Next, btn2Restart, btn2Menu; 
    private Texture levelCompleteTex; 
    private Texture gameOverTex; 
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
    
    private int spawnX;
    private int spawnY;
    private float playerDamageTimer = 0f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        
        camera = new OrthographicCamera();
        camera.setToOrtho(true, 1280, 736);

        if (Gdx.files.internal("fonts/medieval.fnt").exists()) {
            uiFont = new BitmapFont(Gdx.files.internal("fonts/medieval.fnt"), true);
            uiFont.getData().setScale(0.6f);
            
            buttonFont = new BitmapFont(Gdx.files.internal("fonts/medieval.fnt"), true);
            buttonFont.getData().setScale(0.85f); 
            
            largeTitleFont = new BitmapFont(Gdx.files.internal("fonts/medieval.fnt"), true);
            largeTitleFont.getData().setScale(1.2f); 
            
            smallButtonFont = new BitmapFont(Gdx.files.internal("fonts/medieval.fnt"), true);
            smallButtonFont.getData().setScale(0.55f); 
        } else {
            uiFont = new BitmapFont(true);
            uiFont.getData().setScale(1.2f);
            
            buttonFont = new BitmapFont(true);
            buttonFont.getData().setScale(1.8f);
            
            largeTitleFont = new BitmapFont(true);
            largeTitleFont.getData().setScale(2.2f);
            
            smallButtonFont = new BitmapFont(true);
            smallButtonFont.getData().setScale(1.0f);
        }
        
        // Load Textures
        if (Gdx.files.internal("sprites/title_screen.png").exists()) {
            titleScreenTex = new Texture(Gdx.files.internal("sprites/title_screen.png"));
        }
        if (Gdx.files.internal("sprites/button.png").exists()) {
            buttonTex = new Texture(Gdx.files.internal("sprites/button.png"));
        }
        
        // Load and slice button2.png
        if (Gdx.files.internal("sprites/button2.png").exists()) {
            button2Tex = new Texture(Gdx.files.internal("sprites/button2.png"));
            int sliceH = button2Tex.getHeight() / 3; 
            int fullW = button2Tex.getWidth();
            
            btn2Next = new TextureRegion(button2Tex, 0, 0, fullW, sliceH);
            btn2Next.flip(false, true);
            
            btn2Restart = new TextureRegion(button2Tex, 0, sliceH, fullW, sliceH);
            btn2Restart.flip(false, true);
            
            btn2Menu = new TextureRegion(button2Tex, 0, sliceH * 2, fullW, sliceH);
            btn2Menu.flip(false, true);
        }
        
        if (Gdx.files.internal("sprites/level_complete.png").exists()) {
            levelCompleteTex = new Texture(Gdx.files.internal("sprites/level_complete.png"));
        }

        if (Gdx.files.internal("sprites/you_died.png").exists()) {
            gameOverTex = new Texture(Gdx.files.internal("sprites/you_died.png"));
        }
        
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
        
        if (vowStone != null) vowStone.dispose();
        
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
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.05f, 0.05f, 0.1f, 1f);
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        
        if (gameState == GameState.TITLE) {
            renderTitleScreen();
        } else if (gameState == GameState.PLAYING) {
            renderGameplay();
        } else if (gameState == GameState.LEVEL_COMPLETE) {
            renderLevelComplete(); 
        } else if (gameState == GameState.GAME_OVER) {
            renderGameOver(); 
        }
    }

    private void renderTitleScreen() {
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);
        float mx = mousePos.x;
        float my = mousePos.y;

        // UPDATED: Scaled up Title buttons to 460x240
        Rectangle playBtn = new Rectangle((1280 / 2) - 480, 460, 460, 240);
        Rectangle rulesBtn = new Rectangle((1280 / 2) + 20, 460, 460, 240);

        boolean playHover = playBtn.contains(mx, my);
        boolean rulesHover = rulesBtn.contains(mx, my);

        batch.begin();
        if (titleScreenTex != null) {
            batch.draw(titleScreenTex, 0, 0, 1280, 736, 0, 0, titleScreenTex.getWidth(), titleScreenTex.getHeight(), false, true);
        }

        if (buttonTex != null) {
            if (playHover) batch.setColor(0.8f, 0.6f, 1.0f, 1f);
            else batch.setColor(Color.WHITE);
            batch.draw(buttonTex, playBtn.x, playBtn.y, playBtn.width, playBtn.height, 0, 0, buttonTex.getWidth(), buttonTex.getHeight(), false, true);

            if (rulesHover) batch.setColor(0.8f, 0.6f, 1.0f, 1f);
            else batch.setColor(Color.WHITE);
            batch.draw(buttonTex, rulesBtn.x, rulesBtn.y, rulesBtn.width, rulesBtn.height, 0, 0, buttonTex.getWidth(), buttonTex.getHeight(), false, true);
            
            batch.setColor(Color.WHITE);
        }
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (buttonTex == null) {
            shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 0.9f);
            shapeRenderer.rect(playBtn.x, playBtn.y, playBtn.width, playBtn.height);
            shapeRenderer.rect(rulesBtn.x, rulesBtn.y, rulesBtn.width, rulesBtn.height);
            
            if (playHover) {
                shapeRenderer.setColor(0.6f, 0.2f, 1.0f, 0.35f);
                shapeRenderer.rect(playBtn.x, playBtn.y, playBtn.width, playBtn.height);
            }
            if (rulesHover) {
                shapeRenderer.setColor(0.6f, 0.2f, 1.0f, 0.35f);
                shapeRenderer.rect(rulesBtn.x, rulesBtn.y, rulesBtn.width, rulesBtn.height);
            }
        }

        if (showingRules) {
            shapeRenderer.setColor(0.05f, 0.05f, 0.1f, 0.85f); 
            shapeRenderer.rect(0, 0, 1280, 736);
            
            int bx = 290;
            int by = 120;
            int bw = 700;
            int bh = 500;

            shapeRenderer.setColor(0.8f, 0.65f, 0.15f, 1f);
            shapeRenderer.rect(bx - 6, by - 6, bw + 12, bh + 12);
            shapeRenderer.setColor(0.6f, 0.45f, 0.1f, 1f); 
            shapeRenderer.rect(bx - 2, by - 2, bw + 4, bh + 4);

            shapeRenderer.setColor(0.12f, 0.08f, 0.18f, 0.95f);
            shapeRenderer.rect(bx, by, bw, bh);

            shapeRenderer.setColor(0.8f, 0.65f, 0.15f, 0.3f);
            shapeRenderer.rect(bx + 10, by + 10, bw - 20, 2);
            shapeRenderer.rect(bx + 10, by + bh - 12, bw - 20, 2);
            shapeRenderer.rect(bx + 10, by + 10, 2, bh - 20);
            shapeRenderer.rect(bx + bw - 12, by + 10, 2, bh - 20);

            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
            
            batch.begin();
            GlyphLayout titleLayout = new GlyphLayout(uiFont, "HOW TO PLAY");
            uiFont.setColor(Color.GOLD);
            uiFont.draw(batch, titleLayout, bx + (bw - titleLayout.width) / 2, by + 70);
            
            uiFont.setColor(Color.WHITE);
            int textX = bx + 80;
            int textY = by + 150;
            int spacing = 65;

            uiFont.draw(batch, "Move: A / D or Left / Right", textX, textY);
            uiFont.draw(batch, "Jump: W or Space", textX, textY + spacing);
            uiFont.draw(batch, "Attack: J or Z", textX, textY + spacing * 2);
            uiFont.draw(batch, "Switch Class: TAB", textX, textY + spacing * 3);
            
            uiFont.setColor(Color.FIREBRICK);
            GlyphLayout bottomLayout = new GlyphLayout(uiFont, "Reach the Vow Stone to advance");
            uiFont.draw(batch, bottomLayout, bx + (bw - bottomLayout.width) / 2, by + bh - 40);
            batch.end();

            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                showingRules = false;
            }
            return; 
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (playHover && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            currentLevel = 1;
            loadLevel(currentLevel, 5);
            gameState = GameState.PLAYING;
        }
        
        if (rulesHover && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) showingRules = true;

        batch.begin();
        Color customGold = Color.valueOf("ffe14d");

        buttonFont.setColor(playHover ? Color.WHITE : customGold);
        GlyphLayout playLayout = new GlyphLayout(buttonFont, "PLAY");
        buttonFont.draw(batch, playLayout, playBtn.x + (playBtn.width - playLayout.width) / 2, playBtn.y + (playBtn.height - playLayout.height) / 2);

        buttonFont.setColor(rulesHover ? Color.WHITE : customGold);
        GlyphLayout rulesLayout = new GlyphLayout(buttonFont, "RULES");
        buttonFont.draw(batch, rulesLayout, rulesBtn.x + (rulesBtn.width - rulesLayout.width) / 2, rulesBtn.y + (rulesBtn.height - rulesLayout.height) / 2);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            currentLevel = 1;
            loadLevel(currentLevel, 5); 
            gameState = GameState.PLAYING;
        }
    }

    private void renderLevelComplete() {
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);
        float mx = mousePos.x;
        float my = mousePos.y;

        Rectangle topBox = new Rectangle((1280 / 2) - 590, 5, 1180, 480); 

        int btnW = 560;
        int btnH = 96; 
        int startY = 410;
        int gap = btnH + 12; 

        Rectangle nextBtn = new Rectangle((1280 / 2) - (btnW / 2), startY, btnW, btnH);
        Rectangle restartBtn = new Rectangle((1280 / 2) - (btnW / 2), startY + gap, btnW, btnH);
        Rectangle menuBtn = new Rectangle((1280 / 2) - (btnW / 2), startY + gap * 2, btnW, btnH);

        boolean nextHover = nextBtn.contains(mx, my);
        boolean restartHover = restartBtn.contains(mx, my);
        boolean menuHover = menuBtn.contains(mx, my);

        batch.begin();
        
        if (levelCompleteTex != null) {
            batch.draw(levelCompleteTex, 0, 0, 1280, 736, 0, 0, levelCompleteTex.getWidth(), levelCompleteTex.getHeight(), false, true);
        }
        
        if (buttonTex != null) {
            batch.setColor(Color.WHITE);
            batch.draw(buttonTex, topBox.x, topBox.y, topBox.width, topBox.height, 0, 0, buttonTex.getWidth(), buttonTex.getHeight(), false, true);
        }

        if (button2Tex != null) {
            if (nextHover) batch.setColor(0.8f, 0.6f, 1.0f, 1f); else batch.setColor(Color.WHITE);
            batch.draw(btn2Next, nextBtn.x, nextBtn.y, nextBtn.width, nextBtn.height);

            if (restartHover) batch.setColor(0.8f, 0.6f, 1.0f, 1f); else batch.setColor(Color.WHITE);
            batch.draw(btn2Restart, restartBtn.x, restartBtn.y, restartBtn.width, restartBtn.height);

            if (menuHover) batch.setColor(0.8f, 0.6f, 1.0f, 1f); else batch.setColor(Color.WHITE);
            batch.draw(btn2Menu, menuBtn.x, menuBtn.y, menuBtn.width, menuBtn.height);

            batch.setColor(Color.WHITE); 
        }
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (button2Tex == null) {
            shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 0.9f);
            shapeRenderer.rect(topBox.x, topBox.y, topBox.width, topBox.height);
            shapeRenderer.rect(nextBtn.x, nextBtn.y, nextBtn.width, nextBtn.height);
            shapeRenderer.rect(restartBtn.x, restartBtn.y, restartBtn.width, restartBtn.height);
            shapeRenderer.rect(menuBtn.x, menuBtn.y, menuBtn.width, menuBtn.height);
        }

        if (nextHover && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            currentLevel++;
            if (currentLevel > MAX_LEVELS) currentLevel = 1; 
            loadLevel(currentLevel, player.getHealth());
            gameState = GameState.PLAYING;
        }
        if (restartHover && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            loadLevel(currentLevel, 5); 
            gameState = GameState.PLAYING;
        }
        if (menuHover && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            gameState = GameState.TITLE;
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        Color customGold = Color.valueOf("ffe14d");

        largeTitleFont.setColor(customGold);
        String completeText = "TRIAL " + currentLevel + " COMPLETED";
        GlyphLayout completeLayout = new GlyphLayout(largeTitleFont, completeText);
        
        float topCenterY = topBox.y + (topBox.height / 2);
        largeTitleFont.draw(batch, completeLayout, topBox.x + (topBox.width - completeLayout.width) / 2, topCenterY + (completeLayout.height / 2) - 35);

        smallButtonFont.setColor(nextHover ? Color.WHITE : Color.LIGHT_GRAY);
        GlyphLayout nextLayout = new GlyphLayout(smallButtonFont, "NEXT TRIAL");
        float nextTextX = nextBtn.x + (nextBtn.width - nextLayout.width) / 2 - 10; 
        float nextTextY = nextBtn.y + (nextBtn.height - nextLayout.height) / 2 + 16.5f; 
        smallButtonFont.draw(batch, nextLayout, nextTextX, nextTextY);

        smallButtonFont.setColor(restartHover ? Color.WHITE : Color.LIGHT_GRAY);
        GlyphLayout restLayout = new GlyphLayout(smallButtonFont, "RESTART");
        float restTextX = restartBtn.x + (restartBtn.width - restLayout.width) / 2 + 5; 
        float restTextY = restartBtn.y + (restartBtn.height - restLayout.height) / 2 + 4f; 
        smallButtonFont.draw(batch, restLayout, restTextX, restTextY);

        smallButtonFont.setColor(menuHover ? Color.WHITE : Color.LIGHT_GRAY);
        GlyphLayout menuLayout = new GlyphLayout(smallButtonFont, "MAIN MENU");
        float menuTextX = menuBtn.x + (menuBtn.width - menuLayout.width) / 2 - 5; 
        float menuTextY = menuBtn.y + (menuBtn.height - menuLayout.height) / 2 - 8; 
        smallButtonFont.draw(batch, menuLayout, menuTextX, menuTextY);

        batch.end();
    }

    private void renderGameOver() {
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);
        float mx = mousePos.x;
        float my = mousePos.y;

        // UPDATED: Scaled up Game Over buttons to 460x240 and side-by-side
        int btnW = 460;
        int btnH = 240;
        int startY = 460;
        int spacing = 40;

        Rectangle restartBtn = new Rectangle((1280 / 2) - btnW - spacing/2, startY, btnW, btnH);
        Rectangle menuBtn = new Rectangle((1280 / 2) + spacing/2, startY, btnW, btnH);

        boolean restartHover = restartBtn.contains(mx, my);
        boolean menuHover = menuBtn.contains(mx, my);

        batch.begin();
        if (gameOverTex != null) {
            batch.draw(gameOverTex, 0, 0, 1280, 736, 0, 0, gameOverTex.getWidth(), gameOverTex.getHeight(), false, true);
        }

        if (buttonTex != null) {
            // Draw Restart Button (button.png)
            if (restartHover) batch.setColor(0.8f, 0.6f, 1.0f, 1f); else batch.setColor(Color.WHITE);
            batch.draw(buttonTex, restartBtn.x, restartBtn.y, restartBtn.width, restartBtn.height, 0, 0, buttonTex.getWidth(), buttonTex.getHeight(), false, true);

            // Draw Menu Button (button.png)
            if (menuHover) batch.setColor(0.8f, 0.6f, 1.0f, 1f); else batch.setColor(Color.WHITE);
            batch.draw(buttonTex, menuBtn.x, menuBtn.y, menuBtn.width, menuBtn.height, 0, 0, buttonTex.getWidth(), buttonTex.getHeight(), false, true);

            batch.setColor(Color.WHITE);
        }
        batch.end();

        // Handle clicks
        if (restartHover && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            currentLevel = 1;
            loadLevel(currentLevel, 5);
            gameState = GameState.PLAYING;
        }
        if (menuHover && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            gameState = GameState.TITLE;
        }

        // Draw Text
        batch.begin();
        buttonFont.setColor(restartHover ? Color.WHITE : Color.valueOf("ffe14d"));
        GlyphLayout restLayout = new GlyphLayout(buttonFont, "RESTART");
        buttonFont.draw(batch, restLayout, restartBtn.x + (restartBtn.width - restLayout.width) / 2, restartBtn.y + (restartBtn.height - restLayout.height) / 2);

        buttonFont.setColor(menuHover ? Color.WHITE : Color.valueOf("ffe14d"));
        GlyphLayout menuLayout = new GlyphLayout(buttonFont, "MAIN MENU");
        buttonFont.draw(batch, menuLayout, menuBtn.x + (menuBtn.width - menuLayout.width) / 2, menuBtn.y + (menuBtn.height - menuLayout.height) / 2);
        batch.end();
    }

    private void renderGameplay() {
        float dt = Gdx.graphics.getDeltaTime();
        
        // CHECK DEATH
        if (player.getHealth() <= 0) {
            gameState = GameState.GAME_OVER;
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) switchCharacter();
        
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) player.setLeft(true);
        else player.setLeft(false);

        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) player.setRight(true);
        else player.setRight(false);
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) player.jump();
        if (Gdx.input.isKeyJustPressed(Input.Keys.J) || Gdx.input.isKeyJustPressed(Input.Keys.Z)) player.attack();

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

        boolean hitSpikes = false;
        if (playerDamageTimer <= 0) { 
            for (Rectangle trap : mapLoader.getTrapTiles()) {
                if (player.getBounds().intersects(trap)) {
                    hitSpikes = true;
                    break;
                }
            }
        }

        if (player.getBounds().y > 800 || hitSpikes) {
            int remainingHealth = player.getHealth() - 1;
            player.setHealth(remainingHealth);
            if (remainingHealth > 0) {
                player.resetPosition(spawnX, spawnY); 
                playerDamageTimer = 1.0f; 
            } else {
                gameState = GameState.GAME_OVER; 
            }
            return; 
        }

        if (vowStone.checkCollision(player.getBounds())) {
            gameState = GameState.LEVEL_COMPLETE;
            return; 
        }

        batch.begin();
        drawBackground(batch);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        mapLoader.render(shapeRenderer);
        hud.render(shapeRenderer, player);
        for (Projectile p : projectiles) p.render(shapeRenderer);
        shapeRenderer.end();

        batch.begin();
        vowStone.render(batch);
        for (Enemy enemy : enemies) enemy.render(batch);
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
        
        if (uiFont != null) uiFont.dispose();
        if (buttonFont != null) buttonFont.dispose(); 
        if (largeTitleFont != null) largeTitleFont.dispose(); 
        if (smallButtonFont != null) smallButtonFont.dispose(); 
        
        if (titleScreenTex != null) titleScreenTex.dispose();
        if (buttonTex != null) buttonTex.dispose();
        if (button2Tex != null) button2Tex.dispose(); 
        if (levelCompleteTex != null) levelCompleteTex.dispose(); 
        if (gameOverTex != null) gameOverTex.dispose(); 
        
        if (bgLayer1 != null) bgLayer1.dispose();
        if (bgLayer2 != null) bgLayer2.dispose();
        if (bgLayer3 != null) bgLayer3.dispose();
        
        if (player != null) player.dispose();
        if (vowStone != null) vowStone.dispose(); 
    }
}