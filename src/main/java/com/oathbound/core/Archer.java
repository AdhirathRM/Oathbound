package com.oathbound.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.TimeUtils;
import java.awt.Rectangle;
import java.util.List;

/**
 * PB-010 — The Archer Class
 * Ported to LibGDX. Replaced Thread.sleep with dt-based state management.
 */
public class Archer extends Player {

    private final List<Projectile> projectileList;
    private long lastShotTime = 0;
    private final long SHOT_COOLDOWN = 600; 
    
    private final float customAttackDurationSec = 0.45f;

    public Archer(int x, int y, List<Projectile> gameProjectiles) {
        super(x, y); 
        this.projectileList = gameProjectiles;
        loadSprites(); 
    }

    @Override
    protected void loadSprites() {
        if (Gdx.files.internal("sprites/archer_walk.png").exists()) {
            walkSheet = new Texture(Gdx.files.internal("sprites/archer_walk.png"));
            walkFrames = new TextureRegion[6];
            for (int i = 0; i < 6; i++) {
                walkFrames[i] = new TextureRegion(walkSheet, i * width, 0, width, height);
                walkFrames[i].flip(false, true);
            }
        }

        if (Gdx.files.internal("sprites/archer_attack.png").exists()) {
            attackSheet = new Texture(Gdx.files.internal("sprites/archer_attack.png"));
            attackFrames = new TextureRegion[7];
            for (int i = 0; i < 7; i++) {
                attackFrames[i] = new TextureRegion(attackSheet, i * width, 0, width, height);
                attackFrames[i].flip(false, true);
            }
        }
    }

    @Override
    public void attack() {
        long now = TimeUtils.millis();
        
        if (!isAttacking && (now - lastShotTime >= SHOT_COOLDOWN)) {
            isAttacking = true;
            attackFrameIndex = 0; 
            attackAnimTimer = 0f; // Fixed: Use the new float timer
            attackTimer = 0f; 
            lastShotTime = now;
            fireArrow();
        }
    }
    
    @Override
    public void update(float dt, List<Rectangle> solidTiles) {
        physics.update(dt, bounds, solidTiles, 1280, 736);

        // Override base Player update to use custom attack duration
        if (isAttacking) {
            attackTimer += dt;
            if (attackTimer >= customAttackDurationSec) {
                isAttacking = false;
                attackHitbox.setBounds(0, 0, 0, 0); // Clean up ghost hitbox
            } else {
                updateAttackAnimation(dt); // Fixed: Pass dt
            }
        } else {
            updateWalkAnimation(dt); // Fixed: Pass dt
        }
    }

    private void fireArrow() {
        if (projectileList == null) return;
        float arrowSpeed = 800f;
        int spawnX = (facing == 1) ? bounds.x + width : bounds.x - 10;
        int spawnY = bounds.y + (height / 3);
        
        projectileList.add(new Projectile(spawnX, spawnY, arrowSpeed * facing, 0));
    }
}