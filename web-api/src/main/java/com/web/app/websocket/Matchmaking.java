package com.web.app.websocket;

import com.web.app.controllers.matchmaking.JoinLeaveQueueRequestBody;
import com.web.configandsecurity.security.SecurityService;
import com.web.matchmaking.MatchmakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import java.security.Principal;

import static com.web.app.controllers.matchmaking.Matchmaking.ALL_PLAYERS_ELO_TESTING;

@Controller
@RequiredArgsConstructor
public class Matchmaking {

    private final MatchmakingService matchmakingService;
    private final SecurityService securityService;

    @MessageMapping("/matchmaking/join")
    public void join(@Payload JoinLeaveQueueRequestBody request, Principal principal) {
        matchmakingService.joinQueue(securityService.getPlayerId(),ALL_PLAYERS_ELO_TESTING,request.gamemode());
    }

    @MessageMapping("/matchmaking/leave")
    public void leave(@Payload JoinLeaveQueueRequestBody request,Principal principal) {
        matchmakingService.leaveQueue(securityService.getPlayerId(principal), request.gamemode());
    }
    @MessageMapping("/matchmaking/leaveALL")
    public void leaveALL(Principal principal) {
        matchmakingService.leaveQueue(securityService.getPlayerId(principal));
    }
}