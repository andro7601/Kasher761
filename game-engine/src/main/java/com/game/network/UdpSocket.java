package com.game.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import com.game.GameRoomManager.Player;

public final class UdpSocket {


    public static final int MAX_PACKET_SIZE = 1400;
    private static final int MAX_PLAYER_COUNT=100;
    private static final int MAX_PACKET_COUNT=320;

    private ByteBuffer tempBuf = ByteBuffer.allocate(MAX_PACKET_SIZE);

    private final DatagramChannel channel;
    private final int port;


    public final ClientShard[] clientShards;
    public final SocketAddress[] clientAddresses;



    public UdpSocket(int port, Player[] players) throws IOException {
        this.port = port;
        this.channel = DatagramChannel.open();
        this.channel.configureBlocking(false);
        this.channel.setOption(java.net.StandardSocketOptions.SO_RCVBUF, 2 * 1024 * 1024);
        this.channel.bind(new InetSocketAddress("0.0.0.0", port));

        this.clientShards = new ClientShard[players.length];
        this.clientAddresses = new SocketAddress[players.length];
        for(int i=0; i<players.length; ++i) {
            this.clientAddresses[i]=players[i].addrs();
            this.clientShards[i]=new ClientShard(players[i].id());

        }

    }

    public void Empty_OS_BUFFER_IO() {
        SocketAddress addr;
        try { addr = channel.receive(tempBuf); } catch (IOException e) { return ; }
        if (addr == null) return ;
        int index=find_Index(addr);
        if(index==-1) return ;
        tempBuf.clear();
        clientShards[index].Receive_Packet_IO(tempBuf);
    }

    public void SEND_OUT_PACKETS_IO()throws IOException {
        for(int i=0; i<clientShards.length; ++i) {
            ClientShard clientShard=clientShards[i];
            ByteBuffer sendbuf=clientShard.getSendbuf();
            channel.send(sendbuf,clientShard.Address);
        }
    }

    int find_Index(SocketAddress addr){
        for(int i=0;i<clientAddresses.length;i++){
            if(clientAddresses[i].equals(addr)) return i;
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
