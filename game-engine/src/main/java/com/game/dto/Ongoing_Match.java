package com.game.dto;

import com.game.network.UdpSocket;

public class Ongoing_Match {

    private int MAX_AMOUNT_OF_INGOING_SNAPSHOTS = 10;

    public long startTick = Long.MAX_VALUE;

    public Match_Snapshot[] snapshots = new Match_Snapshot[MAX_AMOUNT_OF_INGOING_SNAPSHOTS];

    private volatile boolean active = false;

    public volatile UdpSocket socket;

    public boolean active() {
        return active;
    }

    public void activate(int port, long startTick, long[] players) {
        try {
            this.socket = new UdpSocket(port, players);
            this.startTick = startTick;
            this.active = true;
            System.out.println("Match activated on UDP port " + port
                    + ", starts at tick " + startTick);
        } catch (Exception e) {
            System.err.println("FATAL: Failed to bind UDP port " + port);
            e.printStackTrace();
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
