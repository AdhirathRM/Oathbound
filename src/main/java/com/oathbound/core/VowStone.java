package com.oathbound.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.TimeUtils;
import java.awt.Rectangle; 

/**
 * PB-020 — Vow Stone (Portal)
 * The level's goal point. Touching this triggers level completion.
 * Updated with auto-alignment and a pulsing mystical portal design!
 */
public class VowStone {
    private final Rectangle bounds;
    private final int width = 64;
    private final int height = 96; // Taller than a standard tile
    private boolean activated = false;

    public VowStone(int gridX, int gridY) {
        // ALIGNMENT FIX:
        // A standard tile is 32x32. We want the 96px portal to sit precisely on the floor.
        // We shift the Y position so its bottom aligns with the bottom of the 32px grid cell.
        int alignedY = (gridY + 32) - height;
        
        // We shift the X position to perfectly center the 64px wide portal over the 32px cell.
        int alignedX = gridX - 16;
        
        this.bounds = new Rectangle(alignedX, alignedY, width, height);
    }

    public void render(ShapeRenderer sr) {
        // Calculate a smooth pulsing animation based on time
        long time = TimeUtils.millis();
        float pulse = MathUtils.sin(time / 200.0f) * 4f; 
        float fastPulse = MathUtils.sin(time / 100.0f) * 2f;
        
        if (activated) {
            // Activated state: A bright, flashing cyan/white flash as you teleport
            sr.setColor(0.1f, 0.9f, 1f, 1f); 
            sr.ellipse(bounds.x, bounds.y, width, height);
            
            sr.setColor(1f, 1f, 1f, 1f); // Blinding white core
            sr.ellipse(bounds.x + 10, bounds.y + 15, width - 20, height - 30);
        } else {
            // Dormant / Idle Portal Look
            
            // Layer 1: Outer dark mystical ring
            sr.setColor(40/255f, 20/255f, 80/255f, 1f); 
            sr.ellipse(bounds.x, bounds.y, width, height);
            
            // Layer 2: Glowing deep blue ring (slightly pulsing)
            sr.setColor(50/255f, 50/255f, 180/255f, 1f);
            sr.ellipse(bounds.x + 6 - (pulse/2), bounds.y + 8 - (pulse/2), width - 12 + pulse, height - 16 + pulse);
            
            // Layer 3: Inner bright cyan swirling/pulsing energy
            sr.setColor(0f, 191/255f, 1f, 0.9f);
            sr.ellipse(bounds.x + 14 - pulse, bounds.y + 20 - pulse, width - 28 + (pulse*2), height - 40 + (pulse*2));
            
            // Layer 4: Deep void/gateway center (rapid slight pulse)
            sr.setColor(10/255f, 5/255f, 25/255f, 1f);
            sr.ellipse(bounds.x + 22 - fastPulse, bounds.y + 32 - fastPulse, width - 44 + (fastPulse*2), height - 64 + (fastPulse*2));
        }
    }

    public boolean checkCollision(Rectangle playerBounds) {
        if (!activated && bounds.intersects(playerBounds)) {
            activated = true;
            return true; 
        }
        return false;
    }
}