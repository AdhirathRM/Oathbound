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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
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
    public enum GameState { TITLE, RULES, LEVEL_SELECT, PLAYING, LEVEL_COMPLETE, GAME_OVER }
    private GameState gameState = GameState.TITLE;
    
    // UI & Background Layers
    private Texture titleScreenTex;
    private Texture levelSelectTex; 
    private Texture ruleScreenTex;
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
    private List<Projectile> enemyProjectiles;
    private List<HealDrop> healDrops;
    private Boss boss;

    // Level & Character Management
    private int currentLevel = 1;
    private final int MAX_LEVELS = 10;
    private int currentCharacterIndex = 0; 
    
    private int spawnX;
    private int spawnY;
    private float playerDamageTimer = 0f;
    private float bossDeathTimer = 0f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        
        camera = new OrthographicCamera();
        camera.setToOrtho(true, 1280, 736); // y-down coordinate system

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
        
        if (Gdx.files.internal("sprites/level_select.png").exists()) {
            levelSelectTex = new Texture(Gdx.files.internal("sprites/level_select.png"));
        }
        
        if (Gdx.files.internal("sprites/rule_screen.png").exists()) {
            ruleScreenTex = new Texture(Gdx.files.internal("sprites/rule_screen.png"));
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
        enemyProjectiles = new ArrayList<>();
        healDrops = new ArrayList<>();
        
        loadLevel(currentLevel, 5);
    }

    private void loadLevel(int level, int startingHealth) {
        enemies.clear();
        projectiles.clear(); 
        enemyProjectiles.clear();
        healDrops.clear();
        if (boss != null) boss.dispose();
        boss = null;
        bossDeathTimer = 0f; 
        
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

        if (level == 10) {
            boss = new Boss(1000, 200); 
            if (vowStone != null) vowStone.dispose();
            vowStone = null; // Hide the vow stone until boss is defeated
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
        } else if (gameState == GameState.RULES) {
            renderRulesScreen();
        } else if (gameState == GameState.LEVEL_SELECT) {
            renderLevelSelect();
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

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (playHover && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            gameState = GameState.LEVEL_SELECT;
        }
        
        if (rulesHover && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            gameState = GameState.RULES;
        }

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
            gameState = GameState.LEVEL_SELECT;
        }
    }

    private void renderRulesScreen() {
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);
        float mx = mousePos.x;
        float my = mousePos.y;

        batch.begin();
        if (ruleScreenTex != null) {
            batch.draw(ruleScreenTex, 0, 0, 1280, 736, 0, 0, ruleScreenTex.getWidth(), ruleScreenTex.getHeight(), false, true);
        } else if (bgLayer1 != null) {
            batch.draw(bgLayer1, 0, 0, 1280, 736, 0, 0, bgLayer1.getWidth(), bgLayer1.getHeight(), false, true);
        }
        batch.end();

        int bx = 240;
        int by = 60;
        int bw = 800;
        int bh = 616;

        if (ruleScreenTex == null) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            // Draw ornate scroll-like box
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
        }

        batch.begin();
        GlyphLayout titleLayout = new GlyphLayout(largeTitleFont, "THE OATHBOUND RULES");
        largeTitleFont.setColor(Color.GOLD);
        largeTitleFont.draw(batch, titleLayout, bx + (bw - titleLayout.width) / 2, by + 33);

        uiFont.setColor(Color.WHITE);
        int textX = bx; // Moved left slightly to give more horizontal room
        int textY = by + 113;
        int spacing = 45;

        // Controls Section
        uiFont.setColor(Color.GOLD);
        uiFont.draw(batch, "CONTROLS:", textX - 30, textY);
        uiFont.setColor(Color.WHITE);
        uiFont.draw(batch, "- Move: A / D or Left / Right", textX, textY + spacing);
        uiFont.draw(batch, "- Jump: W or Space", textX, textY + spacing * 2);
        uiFont.draw(batch, "- Attack: J or Z", textX, textY + spacing * 3);
        uiFont.draw(batch, "- Switch Class: TAB", textX, textY + spacing * 4);

        textY += spacing * 5 + 20;
        
        // Trials Section
        uiFont.setColor(Color.GOLD);
        uiFont.draw(batch, "THE TRIALS:", textX - 30, textY);
        uiFont.setColor(Color.WHITE);
        uiFont.draw(batch, "Reach the Vow Stone at the end of each stage", textX, textY + spacing);
        uiFont.draw(batch, "to advance to the next trial.", textX, textY + spacing * 1.8f);
        
        textY += spacing * 2.5f + 10;

        // Level 10 Lore Section
        uiFont.setColor(Color.FIREBRICK);
        uiFont.draw(batch, "TRIAL 10: THE CRIMSON SOVEREIGN", textX - 30, textY);
        uiFont.setColor(Color.WHITE);
        // Made sentences shorter so they easily fit inside without overlapping the edges
        uiFont.draw(batch, "Lord Malakor awaits at the pinnacle. Beware his", textX, textY + spacing);
        uiFont.draw(batch, "Lifesteal, teleportation, and Phase 2", textX, textY + spacing * 1.8f);
        uiFont.draw(batch, "transformation. Striking him may cause Crimson", textX, textY + spacing * 2.6f);
        uiFont.draw(batch, "Essence (Heals) to drop. Survive and conquer!", textX, textY + spacing * 3.4f);

        // Back Button (Top Left)
        Rectangle backBtn = new Rectangle(30, 30, 200, 80);
        boolean backHover = backBtn.contains(mx, my);

        if (buttonTex != null) {
            batch.setColor(backHover ? new Color(0.8f, 0.6f, 1.0f, 1f) : Color.WHITE);
            batch.draw(buttonTex, backBtn.x, backBtn.y, backBtn.width, backBtn.height, 0, 0, buttonTex.getWidth(), buttonTex.getHeight(), false, true);
            batch.setColor(Color.WHITE);
        }

        smallButtonFont.setColor(backHover ? Color.WHITE : Color.valueOf("ffe14d"));
        GlyphLayout backLayout = new GlyphLayout(smallButtonFont, "BACK");
        smallButtonFont.draw(batch, backLayout, backBtn.x + (backBtn.width - backLayout.width) / 2, backBtn.y + (backBtn.height - backLayout.height) / 2 + 3);
        batch.end();

        // Handle Back interaction
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || 
           (backHover && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT))) {
            gameState = GameState.TITLE;
        }
    }

    private void renderLevelSelect() {
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);
        float mx = mousePos.x;
        float my = mousePos.y;

        batch.begin();
        if (levelSelectTex != null) {
            batch.draw(levelSelectTex, 0, 0, 1280, 736, 0, 0, levelSelectTex.getWidth(), levelSelectTex.getHeight(), false, true);
        } else if (bgLayer1 != null) {
            batch.draw(bgLayer1, 0, 0, 1280, 736, 0, 0, bgLayer1.getWidth(), bgLayer1.getHeight(), false, true);
        }
        
        int cols = 5;
        int rows = 2;
        int btnW = 140; 
        int btnH = 140; 
        int spacing = 40;
        int startX = (1280 - (cols * btnW + (cols - 1) * spacing)) / 2;
        int startY = 240;

        for (int i = 0; i < 10; i++) {
            int row = i / cols;
            int col = i % cols;
            int x = startX + col * (btnW + spacing);
            int y = startY + row * (btnH + spacing);
            
            Rectangle btnRect = new Rectangle(x, y, btnW, btnH);
            boolean hovered = btnRect.contains(mx, my);
            
            if (buttonTex != null) {
                batch.setColor(hovered ? new Color(0.8f, 0.6f, 1.0f, 1f) : Color.WHITE);
                batch.draw(buttonTex, x, y, btnW, btnH, 0, 0, buttonTex.getWidth(), buttonTex.getHeight(), false, true);
            }
            
            buttonFont.setColor(hovered ? Color.WHITE : Color.valueOf("ffe14d"));
            GlyphLayout numLayout = new GlyphLayout(buttonFont, String.valueOf(i + 1));
            // Moved the text up by 10 pixels compared to the previous version, and down by 3 px from the last iteration, and 1 px down again.
            buttonFont.draw(batch, numLayout, x + (btnW - numLayout.width) / 2, y + (btnH - numLayout.height) / 2 - 1);
            
            if (hovered && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                currentLevel = i + 1;
                loadLevel(currentLevel, 5);
                gameState = GameState.PLAYING;
            }
        }
        
        // Add Back Button (Bottom Left)
        Rectangle backBtn = new Rectangle(30, 736 - 110, 200, 80);
        boolean backHover = backBtn.contains(mx, my);
        
        if (buttonTex != null) {
            batch.setColor(backHover ? new Color(0.8f, 0.6f, 1.0f, 1f) : Color.WHITE);
            batch.draw(buttonTex, backBtn.x, backBtn.y, backBtn.width, backBtn.height, 0, 0, buttonTex.getWidth(), buttonTex.getHeight(), false, true);
        }
        
        smallButtonFont.setColor(backHover ? Color.WHITE : Color.valueOf("ffe14d"));
        GlyphLayout backLayout = new GlyphLayout(smallButtonFont, "BACK");
        smallButtonFont.draw(batch, backLayout, backBtn.x + (backBtn.width - backLayout.width) / 2, backBtn.y + (backBtn.height - backLayout.height) / 2 + 3);
        
        if (backHover && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            gameState = GameState.TITLE;
        }

        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void renderLevelComplete() {
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);
        float mx = mousePos.x;
        float my = mousePos.y;

        Rectangle topBox = new Rectangle((1280 / 2) - 590, 5, 1180, 480); 

        // Hide next button on Level 10
        boolean showNext = (currentLevel < MAX_LEVELS);

        int btnW = 560;
        int btnH = 96; 
        int startY = showNext ? 410 : 460;
        int gap = btnH + 12; 

        Rectangle nextBtn = new Rectangle((1280 / 2) - (btnW / 2), startY, btnW, btnH);
        Rectangle restartBtn = new Rectangle((1280 / 2) - (btnW / 2), showNext ? startY + gap : startY, btnW, btnH);
        Rectangle menuBtn = new Rectangle((1280 / 2) - (btnW / 2), showNext ? startY + gap * 2 : startY + gap, btnW, btnH);

        boolean nextHover = showNext && nextBtn.contains(mx, my);
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
            if (showNext) {
                if (nextHover) batch.setColor(0.8f, 0.6f, 1.0f, 1f); else batch.setColor(Color.WHITE);
                batch.draw(btn2Next, nextBtn.x, nextBtn.y, nextBtn.width, nextBtn.height);
            }

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
            if (showNext) {
                shapeRenderer.rect(nextBtn.x, nextBtn.y, nextBtn.width, nextBtn.height);
            }
            shapeRenderer.rect(restartBtn.x, restartBtn.y, restartBtn.width, restartBtn.height);
            shapeRenderer.rect(menuBtn.x, menuBtn.y, menuBtn.width, menuBtn.height);
        }

        if (showNext && nextHover && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            currentLevel++;
            if (currentLevel > MAX_LEVELS) {
                gameState = GameState.TITLE; 
            } else {
                loadLevel(currentLevel, player.getHealth());
                gameState = GameState.PLAYING;
            }
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

        if (showNext) {
            smallButtonFont.setColor(nextHover ? Color.WHITE : Color.LIGHT_GRAY);
            GlyphLayout nextLayout = new GlyphLayout(smallButtonFont, "NEXT TRIAL");
            float nextTextX = nextBtn.x + (nextBtn.width - nextLayout.width) / 2 - 10; 
            float nextTextY = nextBtn.y + (nextBtn.height - nextLayout.height) / 2 + 16.5f; 
            smallButtonFont.draw(batch, nextLayout, nextTextX, nextTextY);
        }

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
            if (restartHover) batch.setColor(0.8f, 0.6f, 1.0f, 1f); else batch.setColor(Color.WHITE);
            batch.draw(buttonTex, restartBtn.x, restartBtn.y, restartBtn.width, restartBtn.height, 0, 0, buttonTex.getWidth(), buttonTex.getHeight(), false, true);

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
        
        // --- ENEMY PROJECTILES UPDATE ---
        for (int i = enemyProjectiles.size() - 1; i >= 0; i--) {
            Projectile p = enemyProjectiles.get(i);
            p.update(dt, mapLoader.getSolidTiles());
            if (p.getBounds().intersects(player.getBounds())) {
                 if (playerDamageTimer <= 0) {
                     player.takeDamage(1);
                     playerDamageTimer = 1.0f;
                 }
                 p.deactivate();
            }
            if (!p.isActive()) {
                enemyProjectiles.remove(i);
            }
        }

        // --- HEAL DROPS UPDATE ---
        for (int i = healDrops.size() - 1; i >= 0; i--) {
            HealDrop drop = healDrops.get(i);
            drop.update(dt, mapLoader.getSolidTiles()); // Pass solid tiles for gravity
            
            if (drop.checkPickUp(player.getBounds())) {
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 1));
            }
            if (!drop.isActive()) {
                healDrops.remove(i);
            }
        }

        // --- BOSS UPDATE & LOGIC ---
        if (boss != null) {
            if (!boss.isDead()) {
                boss.update(dt, player, mapLoader.getSolidTiles(), enemyProjectiles);

                // Boss taking damage from player
                if (player.getAttackHitbox().width > 0 && player.getAttackHitbox().intersects(boss.getBounds())) {
                    int hpBefore = boss.getHealth();
                    boss.takeDamage(1); 
                    
                    // 30% Chance to spawn a heal drop when attacking the boss!
                    if (boss.getHealth() < hpBefore && MathUtils.randomBoolean(0.30f)) {
                        healDrops.add(new HealDrop((int)boss.getBounds().x + 30, (int)boss.getBounds().y + 50));
                    }
                }
            } else if (vowStone == null) {
                // Delay Vow Stone spawn by 2 seconds after death
                bossDeathTimer += dt;
                if (bossDeathTimer > 2.0f) {
                    vowStone = new VowStone((int)boss.getBounds().x, (int)boss.getBounds().y);
                }
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
                    if (boss != null && !boss.isDead() && p.getBounds().intersects(boss.getBounds())) {
                        hitEnemyDirectly = true;
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
                    if (boss != null && !boss.isDead()) {
                        float ex = boss.getBounds().x + boss.getBounds().width / 2f;
                        float ey = boss.getBounds().y + boss.getBounds().height / 2f;
                        if ((float) Math.hypot(cx - ex, cy - ey) <= p.getExplosionRadius()) {
                            boss.takeDamage(2);
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
                if (boss != null && !boss.isDead() && p.getBounds().intersects(boss.getBounds())) {
                    boss.takeDamage(1);
                    p.deactivate();
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

        if (vowStone != null && vowStone.checkCollision(player.getBounds())) {
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
        for (Projectile p : enemyProjectiles) p.render(shapeRenderer);
        for (HealDrop drop : healDrops) drop.render(shapeRenderer);
        if (boss != null && !boss.isDead()) boss.renderShapes(shapeRenderer);
        shapeRenderer.end();

        batch.begin();
        if (vowStone != null) vowStone.render(batch);
        for (Enemy enemy : enemies) enemy.render(batch);
        
        // Render the boss and his title text using the existing UI font
        if (boss != null && !boss.isDead()) {
            boss.render(batch);
            
            float by = 680; // Health bar is at 680
            
            // Text glows pulsing red if Boss is in Phase 2 (Health <= 40)
            if (boss.getHealth() <= 40) {
                float pulse = 0.7f + 0.3f * MathUtils.sin(TimeUtils.millis() / 150.0f);
                uiFont.setColor(1f, 0.2f, 0.2f, pulse);
            } else {
                uiFont.setColor(Color.WHITE);
            }
            
            GlyphLayout titleLayout = new GlyphLayout(uiFont, "Lord Malakor, the Crimson Sovereign");
            uiFont.draw(batch, titleLayout, (1280 - titleLayout.width) / 2, by - 35);
            uiFont.setColor(Color.WHITE); // Reset font color
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
        
        if (uiFont != null) uiFont.dispose();
        if (buttonFont != null) buttonFont.dispose(); 
        if (largeTitleFont != null) largeTitleFont.dispose(); 
        if (smallButtonFont != null) smallButtonFont.dispose(); 
        
        if (titleScreenTex != null) titleScreenTex.dispose();
        if (levelSelectTex != null) levelSelectTex.dispose();
        if (ruleScreenTex != null) ruleScreenTex.dispose();
        if (buttonTex != null) buttonTex.dispose();
        if (button2Tex != null) button2Tex.dispose(); 
        if (levelCompleteTex != null) levelCompleteTex.dispose(); 
        if (gameOverTex != null) gameOverTex.dispose(); 
        
        if (bgLayer1 != null) bgLayer1.dispose();
        if (bgLayer2 != null) bgLayer2.dispose();
        if (bgLayer3 != null) bgLayer3.dispose();
        
        if (player != null) player.dispose();
        if (vowStone != null) vowStone.dispose(); 
        if (boss != null) boss.dispose();
    }
}