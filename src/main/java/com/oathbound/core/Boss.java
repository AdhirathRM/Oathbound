package com.oathbound.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import java.awt.Rectangle;
import java.util.List;

/**
 * Lord Malakor, the Crimson Sovereign.
 * Level 10 Vampire Boss with Sprites, Cooldowns, Teleportation, and Lifesteal.
 */
public class Boss {
    public enum Phase { ONE, TRANSFORMING, TWO }
    private Phase currentPhase = Phase.ONE;
    
    // AI States
    public enum Action { CHASING, MELEE, RANGED, BAT_SWARM, TELEPORTING }
    private Action currentAction = Action.CHASING;
    
    private final Rectangle bounds;
    private final PhysicsComponent physics;
    
    private int health = 80;
    private final int maxHealth = 80;
    private float moveSpeed = 120f;
    private int direction = -1;
    
    private boolean isInvincible = false;
    private float invincTimer = 0f;
    private float transformTimer = 0f;
    private boolean isDead = false;

    // --- SPRITES AND ANIMATION ---
    private Texture walkSheet, walk2Sheet, attackSheet, longSheet;
    private TextureRegion[] walkFrames, walk2Frames, attackFrames, longFrames;
    private final int FRAME_WIDTH = 68;
    private final int FRAME_HEIGHT = 68;
    
    // Scale boss up slightly to look imposing
    private final int RENDER_WIDTH = 136; 
    private final int RENDER_HEIGHT = 136;
    
    private int frameIndex = 0;
    private float animTimer = 0f;
    private final float WALK_FRAME_TIME = 0.12f;
    private final float ATTACK_FRAME_TIME = 0.08f;
    
    // To ensure attacks only register damage/shoot once per animation
    private boolean actionFired = false; 

    // --- AI COOLDOWNS & TIMERS ---
    private float meleeCooldown = 0f;
    private float rangedCooldown = 0f;
    private float teleportCooldown = 3.0f; // Initial delay before he can teleport
    private final float MELEE_CD_MAX = 2.5f;
    private final float RANGED_CD_MAX = 3.5f;
    private final float TELEPORT_CD_MAX = 6.0f;
    
    private float teleportTimer = 0f;
    private boolean hasTeleported = false;

    // Visual Feedback
    private boolean recentlyHealed = false; 
    private float healFlashTimer = 0f;

    public Boss(int x, int y) {
        // Physical hitbox (smaller than the render size)
        this.bounds = new Rectangle(x, y, 60, 110);
        this.physics = new PhysicsComponent();
        loadSprites();
    }

    private void loadSprites() {
        // Phase 1 Walk
        if (Gdx.files.internal("sprites/vampire_walk.png").exists()) {
            walkSheet = new Texture(Gdx.files.internal("sprites/vampire_walk.png"));
            walkFrames = new TextureRegion[6];
            for (int i = 0; i < 6; i++) {
                walkFrames[i] = new TextureRegion(walkSheet, i * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT);
                walkFrames[i].flip(false, true);
            }
        }
        // Phase 2 Walk
        if (Gdx.files.internal("sprites/vampire2_walk.png").exists()) {
            walk2Sheet = new Texture(Gdx.files.internal("sprites/vampire2_walk.png"));
            walk2Frames = new TextureRegion[6];
            for (int i = 0; i < 6; i++) {
                walk2Frames[i] = new TextureRegion(walk2Sheet, i * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT);
                walk2Frames[i].flip(false, true);
            }
        }
        if (Gdx.files.internal("sprites/vampire_attack.png").exists()) {
            attackSheet = new Texture(Gdx.files.internal("sprites/vampire_attack.png"));
            attackFrames = new TextureRegion[7];
            for (int i = 0; i < 7; i++) {
                attackFrames[i] = new TextureRegion(attackSheet, i * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT);
                attackFrames[i].flip(false, true);
            }
        }
        if (Gdx.files.internal("sprites/vampire_long.png").exists()) {
            longSheet = new Texture(Gdx.files.internal("sprites/vampire_long.png"));
            longFrames = new TextureRegion[7];
            for (int i = 0; i < 7; i++) {
                longFrames[i] = new TextureRegion(longSheet, i * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT);
                longFrames[i].flip(false, true);
            }
        }
    }

