package com.game.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public final class UdpSocket {


    public static final int MAX_PACKET_SIZE = 1400;

    private ByteBuffer tempBuf = ByteBuffer.allocateDirect(MAX_PACKET_SIZE);

    private final DatagramChannel channel;
    private final int port;


    public final ClientShard[] clientShards;



    public UdpSocket(int port, long[] players) throws IOException {
        this.port = port;
        this.channel = DatagramChannel.open();
        this.channel.configureBlocking(false);
        this.channel.setOption(java.net.StandardSocketOptions.SO_RCVBUF, 2 * 1024 * 1024);
        this.channel.bind(new InetSocketAddress("0.0.0.0", port));

        this.clientShards = new ClientShard[players.length];
        for(int i=0; i<players.length; ++i) {
            this.clientShards[i]=new ClientShard(players[i]);
        }

    }

    public void Empty_OS_BUFFER_IO() {
        while (true) {
            tempBuf.clear();                                  // always reset before receiving
            SocketAddress addr;
            try { addr = channel.receive(tempBuf); } catch (IOException e) { return; }
            if (addr == null) return;                         // nothing left this tick
            int index = find_Index(addr);
            if (index == -1) continue;                        // unknown sender — drop, keep draining
            tempBuf.flip();                                   // position=0, limit=payload length
            tempBuf = clientShards[index].Receive_Packet_IO(tempBuf);
        }
    }

    public void SEND_OUT_PACKETS_IO()throws IOException {
        for(int i=0; i<clientShards.length; ++i) {
            ClientShard clientShard=clientShards[i];
            ByteBuffer sendbuf=clientShard.getSendbuf();
            channel.send(sendbuf,clientShard.Address);
        }
    }

    int find_Index(SocketAddress addr){
        for(int i=0;i<clientShards.length;i++){
            if(clientShards[i].Address.equals(addr)) return i;
        }
        return -1;
    }

    public void close() {
        try {
            channel.close();
        } catch (IOException ignored) {}
    }

    @Override
    public String toString() {
        return "UdpSocket[: " + port + " ]";
    }
}
