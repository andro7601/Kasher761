package com.game.network;

import com.game.dto.Ongoing_Match;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthPacketTest {

    @Test
    public void testAuthPacket() throws IOException, InterruptedException {
        // Create a fake one man match with set port
        int port = 9876;
        long playerId = 42L;
        UUID playerUuid = UUID.randomUUID();
        
        Ongoing_Match match = new Ongoing_Match();
        match.activate(port, 0, new long[]{playerId}, new UUID[]{playerUuid}, "test-match");
        
        UdpSocket udpSocket = match.socket;
        assertNotNull(udpSocket, "UdpSocket should be initialized");
        
        // Define a custom ClientShard subclass to detect when the setter is called
        class TestClientShard extends ClientShard {
            boolean setterCalled = false;

            TestClientShard(long id) {
                super(id);
            }

            @Override
            public void setAddress(SocketAddress Address) {
                this.setterCalled = true;
                super.setAddress(Address);
            }
        }
        
        // Swap out the first client shard in the array with our TestClientShard
        TestClientShard testShard = new TestClientShard(playerId);
        udpSocket.clientShards[0] = testShard;
        
        // Send a correct UDP auth packet to the socket for the player
        try (DatagramChannel clientChannel = DatagramChannel.open()) {
            ByteBuffer packet = ByteBuffer.allocate(17);
            packet.put((byte) 0); // PacketType.AUTH code
            packet.putLong(playerUuid.getMostSignificantBits());
            packet.putLong(playerUuid.getLeastSignificantBits());
            packet.flip();
            
            clientChannel.send(packet, new InetSocketAddress("127.0.0.1", port));
        }
        
        // Poll for up to 2 seconds to process incoming packet
        long start = System.currentTimeMillis();
        while (!testShard.setterCalled && System.currentTimeMillis() - start < 2000) {
            udpSocket.Empty_OS_BUFFER_IO();
            Thread.sleep(10);
        }
        
        // Assert that the setter of socketaddress was called and that the address is no longer null
        assertTrue(testShard.setterCalled, "The setAddress setter should have been called");
        assertNotNull(testShard.Address, "The client shard address should no longer be null");
        
        // Deactivate match to release port
        match.deactivate();
    }
}
