package com.oathbound.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * PB-004 — Tile-Map Loader
 *
 * Parses a .txt level file from the res/levels/ directory and produces:
 *  - A 2D tile-ID grid (for rendering)
 *  - A flat list of solid collision rectangles (for PB-006)
 *  - A list of Vow Stone spawn positions (for PB-020)
 *
 * Tile key (matches level_test.txt):
 *   0  = air       — no collision, not rendered
 *   1  = ground    — solid, rendered dark grey
 *   2  = platform  — solid, rendered brown (one-way in future sprints)
 *   V  = Vow Stone — marks a checkpoint spawn; treated as air for collision
 *
 * Acceptance criteria (PB-004):
 *   - Loader parses a text file
 *   - Maps tile IDs to colours (sprites swapped in later)
 *   - Renders correct tile grid
 *   - No hard-coded coordinates
 */
public class TileMapLoader {

    // ── Constants ────────────────────────────────────────────────────────────

    /** Pixel size of one tile. */
    public static final int TILE_SIZE = 32;

    // Tile ID constants
    public static final int TILE_AIR      = 0;
    public static final int TILE_GROUND   = 1;
    public static final int TILE_PLATFORM = 2;
    public static final int TILE_VOW      = 9; // internal ID for 'V' tokens

    // Placeholder colours — replaced by sprites in a later sprint
    private static final Color COLOR_GROUND   = new Color(60,  60,  60);
    private static final Color COLOR_PLATFORM = new Color(139, 90,  43);
    private static final Color COLOR_VOW      = new Color(100, 180, 255);

    // ── Fields ───────────────────────────────────────────────────────────────

    /** Raw grid of tile IDs. [row][col] */
    private int[][] tileGrid;

    private int rows;
    private int cols;

    /** Solid rectangles used by the collision system (PB-006). */
    private final List<Rectangle> solidTiles = new ArrayList<>();

    /** World-space positions of Vow Stone spawns (PB-020). */
    private final List<int[]> vowStonePositions = new ArrayList<>();

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Loads and parses a level file from the classpath.
     *
     * @param resourcePath path relative to classpath root,
     *                     e.g. "/levels/level_test.txt"
     * @throws RuntimeException if the file cannot be read or parsed
     */
    public void load(String resourcePath) {
        solidTiles.clear();
        vowStonePositions.clear();

        List<int[]> rowList = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(resourcePath);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            String line;
            int row = 0;

            while ((line = br.readLine()) != null) {
                // Skip comment lines
                if (line.startsWith("#") || line.isBlank()) continue;

                String[] tokens = line.trim().split("\\s+");
                int[] tileRow = new int[tokens.length];

                for (int col = 0; col < tokens.length; col++) {
                    int tileId = parseToken(tokens[col]);
                    tileRow[col] = tileId;

                    // Build collision rectangles for solid tiles
                    if (tileId == TILE_GROUND || tileId == TILE_PLATFORM) {
                        solidTiles.add(new Rectangle(
                                col  * TILE_SIZE,
                                row  * TILE_SIZE,
                                TILE_SIZE,
                                TILE_SIZE
                        ));
                    }

                    // Record Vow Stone world positions
                    if (tileId == TILE_VOW) {
                        vowStonePositions.add(new int[]{
                                col * TILE_SIZE,
                                row * TILE_SIZE
                        });
                    }
                }

                rowList.add(tileRow);
                row++;
            }

        } catch (Exception e) {
            throw new RuntimeException("TileMapLoader: failed to load " + resourcePath, e);
        }

        // Convert list → 2D array
        rows = rowList.size();
        cols = rowList.isEmpty() ? 0 : rowList.get(0).length;
        tileGrid = rowList.toArray(new int[0][]);

        System.out.printf("[TileMapLoader] Loaded %s — %d rows × %d cols, " +
                          "%d solid tiles, %d Vow Stones%n",
                resourcePath, rows, cols,
                solidTiles.size(), vowStonePositions.size());
    }

    /**
     * Renders the tile grid.
     * Air tiles are skipped (background shows through).
     * Sprites should replace the colour fills in a later sprint.
     *
     * @param g   the Graphics2D context provided by GamePanel
     */
    public void render(Graphics2D g) {
        if (tileGrid == null) return;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int id = tileGrid[row][col];

                Color fill = colorForTile(id);
                if (fill == null) continue; // TILE_AIR — skip

                int x = col * TILE_SIZE;
                int y = row * TILE_SIZE;

                g.setColor(fill);
                g.fillRect(x, y, TILE_SIZE, TILE_SIZE);

                // Subtle border so individual tiles are visible
                g.setColor(fill.darker());
                g.drawRect(x, y, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    /** @return unmodifiable view of solid collision rectangles (used by PB-006). */
    public List<Rectangle> getSolidTiles() {
        return java.util.Collections.unmodifiableList(solidTiles);
    }

    /** @return list of [x, y] world positions for each Vow Stone (used by PB-020). */
    public List<int[]> getVowStonePositions() {
        return java.util.Collections.unmodifiableList(vowStonePositions);
    }

    public int getRows()     { return rows; }
    public int getCols()     { return cols; }
    public int getTileId(int row, int col) { return tileGrid[row][col]; }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Converts a string token from the level file to an integer tile ID.
     * 'V' → TILE_VOW; numeric strings parsed normally; unknown → TILE_AIR.
     */
    private int parseToken(String token) {
        if (token.equalsIgnoreCase("V")) return TILE_VOW;
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            System.err.println("[TileMapLoader] Unknown token '" + token + "' — treated as air.");
            return TILE_AIR;
        }
    }

    /** Maps a tile ID to its placeholder fill colour. Returns null for air. */
    private Color colorForTile(int id) {
        switch (id) {
            case TILE_GROUND:   return COLOR_GROUND;
            case TILE_PLATFORM: return COLOR_PLATFORM;
            case TILE_VOW:      return COLOR_VOW;
            default:            return null; // air
        }
    }
}