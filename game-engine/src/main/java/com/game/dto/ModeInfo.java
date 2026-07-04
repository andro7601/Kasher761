package com.game.dto;

public record ModeInfo(
        int width,
        int height,
        int[] tiles,
        int[] spawnPoints,
        int playerCount
){}
