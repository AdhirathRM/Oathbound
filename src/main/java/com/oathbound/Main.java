package com.oathbound;

import com.oathbound.core.GameWindow;

/**
 * Entry point for Oathbound: The Ten Trials.
 * Instantiates the game window and starts the game loop.
 */
public class Main {

    public static void main(String[] args) {
        // Launch on the Event Dispatch Thread for Swing thread-safety
        javax.swing.SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.startGame();
        });
    }
}
