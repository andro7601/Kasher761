package com.game.network;

import com.game.dto.PlayerInputState;

import java.net.SocketAddress;
import java.nio.ByteBuffer;


public class ClientShard {
    private final long Player_ID;

    public volatile SocketAddress Address = null;

    public final PlayerInputState inputState = new PlayerInputState();

    ClientShard(long Id){
        this.Player_ID=Id;
    }

    public long getPlayerId() {
        return Player_ID;
    }

    public void setAddress(SocketAddress Address) {
        this.Address = Address;
    }

    public final ByteBuffer receivePacketIO(ByteBuffer buf) {
        return null;
    }

}
