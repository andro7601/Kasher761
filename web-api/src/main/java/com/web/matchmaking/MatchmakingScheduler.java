package com.web.matchmaking;

import com.game.GameRoomManager;
import com.web.GameModeRegistry;
import com.web.db.entities.GameMode;
import com.web.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import static com.web.redis.StartupSchemaInit.BUCKET_COUNT;

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

                    redisService.CREATE_MATCH(toMatch, matchId);
                    gameRoomManager.allocateRoom(primitiveArray,matchId);
                }
            }
        }
    }
}
