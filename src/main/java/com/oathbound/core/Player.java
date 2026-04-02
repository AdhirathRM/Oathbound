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
 * Handles Physics, 56x56 Sprite Animations (Walk/Attack), and Hitboxes.
 */
public class Player {

    // ── Dimensions ───────────────────────────────────────────────────────────
    private final int width = 56;
    private final int height = 56;

    // ── Physics & Bounds ─────────────────────────────────────────────────────
    private final Rectangle bounds;
    private final PhysicsComponent physics;

    // ── Animation Arrays (PB-009) ────────────────────────────────────────────
    private BufferedImage[] walkFrames;
    private BufferedImage[] attackFrames;
    
    // Animation State Tracking
    private int frameIndex = 0;
    private int animTick = 0;
    private final int animSpeed = 8; 

    private int attackFrameIndex = 0;
    private int attackAnimTick = 0;
    private final int attackAnimSpeed = 3; // Attack frames flip faster

    // ── Combat State (PB-012) ───────────────────────────────────────────────
    private boolean isAttacking = false;
    private int facing = 1; // 1 = Right, -1 = Left
    private final Rectangle attackHitbox;
    private final int attackDurationMs = 250; // Increased to fit 7 frames

    // ── Constructor ──────────────────────────────────────────────────────────

    public Player(int startX, int startY) {
        this.bounds = new Rectangle(startX, startY, width, height);
        this.physics = new PhysicsComponent();
        this.attackHitbox = new Rectangle();
        
        loadSprites();
    }

    /**
     * PB-009: Slices both the Walking (6 frames) and Attacking (7 frames) sheets.
     */
    private void loadSprites() {
        try {
            // 1. Load Walking Sheet (336x56)
            var walkRes = getClass().getResourceAsStream("/sprites/knight_walk.png");
            if (walkRes != null) {
                BufferedImage walkSheet = ImageIO.read(walkRes);
                walkFrames = new BufferedImage[6];
                for (int i = 0; i < 6; i++) {
                    walkFrames[i] = walkSheet.getSubimage(i * width, 0, width, height);
                }
            }

            // 2. Load Attack Sheet (392x56)
            var attackRes = getClass().getResourceAsStream("/sprites/knight_attack.png");
            if (attackRes != null) {
                BufferedImage attackSheet = ImageIO.read(attackRes);
                attackFrames = new BufferedImage[7];
                for (int i = 0; i < 7; i++) {
                    attackFrames[i] = attackSheet.getSubimage(i * width, 0, width, height);
                }
            }
            
            System.out.println("[PB-009] All Knight sprites loaded and sliced.");
        } catch (IOException e) {
            System.err.println("[PB-009] Error loading sprites: " + e.getMessage());
        }
    }

    // ── Core Loop ────────────────────────────────────────────────────────────

    public void update(float dt, List<Rectangle> solidTiles) {
        // Update Physics
        physics.update(dt, bounds, solidTiles, GameWindow.WIDTH, GameWindow.HEIGHT);

        if (isAttacking) {
            // Attack Animation Priority
            attackAnimTick++;
            if (attackAnimTick >= attackAnimSpeed) {
                attackAnimTick = 0;
                if (attackFrameIndex < 6) { // Progress through 7 frames (0 to 6)
                    attackFrameIndex++;
                }
            }
            updateHitbox();
        } else {
            // Walking Animation Logic
            attackFrameIndex = 0; // Reset attack counters
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
    }

    public void render(Graphics2D g) {
        BufferedImage currentFrame = null;

        // Choose frame based on state
        if (isAttacking && attackFrames != null) {
            currentFrame = attackFrames[attackFrameIndex];
        } else if (walkFrames != null) {
            currentFrame = walkFrames[frameIndex];
        }

        if (currentFrame != null) {
            if (facing == 1) {
                g.drawImage(currentFrame, bounds.x, bounds.y, width, height, null);
            } else {
                // Flip horizontally
                g.drawImage(currentFrame, bounds.x + width, bounds.y, -width, height, null);
            }
        } else {
            // Fallback Placeholder
            g.setColor(Color.CYAN);
            g.fillRect(bounds.x, bounds.y, width, height);
        }
        
        // PB-012 Debug: You can comment this out once satisfied
        if (isAttacking) {
            g.setColor(new Color(255, 0, 0, 100));
            g.fillRect(attackHitbox.x, attackHitbox.y, attackHitbox.width, attackHitbox.height);
        }
    }

    // ── Combat & Input ───────────────────────────────────────────────────────

    public void attack() {
        if (!isAttacking) {
            isAttacking = true;
            attackFrameIndex = 0; // Start at first frame of animation
            updateHitbox();

            // End attack after a delay
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
        int hbW = 50; 
        int hbH = 40;
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
}