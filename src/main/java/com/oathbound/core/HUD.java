package com.oathbound.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * HUD Class
 * Responsible for rendering the player's health as heart-shaped icons.
 * Uses mathematical primitives for a crisp, resolution-independent look.
 */
public class HUD {

    private final float heartSize = 26;
    private final float spacing = 12;
    private final float startX = 30;
    private final float startY = 30; // Positioned at top-left for Y-Down camera

    public void render(ShapeRenderer sr, Player player) {
        if (player == null) return;

        int health = player.getHealth();
        int maxHealth = player.getMaxHealth();

        // 1. Draw the "Background" Slots (Empty Hearts)
        sr.setColor(0.15f, 0.15f, 0.15f, 0.6f); 
        for (int i = 0; i < maxHealth; i++) {
            float x = startX + i * (heartSize + spacing);
            drawHeartShape(sr, x, startY, heartSize);
        }

        // 2. Draw the "Active" Hearts (Current Health)
        for (int i = 0; i < health; i++) {
            float x = startX + i * (heartSize + spacing);
            
            // Main heart body - classic Firebrick Red
            sr.setColor(220/255f, 20/255f, 60/255f, 1f); 
            drawHeartShape(sr, x, startY, heartSize);

            // Polish: Add a small white "glint" circle to make it look 3D
            sr.setColor(1f, 1f, 1f, 0.4f);
            sr.circle(x + (heartSize * 0.25f), startY + (heartSize * 0.25f), heartSize * 0.12f);
        }
        
        // 3. Draw a thin white outline for the active hearts to make them "pop"
        sr.setColor(Color.WHITE);
        for (int i = 0; i < health; i++) {
            float x = startX + i * (heartSize + spacing);
            drawHeartOutline(sr, x, startY, heartSize);
        }
    }

    /**
     * Helper to draw a filled heart shape.
     * Calculated for a Y-Down coordinate system.
     */
    private void drawHeartShape(ShapeRenderer sr, float x, float y, float size) {
        float r = size / 4f;
        
        // Top humps
        sr.circle(x + r, y + r, r);
        sr.circle(x + 3 * r, y + r, r);
        
        // Bottom point
        sr.triangle(
            x, y + r, 
            x + size, y + r, 
            x + size / 2f, y + size
        );
    }

    /**
     * Helper to draw a heart outline using thin rectangles.
     */
    private void drawHeartOutline(ShapeRenderer sr, float x, float y, float size) {
        float r = size / 4f;
        // Simple 3-point outline for the bottom triangle
        sr.rectLine(x, y + r, x + size / 2f, y + size, 1);
        sr.rectLine(x + size, y + r, x + size / 2f, y + size, 1);
        
        // Top arcs (simplified as rectangles for the outline edges)
        sr.rect(x + r - 1, y, 2, 1);
        sr.rect(x + 3 * r - 1, y, 2, 1);
        sr.rect(x, y + r - 1, 1, 2);
        sr.rect(x + size - 1, y + r - 1, 1, 2);
    }
}