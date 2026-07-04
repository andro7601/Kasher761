package com.web.matchmaking;

import com.web.GameModeRegistry;
import com.web.infra.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MatchmakingService {

    private final GameModeRegistry gameModeRegistry;
    private final RedisService redisService;

    public final void joinQueue(long playerID,int ELO,String gamemode){
        if(!gameModeRegistry.exists(gamemode)){
            throw new IllegalArgumentException("No such gamemode:"+gamemode);
        }
        redisService.ADD_PLAYER_TO_MATCHMAKING(playerID,ELO,gamemode);
    }
    public final void leaveQueue(long playerID,String gamemode){
        if(!gameModeRegistry.exists(gamemode))return ;
        redisService.REMOVE_PLAYER_FROM_MATCHMAKING(playerID,gamemode);
    }
    public final void leaveQueue(long playerID){
        redisService.REMOVE_PLAYER_FROM_MATCHMAKING(playerID);
    }
    public final String getMatch(long playerID) {
        return redisService.GET_MATCH(playerID);
    }
}