    public void update(float dt, Player player, List<Rectangle> solid, List<Projectile> projectiles) {
        if (isDead) return;

        // Visual i-frames / heal flashing
        if (isInvincible) {
            invincTimer += dt;
            if (invincTimer > 0.2f) isInvincible = false;
        }
        if (recentlyHealed) {
            healFlashTimer += dt;
            if (healFlashTimer > 0.3f) recentlyHealed = false;
        }

        // Phase Transition Logic
        if (currentPhase == Phase.TRANSFORMING) {
            transformTimer += dt;
            physics.velocityX = 0;
            physics.update(dt, bounds, solid, 1280, 736);
            if (transformTimer >= 2.5f) {
                currentPhase = Phase.TWO;
                currentAction = Action.CHASING;
                moveSpeed = 220f; // Faster in phase 2!
            }
            return; // Don't do AI while transforming
        }

        // Reduce Cooldowns
        if (meleeCooldown > 0) meleeCooldown -= dt;
        if (rangedCooldown > 0) rangedCooldown -= dt;
        if (teleportCooldown > 0) teleportCooldown -= dt;

        // Distances & Direction
        float cx = bounds.x + bounds.width / 2f;
        float px = player.getBounds().x + player.getBounds().width / 2f;
        float dist = Math.abs(cx - px);
        
        // Only flip direction if we aren't currently locked into an attack/teleport animation
        if (currentAction == Action.CHASING) {
            direction = (px < cx) ? -1 : 1;
        }

        // --- STATE MACHINE ---
        switch (currentAction) {
            case CHASING:
                physics.velocityX = moveSpeed * direction;
                updateWalkAnimation(dt);
                
                // AI Decision Making
                if (dist > 350 && teleportCooldown <= 0) {
                    // If player is too far, teleport to them!
                    startAction(Action.TELEPORTING);
                } else if (dist <= 110 && meleeCooldown <= 0) {
                    startAction(Action.MELEE);
                } else if (dist > 250 && rangedCooldown <= 0) {
                    startAction(Action.RANGED);
                } else if (currentPhase == Phase.TWO && dist > 150 && rangedCooldown <= 0 && MathUtils.randomBoolean(0.02f)) {
                    startAction(Action.BAT_SWARM);
                }
                break;

            case TELEPORTING:
                physics.velocityX = 0;
                teleportTimer += dt;
                
                // At half-duration, instantly move coordinates to flank the player
                if (teleportTimer >= 0.25f && !hasTeleported) {
                    hasTeleported = true;
                    // Teleport behind the player's current running direction
                    float targetX = player.getBounds().x + (direction * 150); 
                    
                    // Keep the boss in bounds
                    if (targetX < 50) targetX = 50;
                    if (targetX > 1150) targetX = 1150;
                    
                    bounds.x = (int) targetX;
                    // Immediately face the player after blinking
                    direction = (player.getBounds().x < bounds.x) ? -1 : 1; 
                }
                
                // Finish teleporting
                if (teleportTimer >= 0.5f) {
                    currentAction = Action.CHASING;
                    teleportCooldown = currentPhase == Phase.ONE ? TELEPORT_CD_MAX : TELEPORT_CD_MAX * 0.7f; // Faster CDs in Phase 2
                }
                break;

            case MELEE:
                physics.velocityX = 0; // Stop moving to attack
                updateMeleeAnimation(dt, player);
                break;

            case RANGED:
                physics.velocityX = 0;
                updateRangedAnimation(dt, projectiles, false);
                break;
                
            case BAT_SWARM:
                physics.velocityX = 0;
                updateRangedAnimation(dt, projectiles, true); // Uses the same animation, fires bats
                break;
        }

        physics.update(dt, bounds, solid, 1280, 736);

        // --- PREVENT OVERLAP WITH PLAYER ---
        if (bounds.intersects(player.getBounds()) && currentAction != Action.TELEPORTING) {
            float pCenterX = player.getBounds().x + player.getBounds().width / 2f;
            float bCenterX = bounds.x + bounds.width / 2f;
            
            int originalX = player.getBounds().x;

            if (pCenterX < bCenterX) {
                player.getBounds().x = bounds.x - player.getBounds().width; // Push player left
            } else {
                player.getBounds().x = bounds.x + bounds.width; // Push player right
            }

            // Undo push if it shoved the player into a solid map tile! (Prevents flying up)
            for (Rectangle tile : solid) {
                if (player.getBounds().intersects(tile)) {
                    player.getBounds().x = originalX;
                    break;
                }
            }
        }

        // Phase Transition Trigger (50% HP)
        if (currentPhase == Phase.ONE && health <= maxHealth / 2) {
            currentPhase = Phase.TRANSFORMING;
            transformTimer = 0;
            currentAction = Action.CHASING;
            System.out.println("Lord Malakor transforms! Phase 2 initiated.");
        }
        
        if (health <= 0) isDead = true;
    }

