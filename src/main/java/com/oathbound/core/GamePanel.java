package com.oathbound.core;

import com.oathbound.state.GameState;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * GamePanel: The central engine for Oathbound.
 * PB-015: Fixed Hero Swapping by disabling Focus Traversal.
 */
public class GamePanel extends JPanel implements Runnable {

    private static final int    TARGET_FPS         = 60;
    private static final long   ONE_BILLION        = 1_000_000_000L;
    private static final double NANOSECONDS_PER_FRAME = (double) ONE_BILLION / TARGET_FPS;

    private Thread gameThread;
    private GameState currentState = GameState.MENU;
    private double deltaTime = 0;

    private final TileMapLoader tileMap = new TileMapLoader();
    
    // Background & Camera Layers
    private java.awt.image.BufferedImage bgLayer1, bgLayer2, bgLayer3;
    private float camX = 0;

    // PB-015: Roster Management
    private Player player; 
    private final List<Player> roster = new ArrayList<>();
    private int currentHeroIndex = 0;

    private final HUD hud = new HUD(); 
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();

    private int currentLevel = 1;
    private final int MAX_LEVELS = 9;
    private VowStone currentVowStone;

    private int explosionX, explosionY, explosionTimer = 0;

    public GamePanel() {
        setPreferredSize(new Dimension(GameWindow.WIDTH, GameWindow.HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);   
        setFocusable(true);        

        // --- THE FIX FOR TAB KEY ---
        // Prevents Java from using TAB to switch between UI components
        this.setFocusTraversalKeysEnabled(false); 
        
        // Initialize the Full Party
        roster.add(new Player(100, 200)); // Knight
        roster.add(new Archer(100, 200, projectiles));
        roster.add(new Mage(100, 200, projectiles));
        roster.add(new Beastman(100, 200));
        roster.add(new Samurai(100, 200));
        
        player = roster.get(currentHeroIndex);
        loadBackground();
        loadLevel(currentLevel);

        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (player == null) return;
                int code = e.getKeyCode();
                
                // PB-015: The Swap Trigger
                if (code == KeyEvent.VK_TAB) {
                    swapHero();
                }

                if (code == KeyEvent.VK_SPACE) player.jump();
                if (code == KeyEvent.VK_LEFT)  player.setLeft(true);
                if (code == KeyEvent.VK_RIGHT) player.setRight(true);
                if (code == KeyEvent.VK_Z)     player.attack();
                
                if (code == KeyEvent.VK_ENTER && currentState == GameState.MENU) {
                    setGameState(GameState.PLAY);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (player == null) return;
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_LEFT)  player.setLeft(false);
                if (code == KeyEvent.VK_RIGHT) player.setRight(false);
            }
        });

        this.requestFocusInWindow();
    }

    private void loadBackground() {
        bgLayer1 = loadBgImage("/sprites/layer_1.png"); // sky/moon
        bgLayer2 = loadBgImage("/sprites/layer_2.png"); // castle
        bgLayer3 = loadBgImage("/sprites/layer_3.png"); // dirt strip
    }

    private java.awt.image.BufferedImage loadBgImage(String path) {
        try {
            java.io.InputStream is = getClass().getResourceAsStream(path);
            return is != null ? javax.imageio.ImageIO.read(is) : null;
        } catch (Exception e) { 
            return null; 
        }
    }

    private void swapHero() {
        System.out.println("[DEBUG] Swapping Hero... Current: " + player.getClass().getSimpleName());
        
        int oldX = player.getBounds().x;
        int oldY = player.getBounds().y;
        int oldHealth = player.getHealth();

        currentHeroIndex = (currentHeroIndex + 1) % roster.size();
        player = roster.get(currentHeroIndex);
        
        player.resetPosition(oldX, oldY);
        player.setHealth(oldHealth); 

        System.out.println("[DEBUG] Swapped to: " + player.getClass().getSimpleName());
    }

    public void stopGameLoop() {
        gameThread = null;
    }

    private void loadLevel(int levelNumber) {
        projectiles.clear();
        enemies.clear();
        explosionTimer = 0;
        tileMap.load("/levels/level_" + levelNumber + ".txt");

        // Use P spawn from map, fall back to (100, 200)
        int[] spawn = tileMap.getPlayerSpawn();
        for (Player p : roster) p.resetPosition(spawn[0], spawn[1]);

        // Spawn enemies from E tokens in the map
        for (int[] pos : tileMap.getEnemyPositions()) {
            enemies.add(new Enemy(pos[0], pos[1]));
        }

        // Vow Stone from V token (use first one found, or fallback)
        List<int[]> vows = tileMap.getVowStonePositions();
        currentVowStone = vows.isEmpty()
            ? new VowStone(1100, 544)
            : new VowStone(vows.get(0)[0], vows.get(0)[1]);
    }

    public void startGameLoop() {
        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    @Override
    public void run() {
        long lastFrameTime = System.nanoTime();
        while (gameThread != null) {
            long now = System.nanoTime();
            deltaTime = (now - lastFrameTime) / (double) ONE_BILLION;
            lastFrameTime = now;
            update(deltaTime);
            repaint();
            long sleepNanos = (long) NANOSECONDS_PER_FRAME - (System.nanoTime() - now);
            if (sleepNanos > 0) {
                try { Thread.sleep(sleepNanos / 1_000_000, (int) (sleepNanos % 1_000_000)); } 
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }

    private void update(double dt) {
        if (currentState == GameState.PLAY) {
            if (!this.hasFocus()) this.requestFocusInWindow();
            updatePlay(dt);
        }
    }

    private void updatePlay(double dt) {
        if (player != null) {
            player.update((float) dt, tileMap.getSolidTiles());

            // Parallax scroll ties to player's world position
            camX = player.getBounds().x;

            if (player.getBounds().y > GameWindow.HEIGHT) {
                player.takeDamage(1);
                int[] spawn = tileMap.getPlayerSpawn();
                player.resetPosition(spawn[0], spawn[1]); 
            }

            if (currentVowStone != null && currentVowStone.checkCollision(player.getBounds())) {
                currentLevel++;
                if (currentLevel > MAX_LEVELS) {
                    currentState = GameState.MENU; 
                    currentLevel = 1;
                } else {
                    loadLevel(currentLevel);
                }
            }
        }

        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update((float) dt, tileMap.getSolidTiles());
            if (!p.isActive()) projectiles.remove(i);
        }

        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.update((float) dt, tileMap.getSolidTiles());
            if (!enemy.isActive()) { enemies.remove(i); continue; }

            for (Projectile p : projectiles) {
                if (p.isActive() && p.getBounds().intersects(enemy.getBounds())) {
                    if (p.isAOE()) triggerExplosion(p);
                    else enemy.takeDamage(1);
                    p.deactivate();
                    break; 
                }
            }
        }
        if (explosionTimer > 0) explosionTimer--;
    }

    private void triggerExplosion(Projectile p) {
        explosionX = (int)p.getBounds().getCenterX();
        explosionY = (int)p.getBounds().getCenterY();
        explosionTimer = 12;
        for (Enemy e : enemies) {
            double dist = Math.sqrt(Math.pow(explosionX - e.getBounds().getCenterX(), 2) + 
                                    Math.pow(explosionY - e.getBounds().getCenterY(), 2));
            if (dist <= p.getExplosionRadius()) e.takeDamage(2); 
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (currentState == GameState.MENU) renderMenu(g2d);
        else if (currentState == GameState.PLAY) renderPlay(g2d);
        
        g2d.dispose();
    }

    private void renderMenu(Graphics2D g) {
        g.setColor(new Color(15, 15, 35));
        g.fillRect(0, 0, GameWindow.WIDTH, GameWindow.HEIGHT);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Serif", Font.BOLD, 60));
        g.drawString("OATHBOUND", GameWindow.WIDTH/2 - 180, 250);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.drawString("Press ENTER to Begin Trial", GameWindow.WIDTH/2 - 120, 350);
    }

    private void renderPlay(Graphics2D g) {
        drawBackground(g);  // 1. sky, then castles, then dirt strip
        
        tileMap.render(g);  // 2. actual solid ground tiles ON TOP of background
        
        if (currentVowStone != null) currentVowStone.render(g);
        for (Enemy e : enemies) e.render(g);
        for (Projectile p : projectiles) p.render(g);
        
        if (explosionTimer > 0) {
            g.setColor(new Color(0, 200, 255, 100));
            int r = 130; 
            g.fillOval(explosionX - r, explosionY - r, r * 2, r * 2);
        }

        if (player != null) player.render(g);
        hud.render(g, player); 
    }
    
    private void drawBackground(Graphics2D g) {
        int w = getWidth();
        int h = getHeight();

        // Layer 1: Sky/moon — stretch to fill entire screen, NO tiling, NO scroll
        if (bgLayer1 != null) {
            g.drawImage(bgLayer1, 0, 0, w, h, null);
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
                g.drawImage(bgLayer2, x, drawY, drawW, drawH, null);
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
                g.drawImage(bgLayer3, x, drawY, null);
            }
        }
    }

    public void setGameState(GameState newState) { this.currentState = newState; }
}