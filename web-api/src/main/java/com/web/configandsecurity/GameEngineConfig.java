package com.web.configandsecurity;

import com.game.GameRoomManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GameEngineConfig {

    @Bean(initMethod = "start")
    public GameRoomManager gameRoomManager() {
        return new GameRoomManager();
    }
}
