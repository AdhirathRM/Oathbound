package com.oathbound.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * PB-014 — Heads Up Display
 * Renders the Player's health bar (hearts) on the screen.
 * Ported to LibGDX ShapeRenderer.
 */
public class HUD {
    
    private final int heartSize = 24;
    private final int spacing = 10;
    private final int startX = 20;
    private final int startY = 700; // In LibGDX, 20 is near the bottom! 

    /**
     * Renders the health UI.
     * Uses getters to stay compatible with Knight, Archer, and Mage subclasses.
     */
    public void render(ShapeRenderer sr, Player player) {
        if (player == null) return;

        // 1. Draw the "Background" Slots (Empty Hearts)
        // LibGDX colors use 0f - 1f floats. RGBA.
        sr.setColor(50/255f, 50/255f, 50/255f, 200/255f);
        for (int i = 0; i < player.getMaxHealth(); i++) {
            int x = startX + (i * (heartSize + spacing));
            sr.rect(x, startY, heartSize, heartSize);
        }

        // 2. Draw the "Active" Hearts (Current Health)
        for (int i = 0; i < player.getHealth(); i++) {
            int x = startX + (i * (heartSize + spacing));
            
            // Draw the main heart body
            sr.setColor(220/255f, 20/255f, 60/255f, 1f); 
            sr.rect(x, startY, heartSize, heartSize);

            // PB-014 Polish: Add a small "glint" to make the UI feel alive
            sr.setColor(1f, 1f, 1f, 80/255f); // Soft white highlight
            sr.rect(x + 3, startY + 3, 7, 7);
        }
        
        // 3. Optional: Draw a thin border around the active heart for extra "pop"
        // Using the same WebGL optimization trick used in TileMapLoader 
        // to avoid flushing the batch by switching to ShapeType.Line
        sr.setColor(Color.WHITE);
        for (int i = 0; i < player.getHealth(); i++) {
            int x = startX + (i * (heartSize + spacing));
            drawOutline(sr, x, startY, heartSize, heartSize);
        }
    }

    // Helper to draw outlines without leaving ShapeType.Filled (Better WebGL Performance)
    private void drawOutline(ShapeRenderer sr, int x, int y, int w, int h) {
        sr.rect(x, y, w, 1);
        sr.rect(x, y + h - 1, w, 1);
        sr.rect(x, y, 1, h);
        sr.rect(x + w - 1, y, 1, h);
    }
}