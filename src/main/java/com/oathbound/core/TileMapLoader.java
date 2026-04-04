package com.oathbound.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * PB-004 — Tile-Map Loader (Updated with Sprites & Dense Parsing)
 *
 * Parses a dense .txt level file from the res/levels/ directory and produces:
 * - A 2D tile-ID grid (for rendering)
 * - A flat list of solid collision rectangles (for PB-006)
 * - Dynamic spawn points for Vow Stones, Enemies, and Players
 */
public class TileMapLoader {

    // ── Constants ────────────────────────────────────────────────────────────

    /** Pixel size of one tile. */
    public static final int TILE_SIZE = 32;

    // Tile ID constants
    public static final int TILE_AIR      = 0;
    public static final int TILE_GROUND   = 1;
    public static final int TILE_PLATFORM = 2;
    public static final int TILE_TRAP     = 3;
    public static final int TILE_ROCK     = 4;
    public static final int TILE_VOW = 9;
    // New tile ID constants
    public static final int TILE_WALL        = 5;  // token 'W' — vertical wall bricks
    public static final int TILE_COLUMN      = 6;  // token 'C' — decorative column
    public static final int TILE_PLATFORM_L  = 7;  // token 'L' — platform left cap
    public static final int TILE_PLATFORM_R  = 8;  // token 'X' — platform right cap
    public static final int TILE_PLATFORM_M  = 10; // token 'M' — platform middle
    public static final int TILE_DAMAGED     = 11; // token 'D' — damaged brick
    public static final int TILE_STAIR       = 12; // token 'S' — stair tile

    // Placeholder colours — used if sprites fail to load
    private static final Color COLOR_GROUND   = new Color(60,  60,  60);
    private static final Color COLOR_PLATFORM = new Color(139, 90,  43);

    // ── Fields ───────────────────────────────────────────────────────────────

    /** Raw grid of tile IDs. [row][col] */
    private int[][] tileGrid;
    private int rows;
    private int cols;

    /** Sprites */
    private BufferedImage floorLeft, floorMid1, floorMid2, floorRight, spikeSprite;
    private BufferedImage wallSprite, columnSprite;
    private BufferedImage platformLeft, platformRight, platformMid, platformShadow;
    private BufferedImage damagedSprite, stairSprite;

    /** Solid rectangles used by the collision system (PB-006). */
    private final List<Rectangle> solidTiles = new ArrayList<>();

    /** World-space positions */
    private final List<int[]> vowStonePositions = new ArrayList<>();
    private final List<int[]> enemyPositions = new ArrayList<>();
    private int[] playerSpawn = new int[]{100, 200}; // Default fallback

    // ── Public API ───────────────────────────────────────────────────────────

    public void load(String resourcePath) {
        loadSprites();
        solidTiles.clear();
        vowStonePositions.clear();
        enemyPositions.clear();

        List<int[]> rowList = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(resourcePath);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            String line;
            int row = 0;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("#") || line.isBlank()) continue;

                // Dense parsing (no spaces)
                line = line.replace(" ", "").trim();
                int[] tileRow = new int[line.length()];

                for (int col = 0; col < line.length(); col++) {
                    char token = line.charAt(col);
                    int tileId = parseToken(token, row, col);
                    tileRow[col] = tileId;

                    // Build collision rectangles for solid tiles
                    if (tileId == TILE_GROUND || tileId == TILE_PLATFORM || tileId == TILE_ROCK
 || tileId == TILE_WALL   || tileId == TILE_COLUMN   || tileId == TILE_DAMAGED
 || tileId == TILE_STAIR  || tileId == TILE_PLATFORM_L || tileId == TILE_PLATFORM_M
 || tileId == TILE_PLATFORM_R) {
                        solidTiles.add(new Rectangle(
                                col * TILE_SIZE,
                                row * TILE_SIZE,
                                TILE_SIZE,
                                TILE_SIZE
                        ));
                    }
                }

