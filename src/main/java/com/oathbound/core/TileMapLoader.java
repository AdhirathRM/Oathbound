package com.oathbound.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * PB-004 — Tile-Map Loader (Procedural Pixel-Art Rendering)
 *
 * Parses a dense .txt level file from the res/levels/ directory and produces:
 * - A 2D tile-ID grid (for rendering)
 * - A flat list of solid collision rectangles (for PB-006)
 * - Dynamic spawn points for Vow Stones, Enemies, and Players
 * * *All graphics are procedurally generated using Java 2D to emulate retro pixel art!*
 */
public class TileMapLoader {

    // ── Constants ────────────────────────────────────────────────────────────

    public static final int TILE_SIZE = 32;

    // Tile ID constants
    public static final int TILE_AIR      = 0;
    public static final int TILE_GROUND   = 1;
    public static final int TILE_PLATFORM = 2;
    public static final int TILE_TRAP     = 3;
    public static final int TILE_ROCK     = 4;
    public static final int TILE_VOW      = 9;
    
    public static final int TILE_WALL        = 5;  // token 'W' — vertical wall bricks
    public static final int TILE_COLUMN      = 6;  // token 'C' — decorative column
    public static final int TILE_PLATFORM_L  = 7;  // token 'L' — platform left cap
    public static final int TILE_PLATFORM_R  = 8;  // token 'X' — platform right cap
    public static final int TILE_PLATFORM_M  = 10; // token 'M' — platform middle
    public static final int TILE_DAMAGED     = 11; // token 'D' — damaged brick
    public static final int TILE_STAIR       = 12; // token 'S' — stair tile

    // ── Fields ───────────────────────────────────────────────────────────────

    private int[][] tileGrid;
    private int rows;
    private int cols;

    private final List<Rectangle> solidTiles = new ArrayList<>();
    private final List<int[]> vowStonePositions = new ArrayList<>();
    private final List<int[]> enemyPositions = new ArrayList<>();
    private int[] playerSpawn = new int[]{100, 200};

    // ── Palette ──────────────────────────────────────────────────────────────
    
    private final Color darkStone   = new Color(60, 55, 80);
    private final Color midStone    = new Color(90, 85, 110);
    private final Color highStone   = new Color(140, 130, 160);
    private final Color flagstone   = new Color(45, 40, 60);
    private final Color mortar      = new Color(25, 20, 35);
    private final Color woodBase    = new Color(80, 55, 25);
    private final Color woodHigh    = new Color(110, 80, 40);
    private final Color paleGold    = new Color(200, 180, 80);
    private final Color ghostBlue   = new Color(80, 120, 200);
    private final Color ivory       = new Color(180, 165, 120);
    private final Color trapBase    = new Color(30, 25, 35);
    private final Color bloodRed    = new Color(160, 20, 20);

    // ── Public API ───────────────────────────────────────────────────────────

