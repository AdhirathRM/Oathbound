package com.oathbound.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.TimeUtils;
import java.awt.Rectangle;
import java.util.List;

/**
 * PB-011 & PB-016 — Projectile Class
 * Ported to LibGDX.
 */
public class Projectile {

    private final Rectangle bounds;
    private float velocityX;
    private float velocityY;
    private boolean active = true;

    private boolean isAOE = false;
    private final int explosionRadius = 130;

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

        bounds.x += (int) (velocityX * dt);
        bounds.y += (int) (velocityY * dt);

        for (Rectangle tile : solidTiles) {
            if (bounds.intersects(tile)) {
                deactivate();
                break;
            }
        }

        if (bounds.x < -100 || bounds.x > 1380 || 
            bounds.y < -100 || bounds.y > 836) {
            active = false;
        }
    }

    public void render(ShapeRenderer sr) {
        if (!active) return;
        
        if (isAOE) {
            float cx = bounds.x + bounds.width / 2f;
            float cy = bounds.y + bounds.height / 2f;
            float r = bounds.width / 2f;

            // 1. Outer Glow (Deep Blue Aura)
            sr.setColor(0f, 80/255f, 1f, 120/255f); 
            sr.circle(cx, cy, r + 6);

            // 2. Main Body (Electric Blue)
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

    public void deactivate() { this.active = false; }
    public boolean isActive() { return active; }
    public Rectangle getBounds() { return bounds; }
    public boolean isAOE() { return isAOE; }
    public int getExplosionRadius() { return explosionRadius; }
}