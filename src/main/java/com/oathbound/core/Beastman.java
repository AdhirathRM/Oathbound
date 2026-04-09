package com.oathbound.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.awt.Rectangle;
import java.util.List;

/**
 * PB-017 — The Beastman Class
 * Ported to LibGDX. High-speed rapid melee attacker.
 */
public class Beastman extends Player {

    private final float customAttackDurationSec = 0.40f; 

    public Beastman(int x, int y) {
        super(x, y);
        this.attackAnimSpeed = 2; // Fast animation speed
        loadSprites();
    }

    @Override
    protected void loadSprites() {
        if (Gdx.files.internal("sprites/beastman_walk.png").exists()) {
            walkSheet = new Texture(Gdx.files.internal("sprites/beastman_walk.png"));
            walkFrames = new TextureRegion[6];
            for (int i = 0; i < 6; i++) {
                walkFrames[i] = new TextureRegion(walkSheet, i * width, 0, width, height);
                walkFrames[i].flip(false, true);
            }
        }
        
        if (Gdx.files.internal("sprites/beastman_attack.png").exists()) {
            attackSheet = new Texture(Gdx.files.internal("sprites/beastman_attack.png"));
            attackFrames = new TextureRegion[7];
            for (int i = 0; i < 7; i++) {
                attackFrames[i] = new TextureRegion(attackSheet, i * width, 0, width, height);
                attackFrames[i].flip(false, true);
            }
        }
    }
    
    @Override
    public void update(float dt, List<Rectangle> solidTiles) {
        physics.update(dt, bounds, solidTiles, 1280, 736);

        if (isAttacking) {
            attackTimer += dt;
            if (attackTimer >= customAttackDurationSec) {
                isAttacking = false;
            }
            updateAttackAnimation();
        } else {
            updateWalkAnimation();
        }
    }

    @Override
    protected void updateHitbox() {
        int hbW = 42; 
        int hbH = 62; 
        int hbX = (facing == 1) ? bounds.x + width - 12 : bounds.x - hbW + 12;
        int hbY = bounds.y + 2;
        attackHitbox.setBounds(hbX, hbY, hbW, hbH);
    }

    @Override
    public void setLeft(boolean pressed) {
        if (pressed) facing = -1;
        physics.velocityX = pressed ? -420f : (physics.velocityX < 0 ? 0 : physics.velocityX);
    }

    @Override
    public void setRight(boolean pressed) {
        if (pressed) facing = 1;
        physics.velocityX = pressed ? 420f : (physics.velocityX > 0 ? 0 : physics.velocityX);
    }
}