                rowList.add(tileRow);
                row++;
            }

        } catch (Exception e) {
            throw new RuntimeException("TileMapLoader: failed to load " + resourcePath, e);
        }

        rows = rowList.size();
        cols = rowList.isEmpty() ? 0 : rowList.get(0).length;
        tileGrid = rowList.toArray(new int[0][]);

        System.out.printf("[TileMapLoader] Loaded %s — %d rows × %d cols, " +
                          "%d solid tiles, %d Enemies, %d Vow Stones%n",
                resourcePath, rows, cols,
                solidTiles.size(), enemyPositions.size(), vowStonePositions.size());
    }

    public void render(Graphics2D g) {
        if (tileGrid == null) return;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int id = tileGrid[row][col];
                if (id == TILE_AIR) continue;

                int x = col * TILE_SIZE;
                int y = row * TILE_SIZE;

                if (id == TILE_GROUND) {
                    boolean hasLeft  = col > 0      && tileGrid[row][col-1] == TILE_GROUND;
                    boolean hasRight = col < cols-1 && tileGrid[row][col+1] == TILE_GROUND;
                    
                    BufferedImage sprite;
                    if (!hasLeft && hasRight)      sprite = floorLeft;
                    else if (hasLeft && !hasRight) sprite = floorRight;
                    else if (!hasLeft)             sprite = floorMid1; // Isolated tile
                    else sprite = (col % 2 == 0)  ? floorMid1 : floorMid2; // Middle tiles
                    
                    if (sprite != null) {
                        g.drawImage(sprite, x, y, null);
                    } else {
                        g.setColor(COLOR_GROUND); 
                        g.fillRect(x, y, TILE_SIZE, TILE_SIZE); 
                    }
                }
                else if (id == TILE_TRAP) {
                    if (spikeSprite != null) {
                        g.drawImage(spikeSprite, x, y, null);
                    } else {
                        g.setColor(Color.RED); 
                        g.fillRect(x, y, TILE_SIZE, TILE_SIZE); 
                    }
                }
                else if (id == TILE_PLATFORM) {
                    g.setColor(COLOR_PLATFORM); g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                }
                else if (id == TILE_ROCK) {
                    g.setColor(Color.GRAY);
                    g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                }
                else if (id == TILE_WALL) {
                    if (wallSprite != null) g.drawImage(wallSprite, x, y, null);
                    else { g.setColor(new Color(80,80,80)); g.fillRect(x, y, TILE_SIZE, TILE_SIZE); }
                }
                else if (id == TILE_COLUMN) {
                    if (columnSprite != null) g.drawImage(columnSprite, x, y, null);
                    else { g.setColor(new Color(100,100,100)); g.fillRect(x, y, TILE_SIZE, TILE_SIZE); }
                }
                else if (id == TILE_PLATFORM_L) {
                    if (platformLeft != null) g.drawImage(platformLeft, x, y, null);
                    if (platformShadow != null) g.drawImage(platformShadow, x, y + TILE_SIZE, null);
                }
                else if (id == TILE_PLATFORM_M) {
                    if (platformMid != null) g.drawImage(platformMid, x, y, null);
                    if (platformShadow != null) g.drawImage(platformShadow, x, y + TILE_SIZE, null);
                }
                else if (id == TILE_PLATFORM_R) {
                    if (platformRight != null) g.drawImage(platformRight, x, y, null);
                    if (platformShadow != null) g.drawImage(platformShadow, x, y + TILE_SIZE, null);
                }
                else if (id == TILE_DAMAGED) {
                    if (damagedSprite != null) g.drawImage(damagedSprite, x, y, null);
                    else { g.setColor(new Color(90,70,60)); g.fillRect(x, y, TILE_SIZE, TILE_SIZE); }
                }
                else if (id == TILE_STAIR) {
                    if (stairSprite != null) g.drawImage(stairSprite, x, y, null);
                    else { g.setColor(new Color(70,70,70)); g.fillRect(x, y, TILE_SIZE, TILE_SIZE); }
                }
            }
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public List<Rectangle> getSolidTiles() {
        return java.util.Collections.unmodifiableList(solidTiles);
    }

    public List<int[]> getVowStonePositions() {
        return java.util.Collections.unmodifiableList(vowStonePositions);
    }
    
    public List<int[]> getEnemyPositions() { 
        return enemyPositions; 
    }
    
    public int[] getPlayerSpawn() { 
        return playerSpawn; 
    }

    public int getRows()     { return rows; }
    public int getCols()     { return cols; }
    public int getTileId(int row, int col) { return tileGrid[row][col]; }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void loadSprites() {
        floorLeft   = loadImage("/sprites/floor_tile_1.png");
        floorMid1   = loadImage("/sprites/floor_tile_2.png");
        floorMid2   = loadImage("/sprites/floor_tile_3.png");
        floorRight  = loadImage("/sprites/floor_tile_4.png");
        spikeSprite = loadImage("/sprites/spikes.png");
        wallSprite     = loadImage("/sprites/wall_1.png");
        columnSprite   = loadImage("/sprites/column_1.png");
        platformLeft   = loadImage("/sprites/platform_1.png");   // left cap
        platformMid    = loadImage("/sprites/platform_2.png");   // middle fill
        platformRight  = loadImage("/sprites/platform_3.png");   // right cap
        platformShadow = loadImage("/sprites/platform_shadow.png");
        damagedSprite  = loadImage("/sprites/damaged_brick_1.png");
        stairSprite    = loadImage("/sprites/stairs_tile_1.png");
    }

    private BufferedImage loadImage(String path) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) { 
                System.err.println("[TileMapLoader] Missing: " + path); 
                return null; 
            }
            return ImageIO.read(is);
        } catch (Exception e) { 
            e.printStackTrace(); 
            return null; 
        }
    }

    private int parseToken(char token, int row, int col) {
        int xPos = col * TILE_SIZE;
        int yPos = row * TILE_SIZE;
        
        switch (token) {
            case '1': return TILE_GROUND;
            case '2': return TILE_PLATFORM;
            case 'W': return TILE_WALL;
            case 'C': return TILE_COLUMN;
            case 'L': return TILE_PLATFORM_L;
            case 'M': return TILE_PLATFORM_M;
            case 'X': return TILE_PLATFORM_R;
            case 'D': return TILE_DAMAGED;
            case 'S': return TILE_STAIR;
            case 'R': return TILE_ROCK;
            case 'T': return TILE_TRAP;
            case 'V': case 'v':
                vowStonePositions.add(new int[]{xPos, yPos});
                return TILE_AIR;
            case 'E': case 'e':
                enemyPositions.add(new int[]{xPos, yPos});
                return TILE_AIR;
            case 'P': case 'p':
                playerSpawn = new int[]{xPos, yPos};
                return TILE_AIR;
            default: return TILE_AIR;
        }
    }
}