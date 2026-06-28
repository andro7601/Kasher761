package com.web;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartupSchemaInit{

    private final RedisTemplate<String, Object> redistemplate;

    public static final String[] GAME_MODES = {"4_PLAYERS", "2_PLAYERS"};

    public static final String PREFIX_FOR_MATCHMAKING ="Matchmaking:";//prefix for everything leading to matchmaking

    public static final String PREFIX_FOR_MATCHMAKING_BUCKETS="Buckets:";//ZSET

    public static final String PREFIX_FOR_MATCHMAKING_MAP_PLAYERID_TO_MATCHID="PlayerIdToMatchId:";//HASHMAP

    public static final String PREFIX_FOR_ARRAY_OF_PLAYERS_BUCKETS_IN_MATCHMAKING="PlayersInMatchMaking:";//LIST

    public static final int MAX_ELO = 1000;

    public static final int BUCKET_COUNT = 11;

    @PostConstruct
    private void redisSchemaInit() {
        for (String gameMode : GAME_MODES) {
            for (int bucketIdx = 0; bucketIdx < BUCKET_COUNT; bucketIdx++) {
                redistemplate.delete(PREFIX_FOR_MATCHMAKING +PREFIX_FOR_MATCHMAKING_BUCKETS+ gameMode + ":" + bucketIdx);
            }
        }
        redistemplate.delete(PREFIX_FOR_MATCHMAKING +PREFIX_FOR_MATCHMAKING_MAP_PLAYERID_TO_MATCHID);
    }


}