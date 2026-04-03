package com.oathbound.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.List;

/**
 * PB-008, PB-009, & PB-012 — Player (The Knight)
 * Square Size: 68x68 
 * Walk Sheet: 408x68 (6 frames) | Attack Sheet: 476x68 (7 frames)
 */
public class Player {

    // ── Dimensions ───────────────────────────────────────────────────────────
    private final int width = 68;
    private final int height = 68;

    // ── Physics & Bounds ─────────────────────────────────────────────────────
    private final Rectangle bounds;
    private final PhysicsComponent physics;

    // ── Animation Arrays (PB-009) ────────────────────────────────────────────
    private BufferedImage[] walkFrames;
    private BufferedImage[] attackFrames;
    
    // Walk Animation State
    private int frameIndex = 0;
    private int animTick = 0;
    private final int animSpeed = 8; 

    // Attack Animation State
    private int attackFrameIndex = 0;
    private int attackAnimTick = 0;
    private final int attackAnimSpeed = 3; 

    // ── Combat State (PB-012) ───────────────────────────────────────────────
    private boolean isAttacking = false;
    private int facing = 1; // 1 = Right, -1 = Left
    private final Rectangle attackHitbox;
    private final int attackDurationMs = 250; 

    // ── Constructor ──────────────────────────────────────────────────────────

    public Player(int startX, int startY) {
        // Initialize physics bounds at 68x68
        this.bounds = new Rectangle(startX, startY, width, height);
        this.physics = new PhysicsComponent();
        this.attackHitbox = new Rectangle();
        
        loadSprites();
    }

    /**
     * Slices the 68px sheets. 
     * Walk: 6 frames (408px total)
     * Attack: 7 frames (476px total)
     */
    private void loadSprites() {
        try {
            // 1. Walking Sheet
            var walkRes = getClass().getResourceAsStream("/sprites/knight_walk.png");
            if (walkRes != null) {
                BufferedImage walkSheet = ImageIO.read(walkRes);
                walkFrames = new BufferedImage[6];
                for (int i = 0; i < 6; i++) {
                    walkFrames[i] = walkSheet.getSubimage(i * width, 0, width, height);
                }
            }

            // 2. Attack Sheet
            var attackRes = getClass().getResourceAsStream("/sprites/knight_attack.png");
            if (attackRes != null) {
                BufferedImage attackSheet = ImageIO.read(attackRes);
                attackFrames = new BufferedImage[7];
                for (int i = 0; i < 7; i++) {
                    attackFrames[i] = attackSheet.getSubimage(i * width, 0, width, height);
                }
            }
            
            System.out.println("[PB-009] Knight 68x68 assets loaded successfully.");
        } catch (IOException e) {
            System.err.println("[PB-009] Error loading 68x68 sprites: " + e.getMessage());
        }
    }

    // ── Core Loop ────────────────────────────────────────────────────────────

    public void update(float dt, List<Rectangle> solidTiles) {
        // 1. Update Physics
        physics.update(dt, bounds, solidTiles, GameWindow.WIDTH, GameWindow.HEIGHT);

        // 2. Handle Animation State Machine
        if (isAttacking) {
            updateAttackAnimation();
        } else {
            updateWalkAnimation();
        }
    }

    private void updateWalkAnimation() {
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

    private void updateAttackAnimation() {
        attackAnimTick++;
        if (attackAnimTick >= attackAnimSpeed) {
            attackAnimTick = 0;
            if (attackFrameIndex < 6) { // Progress to final frame (index 6)
                attackFrameIndex++;
            }
        }
        updateHitbox();
    }

    public void render(Graphics2D g) {
        BufferedImage currentFrame = null;

        // Choose visual state
        if (isAttacking && attackFrames != null) {
            currentFrame = attackFrames[attackFrameIndex];
        } else if (walkFrames != null) {
            currentFrame = walkFrames[frameIndex];
        }

        if (currentFrame != null) {
            if (facing == 1) {
                g.drawImage(currentFrame, bounds.x, bounds.y, width, height, null);
            } else {
                // Flip for Left movement
                g.drawImage(currentFrame, bounds.x + width, bounds.y, -width, height, null);
            }
        } else {
            // Fallback Cyan Box
            g.setColor(Color.CYAN);
            g.fillRect(bounds.x, bounds.y, width, height);
        }
        
        // PB-012 Debug: Hitbox visualization
        if (isAttacking) {
            g.setColor(new Color(255, 0, 0, 100));
            g.fillRect(attackHitbox.x, attackHitbox.y, attackHitbox.width, attackHitbox.height);
        }
    }

    // ── Combat & Input ───────────────────────────────────────────────────────

    public void attack() {
        if (!isAttacking) {
            isAttacking = true;
            attackFrameIndex = 0; 
            updateHitbox();

            // Thread-based reset for attack duration
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

    private void updateHitbox() {
        // Adjusting hitbox for 68x68 scale
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
    public Rectangle getAttackHitbox() { return isAttacking ? attackHitbox : null; }
    public int getFacing() {
    return this.facing;
    }
}