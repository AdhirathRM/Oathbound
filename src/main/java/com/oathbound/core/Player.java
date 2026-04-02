package com.oathbound.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * PB-008 & PB-012 — Player Class
 * * Represents the playable hero. Handles physics, movement direction, 
 * and melee attack hitbox generation.
 */
public class Player {

    // ── Fields ───────────────────────────────────────────────────────────────

    private final Rectangle bounds;
    private final PhysicsComponent physics;
    
    // Player dimensions
    private final int width = 32;
    private final int height = 48;

    // PB-012 Combat Fields
    private boolean isAttacking = false;
    private int facing = 1; // 1 = Right, -1 = Left
    private final Rectangle attackHitbox;
    
    /** How long the melee hitbox stays active (in milliseconds). */
    private final int attackDurationMs = 150;

    // ── Constructor ──────────────────────────────────────────────────────────

    public Player(int startX, int startY) {
        this.bounds = new Rectangle(startX, startY, width, height);
        this.physics = new PhysicsComponent();
        this.attackHitbox = new Rectangle();
    }

    // ── Update & Render ──────────────────────────────────────────────────────

    /**
     * Updates the player's position and resolves collisions.
     */
    public void update(float dt, List<Rectangle> solidTiles) {
        physics.update(dt, bounds, solidTiles, GameWindow.WIDTH, GameWindow.HEIGHT);
        
        // If attacking, keep the hitbox locked to the player's current position
        if (isAttacking) {
            updateHitbox();
        }
    }

    /**
     * Draws the player and their attack hitbox if active.
     */
    public void render(Graphics2D g) {
        // Draw Player Body
        g.setColor(Color.CYAN);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        
        g.setColor(Color.WHITE);
        g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

        // PB-012: Draw Attack Hitbox (Debug Visualization)
        if (isAttacking) {
            g.setColor(new Color(255, 0, 0, 120)); // Semi-transparent red
            g.fillRect(attackHitbox.x, attackHitbox.y, attackHitbox.width, attackHitbox.height);
        }
    }

    // ── Combat Logic ─────────────────────────────────────────────────────────

    /**
     * PB-012 — Triggers a melee swing.
     * Starts a brief timer to deactivate the hitbox after attackDurationMs.
     */
    public void attack() {
        if (!isAttacking) {
            isAttacking = true;
            updateHitbox();

            // Simple timer to end the attack state
            new Thread(() -> {
                try {
                    Thread.sleep(attackDurationMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                isAttacking = false;
            }).start();
        }
    }

    /**
     * Positions the hitbox rectangle relative to the player's bounds and direction.
     */
    private void updateHitbox() {
        int hbW = 40; // Hitbox width
        int hbH = 32; // Hitbox height
        
        // If facing right (1), place in front. If left (-1), place behind.
        int hbX = (facing == 1) ? bounds.x + bounds.width : bounds.x - hbW;
        int hbY = bounds.y + (bounds.height / 4);

        attackHitbox.setBounds(hbX, hbY, hbW, hbH);
    }

    // ── Input Hooks ──────────────────────────────────────────────────────────

    public void setLeft(boolean pressed) {
        if (pressed) facing = -1;
        physics.velocityX = pressed ? -300f : (physics.velocityX < 0 ? 0 : physics.velocityX);
    }

    public void setRight(boolean pressed) {
        if (pressed) facing = 1;
        physics.velocityX = pressed ? 300f : (physics.velocityX > 0 ? 0 : physics.velocityX);
    }

    public void jump() {
        physics.jump();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Rectangle getBounds() { return bounds; }
    
    /** @return the current attack hitbox, or null if not attacking. */
    public Rectangle getAttackHitbox() {
        return isAttacking ? attackHitbox : null;
    }
}