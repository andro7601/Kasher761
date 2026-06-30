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
    private final String matchId;

    public final ClientShard[] clientShards;

    public UdpSocket(int port, long[] players, String matchId) throws IOException {
        this.port = port;
        this.matchId = matchId;
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
            if (index == -1) {
                // Check if this is an AUTH packet from an unknown or reconnected sender
                if (tempBuf.position() >= 45) { // 1 byte type + 8 bytes long + 36 bytes UUID
                    tempBuf.flip(); // flip to read the received bytes
                    if (tempBuf.remaining() >= 45) {
                        byte packetType = tempBuf.get();
                        if (packetType == 0) {
                            long playerId = tempBuf.getLong();
                            byte[] matchIdBytes = new byte[36];
                            tempBuf.get(matchIdBytes);
                            String incomingMatchId = new String(matchIdBytes, java.nio.charset.StandardCharsets.UTF_8);

                            if (incomingMatchId.equals(this.matchId)) {
                                for (ClientShard shard : clientShards) {
                                    if (shard.getPlayerId() == playerId) {
                                        shard.setAddress(addr);
                                        System.out.println("UDP Auth successful for player " + playerId + " from " + addr);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                continue;
            }
            tempBuf.flip();                                   // position=0, limit=payload length
            tempBuf = clientShards[index].Receive_Packet_IO(tempBuf);
        }
    }

    public void SEND_OUT_PACKETS_IO() throws IOException {
        for(int i=0; i<clientShards.length; ++i) {
            ClientShard clientShard=clientShards[i];
            SocketAddress addr = clientShard.Address;
            if (addr != null) {
                ByteBuffer sendbuf=clientShard.getSendbuf();
                channel.send(sendbuf, addr);
            }
        }
    }

    int find_Index(SocketAddress addr){
        for(int i=0;i<clientShards.length;i++){
            if(clientShards[i].Address != null && clientShards[i].Address.equals(addr)) return i;
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
