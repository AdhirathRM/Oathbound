package com.oathbound.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * PB-011 — Projectile Class
 * Handles movement and collision for ranged attacks.
 */
public class Projectile {

    private final Rectangle bounds;
    private float velocityX;
    private float velocityY;
    private boolean active = true;

    // Dimensions for a standard projectile (e.g., a magic bolt or arrow)
    private final int width = 16;
    private final int height = 16;

    public Projectile(int x, int y, float velX, float velY) {
        this.bounds = new Rectangle(x, y, width, height);
        this.velocityX = velX;
        this.velocityY = velY;
    }

    public void update(float dt, List<Rectangle> solidTiles) {
        if (!active) return;

        // Move the projectile
        bounds.x += (int) (velocityX * dt);
        bounds.y += (int) (velocityY * dt);

        // Collision Check: If it hits a solid tile, it "breaks"
        for (Rectangle tile : solidTiles) {
            if (bounds.intersects(tile)) {
                active = false;
                break;
            }
        }

        // Screen Boundary Check: Deactivate if it flies off-screen
        if (bounds.x < 0 || bounds.x > GameWindow.WIDTH || 
            bounds.y < 0 || bounds.y > GameWindow.HEIGHT) {
            active = false;
        }
    }

    public void render(Graphics2D g) {
        if (!active) return;
        
        // Placeholder visual: A glowing yellow bolt
        g.setColor(Color.YELLOW);
        g.fillOval(bounds.x, bounds.y, width, height);
    }

    public boolean isActive() { return active; }
    public Rectangle getBounds() { return bounds; }
}