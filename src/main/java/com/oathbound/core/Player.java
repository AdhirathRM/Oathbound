package com.oathbound.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.List;

/**
 * PB-008, PB-009 — Base Player Class
 * Optimized for Inheritance (PB-010) and Animation Priority.
 */
public class Player {
    // Add to Player.java fields
    public int maxHealth = 3;
    public int currentHealth = 3;

    // ── Dimensions ───────────────────────────────────────────────────────────
    protected final int width = 68;
    protected final int height = 68;

    // ── Physics & Bounds ─────────────────────────────────────────────────────
    protected final Rectangle bounds;
    protected final PhysicsComponent physics;

    // ── Animation Arrays ─────────────────────────────────────────────────────
    protected BufferedImage[] walkFrames;
    protected BufferedImage[] attackFrames;
    
    protected int frameIndex = 0;
    protected int animTick = 0;
    protected final int animSpeed = 8; 

    protected int attackFrameIndex = 0;
    protected int attackAnimTick = 0;
    protected final int attackAnimSpeed = 3; 

    // ── States ───────────────────────────────────────────────────────────────
    protected boolean isAttacking = false;
    protected int facing = 1; // 1 = Right, -1 = Left
    protected final Rectangle attackHitbox;
    protected int attackDurationMs = 250; // Default for Knight; Archer overrides this

    // ── Constructor ──────────────────────────────────────────────────────────

    public Player(int startX, int startY) {
        this.bounds = new Rectangle(startX, startY, width, height);
        this.physics = new PhysicsComponent();
        this.attackHitbox = new Rectangle();
        
        loadSprites();
    }

    /**
     * Default sprite loader (Knight). 
     * Archer class overrides this to load its own 68x68 sheets.
     */
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
        // 1. Always update physics (allows jumping/moving while attacking)
        physics.update(dt, bounds, solidTiles, GameWindow.WIDTH, GameWindow.HEIGHT);

        // 2. Animation State Priority
        if (isAttacking) {
            updateAttackAnimation();
        } else {
            updateWalkAnimation();
        }
    }

    protected void updateWalkAnimation() {
        // Reset attack frames so they start fresh next time
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
        // Increment animation ticks
        attackAnimTick++;
        if (attackAnimTick >= attackAnimSpeed) {
            attackAnimTick = 0;
            // Advance frame until the end of the sheet (index 6)
            if (attackFrameIndex < 6) {
                attackFrameIndex++;
            }
        }
        
        // Update the collision hitbox (only relevant for Knight)
        updateHitbox();
    }

    public void render(Graphics2D g) {
        BufferedImage currentFrame = null;

        // Determine which frame to draw
        if (isAttacking && attackFrames != null) {
            currentFrame = attackFrames[attackFrameIndex];
        } else if (walkFrames != null) {
            currentFrame = walkFrames[frameIndex];
        }

        if (currentFrame != null) {
            if (facing == 1) {
                g.drawImage(currentFrame, bounds.x, bounds.y, width, height, null);
            } else {
                // Flip horizontally for Left
                g.drawImage(currentFrame, bounds.x + width, bounds.y, -width, height, null);
            }
        }
        
        // PB-012 Debug: Show melee hitbox (Hidden for Archer)
        if (isAttacking && !(this instanceof Archer)) {
            g.setColor(new Color(255, 0, 0, 100));
            g.fillRect(attackHitbox.x, attackHitbox.y, attackHitbox.width, attackHitbox.height);
        }
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    public void attack() {
        if (!isAttacking) {
            isAttacking = true;
            attackFrameIndex = 0;
            attackAnimTick = 0;
            updateHitbox();

            // Handle state reset via thread
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

    //dynamic melee attack hitbox and visualization added for PB-012
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
    
    // Add this missing method right here:
    public Rectangle getAttackHitbox() { 
        return isAttacking ? attackHitbox : null; 
    }
}