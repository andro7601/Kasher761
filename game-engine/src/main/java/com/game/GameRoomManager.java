package com.game;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.openhft.affinity.AffinityLock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.game.GameLoop.activeMatches;

public class GameRoomManager {
    public record RoomAllocation(int port, long startTimeMillis) {}

    static final AtomicInteger coreOneMatchCount = new AtomicInteger(0);
    static final AtomicInteger coreTwoMatchCount = new AtomicInteger(0);
    static final int MAX_MATCHES_PER_CORE=1;
    static final ConcurrentHashMap<String,String> activeRooms=new ConcurrentHashMap<>();


    static final long TICK_NS = 1_000_000_000L / 60; // 16,666,666 ns — exact, no rounding
    static final long bootUnixMillis = System.currentTimeMillis();
    static AtomicLong globalTick = new AtomicLong(0);

    public final RoomAllocation allocateRoom(String matchId) {
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


        long graceTicks = 120;

        long matchStartTick = globalTick.get() + graceTicks;
        long startTimeMillis = bootUnixMillis + (matchStartTick * TICK_NS) / 1_000_000L;

        activeMatches[claimedIndex].startTick = matchStartTick;
        return new RoomAllocation(port, startTimeMillis);
    }

    public final void start() {
        System.out.println("Igniting Game Engine: Spawning isolated P-Core pipelines...");

        ExecutorService executor = Executors.newFixedThreadPool(2);


        /*
        AffinityLock.acquireCore() can actually take int as a parameter and locks that
        core, but it's already configured to choose one which is isolated and one which
        isn't already used by openhfts other threads
         */
        executor.submit(() -> {
            try (AffinityLock al = AffinityLock.acquireCore()) {
                System.out.println("Pipeline 0 locked to CPU ID: " + al.cpuId() + ". Starting 60Hz Loop...");
                GameLoop loop = new GameLoop();
                // startIndex: 0, endIndex: MAX, isTimeKeeper: true
                loop.runCorePipeline(0, MAX_MATCHES_PER_CORE, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        executor.submit(() -> {
            try (AffinityLock al = AffinityLock.acquireCore()) {
                System.out.println("Pipeline 1 locked to CPU ID: " + al.cpuId() + ". Starting 60Hz Loop...");
                GameLoop loop = new GameLoop();

                loop.runCorePipeline(MAX_MATCHES_PER_CORE, 2 * MAX_MATCHES_PER_CORE, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}