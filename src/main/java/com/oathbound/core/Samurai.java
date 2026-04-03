package com.oathbound.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * PB-018 — The Samurai Class
 * Features a Dash-Slash with I-Frames for defensive offense.
 */
public class Samurai extends Player {

    private final float dashSpeed = 900f; // Rapid surge
    private final long dashDuration = 200; // 0.2 seconds of pure speed/invincibility
    private long lastDashTime = 0;
    private final long dashCooldown = 800; // Prevents infinite dashing

    public Samurai(int x, int y) {
        super(x, y);
        this.attackDurationMs = 400; 
        this.attackAnimSpeed = 2;
        loadSprites();
    }

    @Override
    protected void loadSprites() {
        try {
            // Load Samurai Assets
            var walkRes = getClass().getResourceAsStream("/sprites/samurai_walk.png");
            if (walkRes != null) {
                BufferedImage walkSheet = ImageIO.read(walkRes);
                walkFrames = new BufferedImage[6];
                for (int i = 0; i < 6; i++) walkFrames[i] = walkSheet.getSubimage(i * width, 0, width, height);
            }
            
            var attackRes = getClass().getResourceAsStream("/sprites/samurai_attack.png");
            if (attackRes != null) {
                BufferedImage attackSheet = ImageIO.read(attackRes);
                attackFrames = new BufferedImage[7];
                for (int i = 0; i < 7; i++) attackFrames[i] = attackSheet.getSubimage(i * width, 0, width, height);
            }
        } catch (IOException e) {
            System.err.println("[Samurai] Using default assets.");
        }
    }

    @Override
    public void attack() {
        long now = System.currentTimeMillis();
        if (!isAttacking && (now - lastDashTime >= dashCooldown)) {
            isAttacking = true;
            invincible = true; // PB-018: Trigger I-Frames
            lastDashTime = now;
            attackFrameIndex = 0;

            // Apply the dash force
            physics.velocityX = dashSpeed * facing;

            new Thread(() -> {
                try {
                    // Dash duration (Invincible phase)
                    Thread.sleep(dashDuration);
                    invincible = false; 
                    physics.velocityX = 0; // Stop the surge

                    // Remaining animation/recovery time
                    Thread.sleep(attackDurationMs - dashDuration);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                isAttacking = false;
            }).start();
        }
    }

    @Override
    public void render(Graphics2D g) {
        // PB-018 Visual: If invincible, draw with an after-image or blue tint
        if (invincible) {
            g.setColor(new Color(100, 200, 255, 100)); // Blue ghost effect
            g.fillRect(bounds.x - (facing * 20), bounds.y, width, height);
        }
        super.render(g);
    }

    @Override
    protected void updateHitbox() {
        // Samurai has a long, narrow "Sheath Strike"
        int hbW = 85; 
        int hbH = 30;
        int hbX = (facing == 1) ? bounds.x + width - 10 : bounds.x - hbW + 10;
        int hbY = bounds.y + (height / 2);
        attackHitbox.setBounds(hbX, hbY, hbW, hbH);
    }
}