package com.kaloyandonev.serverFuserVelocity.Interconnect;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InterconnectRuntimeConstants {
    public static final ConcurrentHashMap<UUID, ShardConnection> CONNECTIONS = new ConcurrentHashMap<>();

    static volatile boolean baselineSet = false;
    public static int dataVer = 0;
    public static byte[] modHashSet = null;

    public record TxKey(UUID sender, long txId) {}
    public record Pending(UUID target, long expiresAtMs) {}
    public static final ConcurrentHashMap<TxKey, Pending> PENDING = new ConcurrentHashMap<>();
}
