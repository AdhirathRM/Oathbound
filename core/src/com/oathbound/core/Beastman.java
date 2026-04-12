package com.oathbound.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import java.util.List;

public class Beastman extends Player {

    private final float customAttackDurationSec = 0.40f; 
    private Texture clawTexture; 

    public Beastman(int x, int y) {
        super(x, y);
        loadSprites();
    }

    @Override
    protected void loadSprites() {
        if (Gdx.files.internal("sprites/beastman_walk.png").exists()) {
            walkSheet = new Texture(Gdx.files.internal("sprites/beastman_walk.png"));
            walkFrames = new TextureRegion[6];
            for (int i = 0; i < 6; i++) {
                walkFrames[i] = new TextureRegion(walkSheet, i * width, 0, width, height);
                walkFrames[i].flip(false, true);
            }
        }
        if (Gdx.files.internal("sprites/beastman_attack.png").exists()) {
            attackSheet = new Texture(Gdx.files.internal("sprites/beastman_attack.png"));
            attackFrames = new TextureRegion[7];
            for (int i = 0; i < 7; i++) {
                attackFrames[i] = new TextureRegion(attackSheet, i * width, 0, width, height);
                attackFrames[i].flip(false, true);
            }
        }

        Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        for (int i = 0; i < 3; i++) {
            int offsetX = 16 + (i * 16);
            int startY = (i == 1) ? 2 : 12;  
            int endY = (i == 1) ? 62 : 52;   
            int thickY = startY + 15;        
            pixmap.fillTriangle(offsetX, startY, offsetX - 4, thickY, offsetX + 4, thickY);
            pixmap.fillTriangle(offsetX - 4, thickY, offsetX + 4, thickY, offsetX, endY);
        }
        clawTexture = new Texture(pixmap);
        pixmap.dispose();
    }
    
    @Override
    public void update(float dt, List<Rectangle> solidTiles) {
        physics.update(dt, bounds, solidTiles, 1280, 736);

        if (isAttacking) {
            attackTimer += dt;
            if (attackTimer >= customAttackDurationSec) {
                isAttacking = false;
                attackHitbox.set(0, 0, 0, 0); 
            } else {
                updateAttackAnimation(dt); 
            }
        } else {
            updateWalkAnimation(dt); 
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        super.render(batch); 

        if (isAttacking && attackHitbox.width > 0 && clawTexture != null) {
            float progress = attackTimer / customAttackDurationSec;
            float easeProgress = (float)(1.0 - Math.pow(1.0 - progress, 3));
            float alpha = 1f - progress; 
            batch.setColor(0.1f, 0.8f, 1.0f, alpha); 

            int clawW = 48;
            int clawH = 64;
            float currentX = attackHitbox.x + (attackHitbox.width - clawW) / 2f;
            float currentY = attackHitbox.y + (attackHitbox.height - clawH) / 2f;
            float yOffset = 20 * (1f - easeProgress);

            TextureRegion clawRegion = new TextureRegion(clawTexture);
            if (facing == -1) clawRegion.flip(true, false);
            float rotation = (facing == 1) ? -25f : 25f;

            batch.draw(clawRegion,
                       currentX, currentY + yOffset - 10,
                       clawW / 2f, clawH / 2f, 
                       clawW, clawH,
                       1.0f, 1.0f, 
                       rotation);
            batch.setColor(Color.WHITE); 
        }
    }

    @Override
    protected void updateHitbox() {
        int hbW = 42; 
        int hbH = 62; 
        int hbX = (facing == 1) ? (int)(bounds.x + bounds.width) - 12 : (int)bounds.x - hbW + 12;
        int hbY = (int)bounds.y + 2;
        attackHitbox.set(hbX, hbY, hbW, hbH);
    }

    @Override
    public void setLeft(boolean pressed) {
        if (pressed) facing = -1;
        physics.velocityX = pressed ? -420f : (physics.velocityX < 0 ? 0 : physics.velocityX);
    }

    @Override
    public void setRight(boolean pressed) {
        if (pressed) facing = 1;
        physics.velocityX = pressed ? 420f : (physics.velocityX > 0 ? 0 : physics.velocityX);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (clawTexture != null) clawTexture.dispose(); 
    }
}