package com.web.redis;

import com.web.GameModeRegistry;
import com.web.db.entities.GameMode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class StartupSchemaInit{

    private final StringRedisTemplate redisTemplate;
    private final GameModeRegistry gameModeRegistry;

    public static final String PREFIX_FOR_MATCHMAKING ="mm:Matchmaking:";//prefix for everything leading to matchmaking

    public static final String PREFIX_FOR_MATCHMAKING_BUCKETS="Buckets:";//ZSET

    public static final String PREFIX_FOR_MATCHMAKING_MAP_PLAYERID_TO_MATCHID="PlayerIdToMatchId:";//STRING

    public static final String PREFIX_FOR_ARRAY_OF_PLAYERS_BUCKETS_IN_MATCHMAKING="PlayersInMatchMaking:";//LIST

    public static final String PREFIX_FOR_MATCH="match:";

    public static final String SUFFIX_FOR_MATCH_INFO=":info";

    public static final String SUFFIX_FOR_MATCH_PLAYERS=":players";

    public static final String PREFIX_FOR_LIVE_PLAYERS="LivePlayers:";

    public static final int MAX_ELO = 1000;

    public static final int BUCKET_COUNT = 11;

    @PostConstruct
    private void redisSchemaInit() {
        Set<String> matchmakingKeys = redisTemplate.keys(PREFIX_FOR_MATCHMAKING + "*");
        if (matchmakingKeys != null && !matchmakingKeys.isEmpty()) {
            redisTemplate.delete(matchmakingKeys);
        }

        for (GameMode gameMode : gameModeRegistry.all()) {
            String gameModeName=gameMode.getName();
            for (int bucketIdx = 0; bucketIdx < BUCKET_COUNT; bucketIdx++) {
                redisTemplate.delete(PREFIX_FOR_MATCHMAKING +PREFIX_FOR_MATCHMAKING_BUCKETS+ gameModeName + ":" + bucketIdx);
            }
        }
    }


}
