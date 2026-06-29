package com.web.controllers.matchmaking;

import com.web.configandsecurity.security.SecurityService;
import com.web.matchmaking.MatchmakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/matchmaking")
public class Matchmaking {
    private final MatchmakingService matchmakingService;
    private final SecurityService securityService;

    private final int ALL_PLAYERS_ELO_TESTING=150;


    @PostMapping("/join")
    public ResponseEntity<?> joinQueue(@RequestBody JoinLeaveQueueRequestBody joinQueueRequestBody){
        matchmakingService.joinQueue(securityService.getPlayerId(),ALL_PLAYERS_ELO_TESTING, joinQueueRequestBody.gamemode());
        return ResponseEntity.ok().build();
    }
    @PostMapping("/leave")
    public ResponseEntity<?> leaveQueue(@RequestBody JoinLeaveQueueRequestBody joinQueueRequestBody){
        matchmakingService.leaveQueue(securityService.getPlayerId(),joinQueueRequestBody.gamemode());
        return ResponseEntity.ok().build();
    }

}
