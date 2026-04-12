package com.oathbound.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class VowStone {
    private final Rectangle bounds;
    private final int width = 96;
    private final int height = 96;
    private final int FRAME_SIZE = 68;
    private boolean activated = false;
    
    private Texture sheet;
    private TextureRegion[] frames;
    private float animTimer = 0f;
    private int frameIndex = 0;
    private final float ANIM_FRAME_TIME = 0.08f;

    public VowStone(int gridX, int gridY) {
        int alignedY = (gridY + 32) - height;
        int alignedX = gridX - 32;
        this.bounds = new Rectangle(alignedX, alignedY, width, height);
        
        if (Gdx.files.internal("sprites/vowstone.png").exists()) {
            sheet = new Texture(Gdx.files.internal("sprites/vowstone.png"));
            int numFrames = sheet.getWidth() / FRAME_SIZE; 
            frames = new TextureRegion[numFrames];
            for (int i = 0; i < numFrames; i++) {
                frames[i] = new TextureRegion(sheet, i * FRAME_SIZE, 0, FRAME_SIZE, FRAME_SIZE);
                frames[i].flip(false, true); 
            }
        }
    }

    public void render(SpriteBatch batch) {
        if (frames != null && frames.length > 0) {
            animTimer += Gdx.graphics.getDeltaTime();
            if (animTimer >= ANIM_FRAME_TIME) {
                animTimer -= ANIM_FRAME_TIME;
                frameIndex = (frameIndex + 1) % frames.length;
            }
            batch.draw(frames[frameIndex], bounds.x, bounds.y, width, height);
        }
    }

    public boolean checkCollision(Rectangle playerBounds) {
        if (!activated && bounds.overlaps(playerBounds)) {
            activated = true;
            return true; 
        }
        return false;
    }
    
    public void dispose() {
        if (sheet != null) sheet.dispose();
    }
}