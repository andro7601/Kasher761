package com.web;

import com.web.db.entities.GameMode;
import com.web.db.repositories.GameModeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GameModeRegistry {

    private final GameModeRepository repository;
    private Map<String, GameMode> modes;
    private Map<String, GameMode> enabledmodes;

    @PostConstruct
    public void load() {
        modes = repository.findAll()
                .stream()
                .collect(Collectors.toMap(GameMode::getName, m -> m));
        enabledmodes =modes.entrySet().stream().filter(m->m.getValue().isEnabled()).collect(Collectors.toMap(Map.Entry::getKey,m -> m.getValue()));
    }

    public GameMode get(String name) { return modes.get(name); }

    public List<GameMode> all() { return List.copyOf(modes.values()); }

    public List<GameMode> allEnabled() { return List.copyOf(enabledmodes.values()); }

    public boolean exists(String name) { return modes.containsKey(name); }
}