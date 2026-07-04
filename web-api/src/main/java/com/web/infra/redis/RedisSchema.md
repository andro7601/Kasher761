# Redis Schema

---

## Matchmaking

### Buckets
```
mm:Matchmaking:Buckets:{gameMode}:{bucketIdx}
```
- **Type:** ZSet
- **Value:** `playerId`
- **Score:** `System.currentTimeMillis()` — join timestamp, used for priority ordering
- **Written:** `RedisService.ADD_PLAYER_TO_MATCHMAKING()` — triggered by `MatchmakingService.joinQueue()` → `POST /matchmaking/join` or STOMP `/app/matchmaking/join`
- **Read:** `RedisService.GET_BUCKET_PLAYERS()` — triggered by `MatchmakingScheduler.matchPlayers()` every 500ms
- **Deleted:** `RedisService.REMOVE_PLAYERS_FROM_MATCHMAKING()` — triggered after match is created inside `RedisService.CREATE_MATCH()`, or when player leaves queue via `DELETE /matchmaking/leave` or STOMP `/app/matchmaking/leave`

---

### Player ID → Match ID
```
mm:Matchmaking:PlayerIdToMatchId:{playerId}
```
- **Type:** String
- **Value:** `matchId`
- **TTL:** 2 hours
- **Written:** `RedisService.CREATE_MATCH()` — triggered by `MatchmakingScheduler.matchPlayers()` when a full group is found
- **Read:** `RedisService.GET_MATCH()` — triggered by `GET /matchmaking/match` to check if player has been matched, and on UDP auth to verify player belongs to a match before accepting packets
- **Deleted:** Expires automatically via TTL when match ends

---

### Player ID → List of Buckets They Are In
```
mm:Matchmaking:PlayersInMatchMaking:{playerId}
```
- **Type:** List
- **Value:** bucket keys e.g. `mm:Matchmaking:Buckets:2_PLAYERS:3`
- **Written:** `RedisService.ADD_PLAYER_TO_MATCHMAKING()` — one entry pushed per game mode the player queues for, triggered by `MatchmakingService.joinQueue()`
- **Read + Deleted:** `RedisService.REMOVE_PLAYERS_FROM_MATCHMAKING()` — popped one by one, each popped key used to `ZREM` the player from that bucket. Triggered after match creation or when player leaves queue

---

## Live Matches

### Match Info
```
match:{matchId}:info
```
- **Type:** Hash
- **Fields:**
    - `gameMode` — e.g. `2_PLAYERS`, `4_PLAYERS`
    - `serverPort` — UDP port the game engine is running on, clients send packets here
- **TTL:** 2 hours
- **Written:** `RedisService.STORE_MATCH()` — triggered inside `RedisService.CREATE_MATCH()` after scheduler forms a group
- **Read:** on UDP reconnect — client re-sends auth packet, server reads `serverPort` to confirm which game instance they belong to
- **Deleted:** Expires automatically via TTL

---

### Match Players
```
match:{matchId}:players
```
- **Type:** Set
- **Value:** `playerId`
- **TTL:** 2 hours
- **Written:** `RedisService.STORE_MATCH()` — triggered inside `RedisService.CREATE_MATCH()`
- **Read:** on UDP auth — server checks if incoming `playerId` exists in this set before accepting their packets. Also read when broadcasting match found event so each player gets enemy list
- **Deleted:** Expires automatically via TTL

---

## Live Players

### Player Info
```
LivePlayers:{playerId}
```
- **Type:** Hash
- **Fields:**
    - `username`
    - `email`
    - `elo`
    - *(anything else needed at runtime)*
- **Written:** on player login / session start — fetched from Postgres once and cached here so hot-path reads never hit the DB
- **Read:** anywhere player info is needed at runtime — match creation to broadcast enemy names, UDP auth to validate identity
- **Deleted:** on logout or session expiry via TTL

---

## Flow Summary

```
Player joins queue
    → ADD_PLAYER_TO_MATCHMAKING
        → ZADD  mm:Matchmaking:Buckets:{gameMode}:{bucketIdx}
        → RPUSH mm:Matchmaking:PlayersInMatchMaking:{playerId}

Scheduler fires every 500ms
    → GET_BUCKET_PLAYERS
        → ZRANGE mm:Matchmaking:Buckets:{gameMode}:{bucketIdx}
    → if players.size() >= playerCount
        → CREATE_MATCH
            → SET   mm:Matchmaking:PlayerIdToMatchId:{playerId}
            → HSET  match:{matchId}:info
            → SADD  match:{matchId}:players
            → REMOVE_PLAYERS_FROM_MATCHMAKING for each player
                → RPOP mm:Matchmaking:PlayersInMatchMaking:{playerId}
                → ZREM mm:Matchmaking:Buckets:{gameMode}:{bucketIdx}
            → notifyPlayers via STOMP /user/queue/match

Player receives MatchFoundEvent over STOMP
    → connects via UDP to serverPort
    → sends auth packet with playerId + matchId
        → server checks mm:Matchmaking:PlayerIdToMatchId:{playerId}
        → server checks match:{matchId}:players contains playerId
        → if valid → accept packets, map IP+port to player session

Player disconnects for > 2s
    → client re-sends UDP auth packet
        → same auth check as above
        → server updates IP+port mapping for that player
        → game resumes

Player leaves queue manually
    → REMOVE_PLAYERS_FROM_MATCHMAKING
        → RPOP mm:Matchmaking:PlayersInMatchMaking:{playerId}
        → ZREM mm:Matchmaking:Buckets:{gameMode}:{bucketIdx}
```