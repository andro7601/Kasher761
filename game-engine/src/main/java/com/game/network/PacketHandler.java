package com.game.network;

import com.game.dto.Match_Snapshot;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class PacketHandler {

    static void handle(ByteBuffer buf) {

    }

    public static void Snapshot_To_Buffer(Match_Snapshot snapshot,ByteBuffer buf) {
        buf.putLong(snapshot.globalTick);
        buf.putInt(snapshot.playerCount);
        for (int i = 0; i < snapshot.playerCount; i++) {
            buf.putInt(snapshot.activePlayers[i].x);
            buf.putInt(snapshot.activePlayers[i].y);
        }
        buf.flip();
    }



    void Load(){

    }
}
