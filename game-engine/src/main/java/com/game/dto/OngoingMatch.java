package com.game.dto;

import com.game.network.UdpSocket;

import java.util.Map;
import java.util.UUID;

public class OngoingMatch {

    private int MAX_AMOUNT_OF_INGOING_SNAPSHOTS = 10;

    public long startTick = Long.MAX_VALUE;//this is referencing the global tick rate/just defaulting to max to skip

    public MatchSnapshot[] snapshots = new MatchSnapshot[MAX_AMOUNT_OF_INGOING_SNAPSHOTS];

    private volatile boolean active = false;

    public volatile UdpSocket socket;

    public boolean active() {
        return active;
    }

    public int[] startingTiles;

    public int PlayerCount;

    public void activate(int port, long startTick, Map<UUID,Long> UuidToPlayerId, String matchId,ModeInfo modeInfo) {
        try {
            this.socket = new UdpSocket(port, UuidToPlayerId,matchId);
            this.startTick = startTick;
            this.active = true;
            System.out.println("Match activated on UDP port " + port
                    + ", starts at tick " + startTick);
        } catch (Exception e) {
            System.err.println("FATAL: Failed to bind UDP port " + port);
            e.printStackTrace();
        }
        this.PlayerCount = modeInfo.playerCount();
        this.startingTiles = modeInfo.tiles();
        for(int i=0;i<10;i++){
            snapshots[i] = new MatchSnapshot();
            snapshots[i].playerCount = this.PlayerCount;
            snapshots[i].activePlayers=new ActivePlayer[PlayerCount];
            if(i==0)continue;
            for(int j=0;j<PlayerCount;j++){
                snapshots[i].activePlayers[j]=new ActivePlayer();
            }
        }

        MatchSnapshot firstSnapshot = snapshots[0];
        firstSnapshot.Tiles = startingTiles;
        firstSnapshot.localTick=0;
        ActivePlayer[] activePlayers = firstSnapshot.activePlayers;
        long[] playerIds = UuidToPlayerId.values().stream()
                .mapToLong(Long::longValue)
                .toArray();
        for (int i = 0; i < firstSnapshot.playerCount; i++) {
            activePlayers[i] = new ActivePlayer(
                    playerIds[i],
                    (short)100,
                    (float)modeInfo.spawnPoints()[2*i],
                    (float)modeInfo.spawnPoints()[2*i+1],
                    (float)3,
                    (short)0,
                    null
                    );
        }

    }

    public void deactivate() {
        this.active = false;
        this.startTick = Long.MAX_VALUE;
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }
}
