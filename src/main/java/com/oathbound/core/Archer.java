package com.oathbound.core;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * PB-010 — The Archer Class
 * A playable subclass of Player. Fires arrows instead of melee swings.
 */
public class Archer extends Player {

    private final List<Projectile> projectileList;
    private long lastShotTime = 0;
    private final long SHOT_COOLDOWN = 600; 

    // Increased duration to ensure 7 frames of animation finish and "hold" the pose
    protected final int attackDurationMs = 450; 

    public Archer(int x, int y, List<Projectile> gameProjectiles) {
        super(x, y); 
        this.projectileList = gameProjectiles;
        
        // Explicitly load Archer sprites to overwrite Knight defaults
        loadSprites(); 
    }

    /**
     * PB-010: Overrides the Knight's sprite loader with Archer-specific assets.
     */
    @Override
    protected void loadSprites() {
        try {
            // Load Walking Sheet (408x68)
            var walkRes = getClass().getResourceAsStream("/sprites/archer_walk.png");
            if (walkRes != null) {
                BufferedImage walkSheet = ImageIO.read(walkRes);
                walkFrames = new BufferedImage[6];
                for (int i = 0; i < 6; i++) {
                    walkFrames[i] = walkSheet.getSubimage(i * width, 0, width, height);
                }
            }

            // Load Attack Sheet (476x68)
            var attackRes = getClass().getResourceAsStream("/sprites/archer_attack.png");
            if (attackRes != null) {
                BufferedImage attackSheet = ImageIO.read(attackRes);
                attackFrames = new BufferedImage[7];
                for (int i = 0; i < 7; i++) {
                    attackFrames[i] = attackSheet.getSubimage(i * width, 0, width, height);
                }
            }
            
            System.out.println("[PB-010] Archer 68x68 sprites loaded.");
        } catch (IOException e) {
            System.err.println("[Archer] Failed to load sprites: " + e.getMessage());
        }
    }

    /**
     * PB-010: Ranged Attack.
     * Fires the projectile and triggers the 7-frame "draw bow" animation.
     */
    @Override
    public void attack() {
        long now = System.currentTimeMillis();
        
        // Prevent overlapping attacks or spamming faster than the cooldown
        if (!isAttacking && (now - lastShotTime >= SHOT_COOLDOWN)) {
            isAttacking = true;
            attackFrameIndex = 0; // Start at frame 0 (Not moving)
            attackAnimTick = 0;
            lastShotTime = now;

            // Spawn the arrow immediately on trigger
            fireArrow();

            // This thread manages the "isAttacking" state duration
            new Thread(() -> {
                try {
                    Thread.sleep(attackDurationMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // Once finished, player returns to "Walk/Idle" state
                isAttacking = false;
            }).start();
        }
    }

    private void fireArrow() {
        if (projectileList == null) return;

        float arrowSpeed = 800f;
        // Adjust spawn point so it appears to come from the bow
        int spawnX = (facing == 1) ? bounds.x + width : bounds.x - 10;
        int spawnY = bounds.y + (height / 3);
        
        projectileList.add(new Projectile(spawnX, spawnY, arrowSpeed * facing, 0));
        System.out.println("[PB-010] Archer loose!");
    }
}