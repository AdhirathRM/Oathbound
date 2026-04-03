package com.oathbound.core;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * PB-014 — Heads Up Display
 * Renders the Player's health bar (hearts) on the screen.
 */
public class HUD {
    
    private final int heartSize = 24;
    private final int spacing = 10;
    private final int startX = 20;
    private final int startY = 20;

    public void render(Graphics2D g, Player player) {
        if (player == null) return;

        // Draw empty/background hearts
        g.setColor(Color.DARK_GRAY);
        for (int i = 0; i < player.maxHealth; i++) {
            g.fillRect(startX + (i * (heartSize + spacing)), startY, heartSize, heartSize);
        }

        // Draw filled red hearts based on current health
        g.setColor(Color.RED);
        for (int i = 0; i < player.currentHealth; i++) {
            g.fillRect(startX + (i * (heartSize + spacing)), startY, heartSize, heartSize);
        }
    }
}