    private void startAction(Action act) {
        currentAction = act;
        frameIndex = 0;
        animTimer = 0;
        actionFired = false;
        physics.velocityX = 0;
        
        if (act == Action.TELEPORTING) {
            teleportTimer = 0f;
            hasTeleported = false;
        }
    }

    private void updateWalkAnimation(float dt) {
        animTimer += dt;
        if (animTimer >= WALK_FRAME_TIME) {
            animTimer -= WALK_FRAME_TIME;
            frameIndex = (frameIndex + 1) % 6;
        }
    }

    private void updateMeleeAnimation(float dt, Player player) {
        animTimer += dt;
        if (animTimer >= ATTACK_FRAME_TIME) {
            animTimer -= ATTACK_FRAME_TIME;
            frameIndex++;
            
            // Strike frame! Check collision with player
            if (frameIndex == 4 && !actionFired) {
                actionFired = true;
                
                // Create a temporary damage hitbox in front of the boss
                int hitW = 75;
                int hitX = (direction == 1) ? (int)(bounds.x + bounds.width) : (int)(bounds.x - hitW);
                Rectangle hitBox = new Rectangle(hitX, bounds.y, hitW, bounds.height);
                
                if (hitBox.intersects(player.getBounds())) {
                    int playerHpBefore = player.getHealth();
                    player.takeDamage(1); 
                    
                    // LIFESTEAL if the player wasn't in i-frames
                    if (player.getHealth() < playerHpBefore) {
                        heal(8);
                        recentlyHealed = true;
                        healFlashTimer = 0f;
                    }
                }
            }
            
            // End animation
            if (frameIndex >= 7) { 
                currentAction = Action.CHASING;
                frameIndex = 0;
                meleeCooldown = currentPhase == Phase.ONE ? MELEE_CD_MAX : MELEE_CD_MAX * 0.6f; // Faster CD in Phase 2
            }
        }
    }

    private void updateRangedAnimation(float dt, List<Projectile> projectiles, boolean isBatSwarm) {
        animTimer += dt;
        if (animTimer >= ATTACK_FRAME_TIME) {
            animTimer -= ATTACK_FRAME_TIME;
            frameIndex++;
            
            // Fire frame!
            if (frameIndex == 4 && !actionFired) {
                actionFired = true;
                int spawnX = (direction == 1) ? (int)(bounds.x + bounds.width) : (int)(bounds.x - 20);
                int spawnY = (int)(bounds.y + bounds.height / 2);
                
                if (isBatSwarm) {
                    // Phase 2 Bat Swarm
                    projectiles.add(new Projectile(spawnX, spawnY, 450 * direction, -120, "BAT"));
                    projectiles.add(new Projectile(spawnX, spawnY, 450 * direction, 0, "BAT"));
                    projectiles.add(new Projectile(spawnX, spawnY, 450 * direction, 120, "BAT"));
                    rangedCooldown = RANGED_CD_MAX; 
                } else {
                    // Phase 1 Red Orb
                    projectiles.add(new Projectile(spawnX, spawnY, 600 * direction, 0, "RED_ORB"));
                    rangedCooldown = currentPhase == Phase.ONE ? RANGED_CD_MAX : RANGED_CD_MAX * 0.7f;
                }
            }
            
            // End animation
            if (frameIndex >= 7) { 
                currentAction = Action.CHASING;
                frameIndex = 0;
            }
        }
    }

    private void heal(int amt) {
        health += amt;
        if (health > maxHealth) health = maxHealth;
    }

    public void takeDamage(int amt) {
        // Can't be hurt during transformation or while actively blinking in teleport
        if (isInvincible || currentPhase == Phase.TRANSFORMING || isDead || currentAction == Action.TELEPORTING) return;
        health -= amt;
        isInvincible = true;
        invincTimer = 0;
    }

