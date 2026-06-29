package com.game;

import com.game.dto.Ongoing_Match;
import com.game.network.UdpSocket;

import java.io.IOException;
import java.util.concurrent.locks.LockSupport;

import static com.game.GameRoomManager.*;

public class GameLoop {

    private static final long SPIN_THRESHOLD_NS = 1_500_000L;
    public static final Ongoing_Match[] activeMatches = new Ongoing_Match[2 * GameRoomManager.MAX_MATCHES_PER_CORE];

    static {
        for (int i = 0; i < activeMatches.length; i++) {
            activeMatches[i] = new Ongoing_Match();
        }
    }


    public void runCorePipeline(int startIndex, int endIndex, boolean isTimeKeeper) throws InterruptedException {
        long localTickCount = 0;
        long engineStartNanos = System.nanoTime();
        while (true) {
            if (isTimeKeeper) {
                globalTick.incrementAndGet();
            }
            long currentTick = globalTick.get();
            for (int i = startIndex; i < endIndex; i++) {
                Ongoing_Match match = activeMatches[i];
                if (!match.active() && currentTick < match.startTick) continue;

                UdpSocket sock = match.socket;
                if (sock == null) continue;


                // TODO: physics, collision, game logic

                // ── 4. SEND: broadcast state to all connected clients ───────────
                // TODO: encode snapshot, sock.sendTo(data, offset, len, clientAddr);
            }
            localTickCount++;
            long targetnanos = engineStartNanos + localTickCount * TICK_NS;
            sleepuntil(targetnanos);
        }
    }

    public void runNetworkIO() throws InterruptedException, IOException {
        long localTickCount = 0;
        long engineStartNanos = System.nanoTime();
        while (true) {
            long currentTick = globalTick.get();
            for (int i = 0; i < 2 * MAX_MATCHES_PER_CORE; i++) {
                Ongoing_Match match = activeMatches[i];
                if (!match.active() && currentTick < match.startTick) continue;

                UdpSocket sock = match.socket;
                if (sock == null) continue;
                sock.Empty_OS_BUFFER_IO();
                sock.SEND_OUT_PACKETS_IO();
            }
            localTickCount++;
            long targetnanos = engineStartNanos + localTickCount * TICK_NS;
            sleepuntil(targetnanos);
        }
    }

    void sleepuntil(long targetnanos) {
        long remaining = targetnanos - System.nanoTime();
        if (remaining <= 0) {
            return;
        }
        long parknanos = remaining - SPIN_THRESHOLD_NS;
        if (parknanos > 0) {
            LockSupport.parkNanos(parknanos);
        }
        while (targetnanos > System.nanoTime()) {
            Thread.onSpinWait();
        }
    }
}