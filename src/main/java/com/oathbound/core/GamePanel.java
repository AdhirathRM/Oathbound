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
import java.awt.Rectangle;

/**
 * GamePanel: The engine room of Oathbound.
 * Handles the 60 FPS loop, State Management, and Projectile tracking.
 */
public class GamePanel extends JPanel implements Runnable {

    private final List<Enemy> enemies = new ArrayList<>();
    private final HUD hud = new HUD(); // PB-014

    // ── Constants ────────────────────────────────────────────────────────────
    private static final int    TARGET_FPS         = 60;
    private static final long   ONE_BILLION        = 1_000_000_000L;
    private static final double NANOSECONDS_PER_FRAME = (double) ONE_BILLION / TARGET_FPS;

    // ── Fields ───────────────────────────────────────────────────────────────
    private Thread gameThread;
    private GameState currentState = GameState.MENU;
    private double deltaTime = 0;

    private final TileMapLoader tileMap = new TileMapLoader();
    private Player player;

    // PB-011: Projectile System
    private final List<Projectile> projectiles = new ArrayList<>();
    private long lastFireTime = 0;
    private final long FIRE_COOLDOWN = 400; // Milliseconds between shots

    // ── Constructor ──────────────────────────────────────────────────────────

    public GamePanel() {
        setPreferredSize(new Dimension(GameWindow.WIDTH, GameWindow.HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);   
        setFocusable(true);        
        
        // Initialize Player (68x68 version)
        //player = new Player(100, 200); 
        player = new Archer(100, 200, projectiles);
        tileMap.load("/levels/level_test.txt");

        // Key Listener
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (player == null) return;

                int code = e.getKeyCode();
                
                // Movement & Melee
                if (code == java.awt.event.KeyEvent.VK_SPACE) player.jump();
                if (code == java.awt.event.KeyEvent.VK_LEFT)  player.setLeft(true);
                if (code == java.awt.event.KeyEvent.VK_RIGHT) player.setRight(true);
                if (code == java.awt.event.KeyEvent.VK_Z)     player.attack();

                // PB-011: Ranged Attack (X Key) with Cooldown
                if (code == java.awt.event.KeyEvent.VK_X) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastFireTime >= FIRE_COOLDOWN) {
                        fireProjectile();
                        lastFireTime = currentTime;
                    }
                }
                
                // Menu Navigation
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
        
        player = new Archer(100, 200, projectiles); 
        tileMap.load("/levels/level_test.txt");
        
        enemies.add(new Enemy(400, 200)); // Spawn a red block enemy
    }

    private void fireProjectile() {
        float speed = 650f;
        // Spawn relative to the center of the 68x68 Knight
        int pX = player.getBounds().x + (player.getBounds().width / 2);
        int pY = player.getBounds().y + (player.getBounds().height / 3);
        
        int direction = player.getFacing(); 
        projectiles.add(new Projectile(pX, pY, speed * direction, 0));
    }

    // ── Game Loop Control ─────────────────────────────────────────────────────

    public void startGameLoop() {
        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    /**
     * PB-001: Cleanly stops the thread.
     * Fixes "Undefined method" error in GameWindow.java.
     */
    public void stopGameLoop() {
        System.out.println("[GameLoop] Stopping...");
        gameThread = null;
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

            // FPS Limiter
            long sleepNanos = (long) NANOSECONDS_PER_FRAME - (System.nanoTime() - now);
            if (sleepNanos > 0) {
                try {
                    Thread.sleep(sleepNanos / 1_000_000, (int) (sleepNanos % 1_000_000));
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    private void update(double dt) {
        if (currentState == GameState.PLAY) {
            updatePlay(dt);
        }
    }

    private void updatePlay(double dt) {
        if (player != null) {
            player.update((float) dt, tileMap.getSolidTiles());
        }

        // PB-021: Pit Detection (Death by falling)
            if (player.getBounds().y > GameWindow.HEIGHT) {
                System.out.println("[PB-021] Player fell into a pit!");
                // You can add player.takeDamage(1) here if you want pits to hurt
                player.resetPosition(100, 200); 
            }

        // Update Projectiles
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update((float) dt, tileMap.getSolidTiles());
            if (!p.isActive()) {
                projectiles.remove(i);
            }
        }

        // Update Enemies & Check Combat
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.update((float) dt, tileMap.getSolidTiles());

            if (!enemy.isActive()) {
                enemies.remove(i);
                continue; 
            }

            // PB-011: Ranged Combat Check
            for (Projectile p : projectiles) {
                if (p.isActive() && p.getBounds().intersects(enemy.getBounds())) {
                    enemy.takeDamage(1);
                    p.deactivate(); // Arrow disappears
                }
            }

            // PB-012: Melee Combat Check
            Rectangle pAttack = player.getAttackHitbox();
            if (pAttack != null && pAttack.intersects(enemy.getBounds())) {
                enemy.takeDamage(1); 
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (currentState == GameState.MENU) {
            renderMenu(g2d);
        } else if (currentState == GameState.PLAY) {
            renderPlay(g2d);
        }
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

        for (Enemy e : enemies) { e.render(g); } // Draw Enemies
        for (Projectile p : projectiles) { p.render(g); } // Draw Arrows

        if (player != null) { player.render(g); }

        hud.render(g, player); // Draw PB-014 Health UI ON TOP of everything
    }

    public void setGameState(GameState newState) { this.currentState = newState; }
}