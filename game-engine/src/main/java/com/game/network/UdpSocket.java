package com.game.network;

import com.game.dto.PlayerInputState;

import static com.game.network.PacketParsing.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.*;


public final class UdpSocket {


    public static final int MAX_PACKET_SIZE = 1400;

    private ByteBuffer tempBuf  = ByteBuffer.allocateDirect(MAX_PACKET_SIZE);
    /** Pre-allocated outgoing buffer — fill, then pass to sendToAll(). Never used concurrently. */
    public  final ByteBuffer sendBuf = ByteBuffer.allocateDirect(MAX_PACKET_SIZE);

    private final DatagramChannel channel;
    private final int port;
    private final String matchId;
    private final Map<UUID, Long> UUID_TO_PLAYER_ID;
    private final int PlayerCount;

    public final ClientShard[] clientShards;

    public UdpSocket(int port, Map<UUID, Long> UuidToPlayerId, String matchId) throws IOException {
        this.port = port;
        this.matchId = matchId;
        this.channel = DatagramChannel.open();
        this.channel.configureBlocking(false);
        this.channel.setOption(java.net.StandardSocketOptions.SO_RCVBUF, 2 * 1024 * 1024);
        this.channel.bind(new InetSocketAddress("0.0.0.0", port));
        this.PlayerCount = UuidToPlayerId.size();
        this.UUID_TO_PLAYER_ID = UuidToPlayerId;


        this.clientShards = new ClientShard[PlayerCount];
        List<Long> PlayerIds = new ArrayList<>(UuidToPlayerId.values());
        for (int i = 0; i < PlayerCount; ++i) {

            this.clientShards[i] = new ClientShard(PlayerIds.get(i));
        }
    }

    // UdpSocket.Empty_OS_BUFFER_IO — parses inline, no ring involved
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


            PacketType type = PacketType.fromByte(tempBuf.get());
            switch (type) {
                case AUTH -> {
                    long first = tempBuf.getLong();
                    long second = tempBuf.getLong();
                    UUID uuid = new UUID(first, second);
                    Long id = UUID_TO_PLAYER_ID.get(uuid);
                    if (id == null) continue;
                    int idx = find_Index(id);
                    if (idx == -1) continue; // defensive: id valid but no matching shard somehow
                    clientShards[idx].setAddress(addr);

                }
                case INPUT -> {

                    int index = find_Index(addr);
                    if(index==-1) continue;

                    long tick = tempBuf.getLong();
                    byte MovementBitMask = tempBuf.get();
                    float angleRad = tempBuf.getFloat();
                    byte gunAndAbilityBitMask = tempBuf.get();

                    PlayerInputState input = clientShards[index].inputState; // direct reference, no ring hop

                    if (tick > input.lastProcessedTick) {
                        input.lastProcessedTick = tick;
                        input.movementBitmask = MovementBitMask;
                        input.right=(MovementBitMask & RIGHT_BIT)>0;
                        input.left=(MovementBitMask & LEFT_BIT)>0;
                        input.up=(MovementBitMask & UP_BIT)>0;
                        input.down=(MovementBitMask & DOWN_BIT)>0;
                        input.angleRad = angleRad;
                    }
                    if ((gunAndAbilityBitMask & AbilityBit) != 0) {
                        input.queueAbility(tick, angleRad);
                    }
                    if ((gunAndAbilityBitMask & gunShotBit) != 0) {
                        input.queueShot(tick, angleRad);
                    }

                }
                case UNKNOWN -> {
                    continue;
                }

            }
        }
    }

    public void SEND_OUT_PACKETS_IO() throws IOException {
        for (int i = 0; i < clientShards.length; ++i) {
            //send out packets yo
        }
    }

    public int find_Index(SocketAddress addr) {
        for (int i = 0; i < clientShards.length; i++) {
            if (clientShards[i].Address != null && clientShards[i].Address.equals(addr)) return i;
        }
        return -1;
    }

    public int find_Index(long playerId) {
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

    /**
     * Flips buf and sends it to every authenticated client.
     * Call buf.clear() before filling, then pass directly here.
     * Send failures are silently dropped — UDP is best-effort.
     */
    public void sendToAll(ByteBuffer buf) {
        buf.flip();
        for (int i = 0; i < clientShards.length; i++) {
            SocketAddress addr = clientShards[i].Address;
            if (addr == null) continue;
            try {
                channel.send(buf, addr);
                buf.rewind(); // reset position for next client
            } catch (IOException ignored) {}
        }
    }

    /** Sends a MATCH_OVER packet to all clients. winnerId = -1 means draw (no survivors). */
    public void sendMatchOver(long winnerId) {
        sendBuf.clear();
        sendBuf.put(PacketType.MATCH_OVER.code);
        sendBuf.putLong(winnerId);
        sendToAll(sendBuf);
    }

    @Override
    public String toString() {
        return "UdpSocket[: " + port + " ]";
    }

    public enum PacketType {
        AUTH(0),
        INPUT(1),
        SNAPSHOT(2),
        MATCH_OVER(3),
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