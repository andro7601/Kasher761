package com.game.gamerules;

import com.game.GameRoomManager;
import com.game.dto.ActiveBullet;
import com.game.dto.ActivePlayer;
import com.game.dto.MatchSnapshot;
import com.game.dto.OngoingMatch;
import com.game.dto.PlayerInputState;
import com.game.network.ClientShard;
import com.game.network.UdpSocket;
import static com.game.gamerules.WorldBuilder.*;

import java.io.IOException;
import java.util.concurrent.locks.LockSupport;



import static com.game.GameRoomManager.*;

public class GameLoop {

    private static final long SPIN_THRESHOLD_NS = 1_500_000L;
    public static final OngoingMatch[] activeMatches = new OngoingMatch[2 * GameRoomManager.MAX_MATCHES_PER_CORE];

    static {
        for (int i = 0; i < activeMatches.length; i++) {
            activeMatches[i] = new OngoingMatch();
        }
    }


    public void runCorePipeline(int startIndex, int endIndex, boolean isTimeKeeper) throws InterruptedException {
        long localTickCount = 0;
        long engineStartNanos = System.nanoTime();
        while (true) {
            if (isTimeKeeper) {
                globalTick.incrementAndGet();
            }
            long currentTick = globalTick.get();
            for (int i = startIndex; i < endIndex; i++) {
                OngoingMatch match = activeMatches[i];
                if (!match.active() || currentTick < match.startTick) continue;

                UdpSocket sock = match.socket;
                if (sock == null) continue;

                sock.Empty_OS_BUFFER_IO();

                ClientShard[] shards=sock.clientShards;

                int indexNext=match.getNextSnapshotIndex();
                int indexPrev=match.getPreviousSnapshotIndex();
                match.snapshots[indexNext].copyFromLastSnapshot(match.snapshots[indexPrev]);
                MatchSnapshot snapshotNext=match.snapshots[indexNext];
                ActivePlayer[] players=snapshotNext.activePlayers;
                int[] tiles = snapshotNext.Tiles;
                int mapWidth = match.mapWidth;

                for(ActivePlayer player : players) {
                    long id = player.playerId;
                    int shardidx = sock.find_Index(id);
                    ClientShard shard = shards[shardidx];
                    PlayerInputState state = shard.inputState;

                    // --- Y velocity ---
                    // check ground from current (pre-move) position
                    boolean grounded = isGrounded(tiles, mapWidth, player);
                    if (grounded && player.vy >= 0f) {
                        player.vy = 0f;                          // cancel any residual downward vel
                        if (state.up) player.vy = vJump;         // jump fires this same tick
                    } else {
                        player.vy += vGravity;                   // in air: accumulate gravity
                    }

                    // --- X velocity: direct assignment, no accumulation ---
                    player.vx = (state.right ? vRight : 0f) + (state.left ? -vRight : 0f);

                    // --- X axis move (try, then block if wall) ---
                    float newX = player.x + player.vx;
                    if (player.vx > 0f) {
                        // moving right: top, mid, bottom of right edge
                        float rightEdge = newX + PLAYER_WIDTH - 1f;
                        if (solidAt(tiles, mapWidth, rightEdge, player.y)                    ||
                            solidAt(tiles, mapWidth, rightEdge, player.y + PLAYER_HEIGHT / 2f) ||
                            solidAt(tiles, mapWidth, rightEdge, player.y + PLAYER_HEIGHT - 1f)) {
                            player.vx = 0f;
                        } else {
                            player.x = newX;
                        }
                    } else if (player.vx < 0f) {
                        // moving left: top, mid, bottom of left edge
                        if (solidAt(tiles, mapWidth, newX, player.y)                    ||
                            solidAt(tiles, mapWidth, newX, player.y + PLAYER_HEIGHT / 2f) ||
                            solidAt(tiles, mapWidth, newX, player.y + PLAYER_HEIGHT - 1f)) {
                            player.vx = 0f;
                        } else {
                            player.x = newX;
                        }
                    }

                    // --- Y axis move ---
                    float newY = player.y + player.vy;
                    if (player.vy > 0f) {
                        // falling down: test bottom-left and bottom-right foot corners
                        float feetY = newY + PLAYER_HEIGHT;
                        if (solidAt(tiles, mapWidth, player.x, feetY) ||
                            solidAt(tiles, mapWidth, player.x + PLAYER_WIDTH - 1f, feetY)) {
                            player.vy = 0f;
                            // snap to top of the tile we just landed on
                            player.y = (float)(Math.floor(feetY / TILE_SIZE) * TILE_SIZE) - PLAYER_HEIGHT;
                        } else {
                            player.y = newY;
                        }
                    } else if (player.vy < 0f) {
                        // jumping up: test top-left and top-right head corners
                        if (solidAt(tiles, mapWidth, player.x, newY) ||
                            solidAt(tiles, mapWidth, player.x + PLAYER_WIDTH - 1f, newY)) {
                            player.vy = 0f;                      // head bumped ceiling
                        } else {
                            player.y = newY;
                        }
                    }

                    player.angleRad = state.angleRad;

                    // --- spawn bullets: fire from current position, no rewind ---
                    PlayerInputState.PendingBatch batch = state.drainShots();
                    float[] angles = batch.angles;
                    for (int k = 0; k < batch.size; k++) {
                        for (int b = 0; b < snapshotNext.bullets.length; b++) {
                            ActiveBullet bullet = snapshotNext.bullets[b];
                            if (!bullet.alive) {
                                bullet.alive         = true;
                                bullet.x             = player.x + PLAYER_WIDTH  / 2f;
                                bullet.y             = player.y + PLAYER_HEIGHT / 2f;
                                bullet.vx            = (float)(Math.cos(angles[k]) * vBullet);
                                bullet.vy            = (float)(Math.sin(angles[k]) * vBullet);
                                bullet.ownerPlayerId = id;
                                bullet.ageTicks      = 0;
                                break;
                            }
                        }
                    }
                }

                // --- bullet tick: move, age, wall-kill, player-hit ---
                ActiveBullet[] bullets = snapshotNext.bullets;
                for (int b = 0; b < bullets.length; b++) {
                    ActiveBullet bullet = bullets[b];
                    if (!bullet.alive) continue;

                    bullet.ageTicks++;
                    if (bullet.ageTicks > BULLET_MAX_LIFETIME_TICKS) {
                        bullet.alive = false;
                        continue;
                    }

                    bullet.x += bullet.vx;
                    bullet.y += bullet.vy;

                    // wall check — bullet is a single point
                    if (solidAt(tiles, mapWidth, bullet.x, bullet.y)) {
                        bullet.alive = false;
                        continue;
                    }

                    // player hit check — bullet point inside player AABB
                    for (ActivePlayer player : players) {
                        if (bullet.ownerPlayerId == player.playerId) continue; // no self-hit
                        if (bullet.x >= player.x                   &&
                            bullet.x <= player.x + PLAYER_WIDTH    &&
                            bullet.y >= player.y                   &&
                            bullet.y <= player.y + PLAYER_HEIGHT) {
                            player.hp = (short) Math.max(0, player.hp - 10);
                            bullet.alive = false;
                            break; // bullet is gone, stop checking more players
                        }
                    }
                }
                // --- win condition: count players still alive ---
                int aliveCount = 0;
                long winnerId  = -1L;
                for (ActivePlayer p : players) {
                    if (p.hp > 0) { aliveCount++; winnerId = p.playerId; }
                }
                if (aliveCount <= 1) {
                    match.deactivate(); // 1 survivor = winnerId won; 0 = draw (winnerId stays -1)
                }
            }
            localTickCount++;
            long targetnanos = engineStartNanos + localTickCount * TICK_NS;
            sleepuntil(targetnanos);
        }
    }

