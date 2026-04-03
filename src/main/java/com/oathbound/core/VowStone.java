package com.oathbound.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * PB-020 — Vow Stone
 * The level's goal point. Touching this triggers level completion.
 */
public class VowStone {
    private final Rectangle bounds;
    private final int width = 64;
    private final int height = 96; // Taller than a standard tile
    private boolean activated = false;

    public VowStone(int x, int y) {
        this.bounds = new Rectangle(x, y, width, height);
    }

    public void render(Graphics2D g) {
        if (activated) {
            g.setColor(new Color(100, 255, 255)); // Glowing Cyan
        } else {
            g.setColor(new Color(50, 50, 150)); // Dormant Blue
        }
        g.fillRect(bounds.x, bounds.y, width, height);
    }

    public boolean checkCollision(Rectangle playerBounds) {
        if (!activated && bounds.intersects(playerBounds)) {
            activated = true;
            return true; 
        }
        return false;
    }
}