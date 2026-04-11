package com.oathbound.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.TimeUtils;
import java.awt.Rectangle;
import java.util.List;

/**
 * PB-011 & PB-016 — Projectile Class
 * Features Mana Bomb Explosion States, Sub-pixel movement, and Boss Orbs!
 */
public class Projectile {

    private final Rectangle bounds;
    private float velocityX;
    private float velocityY;
    private boolean active = true;

    private boolean isAOE = false;
    private final int explosionRadius = 130;
    
    // Special string identifier for Boss projectile rendering
    private String specialType = "";
    
    // Explosion State Variables
    private boolean exploding = false;
    private float explosionTimer = 0f;
    private final float EXPLOSION_DURATION = 0.25f; // Blast lasts 1/4th of a second
    private boolean damageApplied = false;

    // High FPS Sub-pixel movement remainders
    private float remainderX = 0f;
    private float remainderY = 0f;

    // 1. Standard Arrow Constructor
    public Projectile(int x, int y, float velX, float velY) {
        this(x, y, velX, velY, false);
    }

    // 2. AOE Mana Bomb Constructor
    public Projectile(int x, int y, float velX, float velY, boolean isAOE) {
        this.isAOE = isAOE;
        int w = isAOE ? 30 : 18;
        int h = isAOE ? 30 : 6; 
        
        this.bounds = new Rectangle(x, y, w, h);
        this.velocityX = velX;
        this.velocityY = velY;
    }

    // 3. NEW: Boss Special Projectile Constructor
    public Projectile(int x, int y, float velX, float velY, String specialType) {
        this.specialType = specialType;
        
        int w = 24; 
        int h = 24;
        if (specialType.equals("BAT")) {
            w = 16; h = 12; // Smaller hitbox for bats
        }
        
        this.bounds = new Rectangle(x, y, w, h);
        this.velocityX = velX;
        this.velocityY = velY;
    }

    public void update(float dt, List<Rectangle> solidTiles) {
        if (!active) return;

        // If it's exploding, stop moving and process the blast timer
        if (exploding) {
            explosionTimer += dt;
            if (explosionTimer >= EXPLOSION_DURATION) {
                deactivate();
            }
            return; 
        }

        // Apply sub-pixel movement
        remainderX += (velocityX * dt);
        int moveX = (int) remainderX;
        remainderX -= moveX;
        bounds.x += moveX;

        remainderY += (velocityY * dt);
        int moveY = (int) remainderY;
        remainderY -= moveY;
        bounds.y += moveY;

        // Check for wall collisions
        for (Rectangle tile : solidTiles) {
            if (bounds.intersects(tile)) {
                if (isAOE) {
                    triggerExplosion(); // Mana bombs explode on walls!
                } else {
                    deactivate(); // Arrows and Boss magic just break
                }
                break;
            }
        }

        // Despawn if it flies completely off screen
        if (bounds.x < -100 || bounds.x > 1380 || 
            bounds.y < -100 || bounds.y > 836) {
            active = false;
        }
    }

    public void render(ShapeRenderer sr) {
        if (!active) return;
        
        float cx = bounds.x + bounds.width / 2f;
        float cy = bounds.y + bounds.height / 2f;
        long time = TimeUtils.millis();

        if (exploding) {
            // Calculate blast expansion and fade-out
            float progress = explosionTimer / EXPLOSION_DURATION;
            float currentRadius = explosionRadius * (0.2f + 0.8f * progress); // Expand rapidly
            float alpha = 1f - progress; // Fade to invisible

            // Outer blast wave
            sr.setColor(0f, 191/255f, 1f, alpha);
            sr.circle(cx, cy, currentRadius);

            // Bright inner core
            sr.setColor(1f, 1f, 1f, alpha);
            sr.circle(cx, cy, currentRadius * 0.6f);
            
        } else if (specialType.equals("RED_ORB")) {
            // --- BOSS PHASE 1: FLAMING RED ORB ---
            float r = bounds.width / 2f;
            
            // 1. Outer fire glow (flickering)
            float flicker = MathUtils.sin(time / 50.0f) * 3f;
            sr.setColor(1f, 0.2f, 0f, 0.5f); 
            sr.circle(cx, cy, r + 4 + flicker);

            // 2. Main dark red body
            sr.setColor(0.8f, 0f, 0f, 1f); 
            sr.circle(cx, cy, r);

            // 3. Bright orange/yellow pulsing core
            float pulse = MathUtils.sin(time / 80.0f) * 2f; 
            sr.setColor(1f, 0.8f, 0f, 1f); 
            sr.circle(cx, cy, r / 2f + pulse);
            
            // 4. Trailing flame particle effect based on velocity
            sr.setColor(1f, 0.4f, 0f, 0.7f);
            float trailOffset = (velocityX > 0) ? -r : r;
            sr.circle(cx + trailOffset, cy, r * 0.7f);
            sr.circle(cx + trailOffset * 1.8f, cy, r * 0.4f);

        } else if (specialType.equals("BAT")) {
            // --- BOSS PHASE 2: BAT SWARM ---
            sr.setColor(0.2f, 0f, 0.3f, 1f); // Dark purple
            sr.rect(bounds.x, bounds.y, bounds.width, bounds.height);
            
            // Flapping wings animation
            float flapY = MathUtils.sin(time / 30.0f) * 8f;
            sr.triangle(bounds.x + bounds.width/2, bounds.y + bounds.height/2, 
                        bounds.x - 10, bounds.y + bounds.height/2 + flapY, 
                        bounds.x + bounds.width/2, bounds.y + bounds.height + 5);
            sr.triangle(bounds.x + bounds.width/2, bounds.y + bounds.height/2, 
                        bounds.x + bounds.width + 10, bounds.y + bounds.height/2 + flapY, 
                        bounds.x + bounds.width/2, bounds.y + bounds.height + 5);
            
        } else if (isAOE) {
            // --- MAGE: FLYING MANA BOMB ---
            float r = bounds.width / 2f;
            sr.setColor(0f, 80/255f, 1f, 120/255f); 
            sr.circle(cx, cy, r + 6);
            sr.setColor(0f, 191/255f, 1f, 1f); 
            sr.circle(cx, cy, r);
            float pulse = MathUtils.sin(time / 100.0f) * 4f; 
            sr.setColor(220/255f, 1f, 1f, 1f); 
            sr.circle(cx, cy, 7 + (pulse/2f));
            
        } else {
            // --- ARCHER: ARROW ---
            sr.setColor(139/255f, 69/255f, 19/255f, 1f); 
            sr.rect(bounds.x, bounds.y, bounds.width, bounds.height);
            sr.setColor(Color.LIGHT_GRAY);
            if (velocityX > 0) {
                sr.rect(bounds.x + bounds.width - 4, bounds.y - 2, 4, bounds.height + 4);
            } else {
                sr.rect(bounds.x, bounds.y - 2, 4, bounds.height + 4);
            }
        }
    }
    
    public void triggerExplosion() {
        if (!exploding) {
            exploding = true;
            velocityX = 0f;
            velocityY = 0f;
        }
    }

    public void deactivate() { this.active = false; }
    public boolean isActive() { return active; }
    public Rectangle getBounds() { return bounds; }
    
    public boolean isAOE() { return isAOE; }
    public int getExplosionRadius() { return explosionRadius; }
    
    public boolean isExploding() { return exploding; }
    public boolean isDamageApplied() { return damageApplied; }
    public void setDamageApplied(boolean applied) { this.damageApplied = applied; }
}