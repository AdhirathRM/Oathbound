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

    /**
     * Renders the health UI.
     * Uses getters to stay compatible with Knight, Archer, and Mage subclasses.
     */
    public void render(Graphics2D g, Player player) {
        if (player == null) return;

        // 1. Draw the "Background" Slots (Empty Hearts)
        // We use a dark, semi-transparent gray to show where hearts *could* be.
        g.setColor(new Color(50, 50, 50, 200));
        for (int i = 0; i < player.getMaxHealth(); i++) {
            int x = startX + (i * (heartSize + spacing));
            g.fillRect(x, startY, heartSize, heartSize);
        }

        // 2. Draw the "Active" Hearts (Current Health)
        // Using a bright Crimson Red for visibility against dark levels.
        g.setColor(new Color(220, 20, 60)); 
        for (int i = 0; i < player.getHealth(); i++) {
            int x = startX + (i * (heartSize + spacing));
            
            // Draw the main heart body
            g.fillRect(x, startY, heartSize, heartSize);

            // PB-014 Polish: Add a small "glint" to make the UI feel alive
            g.setColor(new Color(255, 255, 255, 80)); // Soft white highlight
            g.fillRect(x + 3, startY + 3, 7, 7);
            
            // Reset color for the next heart in the loop
            g.setColor(new Color(220, 20, 60));
        }
        
        // 3. Optional: Draw a thin border around the active heart for extra "pop"
        g.setColor(Color.WHITE);
        for (int i = 0; i < player.getHealth(); i++) {
            int x = startX + (i * (heartSize + spacing));
            g.drawRect(x, startY, heartSize, heartSize);
        }
    }
}