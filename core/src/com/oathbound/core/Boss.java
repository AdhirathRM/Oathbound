package com.oathbound.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import java.util.List;

public class Boss {
    public enum Phase { ONE, TRANSFORMING, TWO }
    private Phase currentPhase = Phase.ONE;
    
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

    private Texture walkSheet, walk2Sheet, attackSheet, longSheet;
    private TextureRegion[] walkFrames, walk2Frames, attackFrames, longFrames;
    private final int FRAME_WIDTH = 68;
    private final int FRAME_HEIGHT = 68;
    private final int RENDER_WIDTH = 136; 
    private final int RENDER_HEIGHT = 136;
    
    private int frameIndex = 0;
    private float animTimer = 0f;
    private final float WALK_FRAME_TIME = 0.12f;
    private final float ATTACK_FRAME_TIME = 0.08f;
    private boolean actionFired = false; 

    private float meleeCooldown = 0f;
    private float rangedCooldown = 0f;
    private float teleportCooldown = 3.0f;
    private final float MELEE_CD_MAX = 2.5f;
    private final float RANGED_CD_MAX = 3.5f;
    private final float TELEPORT_CD_MAX = 6.0f;
    
    private float teleportTimer = 0f;
    private boolean hasTeleported = false;

    private boolean recentlyHealed = false; 
    private float healFlashTimer = 0f;

    public Boss(int x, int y) {
        this.bounds = new Rectangle(x, y, 60, 110);
        this.physics = new PhysicsComponent();
        loadSprites();
    }

    private void loadSprites() {
        if (Gdx.files.internal("sprites/vampire_walk.png").exists()) {
            walkSheet = new Texture(Gdx.files.internal("sprites/vampire_walk.png"));
            walkFrames = new TextureRegion[6];
            for (int i = 0; i < 6; i++) {
                walkFrames[i] = new TextureRegion(walkSheet, i * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT);
                walkFrames[i].flip(false, true);
            }
        }
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

        if (isInvincible) {
            invincTimer += dt;
            if (invincTimer > 0.2f) isInvincible = false;
        }
        if (recentlyHealed) {
            healFlashTimer += dt;
            if (healFlashTimer > 0.3f) recentlyHealed = false;
        }

        if (currentPhase == Phase.TRANSFORMING) {
            transformTimer += dt;
            physics.velocityX = 0;
            physics.update(dt, bounds, solid, 1280, 736);
            if (transformTimer >= 2.5f) {
                currentPhase = Phase.TWO;
                currentAction = Action.CHASING;
                moveSpeed = 220f;
            }
            return;
        }

        if (meleeCooldown > 0) meleeCooldown -= dt;
        if (rangedCooldown > 0) rangedCooldown -= dt;
        if (teleportCooldown > 0) teleportCooldown -= dt;

        float cx = bounds.x + bounds.width / 2f;
        float px = player.getBounds().x + player.getBounds().width / 2f;
        float dist = Math.abs(cx - px);
        
        if (currentAction == Action.CHASING) {
            direction = (px < cx) ? -1 : 1;
        }

        switch (currentAction) {
            case CHASING:
                physics.velocityX = moveSpeed * direction;
                updateWalkAnimation(dt);
                if (dist > 350 && teleportCooldown <= 0) {
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
                if (teleportTimer >= 0.25f && !hasTeleported) {
                    hasTeleported = true;
                    float targetX = player.getBounds().x + (direction * 150); 
                    if (targetX < 50) targetX = 50;
                    if (targetX > 1150) targetX = 1150;
                    bounds.x = (int) targetX;
                    direction = (player.getBounds().x < bounds.x) ? -1 : 1; 
                }
                if (teleportTimer >= 0.5f) {
                    currentAction = Action.CHASING;
                    teleportCooldown = currentPhase == Phase.ONE ? TELEPORT_CD_MAX : TELEPORT_CD_MAX * 0.7f;
                }
                break;

            case MELEE:
                physics.velocityX = 0;
                updateMeleeAnimation(dt, player);
                break;

            case RANGED:
                physics.velocityX = 0;
                updateRangedAnimation(dt, projectiles, false);
                break;
                
            case BAT_SWARM:
                physics.velocityX = 0;
                updateRangedAnimation(dt, projectiles, true);
                break;
        }

        physics.update(dt, bounds, solid, 1280, 736);

        if (bounds.overlaps(player.getBounds()) && currentAction != Action.TELEPORTING) {
            float pCenterX = player.getBounds().x + player.getBounds().width / 2f;
            float bCenterX = bounds.x + bounds.width / 2f;
            float originalX = player.getBounds().x;

            if (pCenterX < bCenterX) {
                player.getBounds().x = bounds.x - player.getBounds().width;
            } else {
                player.getBounds().x = bounds.x + bounds.width;
            }

            for (Rectangle tile : solid) {
                if (player.getBounds().overlaps(tile)) {
                    player.getBounds().x = originalX;
                    break;
                }
            }
        }

        if (currentPhase == Phase.ONE && health <= maxHealth / 2) {
            currentPhase = Phase.TRANSFORMING;
            transformTimer = 0;
            currentAction = Action.CHASING;
        }
        
        if (health <= 0) isDead = true;
    }

    public void forceTeleport() {
        if (!isDead && currentPhase != Phase.TRANSFORMING && currentAction != Action.TELEPORTING) {
            startAction(Action.TELEPORTING);
            teleportCooldown = currentPhase == Phase.ONE ? TELEPORT_CD_MAX : TELEPORT_CD_MAX * 0.7f;
        }
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
            if (frameIndex == 4 && !actionFired) {
                actionFired = true;
                int hitW = 75;
                int hitX = (direction == 1) ? (int)(bounds.x + bounds.width) : (int)(bounds.x - hitW);
                Rectangle hitBox = new Rectangle(hitX, bounds.y, hitW, bounds.height);
                if (hitBox.overlaps(player.getBounds())) {
                    int playerHpBefore = player.getHealth();
                    player.takeDamage(1); 
                    if (player.getHealth() < playerHpBefore) {
                        heal(8);
                        recentlyHealed = true;
                        healFlashTimer = 0f;
                    }
                }
            }
            if (frameIndex >= 7) { 
                currentAction = Action.CHASING;
                frameIndex = 0;
                meleeCooldown = currentPhase == Phase.ONE ? MELEE_CD_MAX : MELEE_CD_MAX * 0.6f;
            }
        }
    }

