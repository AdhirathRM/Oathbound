package com.oathbound.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.List;

/**
 * PB-008, PB-014, PB-017, PB-018 — Base Player Class
 * Master class for Knight, Archer, Mage, Beastman, and Samurai.
 */
public class Player {

    // ── Dimensions ───────────────────────────────────────────────────────────
    protected final int width = 68;
    protected final int height = 68;

    // ── Physics & Bounds ─────────────────────────────────────────────────────
    protected final Rectangle bounds;
    protected final PhysicsComponent physics;

    // ── Health & I-Frames (PB-014 & PB-018) ──────────────────────────────────
    protected int health = 5;
    protected final int maxHealth = 5;
    protected boolean invincible = false; // Samurai Dash I-Frames

    // ── Animation Arrays ─────────────────────────────────────────────────────
    protected BufferedImage[] walkFrames;
    protected BufferedImage[] attackFrames;
    
    protected int frameIndex = 0;
    protected int animTick = 0;
    protected int animSpeed = 8; 

    protected int attackFrameIndex = 0;
    protected int attackAnimTick = 0;
    protected int attackAnimSpeed = 3; 

    // ── States ───────────────────────────────────────────────────────────────
    protected boolean isAttacking = false;
    protected int facing = 1; // 1 = Right, -1 = Left
    protected final Rectangle attackHitbox;
    protected int attackDurationMs = 250; 

    // ── Constructor ──────────────────────────────────────────────────────────

    public Player(int startX, int startY) {
        this.bounds = new Rectangle(startX, startY, width, height);
        this.physics = new PhysicsComponent();
        this.attackHitbox = new Rectangle();
        
        loadSprites();
    }

    protected void loadSprites() {
        try {
            var walkRes = getClass().getResourceAsStream("/sprites/knight_walk.png");
            if (walkRes != null) {
                BufferedImage walkSheet = ImageIO.read(walkRes);
                walkFrames = new BufferedImage[6];
                for (int i = 0; i < 6; i++) {
                    walkFrames[i] = walkSheet.getSubimage(i * width, 0, width, height);
                }
            }

            var attackRes = getClass().getResourceAsStream("/sprites/knight_attack.png");
            if (attackRes != null) {
                BufferedImage attackSheet = ImageIO.read(attackRes);
                attackFrames = new BufferedImage[7];
                for (int i = 0; i < 7; i++) {
                    attackFrames[i] = attackSheet.getSubimage(i * width, 0, width, height);
                }
            }
        } catch (IOException e) {
            System.err.println("[Player] Error loading sprites: " + e.getMessage());
        }
    }

    // ── Core Loop ────────────────────────────────────────────────────────────

    public void update(float dt, List<Rectangle> solidTiles) {
        physics.update(dt, bounds, solidTiles, GameWindow.WIDTH, GameWindow.HEIGHT);

        if (isAttacking) {
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

    public void render(Graphics2D g) {
        // Visual indicator for I-Frames (Blue tint)
        if (invincible) {
            g.setColor(new Color(100, 200, 255, 80));
            g.fillOval(bounds.x - 5, bounds.y - 5, width + 10, height + 10);
        }

        BufferedImage currentFrame = null;
        if (isAttacking && attackFrames != null) {
            currentFrame = attackFrames[attackFrameIndex];
        } else if (walkFrames != null) {
            currentFrame = walkFrames[frameIndex];
        }

        if (currentFrame != null) {
            if (facing == 1) {
                g.drawImage(currentFrame, bounds.x, bounds.y, width, height, null);
            } else {
                g.drawImage(currentFrame, bounds.x + width, bounds.y, -width, height, null);
            }
        }
        
        // Debug Hitbox: Show for Knight, Beastman, and Samurai
        boolean isMelee = (this instanceof Samurai || this instanceof Beastman || 
                          !(this instanceof Archer || this instanceof Mage));
        
        if (isAttacking && isMelee) {
            g.setColor(new Color(255, 0, 0, 100));
            g.fillRect(attackHitbox.x, attackHitbox.y, attackHitbox.width, attackHitbox.height);
        }
    }

    // ── Actions & Systems ────────────────────────────────────────────────────

    public void takeDamage(int amount) {
        // PB-018: Samurai I-Frame check
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
            attackFrameIndex = 0; 
            attackAnimTick = 0;
            updateHitbox();

            new Thread(() -> {
                try {
                    Thread.sleep(attackDurationMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                isAttacking = false;
            }).start();
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

    public void jump() {
        physics.jump();
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public Rectangle getBounds() { return bounds; }
    public int getFacing() { return this.facing; }
    public Rectangle getAttackHitbox() { return attackHitbox; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
}