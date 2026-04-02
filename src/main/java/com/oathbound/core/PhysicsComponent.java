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
        // ── 1. Apply gravity ──────────────────────────────────────────────────
        velocityY += GRAVITY * dt;
        if (velocityY > MAX_FALL_SPEED) velocityY = MAX_FALL_SPEED;

        // ── 2. Move vertically, then resolve vertical collisions ──────────────
        onGround = false;
        bounds.y += (int) (velocityY * dt);

        for (Rectangle tile : solidTiles) {
            if (!bounds.intersects(tile)) continue;

            if (velocityY > 0) {
                // Falling — land on top of tile
                bounds.y = tile.y - bounds.height;
                onGround = true;
            } else {
                // Rising — hit the underside of a tile
                bounds.y = tile.y + tile.height;
            }
            velocityY = 0f;
            break; // one vertical collision per frame is sufficient
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