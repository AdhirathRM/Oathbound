package com.oathbound.core;

import java.awt.Rectangle;
import java.util.List;

/**
 * PB-002, PB-003, PB-007 — Advanced Physics Component
 * Now includes Coyote Time and Jump Buffering for "Pro" game feel.
 * Updated for variable frame-rate support (Sub-pixel movement).
 */
public class PhysicsComponent {

    // ── Constants ─────────────────────────────────────────────────────────────
    public static final float GRAVITY         = 1400f;
    public static final float MAX_FALL_SPEED  = 900f;
    public static final float JUMP_VELOCITY   = -750f;

    // PB-007 Juice Constants (in milliseconds)
    private static final long COYOTE_TIME_MS   = 150; 
    private static final long JUMP_BUFFER_MS   = 150; 

    // ── State ─────────────────────────────────────────────────────────────────
    public float velocityX = 0f;
    public float velocityY = 0f;
    private boolean onGround = false;

    // High FPS Sub-pixel movement remainders
    private float remainderX = 0f;
    private float remainderY = 0f;

    // PB-007 Timers
    private long lastTimeOnGround = 0;
    private long lastJumpPressTime = 0;

    public void update(float dt, Rectangle bounds, List<Rectangle> solidTiles, int screenW, int screenH) {

        // 1. Apply gravity
        velocityY += GRAVITY * dt;
        if (velocityY > MAX_FALL_SPEED) velocityY = MAX_FALL_SPEED;

        // 2. Ground Probe (Existing logic)
        onGround = false;
        Rectangle groundProbe = new Rectangle(bounds.x + 2, bounds.y + bounds.height, bounds.width - 4, 2);
        for (Rectangle tile : solidTiles) {
            if (groundProbe.intersects(tile)) {
                onGround = true;
                lastTimeOnGround = System.currentTimeMillis(); // Reset Coyote Timer
                break;
            }
        }

        // 3. Move Vertically & Resolve Collisions (with sub-pixel fix)
        remainderY += (velocityY * dt);
        int moveY = (int) remainderY;
        remainderY -= moveY;
        bounds.y += moveY;
        resolveVerticalCollisions(bounds, solidTiles);

        // 4. Move Horizontally & Resolve Collisions (with sub-pixel fix)
        remainderX += (velocityX * dt);
        int moveX = (int) remainderX;
        remainderX -= moveX;
        bounds.x += moveX;
        resolveHorizontalCollisions(bounds, solidTiles);

        // 5. Screen Clamping
        clampToScreen(bounds, screenW, screenH);

        // ── PB-007: THE JUICE LOGIC ──
        checkBufferedJump();
    }

    private void checkBufferedJump() {
        long now = System.currentTimeMillis();
        
        // Is the player within the Coyote Time window?
        boolean canCoyoteJump = (now - lastTimeOnGround <= COYOTE_TIME_MS);
        
        // Did the player press jump recently?
        boolean jumpBuffered = (now - lastJumpPressTime <= JUMP_BUFFER_MS);

        if (jumpBuffered && canCoyoteJump) {
            velocityY = JUMP_VELOCITY;
            remainderY = 0f; // Clear remainder to prevent jumping weirdness
            onGround = false;
            
            // CRITICAL: Clear timers so we don't double-jump
            lastJumpPressTime = 0; 
            lastTimeOnGround = 0; 
        }
    }

    public boolean jump() {
        // Instead of jumping instantly, we "buffer" the intent
        lastJumpPressTime = System.currentTimeMillis();
        return true; 
    }

    // ── Internal Helpers ───────────────────────────────────────────────────────

    private void resolveVerticalCollisions(Rectangle bounds, List<Rectangle> solidTiles) {
        Rectangle bestTile = null;
        int bestOverlap = 0;
        for (Rectangle tile : solidTiles) {
            if (!bounds.intersects(tile)) continue;
            int overlap = Math.min(bounds.y + bounds.height, tile.y + tile.height) - Math.max(bounds.y, tile.y);
            if (overlap > bestOverlap) { bestOverlap = overlap; bestTile = tile; }
        }
        if (bestTile != null) {
            if (velocityY >= 0) {
                bounds.y = bestTile.y - bounds.height;
                onGround = true;
                lastTimeOnGround = System.currentTimeMillis();
            } else {
                bounds.y = bestTile.y + bestTile.height;
            }
            velocityY = 0f;
            remainderY = 0f; // Clear remainder on collision
        }
    }

    private void resolveHorizontalCollisions(Rectangle bounds, List<Rectangle> solidTiles) {
        for (Rectangle tile : solidTiles) {
            if (!bounds.intersects(tile)) continue;
            if (velocityX > 0) bounds.x = tile.x - bounds.width;
            else if (velocityX < 0) bounds.x = tile.x + tile.width;
            
            velocityX = 0f;
            remainderX = 0f; // Clear remainder on collision
            break;
        }
    }

    private void clampToScreen(Rectangle bounds, int screenW, int screenH) {
        if (bounds.x < 0) bounds.x = 0;
        if (bounds.x + bounds.width > screenW) bounds.x = screenW - bounds.width;
        if (bounds.y < 0) { bounds.y = 0; velocityY = 0; remainderY = 0f; }
        // Note: We don't clamp the bottom so PB-021 (Pit Detection) works!
    }

    public boolean isOnGround() { return onGround; }
}