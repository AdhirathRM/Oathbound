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
 * Features a Dash-Slash with I-Frames. Thread.sleep completely removed!
 */
public class Samurai extends Player {

    private final float dashSpeed = 900f; 
    private final float dashDurationSec = 0.2f; 
    private final float customAttackDurationSec = 0.40f; 
    
    private long lastDashTime = 0;
    private final long dashCooldown = 800; 

    public Samurai(int x, int y) {
        super(x, y);
        this.attackAnimSpeed = 2;
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
            attackTimer = 0f; // Reset Web-Safe timer
            lastDashTime = now;
            attackFrameIndex = 0;

            // Apply the dash force instantly
            physics.velocityX = dashSpeed * facing;
        }
    }
    
    @Override
    public void update(float dt, List<Rectangle> solidTiles) {
        physics.update(dt, bounds, solidTiles, 1280, 736);

        if (isAttacking) {
            attackTimer += dt;
            
            // 1. Check if the high-speed dash phase is over
            if (attackTimer >= dashDurationSec && invincible) {
                invincible = false;
                physics.velocityX = 0f; // Stop the surge
            }
            
            // 2. Check if the whole attack animation is over
            if (attackTimer >= customAttackDurationSec) {
                isAttacking = false;
                invincible = false; // Safety catch
            }
            updateAttackAnimation();
        } else {
            updateWalkAnimation();
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        // PB-018 Visual: LibGDX native way to tint sprites!
        // This is much cooler than drawing a blue box. It tints the actual samurai blue.
        if (invincible) {
            batch.setColor(0.4f, 0.8f, 1f, 0.6f); // Semi-transparent blue
            
            // Draw a ghost "after-image" lagging slightly behind
            TextureRegion currentFrame = isAttacking ? attackFrames[attackFrameIndex] : walkFrames[frameIndex];
            if (currentFrame != null) {
                boolean needsFlip = (facing == -1);
                if (currentFrame.isFlipX() != needsFlip) currentFrame.flip(true, false);
                batch.draw(currentFrame, bounds.x - (facing * 20), bounds.y, width, height);
            }
            
            // Reset color so the main body draws normally
            batch.setColor(Color.WHITE); 
        }
        
        super.render(batch);
    }

    @Override
    protected void updateHitbox() {
        int hbW = 85; 
        int hbH = 30;
        int hbX = (facing == 1) ? bounds.x + width - 10 : bounds.x - hbW + 10;
        int hbY = bounds.y + (height / 2);
        attackHitbox.setBounds(hbX, hbY, hbW, hbH);
    }
}