package com.oathbound.core;

import com.oathbound.state.GameState;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * GamePanel is the heart of Oathbound.
 *
 * It is both the Swing rendering surface (JPanel) and the owner of the
 * 60 FPS fixed-timestep game loop (Runnable on a dedicated Thread).
 *
 * Architecture notes (matching the Project Charter):
 *  - PB-001 : 60 FPS loop with delta-time tracking.
 *  - PB-005 : GameState enum drives which update/render branch runs.
 *  - Future sprints will add TileMapLoader, Player, Enemy, etc. here.
 */
public class GamePanel extends JPanel implements Runnable {

    // ── Constants ────────────────────────────────────────────────────────────

    /** Target frames per second (PB-001). */
    private static final int    TARGET_FPS        = 60;

    /** Nanoseconds in one second. */
    private static final long   ONE_BILLION       = 1_000_000_000L;

    /** How many nanoseconds each frame should take. */
    private static final double NANOSECONDS_PER_FRAME = (double) ONE_BILLION / TARGET_FPS;

    // ── Fields ───────────────────────────────────────────────────────────────

    /** The dedicated game-loop thread. */
    private Thread gameThread;

    /** Current game state — starts at MENU (PB-005). */
    private GameState currentState = GameState.MENU;

    /** Running delta-time in seconds, updated every frame. */
    private double deltaTime = 0;

    // ── Constructor ──────────────────────────────────────────────────────────

    public GamePanel() {
        setPreferredSize(new Dimension(GameWindow.WIDTH, GameWindow.HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);   // reduces flicker
        setFocusable(true);        // allows key input once KeyListeners are added
    }

    // ── Game Loop ─────────────────────────────────────────────────────────────

    /**
     * Creates and starts the game-loop thread.
     * Called once by {@link GameWindow#startGame()}.
     */
    public void startGameLoop() {
        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    /**
     * Fixed-timestep game loop running on the gameThread.
     *
     * Implementation:
     *  1. Record the start time of the frame.
     *  2. Update game logic (update).
     *  3. Request a repaint (render via paintComponent).
     *  4. Sleep for the remaining time in the frame budget.
     *
     * PB-001 acceptance criteria:
     *  - Targets 60 FPS.
     *  - Delta-time calculated per frame.
     *  - Loop starts/stops cleanly via gameThread reference.
     */
    @Override
    public void run() {
        long lastFrameTime = System.nanoTime();
        long frameCount    = 0;
        long fpsTimer      = System.nanoTime();

        while (gameThread != null) {
            long now   = System.nanoTime();
            deltaTime  = (now - lastFrameTime) / (double) ONE_BILLION; // seconds
            lastFrameTime = now;

            update(deltaTime);
            repaint();

            // ── FPS limiter ──────────────────────────────────────────────
            long frameEnd     = System.nanoTime();
            long frameElapsed = frameEnd - now;
            long sleepNanos   = (long) NANOSECONDS_PER_FRAME - frameElapsed;

            if (sleepNanos > 0) {
                try {
                    long sleepMs = sleepNanos / 1_000_000;
                    int  extraNs = (int) (sleepNanos % 1_000_000);
                    Thread.sleep(sleepMs, extraNs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // ── Debug FPS counter (prints to console) ───────────────────
            frameCount++;
            if (System.nanoTime() - fpsTimer >= ONE_BILLION) {
                System.out.println("[GameLoop] FPS: " + frameCount);
                frameCount = 0;
                fpsTimer   = System.nanoTime();
            }
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Advances game logic by one frame.
     * Branches on {@link #currentState} (PB-005).
     *
     * @param dt delta-time in seconds since the last frame
     */
    private void update(double dt) {
        switch (currentState) {
            case MENU:
                updateMenu(dt);
                break;
            case PLAY:
                updatePlay(dt);
                break;
            case BOSS:
                updateBoss(dt);
                break;
        }
    }

    /** Placeholder — Sprint 4 (PB-029) will implement the real menu. */
    private void updateMenu(double dt) {
        // TODO PB-029: handle menu navigation input
    }

    /** Placeholder — Sprint 1 onwards will fill this in (PB-002 … PB-007). */
    private void updatePlay(double dt) {
        // TODO PB-002: gravity
        // TODO PB-003: jumping
        // TODO PB-006: collision detection
    }

    /** Placeholder — Sprint 4 (PB-022) will implement the turn-based loop. */
    private void updateBoss(double dt) {
        // TODO PB-022: turn-based boss battle
    }

    // ── Render ────────────────────────────────────────────────────────────────

    /**
     * Paints one frame. Called by Swing via repaint().
     * Branches on {@link #currentState} (PB-005).
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);

        switch (currentState) {
            case MENU:
                renderMenu(g2d);
                break;
            case PLAY:
                renderPlay(g2d);
                break;
            case BOSS:
                renderBoss(g2d);
                break;
        }

        g2d.dispose();
    }

    /** Renders a minimal placeholder title screen. */
    private void renderMenu(Graphics2D g) {
        // Background gradient feel
        g.setColor(new Color(10, 10, 30));
        g.fillRect(0, 0, GameWindow.WIDTH, GameWindow.HEIGHT);

        // Title
        g.setColor(new Color(220, 20, 60));   // Seraphim's crimson
        g.setFont(new Font("Serif", Font.BOLD, 64));
        drawCentredString(g, "OATHBOUND", GameWindow.WIDTH / 2, 260);

        g.setColor(new Color(176, 196, 222)); // burnished steel
        g.setFont(new Font("Serif", Font.ITALIC, 28));
        drawCentredString(g, "The Ten Trials", GameWindow.WIDTH / 2, 320);

        // Prompt
        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        drawCentredString(g, "[ Press ENTER to begin — PB-001 foundation ready ]",
                          GameWindow.WIDTH / 2, GameWindow.HEIGHT - 80);
    }

    /** Placeholder render for the platformer. */
    private void renderPlay(Graphics2D g) {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, GameWindow.WIDTH, GameWindow.HEIGHT);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        drawCentredString(g, "PLAY state — Sprint 1 work starts here",
                          GameWindow.WIDTH / 2, GameWindow.HEIGHT / 2);
    }

    /** Placeholder render for the boss battle. */
    private void renderBoss(Graphics2D g) {
        g.setColor(new Color(30, 0, 0));
        g.fillRect(0, 0, GameWindow.WIDTH, GameWindow.HEIGHT);
        g.setColor(new Color(220, 20, 60));
        g.setFont(new Font("Serif", Font.BOLD, 32));
        drawCentredString(g, "BOSS state — Sprint 4 work starts here",
                          GameWindow.WIDTH / 2, GameWindow.HEIGHT / 2);
    }

    // ── State Transitions ─────────────────────────────────────────────────────

    /**
     * Switches the active game state.
     * All future transition calls (e.g. MENU → PLAY on ENTER) should go through here.
     *
     * @param newState the state to switch into
     */
    public void setGameState(GameState newState) {
        System.out.println("[GamePanel] State: " + currentState + " → " + newState);
        this.currentState = newState;
    }

    public GameState getGameState() {
        return currentState;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Draws a string horizontally centred at (cx, y).
     */
    private void drawCentredString(Graphics2D g, String text, int cx, int y) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, cx - textWidth / 2, y);
    }
}
