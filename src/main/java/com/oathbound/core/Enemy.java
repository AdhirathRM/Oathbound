package com.oathbound.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.awt.Rectangle;
import java.util.List;

public class Enemy {

    private final Rectangle bounds;
    private final PhysicsComponent physics;
    
    private final int FRAME_SIZE = 68;
    private final int renderWidth = 102;
    private final int renderHeight = 102;
    
    private final int hitboxWidth = 51; 
    private final int hitboxHeight = 81; 
    private final int renderOffsetX = (renderWidth - hitboxWidth) / 2;
    private final int renderOffsetY = renderHeight - hitboxHeight;

    private int health = 3;
    private boolean active = true;
    private int direction = 1; 
    private final float moveSpeed = 80f;

    private boolean invincible = false;
    private float iFrameTimer = 0f;
    private final float I_FRAME_DURATION_SEC = 0.40f; 
    
    public enum State { WALKING, ATTACKING, DYING }
    private State currentState = State.WALKING;
    
    private Texture walkSheet;
    private Texture attackSheet;
    private Texture deathSheet;
    private TextureRegion[] walkFrames;
    private TextureRegion[] attackFrames;
    private TextureRegion[] deathFrames;
    
    private float animTimer = 0f;
    private int frameIndex = 0;
    private final float ANIM_FRAME_TIME = 0.1f; 
    private final float ATTACK_FRAME_TIME = 0.08f; 

    private final Rectangle attackHitbox;

    public Enemy(int gridX, int gridY) {
        // ALIGNMENT FIX: 
        // 1. Center the 51px hitbox horizontally over the 32px grid cell.
        int alignedX = gridX + (32 - hitboxWidth) / 2;
        // 2. Ensure their feet (bottom of hitbox) are touching the floor of the cell.
        int alignedY = (gridY + 32) - hitboxHeight;
        
        // 3. Prevent spawning partially outside the screen boundaries to stop "instant falling"
        if (alignedX < 5) alignedX = 5;
        if (alignedX + hitboxWidth > 1275) alignedX = 1275 - hitboxWidth;

        this.bounds = new Rectangle(alignedX, alignedY, hitboxWidth, hitboxHeight);
        this.physics = new PhysicsComponent();
        this.attackHitbox = new Rectangle(0, 0, 0, 0); 
        loadSprites();
    }
    
    private void loadSprites() {
        if (Gdx.files.internal("sprites/enemy_walk.png").exists()) {
            walkSheet = new Texture(Gdx.files.internal("sprites/enemy_walk.png"));
            walkFrames = new TextureRegion[6];
            for (int i = 0; i < 6; i++) {
                walkFrames[i] = new TextureRegion(walkSheet, i * FRAME_SIZE, 0, FRAME_SIZE, FRAME_SIZE);
                walkFrames[i].flip(false, true); 
            }
        }
        
        if (Gdx.files.internal("sprites/enemy_attack.png").exists()) {
            attackSheet = new Texture(Gdx.files.internal("sprites/enemy_attack.png"));
            attackFrames = new TextureRegion[6]; 
            for (int i = 0; i < 6; i++) {
                attackFrames[i] = new TextureRegion(attackSheet, i * FRAME_SIZE, 0, FRAME_SIZE, FRAME_SIZE);
                attackFrames[i].flip(false, true);
            }
        }
        
        if (Gdx.files.internal("sprites/enemy_death.png").exists()) {
            deathSheet = new Texture(Gdx.files.internal("sprites/enemy_death.png"));
            deathFrames = new TextureRegion[6]; 
            for (int i = 0; i < 6; i++) {
                deathFrames[i] = new TextureRegion(deathSheet, i * FRAME_SIZE, 0, FRAME_SIZE, FRAME_SIZE);
                deathFrames[i].flip(false, true);
            }
        }
    }

