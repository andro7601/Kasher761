package com.game.gamerules;

public class WorldBuilder {
    // physics
    public static float vGravity  = 0.5f;
    public static float vJump     = -4.5f;
    public static float vRight    = 2.5f;   // horizontal units/tick
    public static float vBullet   = 8.0f;   // bullet units/tick at spawn

    // world geometry
    public static final float TILE_SIZE     = 32f;  // world units per tile (1 tile = 32×32 px)
    public static final float PLAYER_WIDTH  = 24f;  // collision box width
    public static final float PLAYER_HEIGHT = 48f;  // collision box height; origin is top-left

    // bullet — treated as a single point for hit checks; radius kept for future use
    public static final float BULLET_RADIUS = 4f;

    // tile type constants
    public static final int TILE_AIR  = 0;
    public static final int TILE_WALL = 1;

    // bullet pool / lifetime
    public static final int BULLET_MAX_LIFETIME_TICKS = 60; // 3 sec @ 20 tps
    public static final int MAX_BULLETS_PER_MATCH     = 32;
}
