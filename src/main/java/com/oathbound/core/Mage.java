package com.oathbound.core;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * PB-016 — The Mage Class
 * A subclass of Player focusing on AOE Magic Attacks.
 */
public class Mage extends Player {

    private final List<Projectile> projectileList;
    private long lastSpellTime = 0;
    private final long SPELL_COOLDOWN = 1200; // Mages are slower but more powerful

    public Mage(int x, int y, List<Projectile> gameProjectiles) {
        super(x, y);
        this.projectileList = gameProjectiles;
        this.attackDurationMs = 500; // Longer casting animation
        loadSprites();
    }

    @Override
    protected void loadSprites() {
        try {
            // Load Mage Walking Sheet (68x68, 6 frames)
            var walkRes = getClass().getResourceAsStream("/sprites/mage_walk.png");
            if (walkRes != null) {
                BufferedImage walkSheet = ImageIO.read(walkRes);
                walkFrames = new BufferedImage[6];
                for (int i = 0; i < 6; i++) {
                    walkFrames[i] = walkSheet.getSubimage(i * width, 0, width, height);
                }
            }
            // Load Mage Attack Sheet (68x68, 7 frames)
            var attackRes = getClass().getResourceAsStream("/sprites/mage_attack.png");
            if (attackRes != null) {
                BufferedImage attackSheet = ImageIO.read(attackRes);
                attackFrames = new BufferedImage[7];
                for (int i = 0; i < 7; i++) {
                    attackFrames[i] = attackSheet.getSubimage(i * width, 0, width, height);
                }
            }
        } catch (IOException e) {
            System.err.println("[Mage] Sprites missing, using default.");
        }
    }

    @Override
    public void attack() {
        long now = System.currentTimeMillis();
        if (!isAttacking && (now - lastSpellTime >= SPELL_COOLDOWN)) {
            isAttacking = true;
            attackFrameIndex = 0;
            lastSpellTime = now;

            castFireblast();

            new Thread(() -> {
                try { Thread.sleep(attackDurationMs); } 
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                isAttacking = false;
            }).start();
        }
    }

    private void castFireblast() {
        float spellSpeed = 400f; // Slower than an arrow
        int spawnX = (facing == 1) ? bounds.x + width : bounds.x - 32;
        int spawnY = bounds.y + (height / 3);
        
        // We pass 'true' for AOE capability (we'll update Projectile.java next)
        projectileList.add(new Projectile(spawnX, spawnY, spellSpeed * facing, 0, true));
        System.out.println("[PB-016] Fireblast cast!");
    }
}