    public void update(float dt, List<Rectangle> solidTiles) {
        if (!active) return;
        
        if (currentState == State.DYING) {
            animTimer += dt;
            if (animTimer >= ANIM_FRAME_TIME) {
                animTimer -= ANIM_FRAME_TIME;
                if (frameIndex < 5) { 
                    frameIndex++;
                } else {
                    active = false; 
                }
            }
            physics.velocityX = 0;
            physics.update(dt, bounds, solidTiles, 1280, 736); 
            return; 
        }

        if (invincible) {
            iFrameTimer += dt;
            if (iFrameTimer >= I_FRAME_DURATION_SEC) {
                invincible = false;
            }
        }

        if (currentState == State.ATTACKING) {
            physics.velocityX = 0; 
            physics.update(dt, bounds, solidTiles, 1280, 736);
            
            animTimer += dt;
            if (animTimer >= ATTACK_FRAME_TIME) {
                animTimer -= ATTACK_FRAME_TIME;
                frameIndex++;
                
                if (frameIndex >= 6) {
                    currentState = State.WALKING;
                    frameIndex = 0;
                    attackHitbox.setBounds(0, 0, 0, 0); 
                } else if (frameIndex == 3 || frameIndex == 4) {
                    int hbW = 75;
                    int hbH = 75;
                    int hbX = (direction == 1) ? bounds.x + bounds.width : bounds.x - hbW;
                    int hbY = bounds.y + 15;
                    attackHitbox.setBounds(hbX, hbY, hbW, hbH);
                } else {
                    attackHitbox.setBounds(0, 0, 0, 0);
                }
            }
            return;
        }

        // --- IMPROVED MOVEMENT & AI ---
        physics.velocityX = moveSpeed * direction;
        physics.update(dt, bounds, solidTiles, 1280, 736); 

        // 1. Check for Walls (Proactive Wall Detection + Screen Edge Detection)
        int wallProbeX = (direction == 1) ? bounds.x + bounds.width + 2 : bounds.x - 4;
        
        // Check if the probe is going past the screen edges
        boolean hittingScreenEdge = (direction == 1 && bounds.x + bounds.width >= 1278) 
                                 || (direction == -1 && bounds.x <= 2);
        
        Rectangle wallProbe = new Rectangle(wallProbeX, bounds.y + 10, 2, bounds.height - 20);
        boolean hittingWall = hittingScreenEdge; 
        
        if (!hittingWall) {
            for (Rectangle tile : solidTiles) {
                if (wallProbe.intersects(tile)) {
                    hittingWall = true;
                    break;
                }
            }
        }

        if (hittingWall) {
            direction *= -1;
            physics.velocityX = moveSpeed * direction;
        } 
        // 2. Check for Ledges (Proactive Ledge Detection)
        else if (physics.isOnGround()) {
            boolean nearLedge = true;
            int probeX = (direction == 1) ? bounds.x + bounds.width + 5 : bounds.x - 5;
            int probeY = bounds.y + bounds.height + 2; 
            Rectangle ledgeProbe = new Rectangle(probeX, probeY, 2, 2);
            
            for (Rectangle tile : solidTiles) {
                if (ledgeProbe.intersects(tile)) {
                    nearLedge = false; 
                    break;
                }
            }
            
            if (nearLedge) {
                direction *= -1;
                physics.velocityX = moveSpeed * direction;
            }
        }
        
        animTimer += dt;
        if (animTimer >= ANIM_FRAME_TIME) {
            animTimer -= ANIM_FRAME_TIME;
            frameIndex = (frameIndex + 1) % 6;
        }
    }

    public void takeDamage(int damage) {
        if (!active || currentState == State.DYING) return; 

        if (!invincible) {
            health -= damage;
            invincible = true;
            iFrameTimer = 0f; 
            
            if (health <= 0) {
                currentState = State.DYING;
                frameIndex = 0; 
                animTimer = 0f;
                attackHitbox.setBounds(0, 0, 0, 0); 
            }
        }
    }

    public void triggerAttack() {
        if (currentState == State.WALKING) {
            currentState = State.ATTACKING;
            frameIndex = 0;
            animTimer = 0f;
        }
    }

    public void render(SpriteBatch batch) {
        if (!active) return;
        
        TextureRegion currentFrame = null;
        
        if (currentState == State.DYING && deathFrames != null) {
            currentFrame = deathFrames[frameIndex];
        } else if (currentState == State.ATTACKING && attackFrames != null) {
            currentFrame = attackFrames[frameIndex];
        } else if (walkFrames != null) {
            currentFrame = walkFrames[frameIndex];
        }
        
        if (currentFrame != null) {
            if (invincible && iFrameTimer < 0.15f && currentState != State.DYING) {
                batch.setColor(Color.RED);
            } else {
                batch.setColor(Color.WHITE);
            }
            
            boolean needsFlip = (direction == -1);
            if (currentFrame.isFlipX() != needsFlip) {
                currentFrame.flip(true, false);
            }
            
            batch.draw(currentFrame, bounds.x - renderOffsetX, bounds.y - renderOffsetY, renderWidth, renderHeight);
            batch.setColor(Color.WHITE); 
        }
    }

    public void setDirection(int dir) { this.direction = dir; }
    public Rectangle getAttackHitbox() { return attackHitbox; }
    public boolean isActive() { return active; }
    public Rectangle getBounds() { return bounds; }
    
    public void dispose() {
        if (walkSheet != null) walkSheet.dispose();
        if (attackSheet != null) attackSheet.dispose();
        if (deathSheet != null) deathSheet.dispose();
    }
}