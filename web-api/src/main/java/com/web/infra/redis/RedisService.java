package com.web.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.web.infra.redis.StartupSchemaInit.*;

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

    public void CREATE_MATCH(List<Long> players, String matchId, String gameMode, int serverPort) {
        for (long playerId : players) {
            redisTemplate.opsForValue().set(
                    playerMatchKey(playerId),
                    matchId,
                    Duration.ofHours(2)
            );
            REMOVE_PLAYER_FROM_MATCHMAKING(playerId);
        }
        STORE_MATCH(matchId, gameMode, serverPort, players);
    }

    public void STORE_MATCH(String matchId, String gameMode, int serverPort, List<Long> players) {
        String infoKey = matchInfoKey(matchId);
        redisTemplate.opsForHash().put(infoKey, "gameMode", gameMode);
        redisTemplate.opsForHash().put(infoKey, "serverPort", String.valueOf(serverPort));
        redisTemplate.expire(infoKey, Duration.ofHours(2));

        String playersKey = matchPlayersKey(matchId);
        String[] playerIds = players.stream().map(String::valueOf).toArray(String[]::new);
        redisTemplate.opsForSet().add(playersKey, playerIds);
        redisTemplate.expire(playersKey, Duration.ofHours(2));
    }

    public String GET_MATCH(long playerId) {
        return redisTemplate.opsForValue().get(playerMatchKey(playerId));
    }

    public String GET_MATCH_GAMEMODE(String matchId) {
        return (String) redisTemplate.opsForHash().get(matchInfoKey(matchId), "gameMode");
    }

    public Integer GET_MATCH_PORT(String matchId) {
        String portStr = (String) redisTemplate.opsForHash().get(matchInfoKey(matchId), "serverPort");
        return portStr != null ? Integer.parseInt(portStr) : null;
    }

    public Set<String> GET_MATCH_PLAYERS(String matchId) {
        return redisTemplate.opsForSet().members(matchPlayersKey(matchId));
    }

    public boolean IS_PLAYER_IN_MATCH(String matchId, long playerId) {
        Boolean isMember = redisTemplate.opsForSet().isMember(matchPlayersKey(matchId), String.valueOf(playerId));
        return isMember != null && isMember;
    }

    public void SAVE_PLAYER_INFO(long playerId, String username, String email, int elo) {
        String key = playerInfoKey(playerId);
        redisTemplate.opsForHash().put(key, "username", username);
        redisTemplate.opsForHash().put(key, "email", email);
        redisTemplate.opsForHash().put(key, "elo", String.valueOf(elo));
        redisTemplate.expire(key, Duration.ofHours(24));
    }

    public String GET_PLAYER_INFO_FIELD(long playerId, String field) {
        Object val = redisTemplate.opsForHash().get(playerInfoKey(playerId), field);
        return val != null ? val.toString() : null;
    }

    public void DELETE_PLAYER_INFO(long playerId) {
        redisTemplate.delete(playerInfoKey(playerId));
    }

    public void REMOVE_PLAYERS_FROM_MATCHMAKING(long playerId) {
        REMOVE_PLAYER_FROM_MATCHMAKING(playerId);
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

    private String matchInfoKey(String matchId) {
        return PREFIX_FOR_MATCH + matchId + SUFFIX_FOR_MATCH_INFO;
    }

    private String matchPlayersKey(String matchId) {
        return PREFIX_FOR_MATCH + matchId + SUFFIX_FOR_MATCH_PLAYERS;
    }

    private String playerInfoKey(long playerId) {
        return PREFIX_FOR_LIVE_PLAYERS + playerId;
    }
}
