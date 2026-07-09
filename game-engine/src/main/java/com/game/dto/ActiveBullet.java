package com.game.dto;

public class ActiveBullet {
    public boolean alive;
    public float x, y;
    public float vx, vy;
    public long ownerPlayerId;
    public int ageTicks;

    public void copyFrom(ActiveBullet other) {
        this.alive         = other.alive;
        this.x             = other.x;
        this.y             = other.y;
        this.vx            = other.vx;
        this.vy            = other.vy;
        this.ownerPlayerId = other.ownerPlayerId;
        this.ageTicks      = other.ageTicks;
    }

    public void reset() {
        this.alive    = false;
        this.ageTicks = 0;
    }
}
