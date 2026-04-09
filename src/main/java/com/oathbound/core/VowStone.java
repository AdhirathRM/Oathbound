package com.oathbound.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.awt.Rectangle; 

/**
 * PB-020 — Vow Stone (Portal)
 * The level's goal point. Touching this triggers level completion.
 * Updated to use a 24-frame animated sprite sheet (1632x68)!
 */
public class VowStone {
    private final Rectangle bounds;
    private final int width = 96;  // Scaled up visual size
    private final int height = 96; // Scaled up visual size
    private final int FRAME_SIZE = 68; // True pixel size of each frame
    private boolean activated = false;
    
    private Texture sheet;
    private TextureRegion[] frames;
    private float animTimer = 0f;
    private int frameIndex = 0;
    private final float ANIM_FRAME_TIME = 0.08f; // Animation speed (lower is faster)

    public VowStone(int gridX, int gridY) {
        // ALIGNMENT FIX:
        // A standard tile is 32x32. We want the 96px portal to sit precisely on the floor.
        // We shift the Y position so its bottom aligns with the bottom of the 32px grid cell.
        int alignedY = (gridY + 32) - height;
        
        // We shift the X position to perfectly center the 96px wide portal over the 32px cell.
        int alignedX = gridX - 32;
        
        this.bounds = new Rectangle(alignedX, alignedY, width, height);
        
        if (Gdx.files.internal("sprites/vowstone.png").exists()) {
            sheet = new Texture(Gdx.files.internal("sprites/vowstone.png"));
            
            // Dynamically calculate how many frames are in the sheet (1632 / 68 = 24)
            int numFrames = sheet.getWidth() / FRAME_SIZE; 
            frames = new TextureRegion[numFrames];
            
            for (int i = 0; i < numFrames; i++) {
                frames[i] = new TextureRegion(sheet, i * FRAME_SIZE, 0, FRAME_SIZE, FRAME_SIZE);
                // Flip right-side up for our Y-Down camera
                frames[i].flip(false, true); 
            }
        }
    }

    public void render(SpriteBatch batch) {
        if (frames != null && frames.length > 0) {
            // Update the animation timer using Delta Time
            animTimer += Gdx.graphics.getDeltaTime();
            if (animTimer >= ANIM_FRAME_TIME) {
                animTimer -= ANIM_FRAME_TIME;
                frameIndex = (frameIndex + 1) % frames.length; // Loop back to 0
            }
            
            // Draw the current frame of the animation
            batch.draw(frames[frameIndex], bounds.x, bounds.y, width, height);
        }
    }

    public boolean checkCollision(Rectangle playerBounds) {
        if (!activated && bounds.intersects(playerBounds)) {
            activated = true;
            return true; 
        }
        return false;
    }
    
    // Always clean up textures to avoid memory leaks!
    public void dispose() {
        if (sheet != null) {
            sheet.dispose();
        }
    }
}