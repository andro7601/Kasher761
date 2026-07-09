package com.game.dto;

public class MatchSnapshot {
    public long localTick;//sequential and corresponds to players udp packets which allignes with which
    public int playerCount;
    public ActivePlayer[] activePlayers;
    public int[] Tiles;
    public ActiveBullet[] bullets;

    public void copyFromLastSnapshot(MatchSnapshot prevSnapshot){
        this.localTick = prevSnapshot.localTick+1;
        for(int i = 0; i < playerCount; ++i){
            this.activePlayers[i].copyFromLastSnapshot(prevSnapshot.activePlayers[i]);
        }
        System.arraycopy(prevSnapshot.Tiles, 0, Tiles, 0, Tiles.length);
        for(int i = 0; i < bullets.length; ++i){
            this.bullets[i].copyFrom(prevSnapshot.bullets[i]);
        }
    }
}
