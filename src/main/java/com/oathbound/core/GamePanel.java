package com.oathbound.core;

import com.oathbound.state.GameState;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * GamePanel: The central engine for Oathbound.
 * Supports Knight, Archer, and Mage (PB-016) across 9 Levels (PB-019).
 */
public class GamePanel extends JPanel implements Runnable {

    private static final int    TARGET_FPS         = 60;
    private static final long   ONE_BILLION        = 1_000_000_000L;
    private static final double NANOSECONDS_PER_FRAME = (double) ONE_BILLION / TARGET_FPS;

    private Thread gameThread;
    private GameState currentState = GameState.MENU;
    private double deltaTime = 0;

    private final TileMapLoader tileMap = new TileMapLoader();
    private Player player;
    private final HUD hud = new HUD(); 

    // Entities
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();

    // Level & Checkpoint Management
    private int currentLevel = 1;
    private final int MAX_LEVELS = 9;
    private VowStone currentVowStone;

    // PB-016: Visual Explosion Feedback
    private int explosionX, explosionY;
    private int explosionTimer = 0;

    public GamePanel() {
        setPreferredSize(new Dimension(GameWindow.WIDTH, GameWindow.HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);   
        setFocusable(true);        
        
        // PB-017: Currently playing as the Beastman
        // In GamePanel.java constructor
        player = new Samurai(100, 200);
        
        loadLevel(currentLevel);

        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (player == null) return;
                int code = e.getKeyCode();
                
                if (code == java.awt.event.KeyEvent.VK_SPACE) player.jump();
                if (code == java.awt.event.KeyEvent.VK_LEFT)  player.setLeft(true);
                if (code == java.awt.event.KeyEvent.VK_RIGHT) player.setRight(true);
                if (code == java.awt.event.KeyEvent.VK_Z)     player.attack();
                
                if (code == java.awt.event.KeyEvent.VK_ENTER && currentState == GameState.MENU) {
                    setGameState(GameState.PLAY);
                }
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                if (player == null) return;
                int code = e.getKeyCode();
                if (code == java.awt.event.KeyEvent.VK_LEFT)  player.setLeft(false);
                if (code == java.awt.event.KeyEvent.VK_RIGHT) player.setRight(false);
            }
        });

        currentState = GameState.MENU; 
    }

    private void loadLevel(int levelNumber) {
        System.out.println("[System] Loading Level " + levelNumber);
        projectiles.clear();
        enemies.clear();
        explosionTimer = 0;

        tileMap.load("/levels/level_" + levelNumber + ".txt");

        if (player != null) {
            player.resetPosition(100, 200);
        }

        // Logic for spawning Level Entities
        if (levelNumber == 1) {
            currentVowStone = new VowStone(1100, 544); 
            enemies.add(new Enemy(600, 200));
            enemies.add(new Enemy(660, 200)); // Grouped for AOE testing
        } else {
            currentVowStone = new VowStone(1100, 544);
            enemies.add(new Enemy(500, 200));
        }
    }

    public void startGameLoop() {
        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    public void stopGameLoop() { gameThread = null; }

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
        if (currentState == GameState.PLAY) updatePlay(dt);
    }

    private void updatePlay(double dt) {
        if (player != null) {
            player.update((float) dt, tileMap.getSolidTiles());

            // PB-021: Fall Detection
            if (player.getBounds().y > GameWindow.HEIGHT) {
                player.takeDamage(1); // Optional: Pits hurt!
                player.resetPosition(100, 200); 
            }

            // PB-020: Checkpoint/Next Level
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

        // Update Projectiles
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update((float) dt, tileMap.getSolidTiles());
            if (!p.isActive()) projectiles.remove(i);
        }

        // Update Enemies & Combat
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.update((float) dt, tileMap.getSolidTiles());

            if (!enemy.isActive()) {
                enemies.remove(i);
                continue;
            }

            // Projectile Logic
            for (Projectile p : projectiles) {
                if (p.isActive() && p.getBounds().intersects(enemy.getBounds())) {
                    if (p.isAOE()) {
                        triggerExplosion(p);
                    } else {
                        enemy.takeDamage(1);
                    }
                    p.deactivate();
                    break; 
                }
            }
        }

        // Decay the visual explosion timer
        if (explosionTimer > 0) explosionTimer--;
    }

    private void triggerExplosion(Projectile p) {
        explosionX = (int)p.getBounds().getCenterX();
        explosionY = (int)p.getBounds().getCenterY();
        explosionTimer = 12; // Show blast for 12 frames (~0.2 seconds)

        for (Enemy e : enemies) {
            double dx = explosionX - e.getBounds().getCenterX();
            double dy = explosionY - e.getBounds().getCenterY();
            double distance = Math.sqrt(dx*dx + dy*dy);

            if (distance <= p.getExplosionRadius()) {
                e.takeDamage(2); 
            }
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
        g.setColor(new Color(30, 30, 35));
        g.fillRect(0, 0, GameWindow.WIDTH, GameWindow.HEIGHT);

        tileMap.render(g);
        if (currentVowStone != null) currentVowStone.render(g);
        for (Enemy e : enemies) e.render(g);
        for (Projectile p : projectiles) p.render(g);
        
        // PB-016: Render the Mana Burst Visual
        if (explosionTimer > 0) {
            g.setColor(new Color(0, 200, 255, 100)); // Transparent Blue
            int radius = 130; 
            g.fillOval(explosionX - radius, explosionY - radius, radius * 2, radius * 2);
            g.setColor(new Color(255, 255, 255, 150)); // White Flash center
            g.fillOval(explosionX - 20, explosionY - 20, 40, 40);
        }

        if (player != null) player.render(g);
        hud.render(g, player); 
    }

    public void setGameState(GameState newState) { this.currentState = newState; }
}