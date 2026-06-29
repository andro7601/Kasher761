package com.game.network;

import com.game.dto.Match_Snapshot;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static com.game.network.UdpSocket.MAX_PACKET_SIZE;


public class ClientShard {
    private final long Player_ID;
    public final SocketAddress Address=null;
    private static final int RING_CAPACITY_PER_PLAYER = 32;
    private static final int MASK = RING_CAPACITY_PER_PLAYER - 1;

    ClientShard(long Id){
        this.Player_ID=Id;
    }

    private final ByteBuffer[] buffer=new ByteBuffer[RING_CAPACITY_PER_PLAYER];

    private volatile ByteBuffer Send_Buffer =ByteBuffer.allocateDirect(MAX_PACKET_SIZE);//read By IO

    private ByteBuffer Write_Buffer =ByteBuffer.allocateDirect(MAX_PACKET_SIZE);//

    private volatile long writeseq = 0;
    private volatile long readseq = 0;

    public final ByteBuffer Receive_Packet_IO(ByteBuffer buf) {
        if (writeseq - readseq >= RING_CAPACITY_PER_PLAYER) return buf;
        int idx = (int) (writeseq & MASK);
        ByteBuffer old = buffer[idx];
        buffer[idx] = buf;
        writeseq++;                  // publish last — single writer, volatile is enough
        if (old != null) old.clear();
        return (old != null) ? old : ByteBuffer.allocateDirect(MAX_PACKET_SIZE);
    }

    public final void Read_Packet_LOOP(){
        if(writeseq == readseq) return;
        while (readseq < writeseq) {
            int idx = (int) (readseq & MASK);
            ByteBuffer buf = buffer[idx];
            PacketHandler.handle(buf);
            readseq++;
        }
    }

    public final void Update_Last_Snapshot_Buffer_LOOP(Match_Snapshot snapshot) {
        Write_Buffer.clear();
        PacketHandler.Snapshot_To_Buffer(snapshot, Write_Buffer);
        ByteBuffer temp = Write_Buffer;
        Write_Buffer = Send_Buffer;
        Send_Buffer = temp;
    }

    public ByteBuffer getSendbuf() {
        return  Send_Buffer;
    }
}
