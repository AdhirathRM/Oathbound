package com.oathbound.core;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * PB-017 — The Beastman Class
 * High-speed, short-range rapid melee attacker.
 * Optimized for fast animation cycles and aggressive play.
 */
public class Beastman extends Player {

    public Beastman(int x, int y) {
        super(x, y);
        
        // --- PB-017: Combat Tuning ---
        // 400ms allows all 7 frames to play at speed 2 (14 ticks total)
        // plus a small "recovery" window before returning to idle.
        this.attackDurationMs = 400; 
        
        // Lower number = Faster animation. (2 ticks per frame)
        this.attackAnimSpeed = 2; 
        
        loadSprites();
    }

    @Override
    protected void loadSprites() {
        try {
            // Load Beastman Walking Sheet (68x68, 6 frames)
            var walkRes = getClass().getResourceAsStream("/sprites/beastman_walk.png");
            if (walkRes != null) {
                BufferedImage walkSheet = ImageIO.read(walkRes);
                walkFrames = new BufferedImage[6];
                for (int i = 0; i < 6; i++) {
                    walkFrames[i] = walkSheet.getSubimage(i * width, 0, width, height);
                }
            }
            
            // Load Beastman Claw Attack Sheet (68x68, 7 frames)
            var attackRes = getClass().getResourceAsStream("/sprites/beastman_attack.png");
            if (attackRes != null) {
                BufferedImage attackSheet = ImageIO.read(attackRes);
                attackFrames = new BufferedImage[7];
                for (int i = 0; i < 7; i++) {
                    attackFrames[i] = attackSheet.getSubimage(i * width, 0, width, height);
                }
            }
        } catch (IOException e) {
            System.err.println("[Beastman] Sprites missing, using default Knight assets.");
        }
    }

    /**
     * PB-017: Beastman Hitbox
     * Shorter horizontal reach but taller vertical "swipe" to represent claws.
     */
    @Override
    protected void updateHitbox() {
        int hbW = 42; // Short reach (requires getting close)
        int hbH = 62; // Tall swipe (hits jumping enemies easier)
        
        // Offset so the claw hit comes out from the Beastman's body
        int hbX = (facing == 1) ? bounds.x + width - 12 : bounds.x - hbW + 12;
        int hbY = bounds.y + 2;
        
        attackHitbox.setBounds(hbX, hbY, hbW, hbH);
    }

    // ── Movement Overrides ───────────────────────────────────────────────────

    @Override
    public void setLeft(boolean pressed) {
        if (pressed) facing = -1;
        // Boosted speed: 420f vs Knight's 300f
        physics.velocityX = pressed ? -420f : (physics.velocityX < 0 ? 0 : physics.velocityX);
    }

    @Override
    public void setRight(boolean pressed) {
        if (pressed) facing = 1;
        // Boosted speed: 420f vs Knight's 300f
        physics.velocityX = pressed ? 420f : (physics.velocityX > 0 ? 0 : physics.velocityX);
    }
}