    public void runNetworkIO() throws IOException {
        long localTickCount = 0;
        long engineStartNanos = System.nanoTime();
        while (true) {
            long currentTick = globalTick.get();
            for (int i = 0; i < 2 * MAX_MATCHES_PER_CORE; i++) {
                OngoingMatch match = activeMatches[i];
                if (!match.active() || currentTick < match.startTick) continue;
                UdpSocket sock = match.socket;
                if (sock == null) continue;
                sock.Empty_OS_BUFFER_IO();
                sock.SEND_OUT_PACKETS_IO();
            }
            localTickCount++;
            long targetnanos = engineStartNanos + localTickCount * TICK_NS;
            sleepuntil(targetnanos);
        }
    }

    /**
     * Returns true if worldX/worldY falls inside a wall tile.
     * Out-of-bounds coords are treated as solid so players can't escape the map.
     * Conversion: col = (int)(worldX / TILE_SIZE), row = (int)(worldY / TILE_SIZE)
     */
    private static boolean solidAt(int[] tiles, int mapWidth, float worldX, float worldY) {
        int col = (int)(worldX / TILE_SIZE);
        int row = (int)(worldY / TILE_SIZE);
        if (col < 0 || row < 0 || col >= mapWidth) return true; // treat edge as wall
        int idx = row * mapWidth + col;
        if (idx >= tiles.length) return true;
        return tiles[idx] == TILE_WALL;
    }

    /**
     * Grounded = solid tile exists one pixel below each foot corner.
     * Player origin is top-left; feet are at player.y + PLAYER_HEIGHT.
     */
    private static boolean isGrounded(int[] tiles, int mapWidth, ActivePlayer p) {
        float feetY = p.y + PLAYER_HEIGHT + 1f;
        return solidAt(tiles, mapWidth, p.x,                   feetY)
            || solidAt(tiles, mapWidth, p.x + PLAYER_WIDTH - 1f, feetY);
    }

    void sleepuntil(long targetnanos) {
        long remaining = targetnanos - System.nanoTime();
        if (remaining <= 0) {
            return;
        }
        long parknanos = remaining - SPIN_THRESHOLD_NS;
        if (parknanos > 0) {
            LockSupport.parkNanos(parknanos);
        }
        while (targetnanos > System.nanoTime()) {
            Thread.onSpinWait();
        }
    }
}