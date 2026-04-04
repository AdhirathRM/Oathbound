package com.oathbound.core;

import javax.swing.JFrame;

/**
 * GameWindow creates and configures the main application window (JFrame).
 * It owns a single {@link GamePanel} which handles all rendering and the
 * game loop.
 *
 * Responsibilities:
 * - Set window title, size, and close behaviour.
 * - Add the GamePanel and make the window visible.
 * - Delegate game-loop start to GamePanel.
 */
public class GameWindow {

    // ── Constants ────────────────────────────────────────────────────────────
    public static final String TITLE  = "Oathbound: The Ten Trials";
    
    // Fixed dimensions to ensure the map fits perfectly without floating.
    // WIDTH: 1280 fits exactly 40 tiles (1280 / 32 = 40)
    // HEIGHT: 736 fits exactly 23 tiles (736 / 32 = 23). 
    // Note: If your map text file has a different number of rows, change HEIGHT 
    // to (number_of_rows * 32) so the ground sits flush at the bottom!
    public static final int    WIDTH  = 1280;
    public static final int    HEIGHT = 736;

    // ── Fields ───────────────────────────────────────────────────────────────
    private final JFrame    frame;
    private final GamePanel gamePanel;

    // ── Constructor ──────────────────────────────────────────────────────────

    /**
     * Builds the window and attaches the GamePanel.
     * Does NOT make the window visible yet; call {@link #startGame()} for that.
     */
    public GameWindow() {
        gamePanel = new GamePanel();

        frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // handle manually
        frame.setResizable(false);

        // Stop the game loop cleanly before JVM exits
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                gamePanel.stopGameLoop();
                frame.dispose();
                System.exit(0); // Ensuring the process terminates fully
            }
        });

        frame.add(gamePanel);
        
        // pack() resizes the window so the GamePanel exactly matches its preferred size (WIDTH x HEIGHT)
        frame.pack();
        frame.setLocationRelativeTo(null); // Centers the window on the screen
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Makes the window visible, then starts the game loop.
     * Called once from {@link com.oathbound.Main}.
     */
    public void startGame() {
        frame.setVisible(true);
        gamePanel.startGameLoop();
    }
}