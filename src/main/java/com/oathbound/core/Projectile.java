package com.oathbound.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * PB-011 & PB-016 — Projectile Class
 * Supports Archer arrows and Mage's Blue Mana Bombs (AOE).
 */
public class Projectile {

    private final Rectangle bounds;
    private float velocityX;
    private float velocityY;
    private boolean active = true;

    // PB-016: AOE specialized fields
    private boolean isAOE = false;
    private final int explosionRadius = 130; // Slightly larger for "Mana Burst"

    /**
     * Standard Constructor (Defaults to Archer)
     */
    public Projectile(int x, int y, float velX, float velY) {
        this(x, y, velX, velY, false);
    }

    /**
     * Specialized Constructor (Supports Mage AOE)
     */
    public Projectile(int x, int y, float velX, float velY, boolean isAOE) {
        this.isAOE = isAOE;
        
        // Define size based on magic type
        int w = isAOE ? 30 : 18;
        int h = isAOE ? 30 : 6; 
        
        this.bounds = new Rectangle(x, y, w, h);
        this.velocityX = velX;
        this.velocityY = velY;
    }

    public void update(float dt, List<Rectangle> solidTiles) {
        if (!active) return;

        // Linear movement
        bounds.x += (int) (velocityX * dt);
        bounds.y += (int) (velocityY * dt);

        // Map Collision: Projectile breaks if it hits a wall/floor
        for (Rectangle tile : solidTiles) {
            if (bounds.intersects(tile)) {
                deactivate();
                break;
            }
        }

        // Screen Boundary Check
        if (bounds.x < -100 || bounds.x > GameWindow.WIDTH + 100 || 
            bounds.y < -100 || bounds.y > GameWindow.HEIGHT + 100) {
            active = false;
        }
    }

    public void render(Graphics2D g) {
        if (!active) return;
        
        if (isAOE) {
            // --- PB-016: Blue Mana Bomb Visuals ---
            
            // 1. Outer Glow (Deep Blue Aura)
            g.setColor(new Color(0, 80, 255, 120)); 
            g.fillOval(bounds.x - 6, bounds.y - 6, bounds.width + 12, bounds.height + 12);

            // 2. Main Body (Electric Blue)
            g.setColor(new Color(0, 191, 255)); 
            g.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);

            // 3. Pulsing Core (Bright Cyan)
            long time = System.currentTimeMillis();
            int pulse = (int)(Math.sin(time / 100.0) * 4); // Creates a breathing effect
            g.setColor(new Color(220, 255, 255)); 
            g.fillOval(bounds.x + 8 - (pulse/2), bounds.y + 8 - (pulse/2), 14 + pulse, 14 + pulse);
            
        } else {
            // --- Archer Arrow Visual ---
            g.setColor(new Color(139, 69, 19)); // Wooden Shaft
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            
            // Arrowhead
            g.setColor(Color.LIGHT_GRAY);
            if (velocityX > 0) {
                g.fillRect(bounds.x + bounds.width - 4, bounds.y - 2, 4, bounds.height + 4);
            } else {
                g.fillRect(bounds.x, bounds.y - 2, 4, bounds.height + 4);
            }
        }
    }

    public void deactivate() {
        this.active = false;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public boolean isActive() { return active; }
    public Rectangle getBounds() { return bounds; }
    public boolean isAOE() { return isAOE; }
    public int getExplosionRadius() { return explosionRadius; }
}