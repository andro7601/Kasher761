package com.web.infra.db.repositories;

import com.web.infra.db.entities.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameModeRepository extends JpaRepository<GameMode, Long> {
    List<GameMode> findAllByEnabled(boolean enabled);
}
