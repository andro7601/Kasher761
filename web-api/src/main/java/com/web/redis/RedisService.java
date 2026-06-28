package com.web.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.web.StartupSchemaInit.*;

@RequiredArgsConstructor
@Component
public class RedisService {

    private final RedisTemplate<String, Object> redistemplate;

    public void ADD_PLAYER_TO_MATCHMAKING(String playerId, int playerELO, String gamemode) {
        int bucketIndex = playerELO / 100;
        String bucketKey = PREFIX_FOR_MATCHMAKING_BUCKETS + gamemode + ":" + bucketIndex;

        redistemplate.opsForZSet().add(bucketKey, playerId, System.currentTimeMillis());
        redistemplate.opsForList().rightPush(
                PREFIX_FOR_ARRAY_OF_PLAYERS_BUCKETS_IN_MATCHMAKING + playerId, bucketKey);
    }

    public void REMOVE_PLAYERS_FROM_MATCHMAKING(String playerId) {
        Object popped;
        while ((popped = redistemplate.opsForList().rightPop(
                PREFIX_FOR_ARRAY_OF_PLAYERS_BUCKETS_IN_MATCHMAKING + playerId)) != null) {
            redistemplate.opsForZSet().remove(popped.toString(), playerId);
        }
    }
    public List<String> GET_BUCKET_PLAYERS(String gamemode, int bucketIndex) {
        String bucketKey = PREFIX_FOR_MATCHMAKING_BUCKETS + gamemode + ":" + bucketIndex;
        Set<Object> result = redistemplate.opsForZSet().range(bucketKey, 0, -1);
        if (result == null) return List.of();
        return result.stream().map(Object::toString).collect(Collectors.toList());
    }

    public void CREATE_MATCH(List<String> players, String matchId) {
        for (String playerId : players) {
            redistemplate.opsForValue().set(
                    PREFIX_FOR_MATCHMAKING_MAP_PLAYERID_TO_MATCHID + playerId,
                    matchId,
                    Duration.ofHours(2)
            );
            REMOVE_PLAYERS_FROM_MATCHMAKING(playerId);
        }
    }
}