    private void updateRangedAnimation(float dt, List<Projectile> projectiles, boolean isBatSwarm) {
        animTimer += dt;
        if (animTimer >= ATTACK_FRAME_TIME) {
            animTimer -= ATTACK_FRAME_TIME;
            frameIndex++;
            if (frameIndex == 4 && !actionFired) {
                actionFired = true;
                int spawnX = (direction == 1) ? (int)(bounds.x + bounds.width) : (int)(bounds.x - 20);
                int spawnY = (int)(bounds.y + bounds.height / 2);
                if (isBatSwarm) {
                    projectiles.add(new Projectile(spawnX, spawnY, 450 * direction, -120, "BAT"));
                    projectiles.add(new Projectile(spawnX, spawnY, 450 * direction, 0, "BAT"));
                    projectiles.add(new Projectile(spawnX, spawnY, 450 * direction, 120, "BAT"));
                    rangedCooldown = RANGED_CD_MAX; 
                } else {
                    projectiles.add(new Projectile(spawnX, spawnY, 600 * direction, 0, "RED_ORB"));
                    rangedCooldown = currentPhase == Phase.ONE ? RANGED_CD_MAX : RANGED_CD_MAX * 0.7f;
                }
            }
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
        if (isInvincible || currentPhase == Phase.TRANSFORMING || isDead || currentAction == Action.TELEPORTING) return;
        health -= amt;
        isInvincible = true;
        invincTimer = 0;
    }

    public void render(SpriteBatch batch) {
        if (isDead) return;

        TextureRegion currentFrame = null;
        if (currentPhase == Phase.TRANSFORMING) {
            if (walkFrames != null) currentFrame = walkFrames[0];
        } else if (currentAction == Action.TELEPORTING) {
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
            if (currentFrame.isFlipX() != needsFlip) currentFrame.flip(true, false);

            float alpha = 1.0f;
            if (currentAction == Action.TELEPORTING) {
                if (!hasTeleported) alpha = 1.0f - (teleportTimer / 0.25f);
                else alpha = (teleportTimer - 0.25f) / 0.25f;
                alpha = MathUtils.clamp(alpha, 0f, 1f);
            }

            if (recentlyHealed) batch.setColor(0f, 1f, 0f, alpha);
            else if (isInvincible) batch.setColor(1f, 0f, 0f, alpha);
            else if (currentPhase == Phase.TWO) batch.setColor(1.0f, 0.7f, 0.7f, alpha);
            else batch.setColor(1f, 1f, 1f, alpha);

            int drawX = (int)bounds.x - (RENDER_WIDTH - (int)bounds.width) / 2;
            int drawY = (int)bounds.y - (RENDER_HEIGHT - (int)bounds.height);
            batch.draw(currentFrame, drawX, drawY, RENDER_WIDTH, RENDER_HEIGHT);
            batch.setColor(Color.WHITE); 
        }
    }

    public void renderShapes(ShapeRenderer sr) {
        if (isDead) return;
        
        float cx = bounds.x + bounds.width / 2f;
        float cy = bounds.y + bounds.height / 2f;

        if (currentPhase == Phase.TRANSFORMING) {
            float pulse = MathUtils.sin(transformTimer * 15f) * 15f;
            float radius = 80f + (transformTimer * 10f) + pulse;
            sr.setColor(1f, 0f, 0f, 0.3f);
            sr.circle(cx, cy, radius);
            sr.setColor(1f, 0.2f, 0.2f, 0.5f);
            sr.circle(cx, cy, radius * 0.7f);
        }

        float barW = 1000;
        float bx = (1280 - barW) / 2;
        float by = 680; 
        sr.setColor(Color.BLACK);
        sr.rect(bx - 4, by - 4, barW + 8, 33);
        sr.setColor(Color.RED);
        sr.rect(bx, by, barW * ((float)health / maxHealth), 25);
        if (currentPhase == Phase.TRANSFORMING) {
            sr.setColor(Color.PURPLE);
            sr.rect(bx, by - 15, barW * (transformTimer / 2.5f), 5);
        }
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