package com.game.dto;

public class Match_Snapshot {
    public long globalTick;
    public int playerCount;
    public ActivePlayer[] activePlayers = new ActivePlayer[playerCount];
}
