CREATE TABLE game_modes
(
    id           SERIAL PRIMARY KEY,
    name         VARCHAR(50) UNIQUE NOT NULL,
    player_count INT                NOT NULL,
    enabled      BOOLEAN            NOT NULL DEFAULT true,
    width        INT                NOT NULL,
    height       INT                NOT NULL,
    tiles        INT[]              NOT NULL,
    spawn_points INT[]              NOT NULL
);

INSERT INTO game_modes (name, player_count, enabled, width, height, tiles, spawn_points)
VALUES ('2_PLAYERS', 2, true, 10, 10, '{0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0}', '{1,1, 8,8}'),
       ('4_PLAYERS', 4, true, 10, 10, '{0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0}', '{1,1, 8,1, 1,8, 8,8}');