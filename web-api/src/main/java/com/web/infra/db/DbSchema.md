# Database Schema (PostgreSQL)

---

## Users

### `users`
- **Type:** PostgreSQL Table
- **Engine/Migration:** Created in `V1__create_users_table.sql`
- **Columns:**
    - `id` — `BIGSERIAL` (Primary Key)
    - `username` — `VARCHAR(64)` (NOT NULL, UNIQUE)
    - `email` — `VARCHAR(255)` (NOT NULL, UNIQUE)
    - `password` — `VARCHAR(255)` (NOT NULL, BCrypt hashed)
- **Written:** `AuthService.register()` — triggered by `POST /api/auth/register` to register a new user account.
- **Read:** 
    - `AuthService.login()` — triggered by `POST /api/auth/login` to authenticate and fetch username/email.
    - `AuthService.register()` — checks uniqueness of `username` and `email` before inserting.
- **Deleted:** N/A (Currently no account deletion flow implemented)

---

## Game Modes

### `game_modes`
- **Type:** PostgreSQL Table
- **Engine/Migration:** Created in `V2__create_game_modes.sql`
- **Columns:**
    - `id` — `SERIAL` (Primary Key)
    - `name` — `VARCHAR(50)` (NOT NULL, UNIQUE) — e.g. `2_PLAYERS`, `4_PLAYERS`
    - `player_count` — `INT` (NOT NULL)
    - `enabled` — `BOOLEAN` (NOT NULL, DEFAULT `true`)
    - `width` — `INT` (NOT NULL)
    - `height` — `INT` (NOT NULL)
    - `tiles` — `INT[]` (NOT NULL)
    - `spawn_points` — `INT[]` (NOT NULL)
- **Initial Data:** Seeded automatically during migration:
    - `('2_PLAYERS', 2, true, ...)`
    - `('4_PLAYERS', 4, true, ...)`
- **Written:** Flyway migration `V2__create_game_modes.sql` at boot time.
- **Read:** `GameModeRegistry` at startup / query time to register supported game modes and query enabled configurations.
- **Deleted:** N/A

---

## Flow Summary

```
User Registration:
    → POST /api/auth/register
        → Check if username/email exists in PostgreSQL 'users' table
        → If unique, insert new record into 'users' (password BCrypt-hashed)
        → Write user cache details to Redis (LivePlayers:{playerId})
        → Return JWT token containing user id/username/email

User Login:
    → POST /api/auth/login
        → Query 'users' table in PostgreSQL by username
        → Validate password hash using BCrypt
        → Write user cache details to Redis (LivePlayers:{playerId})
        → Return JWT token

Matchmaking Initialization:
    → Application Boot
        → GameModeRegistry loads all enabled game modes from PostgreSQL 'game_modes' table
        → Scheduler queries game modes to drive matchmaking buckets loop in Redis
```
