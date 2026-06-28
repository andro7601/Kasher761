CREATE TABLE game_modes
(
    id           SERIAL PRIMARY KEY,
    name         VARCHAR(50) UNIQUE NOT NULL,
    player_count INT                NOT NULL,
    enabled      BOOLEAN            NOT NULL DEFAULT true
);

INSERT INTO game_modes (name, player_count, enabled)
VALUES ('2_PLAYERS', 2, true),
       ('4_PLAYERS', 4, true);