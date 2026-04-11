package com.oathbound.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import java.awt.Rectangle;
import java.util.List;

/**
 * Crimson Essence: Dropped by boss to heal the player.
 * Now features gravity to naturally land on the floor!
 */
public class HealDrop {
    private final Rectangle bounds;
    private float lifeTimer = 0f;
    private final float MAX_LIFE = 10.0f; // Lasts 10 seconds to give you time to grab it
    private boolean active = true;
    
    private float velocityY = -200f; // Pops upwards slightly on spawn
    private boolean onGround = false;

    public HealDrop(int x, int y) {
        this.bounds = new Rectangle(x, y, 24, 24);
    }

    public void update(float dt, List<Rectangle> solidTiles) {
        lifeTimer += dt;
        if (lifeTimer >= MAX_LIFE) active = false;
        
        // Apply Gravity
        if (!onGround) {
            velocityY += 800f * dt; 
            bounds.y += (int)(velocityY * dt);
            
            // Check floor collision
            for (Rectangle tile : solidTiles) {
                if (bounds.intersects(tile)) {
                    bounds.y = tile.y - bounds.height; // Snap to top of the floor block
                    velocityY = 0;
                    onGround = true;
                    break;
                }
            }
        } else {
            // Gentle hover effect when resting on the ground
            bounds.y += MathUtils.sin(lifeTimer * 6) * 0.6f;
        }
    }

    public void render(ShapeRenderer sr) {
        if (!active) return;
        
        // Blinking effect before disappearing
        if (lifeTimer > MAX_LIFE - 2.5f && (int)(lifeTimer * 15) % 2 == 0) return;

        // Draw a pulsing heart shape
        float size = 20;
        float pulse = 1.0f + (MathUtils.sin(lifeTimer * 5) * 0.1f);
        float s = size * pulse;
        float r = s / 4f;
        
        sr.setColor(Color.RED);
        sr.circle(bounds.x + r, bounds.y + r, r);
        sr.circle(bounds.x + 3 * r, bounds.y + r, r);
        sr.triangle(bounds.x, bounds.y + r, bounds.x + s, bounds.y + r, bounds.x + s / 2f, bounds.y + s);
        
        // White glint
        sr.setColor(1, 1, 1, 0.4f);
        sr.circle(bounds.x + r, bounds.y + r, r / 2f);
    }

    public boolean checkPickUp(Rectangle playerBounds) {
        if (active && bounds.intersects(playerBounds)) {
            active = false;
            return true;
        }
        return false;
    }

    public boolean isActive() { return active; }
}