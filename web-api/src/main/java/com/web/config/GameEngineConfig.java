package com.web.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.game.GameRoomManager;

@Configuration
public class GameEngineConfig {
    @Bean
    public GameRoomManager gameRoomManager() {
        return new GameRoomManager();
    }
    @PostConstruct
    public void igniteEngine() {
        System.out.println("Spring Boot pausing web server... Booting Game Engine first.");
        gameRoomManager().start();

        try { Thread.sleep(500); } catch (InterruptedException e) { }

        System.out.println("Engine Online. Spring Boot is now allowed to open Port 8080.");
    }
}
