package com.game.dto;

public class MatchSnapshot {
    public long localTick;//sequential and corresponds to players udp packets which allignes with which
    public int playerCount;
    public ActivePlayer[] activePlayers;
    public int[] Tiles;
    public void copyFromLastSnapshot(MatchSnapshot prevSnapshot){
        this.localTick = prevSnapshot.localTick+1;
        for(int i = 0; i < playerCount; ++i){
            this.activePlayers[i].copyFromLastSnapshot(prevSnapshot.activePlayers[i]);
        }
        this.Tiles = prevSnapshot.Tiles;
    }
}
