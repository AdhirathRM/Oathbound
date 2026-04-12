package com.oathbound.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TileMapLoader {

    public static final int TILE_SIZE = 32;

    public static final int TILE_AIR      = 0;
    public static final int TILE_GROUND   = 1;
    public static final int TILE_PLATFORM = 2;
    public static final int TILE_TRAP     = 3;
    public static final int TILE_ROCK     = 4;
    public static final int TILE_VOW      = 9;
    
    public static final int TILE_WALL        = 5; 
    public static final int TILE_COLUMN      = 6; 
    public static final int TILE_PLATFORM_L  = 7; 
    public static final int TILE_PLATFORM_R  = 8; 
    public static final int TILE_PLATFORM_M  = 10;
    public static final int TILE_DAMAGED     = 11;
    public static final int TILE_STAIR_L     = 12; 
    public static final int TILE_STAIR_R     = 13; 

    private int[][] tileGrid;
    private int rows;
    private int cols;

    private final List<Rectangle> solidTiles = new ArrayList<>();
    private final List<Rectangle> trapTiles = new ArrayList<>(); 
    private final List<int[]> vowStonePositions = new ArrayList<>();
    private final List<int[]> enemyPositions = new ArrayList<>();
    private int[] playerSpawn = new int[]{100, 200};

    private final Color darkStone   = new Color(60/255f, 55/255f, 80/255f, 1f);
    private final Color midStone    = new Color(90/255f, 85/255f, 110/255f, 1f);
    private final Color highStone   = new Color(140/255f, 130/255f, 160/255f, 1f);
    private final Color flagstone   = new Color(45/255f, 40/255f, 60/255f, 1f);
    private final Color mortar      = new Color(25/255f, 20/255f, 35/255f, 1f);
    private final Color woodBase    = new Color(80/255f, 55/255f, 25/255f, 1f);
    private final Color woodHigh    = new Color(110/255f, 80/255f, 40/255f, 1f);
    private final Color paleGold    = new Color(200/255f, 180/255f, 80/255f, 1f);
    private final Color ghostBlue   = new Color(80/255f, 120/255f, 200/255f, 1f);
    private final Color ivory       = new Color(180/255f, 165/255f, 120/255f, 1f);
    private final Color trapBase    = new Color(30/255f, 25/255f, 35/255f, 1f);
    private final Color bloodRed    = new Color(160/255f, 20/255f, 20/255f, 1f);
    private final Color black       = Color.BLACK;

    public void load(String resourcePath) {
        solidTiles.clear();
        trapTiles.clear(); 
        vowStonePositions.clear();
        enemyPositions.clear();

        List<int[]> rowList = new ArrayList<>();

        FileHandle file = Gdx.files.internal(resourcePath);
        if (!file.exists()) return;

        try (BufferedReader br = file.reader(8192)) {
            String line;
            int row = 0;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) continue;

                line = line.replace(" ", "").trim();
                int[] tileRow = new int[line.length()];

                for (int col = 0; col < line.length(); col++) {
                    char token = line.charAt(col);
                    int tileId = parseToken(token, row, col);
                    tileRow[col] = tileId;

                    if (tileId == TILE_GROUND || tileId == TILE_PLATFORM || tileId == TILE_ROCK
                     || tileId == TILE_WALL   || tileId == TILE_COLUMN   || tileId == TILE_DAMAGED
                     || tileId == TILE_STAIR_L || tileId == TILE_STAIR_R || tileId == TILE_PLATFORM_L
                     || tileId == TILE_PLATFORM_M || tileId == TILE_PLATFORM_R) {
                        solidTiles.add(new Rectangle(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE));
                    } else if (tileId == TILE_TRAP) {
                        trapTiles.add(new Rectangle(col * TILE_SIZE, row * TILE_SIZE + (TILE_SIZE / 2), TILE_SIZE, TILE_SIZE / 2));
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

    public void render(ShapeRenderer sr) {
        if (tileGrid == null) return;
        
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int id = tileGrid[row][col];
                if (id == TILE_AIR) continue;

                int x = col * TILE_SIZE;
                int y = row * TILE_SIZE;

                switch (id) {
                    case TILE_GROUND:
                        boolean isTop = row == 0 || tileGrid[row - 1][col] != TILE_GROUND;
                        drawGroundTile(sr, x, y, col, row, isTop);
                        break;
                    case TILE_TRAP:
                        drawSpikeTrap(sr, x, y);
                        break;
                    case TILE_PLATFORM:
                    case TILE_PLATFORM_L:
                    case TILE_PLATFORM_M:
                    case TILE_PLATFORM_R:
                        drawPlatform(sr, x, y, id);
                        break;
                    case TILE_ROCK:
                        drawRock(sr, x, y);
                        break;
                    case TILE_WALL:
                        drawWall(sr, x, y);
                        break;
                    case TILE_COLUMN:
                        drawColumn(sr, x, y);
                        break;
                    case TILE_DAMAGED:
                        drawDamaged(sr, x, y);
                        break;
                    case TILE_STAIR_L:
                        drawStair(sr, x, y, false);
                        break;
                    case TILE_STAIR_R:
                        drawStair(sr, x, y, true);
                        break;
                }
            }
        }
    }

    private void drawOutline(ShapeRenderer sr, int x, int y, int w, int h) {
        sr.rect(x, y, w, 1);
        sr.rect(x, y + h - 1, w, 1);
        sr.rect(x, y, 1, h);
        sr.rect(x + w - 1, y, 1, h);
    }

    private void drawLine(ShapeRenderer sr, int x1, int y1, int x2, int y2) {
        sr.rectLine(x1, y1, x2, y2, 1);
    }

    private void drawGroundTile(ShapeRenderer sr, int x, int y, int col, int row, boolean isTop) {
        sr.setColor(flagstone);
        sr.rect(x, y, TILE_SIZE, TILE_SIZE);
        sr.setColor(90/255f, 40/255f, 130/255f, 90/255f); 
        sr.rect(x, y + TILE_SIZE - 10, TILE_SIZE, 10);
        sr.setColor(mortar);
        int offset = (row % 2 == 0) ? 0 : TILE_SIZE / 2;
        drawLine(sr, x, y + TILE_SIZE / 2, x + TILE_SIZE, y + TILE_SIZE / 2); 
        drawLine(sr, x + offset, y, x + offset, y + TILE_SIZE / 2); 
        int offBot = (offset == 0) ? TILE_SIZE / 2 : 0;
        drawLine(sr, x + offBot, y + TILE_SIZE / 2, x + offBot, y + TILE_SIZE); 
        if (isTop) {
            sr.setColor(paleGold);
            sr.rect(x, y, TILE_SIZE, 2);
            sr.setColor(150/255f, 130/255f, 60/255f, 1f);
            sr.rect(x, y + 2, TILE_SIZE, 2);
        } else {
            sr.setColor(mortar);
            drawLine(sr, x, y, x + TILE_SIZE, y);
        }
        sr.setColor(black);
        drawOutline(sr, x, y, TILE_SIZE - 1, TILE_SIZE - 1);
    }

    private void drawWall(ShapeRenderer sr, int x, int y) {
        sr.setColor(darkStone);
        sr.rect(x, y, TILE_SIZE, TILE_SIZE);
        sr.setColor(90/255f, 40/255f, 130/255f, 60/255f);
        sr.rect(x, y, TILE_SIZE, TILE_SIZE);
        sr.setColor(mortar);
        drawOutline(sr, x, y, TILE_SIZE - 1, TILE_SIZE - 1);
        drawLine(sr, x + 4, y + TILE_SIZE, x + 4, y + 12);
        drawLine(sr, x + TILE_SIZE - 4, y + TILE_SIZE, x + TILE_SIZE - 4, y + 12);
        drawLine(sr, x + 4, y + 12, x + 10, y + 4);
        drawLine(sr, x + TILE_SIZE - 4, y + 12, x + TILE_SIZE - 10, y + 4);
        drawLine(sr, x + 10, y + 4, x + TILE_SIZE - 10, y + 4); 
        sr.setColor(midStone);
        sr.rect(x + 6, y + 6, TILE_SIZE - 12, TILE_SIZE - 12);
        sr.setColor(highStone);
        drawLine(sr, x + 6, y + 6, x + TILE_SIZE - 6, y + 6);
    }

    private void drawDamaged(ShapeRenderer sr, int x, int y) {
        drawWall(sr, x, y); 
        sr.setColor(black);
        sr.rect(x + 14, y + 4, 4, 6);
        sr.rect(x + 10, y + 10, 6, 4);
        sr.rect(x + 8, y + 14, 4, 8);
        sr.rect(x + 20, y + 18, 6, 4);
        sr.setColor(midStone);
        sr.rect(x + 6, y + 26, 4, 4);
        sr.rect(x + 22, y + 28, 6, 2);
        sr.setColor(highStone);
        sr.rect(x + 8, y + 28, 2, 2);
    }

    private void drawPlatform(ShapeRenderer sr, int x, int y, int type) {
        sr.setColor(woodBase);
        sr.rect(x, y + 4, TILE_SIZE, 12);
        sr.setColor(woodHigh);
        sr.rect(x, y + 4, TILE_SIZE, 2);
        drawLine(sr, x + 2, y + 8, x + TILE_SIZE - 4, y + 8);
        drawLine(sr, x + 6, y + 12, x + TILE_SIZE - 2, y + 12);
        sr.setColor(40/255f, 25/255f, 10/255f, 1f);
        sr.rect(x, y + 14, TILE_SIZE, 4);
        if (type == TILE_PLATFORM_L || type == TILE_PLATFORM) {
            sr.setColor(40/255f, 40/255f, 45/255f, 1f);
            sr.rect(x, y + 2, 4, 16);
            sr.setColor(150/255f, 150/255f, 150/255f, 1f);
            sr.rect(x + 1, y + 6, 2, 2);
            sr.rect(x + 1, y + 12, 2, 2);
        }
        if (type == TILE_PLATFORM_R || type == TILE_PLATFORM) {
            sr.setColor(40/255f, 40/255f, 45/255f, 1f);
            sr.rect(x + TILE_SIZE - 4, y + 2, 4, 16);
            sr.setColor(150/255f, 150/255f, 150/255f, 1f);
            sr.rect(x + TILE_SIZE - 3, y + 6, 2, 2);
            sr.rect(x + TILE_SIZE - 3, y + 12, 2, 2);
        }
    }

    private void drawColumn(ShapeRenderer sr, int x, int y) {
        sr.setColor(ivory);
        sr.rect(x + 6, y, TILE_SIZE - 12, TILE_SIZE);
        sr.setColor(140/255f, 125/255f, 90/255f, 1f);
        drawLine(sr, x + 10, y, x + 10, y + TILE_SIZE);
        drawLine(sr, x + 16, y, x + 16, y + TILE_SIZE);
        drawLine(sr, x + 22, y, x + 22, y + TILE_SIZE);
        sr.setColor(220/255f, 210/255f, 180/255f, 1f);
        drawLine(sr, x + 9, y, x + 9, y + TILE_SIZE);
        drawLine(sr, x + 15, y, x + 15, y + TILE_SIZE);
        drawLine(sr, x + 21, y, x + 21, y + TILE_SIZE);
        sr.setColor(paleGold);
        sr.rect(x + 4, y, TILE_SIZE - 8, 3);
        sr.rect(x + 4, y + TILE_SIZE - 3, TILE_SIZE - 8, 3);
        sr.setColor(mortar);
        drawOutline(sr, x + 4, y, TILE_SIZE - 8, TILE_SIZE - 1);
    }

    private void drawStair(ShapeRenderer sr, int x, int y, boolean faceRight) {
        for (int i = 0; i < 4; i++) {
            int stepHeight = 8;
            int stepWidth = (i + 1) * 8;
            int drawX = faceRight ? x : x + TILE_SIZE - stepWidth;
            int drawY = y + i * stepHeight;
            sr.setColor(darkStone);
            sr.rect(drawX, drawY, stepWidth, stepHeight);
            sr.setColor(paleGold);
            sr.rect(drawX, drawY, stepWidth, 2);
            sr.setColor(black);
            drawOutline(sr, drawX, drawY, stepWidth, stepHeight);
        }
    }

    private void drawRock(ShapeRenderer sr, int x, int y) {
        sr.setColor(20/255f, 20/255f, 25/255f, 1f);
        sr.rect(x + 2, y + 4, TILE_SIZE - 4, TILE_SIZE - 4);
        sr.setColor(50/255f, 60/255f, 80/255f, 1f);
        sr.rect(x + 6, y + 8, 14, 10);
        sr.rect(x + 12, y + 16, 12, 12);
        sr.setColor(ghostBlue);
        sr.rect(x + 8, y + 10, 8, 6);
        sr.setColor(220/255f, 230/255f, 255/255f, 1f);
        sr.rect(x + 6, y + 8, 8, 2);
        sr.rect(x + 12, y + 16, 8, 2);
        sr.setColor(black);
        drawOutline(sr, x + 2, y + 4, TILE_SIZE - 5, TILE_SIZE - 5);
    }

    private void drawSpikeTrap(ShapeRenderer sr, int x, int y) {
        sr.setColor(trapBase);
        sr.rect(x, y + TILE_SIZE - 8, TILE_SIZE, 8);
        sr.setColor(mortar);
        sr.rect(x, y + TILE_SIZE - 8, TILE_SIZE, 2);
        int[] spikeX = {4, 16, 28};
        for (int sx : spikeX) {
            sr.setColor(150/255f, 150/255f, 160/255f, 1f);
            sr.rect(x + sx - 3, y + TILE_SIZE - 12, 6, 4);
            sr.rect(x + sx - 2, y + TILE_SIZE - 18, 4, 6);
            sr.rect(x + sx - 1, y + TILE_SIZE - 24, 2, 6);
            sr.setColor(Color.WHITE);
            sr.rect(x + sx - 1, y + TILE_SIZE - 24, 1, 10);
            sr.setColor(ghostBlue);
            sr.rect(x + sx - 2, y + TILE_SIZE - 25, 1, 1);
            sr.rect(x + sx, y + TILE_SIZE - 25, 1, 1);
            sr.rect(x + sx - 1, y + TILE_SIZE - 26, 1, 1);
        }
        sr.setColor(bloodRed);
        sr.rect(x + 15, y + TILE_SIZE - 22, 2, 2);
        sr.rect(x + 16, y + TILE_SIZE - 18, 2, 4);
        sr.rect(x + 28, y + TILE_SIZE - 20, 2, 2);
        sr.rect(x + 6, y + TILE_SIZE - 8, 4, 2); 
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
            case 'S': return TILE_STAIR_L;
            case 's': return TILE_STAIR_R;
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

    public List<Rectangle> getSolidTiles() { return Collections.unmodifiableList(solidTiles); }
    public List<Rectangle> getTrapTiles() { return Collections.unmodifiableList(trapTiles); }
    public List<int[]> getVowStonePositions() { return Collections.unmodifiableList(vowStonePositions); }
    public List<int[]> getEnemyPositions() { return enemyPositions; }
    public int[] getPlayerSpawn() { return playerSpawn; }
}