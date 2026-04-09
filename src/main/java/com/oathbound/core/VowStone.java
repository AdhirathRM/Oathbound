package com.oathbound.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.awt.Rectangle; // Kept for PhysicsComponent interoperability

/**
 * PB-020 — Vow Stone
 * The level's goal point. Touching this triggers level completion.
 * Ported to LibGDX ShapeRenderer.
 */
public class VowStone {
    private final Rectangle bounds;
    private final int width = 64;
    private final int height = 96; // Taller than a standard tile
    private boolean activated = false;

    public VowStone(int x, int y) {
        this.bounds = new Rectangle(x, y, width, height);
    }

    public void render(ShapeRenderer sr) {
        if (activated) {
            sr.setColor(100/255f, 1f, 1f, 1f); // Glowing Cyan (R, G, B, A)
        } else {
            sr.setColor(50/255f, 50/255f, 150/255f, 1f); // Dormant Blue
        }
        sr.rect(bounds.x, bounds.y, width, height);
    }

    public boolean checkCollision(Rectangle playerBounds) {
        if (!activated && bounds.intersects(playerBounds)) {
            activated = true;
            return true; 
        }
        return false;
    }
}