# Match Snapshot & Tick Architecture

## Overview

Each `OngoingMatch` runs its own fixed-size ring buffer of `MatchSnapshot`s. The tick loop advances by copying the previous snapshot's state into the next ring slot, then applying physics on top of the copy. No new objects are allocated during the match — everything is pre-allocated once at `activate()` and mutated in place.

```
Matchmaking (web-api)              GameRoomManager / OngoingMatch (game-engine)
──────────────────────             ──────────────────────────────────────────
1. Scheduler matches players
2. Picks random eligible map
   via GameModeRegistry
3. Builds ModeInfo(width, height,
   tiles, spawnPoints, playerCount)
4. allocateRoom(..., ModeInfo) ───> 5. activate(port, startTick,
                                       uuidToPlayerId, matchId, modeInfo)
                                    6. Pre-allocates 10 MatchSnapshot slots,
                                       each with a full ActivePlayer[] array
                                    7. Slot 0 seeded with real starting state
                                       (hp=100, spawn positions, no gun)
                                    8. Tick loop runs, ring-indexed by
                                       localTick % 10
```

## Data flow: how a match starts

1. **`ModeInfo`** (record) is the DTO boundary crossed from `web-api` → `game-engine`. Carries `width`, `height`, `tiles`, `spawnPoints`, `playerCount` — plain data, no JPA entity, avoids circular module dependency.
2. **`OngoingMatch.activate(...)`**:
    - Opens the `UdpSocket` for this match.
    - Stores `startTick` (the **global** tick at which this match begins — used to gate whether the match should be processed yet: `currentTick < match.startTick` → skip).
    - Allocates all 10 `MatchSnapshot` slots up front:
        - Every slot gets `playerCount` set and a correctly-sized `ActivePlayer[]` array.
        - Slots 1–9 are filled with dummy `new ActivePlayer()` (no-arg constructor) — placeholder objects that exist so `copyFromLastSnapshot` never touches `null`.
        - Slot 0 is filled with **real** starting data: `hp=100`, position from `spawnPoints[2*i]`/`spawnPoints[2*i+1]`, `angleRad=3`, `ability=0`, `type=null` (no gun yet).
3. Tiles (`startingTiles`) and dimensions ride along in `ModeInfo` — every snapshot shares the same static tile array by reference (map geometry never changes mid-match, so no need to copy it per snapshot beyond the reference assignment already in `copyFromLastSnapshot`).

## Per-tick flow (forward simulation)

```
prevSnapshot = snapshots[(tick - 1 + 10) % 10]
currSnapshot = snapshots[tick % 10]

currSnapshot.copyFromLastSnapshot(prevSnapshot)
    → localTick = prevSnapshot.localTick + 1
    → for each player: currSnapshot.activePlayers[i].copyFromLastSnapshot(prevSnapshot.activePlayers[i])
        → copies x, y, vx, vy, angleRad, ability, hp, type, team,
          lastProcessedTick, lastInputInfo, lastInputUsedInRow
    → Tiles = prevSnapshot.Tiles (reference copy, static data)

// then, NOT YET IMPLEMENTED:
for each player in currSnapshot.activePlayers:
    apply physics using their stored input (lastInputInfo / useLastInput() / onNewPacketArrived())
    update x, y, vx, vy accordingly

encode currSnapshot → broadcast to all clients over UDP
```

**Key rule:** the only place state actually changes is the physics step, immediately after the copy. The copy itself never mutates anything except pulling forward what already existed.

## Rewind / lag compensation (read-only, no resimulation)

When a shot needs to be validated against a player's past position:

```
shooterClaimedTick = incomingPacket.lastProcessedTick
historicalSnapshot = snapshots[(int)(shooterClaimedTick % 10)]
pastPlayerPositions = historicalSnapshot.activePlayers

hit = checkHitGeometry(shotOrigin, shotDirection, pastPlayerPositions[targetIndex])

if (hit):
    // damage applied to the CURRENT snapshot's player, not the historical one
    currentSnapshot.activePlayers[targetIndex].hp -= damage
```

- History is **read only** — never mutated, never resimulated forward.
- Only the **result** (damage) gets applied to the live/current state.
- **Constraint:** the ring only holds 10 ticks (~167ms at 60Hz). If `currentTick - shooterClaimedTick >= 10`, the referenced snapshot has already been overwritten — this needs an explicit guard (reject the shot, or clamp to the oldest available tick). **Not yet implemented.**

## Tick-based cooldowns (not real-time)

Fire rate is expressed in ticks, not milliseconds, to avoid float drift over a long match:

```
cooldownTicks = ticksPerSecond / firePerSecond   // e.g. 60 / 15 = 4 ticks between shots
```

`shootCooldownTicks` / `abilityCooldownTicks` decrement each tick; a shot is only legal when the relevant cooldown is `0`.

## Bullet pool (planned, not yet added to `OngoingMatch`)

Fixed-size array (e.g. `Bullet[100]`), pre-allocated once, reused via an `active` boolean flag per slot — same object-pool pattern as everything else here. Brute-force linear scan to find a free slot is fine at this scale (well under the tick time budget). Needs an explicit "pool exhausted" guard rather than silently dropping shots.

## Known gaps / TODO

- [ ] **Physics step** — nothing yet turns stored input bytes into actual `x, y, vx, vy` updates.
- [ ] **`GameModeRules` reference** — `OngoingMatch` doesn't yet hold which ruleset (win condition, etc.) governs this match.
- [ ] **Bullet pool** — not yet a field on `OngoingMatch`.
- [ ] **Rewind bounds check** — no guard yet for a referenced tick that's already scrolled out of the 10-slot ring.
- [ ] **`matchId` not stored as a field** on `OngoingMatch` (only passed into `activate()`).
- [ ] **Map width/height** — currently only `tiles` (flat array) is stored on `OngoingMatch`; `width`/`height` from `ModeInfo` aren't persisted, but are required to interpret `idx = y*width+x`.
- [ ] Confirm ring-index math actually reads `snapshots[0]` on the very first tick (`(currentTick - match.startTick) % 10`, not raw `globalTick % 10`, if matches can start at different global ticks).

## Class summary

| Class | Role |
|---|---|
| `ModeInfo` | DTO crossing web-api → game-engine boundary; map + mode data for one match |
| `OngoingMatch` | Owns the UDP socket, the 10-slot snapshot ring, and match lifecycle (`activate`/`deactivate`) |
| `MatchSnapshot` | One tick's full world state: tile reference, tick number, all players |
| `ActivePlayer` | One player's mutable state for one snapshot slot; reused in place every tick, never reallocated |
| `GunType` | Enum of weapon types; `null` on `ActivePlayer.type` is valid (no gun picked up yet) |