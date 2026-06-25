package com.web.config;

import com.game.GameRoomManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GameEngineConfig {

    @Bean
    public GameRoomManager gameRoomManager() {
        return new GameRoomManager();
    }

    @PostConstruct
    public void igniteEngine() {
        gameRoomManager().start();
    }
}
