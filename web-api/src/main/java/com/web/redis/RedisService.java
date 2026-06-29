package com.web.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.web.StartupSchemaInit.*;

@RequiredArgsConstructor
@Component
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public void ADD_PLAYER_TO_MATCHMAKING(long playerId, int playerELO, String gamemode) {
        REMOVE_PLAYER_FROM_MATCHMAKING(playerId, gamemode);

        int bucketIndex = Math.max(0, Math.min(playerELO / 100, BUCKET_COUNT - 1));
        String bucketKey = bucketKey(gamemode, bucketIndex);
        String playerIdValue = String.valueOf(playerId);

        redisTemplate.opsForZSet().add(bucketKey, playerIdValue, System.currentTimeMillis());

        String playerBucketsKey = playerBucketsKey(playerId);
        redisTemplate.opsForList().remove(playerBucketsKey, 0, bucketKey);
        redisTemplate.opsForList().rightPush(playerBucketsKey, bucketKey);
        redisTemplate.expire(playerBucketsKey, 120, TimeUnit.MINUTES);
    }

    public void REMOVE_PLAYER_FROM_MATCHMAKING(long playerId) {
        String playerIdValue = String.valueOf(playerId);
        String playerBucketsKey = playerBucketsKey(playerId);
        String bucketKey;
        while ((bucketKey = redisTemplate.opsForList().rightPop(playerBucketsKey)) != null) {
            redisTemplate.opsForZSet().remove(bucketKey, playerIdValue);
        }
    }

    public void REMOVE_PLAYER_FROM_MATCHMAKING(long playerId, String gamemode) {
        String playerBucketsKey = playerBucketsKey(playerId);
        List<String> currentBuckets = redisTemplate.opsForList().range(playerBucketsKey, 0, -1);
        if (currentBuckets == null || currentBuckets.isEmpty()) {
            return;
        }

        String playerIdValue = String.valueOf(playerId);
        String gamemodeBucketPrefix = PREFIX_FOR_MATCHMAKING + PREFIX_FOR_MATCHMAKING_BUCKETS + gamemode + ":";
        List<String> bucketsToKeep = new ArrayList<>();

        for (String currentBucket : currentBuckets) {
            if (currentBucket.startsWith(gamemodeBucketPrefix)) {
                redisTemplate.opsForZSet().remove(currentBucket, playerIdValue);
            } else {
                bucketsToKeep.add(currentBucket);
            }
        }

        redisTemplate.delete(playerBucketsKey);
        if (!bucketsToKeep.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(playerBucketsKey, bucketsToKeep);
            redisTemplate.expire(playerBucketsKey, 120, TimeUnit.MINUTES);
        }
    }

    public List<Long> GET_BUCKET_PLAYERS(String gamemode, int bucketIndex) {
        Set<String> result = redisTemplate.opsForZSet().range(bucketKey(gamemode, bucketIndex), 0, -1);
        if (result == null) return List.of();
        return result.stream().map(Long::valueOf).toList();
    }

    public void CREATE_MATCH(List<Long> players, String matchId) {
        for (long playerId : players) {
            redisTemplate.opsForValue().set(
                    playerMatchKey(playerId),
                    matchId,
                    Duration.ofHours(2)
            );
            REMOVE_PLAYER_FROM_MATCHMAKING(playerId);
        }
    }

    private String bucketKey(String gamemode, int bucketIndex) {
        return PREFIX_FOR_MATCHMAKING + PREFIX_FOR_MATCHMAKING_BUCKETS + gamemode + ":" + bucketIndex;
    }

    private String playerBucketsKey(long playerId) {
        return PREFIX_FOR_MATCHMAKING + PREFIX_FOR_ARRAY_OF_PLAYERS_BUCKETS_IN_MATCHMAKING + playerId;
    }

    private String playerMatchKey(long playerId) {
        return PREFIX_FOR_MATCHMAKING + PREFIX_FOR_MATCHMAKING_MAP_PLAYERID_TO_MATCHID + playerId;
    }
}
