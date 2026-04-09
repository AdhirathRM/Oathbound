package com.oathbound.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.awt.Rectangle; // Kept for PhysicsComponent interoperability
import java.util.List;

public class Player {

    protected final int width = 68;
    protected final int height = 68;

    protected final Rectangle bounds;
    protected final PhysicsComponent physics;

    protected int health = 5;
    protected final int maxHealth = 5;
    protected boolean invincible = false;

    protected Texture walkSheet;
    protected Texture attackSheet;
    protected TextureRegion[] walkFrames;
    protected TextureRegion[] attackFrames;
    
    protected int frameIndex = 0;
    protected int animTick = 0;
    protected int animSpeed = 8; 

    protected int attackFrameIndex = 0;
    protected int attackAnimTick = 0;
    protected int attackAnimSpeed = 3; 

    protected boolean isAttacking = false;
    protected int facing = 1; 
    protected final Rectangle attackHitbox;
    
    // WASM-safe Timer (No Threads!)
    protected float attackTimer = 0f;
    protected final float ATTACK_DURATION_SEC = 0.25f;

    public Player(int startX, int startY) {
        this.bounds = new Rectangle(startX, startY, width, height);
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
                walkFrames[i].flip(false, true); // Flip Y to match camera
            }
        }

        if (Gdx.files.internal("sprites/knight_attack.png").exists()) {
            attackSheet = new Texture(Gdx.files.internal("sprites/knight_attack.png"));
            attackFrames = new TextureRegion[7];
            for (int i = 0; i < 7; i++) {
                attackFrames[i] = new TextureRegion(attackSheet, i * width, 0, width, height);
                attackFrames[i].flip(false, true); // Flip Y to match camera
            }
        }
    }

    public void update(float dt, List<Rectangle> solidTiles) {
        physics.update(dt, bounds, solidTiles, 1280, 736);

        if (isAttacking) {
            attackTimer += dt;
            if (attackTimer >= ATTACK_DURATION_SEC) {
                isAttacking = false;
            }
            updateAttackAnimation();
        } else {
            updateWalkAnimation();
        }
    }

    protected void updateWalkAnimation() {
        attackFrameIndex = 0;
        attackAnimTick = 0;

        if (Math.abs(physics.velocityX) > 0.1f) {
            animTick++;
            if (animTick >= animSpeed) {
                animTick = 0;
                frameIndex = (frameIndex + 1) % 6; 
            }
        } else {
            frameIndex = 0; 
        }
    }

    protected void updateAttackAnimation() {
        attackAnimTick++;
        if (attackAnimTick >= attackAnimSpeed) {
            attackAnimTick = 0;
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
            batch.draw(currentFrame, bounds.x, bounds.y, width, height);
        }
    }

    public void takeDamage(int amount) {
        if (invincible) return;
        health -= amount;
        if (health < 0) health = 0;
        if (health == 0) respawn();
    }

    public void setHealth(int h) { this.health = h; }

    public void respawn() {
        health = maxHealth;
        resetPosition(100, 200);
    }

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
            attackAnimTick = 0;
            updateHitbox();
        }
    }

    protected void updateHitbox() {
        int hbW = 60; 
        int hbH = 45;
        int hbX = (facing == 1) ? bounds.x + width : bounds.x - hbW;
        int hbY = bounds.y + (height / 4);
        attackHitbox.setBounds(hbX, hbY, hbW, hbH);
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
    }

    public Rectangle getBounds() { return bounds; }
    public int getFacing() { return this.facing; }
    public Rectangle getAttackHitbox() { return attackHitbox; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
}