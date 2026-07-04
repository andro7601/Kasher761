package com.game.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.*;


public final class UdpSocket {


    public static final int MAX_PACKET_SIZE = 1400;

    private ByteBuffer tempBuf = ByteBuffer.allocateDirect(MAX_PACKET_SIZE);

    private final DatagramChannel channel;
    private final int port;
    private final String matchId;
    private final Map<UUID, Long> UUID_TO_PLAYER_ID;
    private final int PlayerCount;

    public final ClientShard[] clientShards;

    public UdpSocket(int port, Map<UUID,Long> UuidToPlayerId, String matchId) throws IOException {
        this.port = port;
        this.matchId = matchId;
        this.channel = DatagramChannel.open();
        this.channel.configureBlocking(false);
        this.channel.setOption(java.net.StandardSocketOptions.SO_RCVBUF, 2 * 1024 * 1024);
        this.channel.bind(new InetSocketAddress("0.0.0.0", port));
        this.PlayerCount = UuidToPlayerId.size();
        this.UUID_TO_PLAYER_ID=UuidToPlayerId;


        this.clientShards = new ClientShard[PlayerCount];
        List<Long> PlayerIds = new ArrayList<>(UuidToPlayerId.values());
        for (int i = 0; i < PlayerCount; ++i) {

            this.clientShards[i] = new ClientShard(PlayerIds.get(i));
        }
    }

    public void Empty_OS_BUFFER_IO() {
        while (true) {
            tempBuf.clear();
            SocketAddress addr;
            try {
                addr = channel.receive(tempBuf);
            } catch (IOException e) {
                return;
            }
            if (addr == null) return;

            tempBuf.flip();
            if (!tempBuf.hasRemaining()) continue;

            byte firstbyte = tempBuf.get();
            PacketType TYPE = PacketType.fromByte(firstbyte);

            switch (TYPE) {
                case AUTH:
                    if (tempBuf.remaining() < 16) continue;

                    long first8bits = tempBuf.getLong();
                    long second8bits = tempBuf.getLong();
                    UUID key = new UUID(first8bits, second8bits);

                    Long playerId = UUID_TO_PLAYER_ID.get(key);
                    if (playerId == null) continue;

                    int authIndex = find_Index(playerId);
                    if (authIndex == -1) continue;

                    clientShards[authIndex].setAddress(addr);

                    continue;

                case INPUT:
                    int inputIndex = find_Index(addr);

                    if (inputIndex == -1) continue;

                    tempBuf = clientShards[inputIndex].Receive_Packet_IO(tempBuf);

                    continue;

                case UNKNOWN:continue;
                default:
                    continue;
            }
        }
    }

    public void SEND_OUT_PACKETS_IO() throws IOException {
        for (int i = 0; i < clientShards.length; ++i) {
            ClientShard clientShard = clientShards[i];
            SocketAddress addr = clientShard.Address;
            if (addr != null) {
                ByteBuffer sendbuf = clientShard.getSendbuf();
                channel.send(sendbuf, addr);
            }
        }
    }

    int find_Index(SocketAddress addr) {
        for (int i = 0; i < clientShards.length; i++) {
            if (clientShards[i].Address != null && clientShards[i].Address.equals(addr)) return i;
        }
        return -1;
    }

    int find_Index(long playerId) {
        for (int i = 0; i < clientShards.length; i++) {
            if (clientShards[i].getPlayerId() == playerId) return i;
        }
        return -1;
    }

    public void close() {
        try {
            channel.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public String toString() {
        return "UdpSocket[: " + port + " ]";
    }

    enum PacketType {
        AUTH(0),
        INPUT(1),
        UNKNOWN(-1);

        public final byte code;
        private static final PacketType[] LOOKUP = new PacketType[256];

        static {
            for (PacketType type : values()) {
                if (type.code >= 0) {
                    LOOKUP[type.code] = type;
                }
            }
        }

        PacketType(int code) {
            this.code = (byte) code;
        }

        public static PacketType fromByte(byte b) {
            int index = b & 0xFF;
            PacketType type = LOOKUP[index];
            return type != null ? type : UNKNOWN;
        }
    }

}