package com.game.network;

import com.game.dto.Match_Snapshot;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;


public class ClientShard {
    private final long Player_ID;
    private static final int RING_CAPACITY_PER_PLAYER = 32;
    private static final int MASK = RING_CAPACITY_PER_PLAYER - 1;

    ClientShard(long Id){
        this.Player_ID=Id;
    }

    private final ByteBuffer[] buffer=new ByteBuffer[RING_CAPACITY_PER_PLAYER];

    private final AtomicLong writeseq=new AtomicLong(0);
    private final AtomicLong readseq=new AtomicLong(0);

    public volatile Match_Snapshot lastSnapshot;

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
        while(writeseq.get() < readseq.get()) {
            int idx=(int)(writeseq.get() & MASK);
            ByteBuffer buf = buffer[idx];
            PacketHandler.handle(buf);
            readseq.incrementAndGet();
        }
    }

    public final void Update_Snapshot(Match_Snapshot snapshot) {
        lastSnapshot=snapshot;
    }

    public final void Send_Out_Snapshot(){
        PacketHandler.handle(lastSnapshot);
    }

}
