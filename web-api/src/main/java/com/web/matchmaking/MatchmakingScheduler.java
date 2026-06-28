package com.web.matchmaking;

import com.web.GameModeRegistry;
import com.web.db.entities.GameMode;
import com.web.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.web.StartupSchemaInit.BUCKET_COUNT;

@RequiredArgsConstructor
@Component
public class MatchmakingScheduler {

    private final RedisService redisService;
    private final GameModeRegistry gameModeRegistry;

    @Scheduled(fixedDelay = 500)
    public void matchPlayers() {
        for (GameMode gameMode : gameModeRegistry.allEnabled()) {
            int playerCount = gameMode.getPlayerCount();
            for (int bucketIdx = 0; bucketIdx < BUCKET_COUNT; bucketIdx++) {
                List<String> players = redisService.GET_BUCKET_PLAYERS(gameMode.getName(), bucketIdx);
                int count = players.size() / playerCount;
                for (int i = 0; i < count; i++) {
                    List<String> toMatch = players.subList(
                            i * playerCount,
                            (i + 1) * playerCount
                    );
                    String matchId = UUID.randomUUID().toString();
                    redisService.CREATE_MATCH(toMatch, matchId);
                }
            }
        }
    }
}
