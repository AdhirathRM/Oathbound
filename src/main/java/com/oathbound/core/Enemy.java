package com.oathbound.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * PB-013 — Basic Enemy
 * A patrolling red block that takes damage.
 */
public class Enemy {

    private final Rectangle bounds;
    private final PhysicsComponent physics;
    private final int width = 64;
    private final int height = 64;

    private int health = 3;
    private boolean active = true;
    private int direction = 1; // 1 = Right, -1 = Left
    private final float moveSpeed = 80f; // Slower than the player

    // Invincibility Frames (I-Frames) to prevent taking rapid-fire damage
    private long lastDamageTime = 0;
    private final long I_FRAME_DURATION = 400;

    public Enemy(int startX, int startY) {
        this.bounds = new Rectangle(startX, startY, width, height);
        this.physics = new PhysicsComponent();
    }

    public void update(float dt, List<Rectangle> solidTiles) {
        if (!active) return;

        physics.velocityX = moveSpeed * direction;
        physics.update(dt, bounds, solidTiles, GameWindow.WIDTH, GameWindow.HEIGHT);

        // Turn around if it hits a wall
        if (physics.velocityX == 0) {
            direction *= -1; 
        }
    }

    public void takeDamage(int damage) {
        long now = System.currentTimeMillis();
        if (now - lastDamageTime >= I_FRAME_DURATION) {
            health -= damage;
            lastDamageTime = now;
            System.out.println("[Enemy] Hit! Health: " + health);

            if (health <= 0) {
                active = false;
                System.out.println("[Enemy] Destroyed!");
            }
        }
    }

    public void render(Graphics2D g) {
        if (!active) return;
        
        // Flash white when damaged, otherwise red
        if (System.currentTimeMillis() - lastDamageTime < 150) {
            g.setColor(Color.WHITE);
        } else {
            g.setColor(Color.RED);
        }
        g.fillRect(bounds.x, bounds.y, width, height);
    }

    public boolean isActive() { return active; }
    public Rectangle getBounds() { return bounds; }
}