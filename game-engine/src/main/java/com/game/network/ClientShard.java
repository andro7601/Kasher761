package com.game.network;

import com.game.dto.Match_Snapshot;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import static com.game.network.UdpSocket.MAX_PACKET_SIZE;


public class ClientShard {
    private final long Player_ID;
    public SocketAddress Address;
    private static final int RING_CAPACITY_PER_PLAYER = 32;
    private static final int MASK = RING_CAPACITY_PER_PLAYER - 1;

    ClientShard(long Id){
        this.Player_ID=Id;
    }

    private final ByteBuffer[] buffer=new ByteBuffer[RING_CAPACITY_PER_PLAYER];
    private ByteBuffer sendbuf = ByteBuffer.allocateDirect(MAX_PACKET_SIZE);

    private final AtomicLong writeseq=new AtomicLong(0);
    private final AtomicLong readseq=new AtomicLong(0);

    public final void Receive_Packet_IO(ByteBuffer buf) {
        if (writeseq.get() - readseq.get() >= RING_CAPACITY_PER_PLAYER) return;
        int idx = (int) (writeseq.get() & MASK);
        ByteBuffer temp = buf;
        buf=buffer[idx];
        buffer[idx]=temp;
        writeseq.incrementAndGet();
    }
    public final void Read_Packet_LOOP(){
        if(writeseq.get() == readseq.get()) return;
        while (readseq.get() < writeseq.get()) {
            int idx = (int) (readseq.get() & MASK);
            ByteBuffer buf = buffer[idx];
            PacketHandler.handle(buf);
            readseq.incrementAndGet();
        }
    }

    public final void Update_Last_Snapshot_Buffer_LOOP(Match_Snapshot snapshot) {
        sendbuf.clear();
        //transform snapshot into buffer living in sendbuf
        sendbuf=PacketHandler.handle(snapshot,sendbuf);
    }

    public ByteBuffer getSendbuf() {
        return sendbuf;
    }
}
