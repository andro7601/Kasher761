package com.web.matchmaking;

import com.game.GameRoomManager;
import com.game.RoomAllocation;
import com.game.dto.ModeInfo;
import com.web.GameModeRegistry;
import com.web.infra.db.entities.GameMode;
import com.web.infra.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.web.infra.redis.StartupSchemaInit.BUCKET_COUNT;

@RequiredArgsConstructor
@Component
public class MatchmakingScheduler {

    private final RedisService redisService;
    private final GameModeRegistry gameModeRegistry;
    private final GameRoomManager gameRoomManager;


    @Scheduled(fixedDelay = 500)
    public void matchPlayers() {
        for (GameMode gameMode : gameModeRegistry.allEnabled()) {
            int playerCount = gameMode.getPlayerCount();
            for (int bucketIdx = 0; bucketIdx < BUCKET_COUNT; bucketIdx++) {
                List<Long> players = redisService.GET_BUCKET_PLAYERS(gameMode.getName(), bucketIdx);
                int count = players.size() / playerCount;
                for (int i = 0; i < count; i++) {
                    List<Long> toMatch = players.subList(i * playerCount, (i + 1) * playerCount);
                    long[] primitiveArray = toMatch.stream().mapToLong(Long::longValue).toArray();
                    String matchId = UUID.randomUUID().toString();

                    RoomAllocation allocation = gameRoomManager.allocateRoom(
                            primitiveArray,
                            matchId,
                            new ModeInfo(
                              gameMode.getWidth(),
                              gameMode.getHeight(),
                              gameMode.getTiles(),
                              gameMode.getSpawnPoints(),
                              gameMode.getPlayerCount()
                            )
                    );
                    if (allocation != null) {
                        redisService.CREATE_MATCH(toMatch, matchId, gameMode.getName(), allocation.port());
                    }
                }
            }
        }
    }
}
