package com.game.dto;

public class ActivePlayer {
    public long playerId;
    public float x,y;
    public float vx, vy;
    public float angleRad;
    public short ability;
    public short hp;
    public GunType type;
    public int team;
    public long lastProcessedTick; // the server tick this player's last applied input was stamped with
    // what order client sent them if its behind this just drop

    public ActivePlayer(long playerId, short hp, float x, float y, float angleRad, short ability, GunType type) {
        this.hp = hp;
        this.x = x;
        this.y = y;
        this.angleRad = angleRad;
        this.ability = ability;
        this.type = type; // null is valid here — player hasn't picked up a gun yet
    }
    public ActivePlayer() {}

    private byte[] lastInputInfo; // if engine didn't receive the packets it'll just use this
    public short lastInputUsedInRow;

    public boolean canUseLastInput() {
        return lastInputUsedInRow < 10;
    }

    public byte[] onNewPacketArrived() {
        lastInputUsedInRow = 0;
        return lastInputInfo;
    } // caller overwrites this buffer in place with fresh bytes — no new array, no GC pressure

    public byte[] useLastInput() {
        lastInputUsedInRow++; // one more tick relying on stale input
        return lastInputInfo;
    } // reapply the same stale bytes again, after checking canUseLastInput()

    public void pickUpGun(GunType gun) {
        type = gun;
    }

    public void turnAround(short radInShort) {
        float angle = (radInShort / (float) Short.MAX_VALUE) * (float) Math.PI;
        angleRad = angle;
    }

    public boolean hasAbility() {
        return ability > 100;
    }

    public void moveTo(float x, float y) {
        this.x = x;
        this.y = y;
    } // ideally called after engine resolves position from inputs; also used at match start

    public void copyFromLastSnapshot(ActivePlayer other) {
        this.playerId = other.playerId;
        this.x = other.x; this.y = other.y;
        this.vx = other.vx; this.vy = other.vy;
        this.angleRad = other.angleRad;
        this.ability = other.ability;
        this.hp = other.hp;
        this.type = other.type;
        this.team = other.team;
        this.lastProcessedTick = other.lastProcessedTick;
        this.lastInputInfo = other.lastInputInfo;
        this.lastInputUsedInRow = other.lastInputUsedInRow;
    }

}