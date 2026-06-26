package com.game;

import java.util.concurrent.locks.LockSupport;

public class GameLoop {

    public static class MatchSnapshot {
        public long startTick = Long.MAX_VALUE; // Default to max so it's ignored until allocated
        public int notimplementingothersyet;
    }

    public static final MatchSnapshot[] activeMatches = new MatchSnapshot[2 * GameRoomManager.MAX_MATCHES_PER_CORE];
    static {
        for (int i = 0; i < activeMatches.length; i++) {
            activeMatches[i] = new MatchSnapshot();
        }
    }

    public void runCorePipeline(int startIndex, int endIndex, boolean isTimeKeeper) throws InterruptedException {
        long frameDurationNanos = GameRoomManager.TICK_NS;

        while (true) {
            long loopStartNanos = System.nanoTime();

            if (isTimeKeeper) {
                GameRoomManager.globalTick.incrementAndGet();
            }

            long currentTick = GameRoomManager.globalTick.get();

            for (int i = startIndex; i < endIndex; i++) {
                MatchSnapshot match = activeMatches[i];

                if (currentTick < match.startTick) {
                    continue;
                }

                // TODO: Read UDP Input Buffer
                // TODO: Update Spatial Grid / Physics
                // TODO: Blast UDP Output Buffer
            }

            long SPIN_THRESHOLD = 1_000_000; // 1ms
            long nextFrameTime = loopStartNanos + frameDurationNanos;

            while (true) {
                long remaining = nextFrameTime - System.nanoTime();

                if (remaining <= 0) {
                    break;
                }

                if (remaining > SPIN_THRESHOLD) {
                    LockSupport.parkNanos(remaining);
                } else {
                    Thread.onSpinWait();
                }
            }
        }
    }
}