package com.oathbound.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * PB-008 — Player Class
 * * Represents the playable hero. Uses composition to handle physics.
 */
public class Player {

    private final Rectangle bounds;
    private final PhysicsComponent physics;
    
    // Player-specific stats
    private final int width = 32;
    private final int height = 48;

    public Player(int startX, int startY) {
        this.bounds = new Rectangle(startX, startY, width, height);
        this.physics = new PhysicsComponent();
    }

    /**
     * Updates the player's position and resolves collisions.
     */
    public void update(float dt, List<Rectangle> solidTiles) {
        physics.update(dt, bounds, solidTiles, GameWindow.WIDTH, GameWindow.HEIGHT);
    }

    /**
     * Draws the player. Currently a placeholder until PB-009 (Sprites).
     */
    public void render(Graphics2D g) {
        g.setColor(Color.CYAN);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        
        // Optional: Draw a small outline to show the hitbox clearly
        g.setColor(Color.WHITE);
        g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    // Input hooks for GamePanel/KeyHandler
    public void setLeft(boolean pressed) {
        physics.velocityX = pressed ? -300f : (physics.velocityX < 0 ? 0 : physics.velocityX);
    }

    public void setRight(boolean pressed) {
        physics.velocityX = pressed ? 300f : (physics.velocityX > 0 ? 0 : physics.velocityX);
    }

    public void jump() {
        physics.jump();
    }

    public Rectangle getBounds() { return bounds; }
}