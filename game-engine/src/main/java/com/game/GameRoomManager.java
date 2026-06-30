package com.game;

import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.openhft.affinity.Affinity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.game.GameLoop.activeMatches;

public class GameRoomManager {

    public record RoomAllocation(
            int port,
            long startTimeMillis,
            int claimedindex
    ) {}


    static final AtomicInteger coreOneMatchCount = new AtomicInteger(0);
    static final AtomicInteger coreTwoMatchCount = new AtomicInteger(0);

    static final int MAX_MATCHES_PER_CORE=1;

    static final ConcurrentHashMap<String,String> activeRooms=new ConcurrentHashMap<>();


    public static final long TICK_NS = 1_000_000_000L / 60; // 16,666,666 ns — exact, no rounding
    public static final long bootUnixMillis = System.currentTimeMillis();
    public static AtomicLong globalTick = new AtomicLong(0);
    long Match_Start_IN = 120;


    private final int Max_Number_Of_Active_Players=100;

    public final void start() {
        System.out.println("Igniting Game Engine: Spawning isolated P-Core pipelines...");

        ExecutorService executor = Executors.newFixedThreadPool(3);

        GameLoop loop = new GameLoop();

        executor.submit(() -> {
            try {
                pinToCore(0);
                System.out.println("Pipeline 0 pinned to CPU 0. Starting 60Hz Loop...");
                loop.runCorePipeline(0, MAX_MATCHES_PER_CORE, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        executor.submit(() -> {
            try {
                pinToCore(1);
                System.out.println("Pipeline 1 pinned to CPU 1. Starting 60Hz Loop...");
                loop.runCorePipeline(MAX_MATCHES_PER_CORE, 2 * MAX_MATCHES_PER_CORE, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        executor.submit(() -> {
            try {
                pinToCore(2);
                System.out.println("Network I/O thread pinned to  CPU 2. Starting 60Hz Loop...");
                loop.runNetworkIO();
            }catch (Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    public final RoomAllocation allocateRoom(long[] playerIDs,String matchId) {

        int port=-1;
        int claimedIndex=-1;
        while (true) {
            int currentLoad = coreOneMatchCount.get();
            if (currentLoad >= MAX_MATCHES_PER_CORE) {
                break;
            }
            if (coreOneMatchCount.compareAndSet(currentLoad, currentLoad + 1)) {
                activeRooms.put(matchId, "CORE_0");
                port= 5000 + currentLoad; // Success on Core 2!
                claimedIndex =currentLoad;
                break;
            }
        }
        if(port==-1) {
            while (true) {
                int currentLoad = coreTwoMatchCount.get();
                if (currentLoad >= MAX_MATCHES_PER_CORE) {
                    break;
                }
                if (coreTwoMatchCount.compareAndSet(currentLoad, currentLoad + 1)) {
                    activeRooms.put(matchId, "CORE_1");
                    port = 6000 + currentLoad;
                    claimedIndex =MAX_MATCHES_PER_CORE+currentLoad;
                    break;
                }
            }
        }
        if(port==-1)return null;



        long matchStartTick = globalTick.get() + Match_Start_IN;
        long startTimeMillis = bootUnixMillis + (matchStartTick * TICK_NS) / 1_000_000L;

        activeMatches[claimedIndex].activate(port, matchStartTick, playerIDs, matchId);
        return new RoomAllocation(port, startTimeMillis, claimedIndex);
    }

    private static void pinToCore(int cpuId) {
        BitSet mask = new BitSet();
        mask.set(cpuId);
        Affinity.setAffinity(mask);
        int actual = Affinity.getCpu();
        if (actual != cpuId) {
            System.err.println("WARNING: Failed to pin to CPU " + cpuId + ", running on CPU " + actual);
        }
    }
}