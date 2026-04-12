package com.oathbound.core;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.TimeUtils;
import java.util.List;

/**
 * PB-002, PB-003, PB-007 — Advanced Physics Component
 * Now includes Coyote Time and Jump Buffering for "Pro" game feel.
 * Updated for variable frame-rate support (Sub-pixel movement).
 */
public class PhysicsComponent {

    public static final float GRAVITY         = 1400f;
    public static final float MAX_FALL_SPEED  = 900f;
    public static final float JUMP_VELOCITY   = -750f;

    private static final long COYOTE_TIME_MS   = 150; 
    private static final long JUMP_BUFFER_MS   = 150; 

    public float velocityX = 0f;
    public float velocityY = 0f;
    private boolean onGround = false;

    private float remainderX = 0f;
    private float remainderY = 0f;

    private long lastTimeOnGround = 0;
    private long lastJumpPressTime = 0;

    public void update(float dt, Rectangle bounds, List<Rectangle> solidTiles, int screenW, int screenH) {

        velocityY += GRAVITY * dt;
        if (velocityY > MAX_FALL_SPEED) velocityY = MAX_FALL_SPEED;

        onGround = false;
        Rectangle groundProbe = new Rectangle(bounds.x + 2, bounds.y + bounds.height, bounds.width - 4, 2);
        for (Rectangle tile : solidTiles) {
            if (groundProbe.overlaps(tile)) {
                onGround = true;
                lastTimeOnGround = TimeUtils.millis();
                break;
            }
        }

        remainderY += (velocityY * dt);
        int moveY = (int) remainderY;
        remainderY -= moveY;
        bounds.y += moveY;
        resolveVerticalCollisions(bounds, solidTiles);

        remainderX += (velocityX * dt);
        int moveX = (int) remainderX;
        remainderX -= moveX;
        bounds.x += moveX;
        resolveHorizontalCollisions(bounds, solidTiles);

        clampToScreen(bounds, screenW, screenH);

        checkBufferedJump();
    }

    private void checkBufferedJump() {
        long now = TimeUtils.millis();
        
        boolean canCoyoteJump = (now - lastTimeOnGround <= COYOTE_TIME_MS);
        boolean jumpBuffered = (now - lastJumpPressTime <= JUMP_BUFFER_MS);

        if (jumpBuffered && canCoyoteJump) {
            velocityY = JUMP_VELOCITY;
            remainderY = 0f;
            onGround = false;
            lastJumpPressTime = 0; 
            lastTimeOnGround = 0; 
        }
    }

    public boolean jump() {
        lastJumpPressTime = TimeUtils.millis();
        return true; 
    }

    private void resolveVerticalCollisions(Rectangle bounds, List<Rectangle> solidTiles) {
        Rectangle bestTile = null;
        int bestOverlap = 0;
        for (Rectangle tile : solidTiles) {
            if (!bounds.overlaps(tile)) continue;
            int overlap = (int)(Math.min(bounds.y + bounds.height, tile.y + tile.height) - Math.max(bounds.y, tile.y));
            if (overlap > bestOverlap) { bestOverlap = overlap; bestTile = tile; }
        }
        if (bestTile != null) {
            if (velocityY >= 0) {
                bounds.y = bestTile.y - bounds.height;
                onGround = true;
                lastTimeOnGround = TimeUtils.millis();
            } else {
                bounds.y = bestTile.y + bestTile.height;
            }
            velocityY = 0f;
            remainderY = 0f;
        }
    }

    private void resolveHorizontalCollisions(Rectangle bounds, List<Rectangle> solidTiles) {
        for (Rectangle tile : solidTiles) {
            if (!bounds.overlaps(tile)) continue;
            if (velocityX > 0) bounds.x = tile.x - bounds.width;
            else if (velocityX < 0) bounds.x = tile.x + tile.width;
            velocityX = 0f;
            remainderX = 0f;
            break;
        }
    }

    private void clampToScreen(Rectangle bounds, int screenW, int screenH) {
        if (bounds.x < 0) bounds.x = 0;
        if (bounds.x + bounds.width > screenW) bounds.x = screenW - bounds.width;
        if (bounds.y < 0) { bounds.y = 0; velocityY = 0; remainderY = 0f; }
    }

    public boolean isOnGround() { return onGround; }
}