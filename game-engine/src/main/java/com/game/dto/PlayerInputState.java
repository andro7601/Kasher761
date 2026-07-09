package com.game.dto;

public class PlayerInputState {
    public volatile byte movementBitmask;
    public volatile float angleRad;
    public volatile long lastProcessedTick;

    public volatile boolean up;
    public volatile boolean down;
    public volatile boolean left;
    public volatile boolean right;

    private static final int MAX_PENDING = 8;

    private final long[] pendingShotTicks = new long[MAX_PENDING];
    private final float[] pendingShotAngles = new float[MAX_PENDING];
    private volatile int pendingShotCount = 0;

    private final long[] pendingAbilityTicks = new long[MAX_PENDING];
    private final float[] pendingAbilityAngles = new float[MAX_PENDING];
    private volatile int pendingAbilityCount = 0;

    public synchronized void queueShot(long tick, float angle) {
        if (pendingShotCount < MAX_PENDING) {
            pendingShotTicks[pendingShotCount] = tick;
            pendingShotAngles[pendingShotCount] = angle;
            pendingShotCount++;
        }
    }

    public synchronized void queueAbility(long tick, float angle) {
        if (pendingAbilityCount < MAX_PENDING) {
            pendingAbilityTicks[pendingAbilityCount] = tick;
            pendingAbilityAngles[pendingAbilityCount] = angle;
            pendingAbilityCount++;
        }
    }

    public static final class PendingBatch {
        public final long[] ticks;
        public final float[] angles;
        public int size;
        PendingBatch(long[] ticks, float[] angles) { this.ticks = ticks; this.angles = angles;this.size=ticks.length; }
    }

    public synchronized PendingBatch drainShots() {
        PendingBatch batch = new PendingBatch(
                java.util.Arrays.copyOf(pendingShotTicks, pendingShotCount),
                java.util.Arrays.copyOf(pendingShotAngles, pendingShotCount)
        );
        pendingShotCount = 0;
        return batch;
    }
}
