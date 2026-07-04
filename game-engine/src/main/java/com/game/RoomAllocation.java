package com.game;

import java.util.Map;
import java.util.UUID;

public record RoomAllocation(
        int port,
        long startTimeMillis,
        int claimedindex,
        Map<Long, UUID> playerID_To_UUID
) {}