    public void load(String resourcePath) {
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

                line = line.replace(" ", "").trim();
                int[] tileRow = new int[line.length()];

                for (int col = 0; col < line.length(); col++) {
                    char token = line.charAt(col);
                    int tileId = parseToken(token, row, col);
                    tileRow[col] = tileId;

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
    }

    public void render(Graphics2D g) {
        if (tileGrid == null) return;
        
        // Strict Retro Pixel Art Constraint: Antialiasing OFF
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int id = tileGrid[row][col];
                if (id == TILE_AIR) continue;

                int x = col * TILE_SIZE;
                int y = row * TILE_SIZE;

                switch (id) {
                    case TILE_GROUND:
                        boolean isTop = row == 0 || tileGrid[row - 1][col] != TILE_GROUND;
                        drawGroundTile(g, x, y, col, row, isTop);
                        break;
                    case TILE_TRAP:
                        drawSpikeTrap(g, x, y);
                        break;
                    case TILE_PLATFORM:
                    case TILE_PLATFORM_L:
                    case TILE_PLATFORM_M:
                    case TILE_PLATFORM_R:
                        drawPlatform(g, x, y, id);
                        break;
                    case TILE_ROCK:
                        drawRock(g, x, y, col, row);
                        break;
                    case TILE_WALL:
                        drawWall(g, x, y, col, row);
                        break;
                    case TILE_COLUMN:
                        drawColumn(g, x, y);
                        break;
                    case TILE_DAMAGED:
                        drawDamaged(g, x, y, col, row);
                        break;
                    case TILE_STAIR:
                        drawStair(g, x, y, col);
                        break;
                }
            }
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public List<Rectangle> getSolidTiles() { return java.util.Collections.unmodifiableList(solidTiles); }
    public List<int[]> getVowStonePositions() { return java.util.Collections.unmodifiableList(vowStonePositions); }
    public List<int[]> getEnemyPositions() { return enemyPositions; }
    public int[] getPlayerSpawn() { return playerSpawn; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getTileId(int row, int col) { return tileGrid[row][col]; }

    // ── Procedural Pixel Drawing Helpers ─────────────────────────────────────
    
    private void drawGroundTile(Graphics2D g, int x, int y, int col, int row, boolean isTop) {
        g.setColor(flagstone);
        g.fillRect(x, y, TILE_SIZE, TILE_SIZE);

        // Purple-blue bottom shade tint
        g.setColor(new Color(90, 40, 130, 90)); 
        g.fillRect(x, y + TILE_SIZE - 10, TILE_SIZE, 10);

        // Staggered mortar lines
        g.setColor(mortar);
        int offset = (row % 2 == 0) ? 0 : TILE_SIZE / 2;
        g.drawLine(x, y + TILE_SIZE / 2, x + TILE_SIZE, y + TILE_SIZE / 2); 
        g.drawLine(x + offset, y, x + offset, y + TILE_SIZE / 2); 
        int offBot = (offset == 0) ? TILE_SIZE / 2 : 0;
        g.drawLine(x + offBot, y + TILE_SIZE / 2, x + offBot, y + TILE_SIZE); 

        if (isTop) {
            // Pale gold highlight on top edge
            g.setColor(paleGold);
            g.fillRect(x, y, TILE_SIZE, 2);
            g.setColor(new Color(150, 130, 60)); // Transition gold
            g.fillRect(x, y + 2, TILE_SIZE, 2);
        } else {
            g.setColor(mortar);
            g.drawLine(x, y, x + TILE_SIZE, y);
        }

        g.setColor(Color.BLACK);
        g.drawRect(x, y, TILE_SIZE - 1, TILE_SIZE - 1);
    }

    private void drawWall(Graphics2D g, int x, int y, int col, int row) {
        g.setColor(darkStone);
        g.fillRect(x, y, TILE_SIZE, TILE_SIZE);

        // Slight purple hue wash
        g.setColor(new Color(90, 40, 130, 60));
        g.fillRect(x, y, TILE_SIZE, TILE_SIZE);

        // Blocky gothic arch shadow framing
        g.setColor(mortar);
        g.drawRect(x, y, TILE_SIZE - 1, TILE_SIZE - 1);
        g.drawLine(x + 4, y + TILE_SIZE, x + 4, y + 12);
        g.drawLine(x + TILE_SIZE - 4, y + TILE_SIZE, x + TILE_SIZE - 4, y + 12);
        g.drawLine(x + 4, y + 12, x + 10, y + 4);
        g.drawLine(x + TILE_SIZE - 4, y + 12, x + TILE_SIZE - 10, y + 4);
        g.drawLine(x + 10, y + 4, x + TILE_SIZE - 10, y + 4); 

        // Highlights
        g.setColor(midStone);
        g.fillRect(x + 6, y + 6, TILE_SIZE - 12, TILE_SIZE - 12);
        g.setColor(highStone);
        g.drawLine(x + 6, y + 6, x + TILE_SIZE - 6, y + 6);
    }

    private void drawDamaged(Graphics2D g, int x, int y, int col, int row) {
        drawWall(g, x, y, col, row); 
        
        // Deep black crack insets
        g.setColor(Color.BLACK);
        g.fillRect(x + 14, y + 4, 4, 6);
        g.fillRect(x + 10, y + 10, 6, 4);
        g.fillRect(x + 8, y + 14, 4, 8);
        g.fillRect(x + 20, y + 18, 6, 4);
        
        // Rubble dust at base
        g.setColor(midStone);
        g.fillRect(x + 6, y + 26, 4, 4);
        g.fillRect(x + 22, y + 28, 6, 2);
        g.setColor(highStone);
        g.fillRect(x + 8, y + 28, 2, 2);
    }

    private void drawPlatform(Graphics2D g, int x, int y, int type) {
        // Base weathered timber
        g.setColor(woodBase);
        g.fillRect(x, y + 4, TILE_SIZE, 12);
        
        // Highlight grain
        g.setColor(woodHigh);
        g.fillRect(x, y + 4, TILE_SIZE, 2);
        g.drawLine(x + 2, y + 8, x + TILE_SIZE - 4, y + 8);
        g.drawLine(x + 6, y + 12, x + TILE_SIZE - 2, y + 12);

        // Shadow under platform
        g.setColor(new Color(40, 25, 10));
        g.fillRect(x, y + 14, TILE_SIZE, 4);

        // Metal caps and nails
        g.setColor(new Color(40, 40, 45)); 
        if (type == TILE_PLATFORM_L || type == TILE_PLATFORM) {
            g.fillRect(x, y + 2, 4, 16);
            g.setColor(new Color(150, 150, 150)); // Nail
            g.fillRect(x + 1, y + 6, 2, 2);
            g.fillRect(x + 1, y + 12, 2, 2);
        }
        if (type == TILE_PLATFORM_R || type == TILE_PLATFORM) {
            g.setColor(new Color(40, 40, 45));
            g.fillRect(x + TILE_SIZE - 4, y + 2, 4, 16);
            g.setColor(new Color(150, 150, 150)); // Nail
            g.fillRect(x + TILE_SIZE - 3, y + 6, 2, 2);
            g.fillRect(x + TILE_SIZE - 3, y + 12, 2, 2);
        }
    }

    private void drawColumn(Graphics2D g, int x, int y) {
        g.setColor(ivory);
        g.fillRect(x + 6, y, TILE_SIZE - 12, TILE_SIZE);
        
        // Fluted ivory shadows
        g.setColor(new Color(140, 125, 90));
        g.drawLine(x + 10, y, x + 10, y + TILE_SIZE);
        g.drawLine(x + 16, y, x + 16, y + TILE_SIZE);
        g.drawLine(x + 22, y, x + 22, y + TILE_SIZE);

        // Flute highlights
        g.setColor(new Color(220, 210, 180));
        g.drawLine(x + 9, y, x + 9, y + TILE_SIZE);
        g.drawLine(x + 15, y, x + 15, y + TILE_SIZE);
        g.drawLine(x + 21, y, x + 21, y + TILE_SIZE);

        // Ornate Caps with pale gold trim
        g.setColor(paleGold);
        g.fillRect(x + 4, y, TILE_SIZE - 8, 3);
        g.fillRect(x + 4, y + TILE_SIZE - 3, TILE_SIZE - 8, 3);
        
        g.setColor(mortar);
        g.drawRect(x + 4, y, TILE_SIZE - 8, TILE_SIZE - 1);
    }

    private void drawStair(Graphics2D g, int x, int y, int col) {
        boolean faceRight = (col % 2 == 0);
        for (int i = 0; i < 4; i++) {
            int stepHeight = 8;
            int stepWidth = (i + 1) * 8;
            int drawX = faceRight ? x : x + TILE_SIZE - stepWidth;
            int drawY = y + i * stepHeight;
            
            g.setColor(darkStone);
            g.fillRect(drawX, drawY, stepWidth, stepHeight);
            
            // Pale gold edge highlight
            g.setColor(paleGold);
            g.fillRect(drawX, drawY, stepWidth, 2);
            
            g.setColor(Color.BLACK);
            g.drawRect(drawX, drawY, stepWidth, stepHeight);
        }
    }

    private void drawRock(Graphics2D g, int x, int y, int col, int row) {
        g.setColor(new Color(20, 20, 25)); // near-black
        g.fillRect(x + 2, y + 4, TILE_SIZE - 4, TILE_SIZE - 4);
        
        // Blocky blue-grey facets
        g.setColor(new Color(50, 60, 80));
        g.fillRect(x + 6, y + 8, 14, 10);
        g.fillRect(x + 12, y + 16, 12, 12);

        g.setColor(ghostBlue);
        g.fillRect(x + 8, y + 10, 8, 6);
        
        // Cold white highlight
        g.setColor(new Color(220, 230, 255));
        g.fillRect(x + 6, y + 8, 8, 2);
        g.fillRect(x + 12, y + 16, 8, 2);
        
        g.setColor(Color.BLACK);
        g.drawRect(x + 2, y + 4, TILE_SIZE - 5, TILE_SIZE - 5);
    }

    private void drawSpikeTrap(Graphics2D g, int x, int y) {
        // Black iron base
        g.setColor(trapBase);
        g.fillRect(x, y + TILE_SIZE - 8, TILE_SIZE, 8);
        g.setColor(mortar);
        g.fillRect(x, y + TILE_SIZE - 8, TILE_SIZE, 2);

        // Silver spikes
        g.setColor(new Color(150, 150, 160));
        int[] spikeX = {4, 16, 28};
        for (int sx : spikeX) {
            g.fillRect(x + sx - 3, y + TILE_SIZE - 12, 6, 4);
            g.fillRect(x + sx - 2, y + TILE_SIZE - 18, 4, 6);
            g.fillRect(x + sx - 1, y + TILE_SIZE - 24, 2, 6);
            
            // Spike highlight
            g.setColor(Color.WHITE);
            g.fillRect(x + sx - 1, y + TILE_SIZE - 24, 1, 10);
            
            // Ghostly blue pixel halo
            g.setColor(ghostBlue);
            g.fillRect(x + sx - 2, y + TILE_SIZE - 25, 1, 1);
            g.fillRect(x + sx, y + TILE_SIZE - 25, 1, 1);
            g.fillRect(x + sx - 1, y + TILE_SIZE - 26, 1, 1);

            g.setColor(new Color(150, 150, 160)); // reset
        }
        
        // Blood red drips
        g.setColor(bloodRed);
        g.fillRect(x + 15, y + TILE_SIZE - 22, 2, 2);
        g.fillRect(x + 16, y + TILE_SIZE - 18, 2, 4);
        g.fillRect(x + 28, y + TILE_SIZE - 20, 2, 2);
        g.fillRect(x + 6, y + TILE_SIZE - 8, 4, 2); 
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