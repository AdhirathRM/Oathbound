package com.oathbound.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.TimeUtils;
import java.awt.Rectangle;
import java.util.List;

/**
 * PB-011 & PB-016 — Projectile Class
 * Features Mana Bomb Explosion States and sub-pixel movement!
 */
public class Projectile {

    private final Rectangle bounds;
    private float velocityX;
    private float velocityY;
    private boolean active = true;

    private boolean isAOE = false;
    private final int explosionRadius = 130;
    
    // Explosion State Variables
    private boolean exploding = false;
    private float explosionTimer = 0f;
    private final float EXPLOSION_DURATION = 0.25f; // Blast lasts 1/4th of a second
    private boolean damageApplied = false;

    // High FPS Sub-pixel movement remainders
    private float remainderX = 0f;
    private float remainderY = 0f;

    public Projectile(int x, int y, float velX, float velY) {
        this(x, y, velX, velY, false);
    }

    public Projectile(int x, int y, float velX, float velY, boolean isAOE) {
        this.isAOE = isAOE;
        int w = isAOE ? 30 : 18;
        int h = isAOE ? 30 : 6; 
        
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
                    deactivate(); // Arrows just break
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
            
        } else if (isAOE) {
            // Flying Mana Bomb Visual
            float r = bounds.width / 2f;

            // 1. Outer Glow
            sr.setColor(0f, 80/255f, 1f, 120/255f); 
            sr.circle(cx, cy, r + 6);

            // 2. Main Body
            sr.setColor(0f, 191/255f, 1f, 1f); 
            sr.circle(cx, cy, r);

            // 3. Pulsing Core
            long time = TimeUtils.millis();
            float pulse = MathUtils.sin(time / 100.0f) * 4f; 
            sr.setColor(220/255f, 1f, 1f, 1f); 
            sr.circle(cx, cy, 7 + (pulse/2f));
            
        } else {
            // Archer Arrow Visual
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