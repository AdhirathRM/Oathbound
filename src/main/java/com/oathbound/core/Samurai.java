package com.oathbound.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.TimeUtils;
import java.awt.Rectangle;
import java.util.List;

/**
 * PB-018 — The Samurai Class
 * Updated for exact hitbox offsets & movement locking!
 */
public class Samurai extends Player {

    private final float dashSpeed = 900f; 
    private final float dashDurationSec = 0.2f; 
    private final float customAttackDurationSec = 0.40f; 
    
    private long lastDashTime = 0;
    private final long dashCooldown = 800; 

    public Samurai(int x, int y) {
        super(x, y);
        loadSprites();
    }

    @Override
    protected void loadSprites() {
        if (Gdx.files.internal("sprites/samurai_walk.png").exists()) {
            walkSheet = new Texture(Gdx.files.internal("sprites/samurai_walk.png"));
            walkFrames = new TextureRegion[6];
            for (int i = 0; i < 6; i++) {
                walkFrames[i] = new TextureRegion(walkSheet, i * width, 0, width, height);
                walkFrames[i].flip(false, true);
            }
        }
        if (Gdx.files.internal("sprites/samurai_attack.png").exists()) {
            attackSheet = new Texture(Gdx.files.internal("sprites/samurai_attack.png"));
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
        if (!isAttacking && (now - lastDashTime >= dashCooldown)) {
            isAttacking = true;
            invincible = true; 
            attackTimer = 0f; 
            attackAnimTimer = 0f; 
            lastDashTime = now;
            attackFrameIndex = 0;

            physics.velocityX = dashSpeed * facing;
        }
    }
    
    @Override
    public void update(float dt, List<Rectangle> solidTiles) {
        physics.update(dt, bounds, solidTiles, 1280, 736);

        if (isAttacking) {
            attackTimer += dt;
            
            if (attackTimer >= dashDurationSec && invincible) {
                invincible = false;
                physics.velocityX = 0f; 
            }
            
            if (attackTimer >= customAttackDurationSec) {
                isAttacking = false;
                invincible = false; 
                attackHitbox.setBounds(0, 0, 0, 0); 
            } else {
                updateAttackAnimation(dt);
            }
        } else {
            updateWalkAnimation(dt); 
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        if (invincible) {
            TextureRegion currentFrame = isAttacking ? attackFrames[attackFrameIndex] : walkFrames[frameIndex];
            if (currentFrame != null) {
                boolean needsFlip = (facing == -1);
                if (currentFrame.isFlipX() != needsFlip) currentFrame.flip(true, false);
                
                for (int i = 1; i <= 3; i++) {
                    float alpha = 0.6f - (i * 0.15f); 
                    batch.setColor(0.2f, 0.9f, 1f, alpha); 
                    
                    // Render ghosts with the correct render offset!
                    batch.draw(currentFrame, (bounds.x - renderOffsetX) - (facing * (35 * i)), bounds.y - renderOffsetY, width, height);
                }
            }
            batch.setColor(Color.WHITE); 
        }
        
        super.render(batch);
    }

    @Override
    protected void updateHitbox() {
        int hbW = 120; 
        int hbH = 30;
        // Use bounds.width to align the massive slash to the physical body
        int hbX = (facing == 1) ? bounds.x + bounds.width - 10 : bounds.x - hbW + 10;
        int hbY = bounds.y + (bounds.height / 2);
        attackHitbox.setBounds(hbX, hbY, hbW, hbH);
    }
    
    @Override
    public void setLeft(boolean pressed) {
        if (invincible) return; 
        super.setLeft(pressed);
    }

    @Override
    public void setRight(boolean pressed) {
        if (invincible) return; 
        super.setRight(pressed);
    }
}