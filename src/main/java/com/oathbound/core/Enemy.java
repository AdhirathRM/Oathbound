package com.oathbound.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.TimeUtils;
import java.awt.Rectangle;
import java.util.List;

/**
 * PB-013 — Basic Enemy
 * Ported to LibGDX. Uses ShapeRenderer and TimeUtils.
 */
public class Enemy {

    private final Rectangle bounds;
    private final PhysicsComponent physics;
    private final int width = 64;
    private final int height = 64;

    private int health = 3;
    private boolean active = true;
    private int direction = 1; // 1 = Right, -1 = Left
    private final float moveSpeed = 80f;

    // Invincibility Frames (I-Frames)
    private long lastDamageTime = 0;
    private final long I_FRAME_DURATION = 400;

    public Enemy(int startX, int startY) {
        this.bounds = new Rectangle(startX, startY, width, height);
        this.physics = new PhysicsComponent();
    }

    public void update(float dt, List<Rectangle> solidTiles) {
        if (!active) return;

        physics.velocityX = moveSpeed * direction;
        // Replaced GameWindow constraints with fixed screen size for now
        physics.update(dt, bounds, solidTiles, 1280, 736); 

        // Turn around if it hits a wall
        if (physics.velocityX == 0) {
            direction *= -1; 
        }
    }

    public void takeDamage(int damage) {
        long now = TimeUtils.millis();
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

    public void render(ShapeRenderer sr) {
        if (!active) return;
        
        // Flash white when damaged, otherwise red
        if (TimeUtils.millis() - lastDamageTime < 150) {
            sr.setColor(Color.WHITE);
        } else {
            sr.setColor(Color.RED);
        }
        sr.rect(bounds.x, bounds.y, width, height);
    }

    public boolean isActive() { return active; }
    public Rectangle getBounds() { return bounds; }
}