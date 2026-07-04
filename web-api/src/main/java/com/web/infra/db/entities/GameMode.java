package com.web.infra.db.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "game_modes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameMode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private int playerCount;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(nullable = false, columnDefinition = "int[]")
    private int[] tiles;

    @Column(name = "spawn_points", nullable = false, columnDefinition = "int[]")
    private int[] spawnPoints;
}