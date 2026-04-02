package com.oathbound.state;

/**
 * PB-005 — GameState Enum
 *
 * Controls which branch of the game loop is currently active.
 * Transitions: MENU → PLAY → BOSS
 */
public enum GameState {

    /** The main menu screen is displayed. */
    MENU,

    /** Platformer gameplay (Levels 1–9) is active. */
    PLAY,

    /** Turn-based boss battle (Level 10) is active. */
    BOSS
}
