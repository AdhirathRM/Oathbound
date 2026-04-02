package com.oathbound.core;

import java.awt.Rectangle;
import java.util.List;

/**
 * PB-002 — Gravity System
 *
 * A self-contained physics component that can be attached to any entity
 * (Player, Enemy, etc.) that needs gravity and vertical collision.
 *
 * Design — composition over inheritance:
 *   Each entity owns one PhysicsComponent. This keeps physics logic
 *   in one place and out of the class hierarchy, making PB-008 (abstract
 *   Player) cleaner when that sprint arrives.
 *
 * Acceptance criteria (PB-002):
 *   ✔ Entity accelerates downward each frame (gravity applied to velocityY)
 *   ✔ Landing on a solid tile stops falling (velocityY zeroed, onGround = true)
 *   ✔ Terminal velocity capped (MAX_FALL_SPEED)
 *
 * PB-003 hook:
 *   jump() is defined here so PB-003 can call it without coupling to a
 *   specific entity class. The jump method checks onGround before applying
 *   upward velocity, giving the correct arc when combined with gravity.
 */
public class PhysicsComponent {

    // ── Physics Constants ─────────────────────────────────────────────────────

    /** Downward acceleration in pixels per second². Tweak for game feel. */
    public static final float GRAVITY         = 1400f;

    /** Maximum downward speed in pixels per second (terminal velocity). */
    public static final float MAX_FALL_SPEED  = 900f;

    /** Upward velocity applied on jump (PB-003). Negative = up in screen space. */
    public static final float JUMP_VELOCITY   = -620f;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Current horizontal velocity in px/s. Set externally by movement code. */
    public float velocityX = 0f;

    /** Current vertical velocity in px/s. Negative = moving up. */
    public float velocityY = 0f;

    /** True when the entity is resting on a solid surface this frame. */
    private boolean onGround = false;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Applies gravity, moves the entity bounds, then resolves collisions
     * against the solid tile list produced by {@link TileMapLoader}.
     *
     * Call order inside GamePanel.updatePlay():
     *   1. physics.update(dt, bounds, solidTiles)
     *   2. Use bounds.x / bounds.y as the entity's new world position.
     *
     * @param dt         delta-time in seconds (from the game loop)
     * @param bounds     the entity's current world-space rectangle;
     *                   x and y are mutated in place
     * @param solidTiles collision rectangles from TileMapLoader
     */
    public void update(float dt, Rectangle bounds, List<Rectangle> solidTiles) {
        update(dt, bounds, solidTiles, 1280, 720);
    }

    /**
     * Overload that also clamps the entity to the screen boundaries.
     *
     * @param screenW  screen width  in pixels (GameWindow.WIDTH)
     * @param screenH  screen height in pixels (GameWindow.HEIGHT)
     */
    public void update(float dt, Rectangle bounds, List<Rectangle> solidTiles,
                       int screenW, int screenH) {

        // ── 1. Apply gravity ──────────────────────────────────────────────────
        velocityY += GRAVITY * dt;
        if (velocityY > MAX_FALL_SPEED) velocityY = MAX_FALL_SPEED;

        // ── 2. Move vertically, then resolve vertical collisions ──────────────
        //
        // FIX (intermittent jump): We snap the player DOWN by 1 pixel before
        // checking so that floating-point rounding can't leave the player 1 px
        // above the tile surface where onGround would never be set.
        // We then probe for ground contact BEFORE moving so that landing is
        // detected on the very frame the player comes to rest.
        onGround = false;

        // Ground-probe: peek 2px below current feet — if a tile is right there,
        // we are standing on it regardless of velocityY (handles the case where
        // the player is stationary and gravity hasn't produced a positive dy yet
        // because last frame's collision zeroed it).
        Rectangle groundProbe = new Rectangle(bounds.x + 2, bounds.y + bounds.height,
                                               bounds.width - 4, 2);
        for (Rectangle tile : solidTiles) {
            if (groundProbe.intersects(tile)) {
                onGround = true;
                break;
            }
        }

        bounds.y += (int) (velocityY * dt);

        // Find the closest tile collision in the vertical axis (most overlap)
        // so that corner-grazing a seam doesn't cause random non-collisions.
        int     bestOverlap = 0;
        Rectangle bestTile  = null;
        for (Rectangle tile : solidTiles) {
            if (!bounds.intersects(tile)) continue;
            int overlap = Math.min(bounds.y + bounds.height, tile.y + tile.height)
                        - Math.max(bounds.y, tile.y);
            if (overlap > bestOverlap) { bestOverlap = overlap; bestTile = tile; }
        }
        if (bestTile != null) {
            if (velocityY >= 0) {
                // Falling — land on top of tile
                bounds.y = bestTile.y - bounds.height;
                onGround  = true;
            } else {
                // Rising — hit the underside of a tile
                bounds.y = bestTile.y + bestTile.height;
            }
            velocityY = 0f;
        }

        // ── 3. Move horizontally, then resolve horizontal collisions ──────────
        bounds.x += (int) (velocityX * dt);

        for (Rectangle tile : solidTiles) {
            if (!bounds.intersects(tile)) continue;

            if (velocityX > 0) {
                // Moving right — push left of tile
                bounds.x = tile.x - bounds.width;
            } else if (velocityX < 0) {
                // Moving left — push right of tile
                bounds.x = tile.x + tile.width;
            }
            velocityX = 0f;
            break;
        }

        // ── 4. Screen-boundary clamping ───────────────────────────────────────
        // FIX (off-screen): clamp X so the player can't walk off either edge.
        if (bounds.x < 0) {
            bounds.x  = 0;
            velocityX = 0f;
        }
        if (bounds.x + bounds.width > screenW) {
            bounds.x  = screenW - bounds.width;
            velocityX = 0f;
        }
        // Clamp Y: prevent falling off the bottom of the screen.
        if (bounds.y + bounds.height > screenH) {
            bounds.y  = screenH - bounds.height;
            velocityY = 0f;
            onGround  = true;
        }
        // Prevent going above the top of the screen.
        if (bounds.y < 0) {
            bounds.y  = 0;
            velocityY = 0f;
        }
    }

    /**
     * PB-003 hook — applies an upward impulse if the entity is on the ground.
     * Returns true if the jump was actually performed (so callers can trigger
     * a jump animation/SFX later).
     *
     * @return true if jump was applied, false if the entity was airborne
     */
    public boolean jump() {
        if (onGround) {
            velocityY = JUMP_VELOCITY;
            onGround  = false;
            return true;
        }
        return false;
    }

    /** @return true if the entity is resting on solid ground this frame. */
    public boolean isOnGround() { return onGround; }
}