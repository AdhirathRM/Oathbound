package com.oathbound.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.TimeUtils;
import java.util.List;

public class Mage extends Player {

    private final List<Projectile> projectileList;
    private long lastSpellTime = 0;
    private final long SPELL_COOLDOWN = 1200;
    private final float customAttackDurationSec = 0.50f;

    public Mage(int x, int y, List<Projectile> gameProjectiles) {
        super(x, y);
        this.projectileList = gameProjectiles;
        loadSprites();
    }

    @Override
    protected void loadSprites() {
        if (Gdx.files.internal("sprites/mage_walk.png").exists()) {
            walkSheet = new Texture(Gdx.files.internal("sprites/mage_walk.png"));
            walkFrames = new TextureRegion[6];
            for (int i = 0; i < 6; i++) {
                walkFrames[i] = new TextureRegion(walkSheet, i * width, 0, width, height);
                walkFrames[i].flip(false, true);
            }
        }
        if (Gdx.files.internal("sprites/mage_attack.png").exists()) {
            attackSheet = new Texture(Gdx.files.internal("sprites/mage_attack.png"));
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
        if (!isAttacking && (now - lastSpellTime >= SPELL_COOLDOWN)) {
            isAttacking = true;
            attackFrameIndex = 0;
            attackAnimTimer = 0f; 
            attackTimer = 0f;
            lastSpellTime = now;
            castFireblast();
        }
    }
    
    @Override
    public void update(float dt, List<Rectangle> solidTiles) {
        physics.update(dt, bounds, solidTiles, 1280, 736);

        if (isAttacking) {
            attackTimer += dt;
            if (attackTimer >= customAttackDurationSec) {
                isAttacking = false;
                attackHitbox.set(0, 0, 0, 0); 
            } else {
                updateAttackAnimation(dt); 
            }
        } else {
            updateWalkAnimation(dt); 
        }
    }

    private void castFireblast() {
        float spellSpeed = 400f; 
        int spawnX = (facing == 1) ? (int)(bounds.x + bounds.width) : (int)bounds.x - 32;
        int spawnY = (int)(bounds.y + (bounds.height / 3));
        projectileList.add(new Projectile(spawnX, spawnY, spellSpeed * facing, 0, true));
    }
}