    // --- NEW BATCH RENDERER FOR SPRITES ---
    public void render(SpriteBatch batch) {
        if (isDead) return;

        TextureRegion currentFrame = null;
        
        if (currentPhase == Phase.TRANSFORMING) {
            // Shake or freeze frame while transforming
            if (walkFrames != null) currentFrame = walkFrames[0];
        } else if (currentAction == Action.TELEPORTING) {
            // Freeze on a specific frame while fading
            if (currentPhase == Phase.TWO && walk2Frames != null) currentFrame = walk2Frames[0];
            else if (walkFrames != null) currentFrame = walkFrames[0];
        } else if (currentAction == Action.MELEE && attackFrames != null && frameIndex < attackFrames.length) {
            currentFrame = attackFrames[frameIndex];
        } else if ((currentAction == Action.RANGED || currentAction == Action.BAT_SWARM) && longFrames != null && frameIndex < longFrames.length) {
            currentFrame = longFrames[frameIndex];
        } else if (currentPhase == Phase.TWO && walk2Frames != null) {
            currentFrame = walk2Frames[frameIndex];
        } else if (walkFrames != null) {
            currentFrame = walkFrames[frameIndex];
        }

        if (currentFrame != null) {
            boolean needsFlip = (direction == -1);
            if (currentFrame.isFlipX() != needsFlip) {
                currentFrame.flip(true, false);
            }

            // Alpha transparency for Teleport
            float alpha = 1.0f;
            if (currentAction == Action.TELEPORTING) {
                // Fade out before teleport, fade in after
                if (!hasTeleported) alpha = 1.0f - (teleportTimer / 0.25f);
                else alpha = (teleportTimer - 0.25f) / 0.25f;
                alpha = MathUtils.clamp(alpha, 0f, 1f);
            }

            // Flash colors
            if (recentlyHealed) {
                batch.setColor(0f, 1f, 0f, alpha); // Green Lifesteal
            } else if (isInvincible) {
                batch.setColor(1f, 0f, 0f, alpha); // Red Hit
            } else if (currentPhase == Phase.TWO) {
                batch.setColor(1.0f, 0.7f, 0.7f, alpha); // Permanent angry red tint in Phase 2
            } else {
                batch.setColor(1f, 1f, 1f, alpha);
            }

            // Center the larger sprite over the physical hitbox
            int drawX = (int)bounds.x - (RENDER_WIDTH - (int)bounds.width) / 2;
            int drawY = (int)bounds.y - (RENDER_HEIGHT - (int)bounds.height);
            
            batch.draw(currentFrame, drawX, drawY, RENDER_WIDTH, RENDER_HEIGHT);
            batch.setColor(Color.WHITE); 
        }
    }

    // --- RENDERER FOR UI & EFFECTS ---
    public void renderShapes(ShapeRenderer sr) {
        if (isDead) return;
        
        float cx = bounds.x + bounds.width / 2f;
        float cy = bounds.y + bounds.height / 2f;

        // Glowing Red Mana Transformation Aura
        if (currentPhase == Phase.TRANSFORMING) {
            // Calculate a pulsing sine wave effect
            float pulse = MathUtils.sin(transformTimer * 15f) * 15f;
            float radius = 80f + (transformTimer * 10f) + pulse; // Aura grows as timer increases
            
            sr.setColor(1f, 0f, 0f, 0.3f); // Transparent dark red outer
            sr.circle(cx, cy, radius);
            
            sr.setColor(1f, 0.2f, 0.2f, 0.5f); // Transparent bright red inner
            sr.circle(cx, cy, radius * 0.7f);
        }

        // Boss Health Bar UI (Moved to Bottom)
        float barW = 1000;
        float bx = (1280 - barW) / 2;
        float by = 680; 
        
        sr.setColor(Color.BLACK);
        sr.rect(bx - 4, by - 4, barW + 8, 33);
        sr.setColor(Color.RED);
        sr.rect(bx, by, barW * ((float)health / maxHealth), 25);
        
        // Transforming bar indicator
        if (currentPhase == Phase.TRANSFORMING) {
            sr.setColor(Color.PURPLE);
            sr.rect(bx, by - 15, barW * (transformTimer / 2.5f), 5);
        }
    }

    // --- NEW: RENDER BOSS NAME TEXT ---
    public void renderUIText(SpriteBatch batch, com.badlogic.gdx.graphics.g2d.BitmapFont font) {
        if (isDead) return;
        float barW = 1000;
        float bx = (1280 - barW) / 2;
        float by = 680;
        
        font.setColor(Color.WHITE);
        font.draw(batch, "Lord Malakor, the Crimson Sovereign", bx, by + 45);
    }

    public void dispose() {
        if (walkSheet != null) walkSheet.dispose();
        if (walk2Sheet != null) walk2Sheet.dispose();
        if (attackSheet != null) attackSheet.dispose();
        if (longSheet != null) longSheet.dispose();
    }

    public int getHealth() { return health; }
    public boolean isDead() { return isDead; }
    public Rectangle getBounds() { return bounds; }
}