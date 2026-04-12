package com.oathbound.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import java.util.List;

public class Player {

    protected final int width = 68;
    protected final int height = 68;
    
    protected final int hitboxWidth = 30; 
    protected final int hitboxHeight = 54; 
    
    protected final int renderOffsetX = (width - hitboxWidth) / 2;
    protected final int renderOffsetY = height - hitboxHeight;

    protected final Rectangle bounds;
    protected final PhysicsComponent physics;

    protected int health = 5;
    protected final int maxHealth = 5;
    protected boolean invincible = false;

    protected Texture walkSheet;
    protected Texture attackSheet;
    protected Texture slashTexture; 
    protected TextureRegion[] walkFrames;
    protected TextureRegion[] attackFrames;
    
    protected int frameIndex = 0;
    protected float animTimer = 0f;
    protected final float ANIM_FRAME_TIME = 0.08f; 

    protected int attackFrameIndex = 0;
    protected float attackAnimTimer = 0f;
    protected final float ATTACK_ANIM_FRAME_TIME = 0.035f; 

    protected boolean isAttacking = false;
    protected int facing = 1; 
    protected final Rectangle attackHitbox;
    
    protected float attackTimer = 0f;
    protected final float ATTACK_DURATION_SEC = 0.25f;

    public Player(int startX, int startY) {
        this.bounds = new Rectangle(startX, startY, hitboxWidth, hitboxHeight);
        this.physics = new PhysicsComponent();
        this.attackHitbox = new Rectangle();
        loadSprites();
    }

    protected void loadSprites() {
        if (Gdx.files.internal("sprites/knight_walk.png").exists()) {
            walkSheet = new Texture(Gdx.files.internal("sprites/knight_walk.png"));
            walkFrames = new TextureRegion[6];
            for (int i = 0; i < 6; i++) {
                walkFrames[i] = new TextureRegion(walkSheet, i * width, 0, width, height);
                walkFrames[i].flip(false, true); 
            }
        }

        if (Gdx.files.internal("sprites/knight_attack.png").exists()) {
            attackSheet = new Texture(Gdx.files.internal("sprites/knight_attack.png"));
            attackFrames = new TextureRegion[7];
            for (int i = 0; i < 7; i++) {
                attackFrames[i] = new TextureRegion(attackSheet, i * width, 0, width, height);
                attackFrames[i].flip(false, true); 
            }
        }
        
        Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fillCircle(32, 32, 32);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(new Color(0f, 0f, 0f, 0f)); 
        pixmap.fillCircle(12, 32, 32);
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        slashTexture = new Texture(pixmap);
        pixmap.dispose(); 
    }

    public void update(float dt, List<Rectangle> solidTiles) {
        physics.update(dt, bounds, solidTiles, 1280, 736);

        if (isAttacking) {
            attackTimer += dt;
            if (attackTimer >= ATTACK_DURATION_SEC) {
                isAttacking = false;
                attackHitbox.set(0, 0, 0, 0); 
            } else {
                updateAttackAnimation(dt);
            }
        } else {
            updateWalkAnimation(dt);
        }
    }

    protected void updateWalkAnimation(float dt) {
        attackFrameIndex = 0;
        attackAnimTimer = 0f;

        if (Math.abs(physics.velocityX) > 0.1f) {
            animTimer += dt;
            if (animTimer >= ANIM_FRAME_TIME) {
                animTimer -= ANIM_FRAME_TIME; 
                frameIndex = (frameIndex + 1) % 6; 
            }
        } else {
            frameIndex = 0; 
            animTimer = 0f;
        }
    }

    protected void updateAttackAnimation(float dt) {
        attackAnimTimer += dt;
        if (attackAnimTimer >= ATTACK_ANIM_FRAME_TIME) {
            attackAnimTimer -= ATTACK_ANIM_FRAME_TIME;
            if (attackFrameIndex < 6) { 
                attackFrameIndex++;
            }
        }
        updateHitbox();
    }

    public void render(SpriteBatch batch) {
        TextureRegion currentFrame = null;
        if (isAttacking && attackFrames != null) {
            currentFrame = attackFrames[attackFrameIndex];
        } else if (walkFrames != null) {
            currentFrame = walkFrames[frameIndex];
        }

        if (currentFrame != null) {
            boolean needsFlip = (facing == -1);
            if (currentFrame.isFlipX() != needsFlip) {
                currentFrame.flip(true, false);
            }
            batch.draw(currentFrame, bounds.x - renderOffsetX, bounds.y - renderOffsetY, width, height);
        }
        
        if (isAttacking && attackHitbox.width > 0 && slashTexture != null) {
            float progress = attackTimer / ATTACK_DURATION_SEC; 
            float alpha = 1f - progress;
            batch.setColor(0.7f, 0.9f, 1.0f, alpha);
            
            int slashW = 32;
            int slashH = 64;
            
            float startX = (facing == 1) ? attackHitbox.x - 10 : attackHitbox.x + attackHitbox.width + 10 - slashW;
            float endX = (facing == 1) ? attackHitbox.x + attackHitbox.width - slashW : attackHitbox.x;
            
            float easeProgress = (float)(1.0 - Math.pow(1.0 - progress, 3));
            float currentX = startX + (endX - startX) * easeProgress;
            float currentY = attackHitbox.y - (slashH - attackHitbox.height) / 2f;
            
            TextureRegion slashRegion = new TextureRegion(slashTexture);
            if (facing == -1) slashRegion.flip(true, false);
            
            batch.draw(slashRegion, currentX, currentY, slashW, slashH);
            batch.setColor(Color.WHITE); 
        }
    }

    public void takeDamage(int amount) {
        if (invincible) return;
        health -= amount;
        if (health < 0) health = 0;
    }

    public void setHealth(int h) { this.health = h; }

    public void resetPosition(int x, int y) {
        this.bounds.x = x;
        this.bounds.y = y;
        if (physics != null) {
            physics.velocityX = 0;
            physics.velocityY = 0;
        }
    }

    public void attack() {
        if (!isAttacking) {
            isAttacking = true;
            attackTimer = 0f;
            attackFrameIndex = 0; 
            attackAnimTimer = 0f; 
            updateHitbox();
        }
    }

    protected void updateHitbox() {
        int hbW = 60; 
        int hbH = 45;
        int hbX = (facing == 1) ? (int)(bounds.x + bounds.width) : (int)(bounds.x) - hbW;
        int hbY = (int)(bounds.y + (bounds.height / 4));
        attackHitbox.set(hbX, hbY, hbW, hbH);
    }

    public void setLeft(boolean pressed) {
        if (pressed) facing = -1;
        physics.velocityX = pressed ? -300f : (physics.velocityX < 0 ? 0 : physics.velocityX);
    }

    public void setRight(boolean pressed) {
        if (pressed) facing = 1;
        physics.velocityX = pressed ? 300f : (physics.velocityX > 0 ? 0 : physics.velocityX);
    }

    public void jump() { physics.jump(); }

    public void dispose() {
        if (walkSheet != null) walkSheet.dispose();
        if (attackSheet != null) attackSheet.dispose();
        if (slashTexture != null) slashTexture.dispose(); 
    }

    public Rectangle getBounds() { return bounds; }
    public int getFacing() { return this.facing; }
    public Rectangle getAttackHitbox() { return attackHitbox; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
}