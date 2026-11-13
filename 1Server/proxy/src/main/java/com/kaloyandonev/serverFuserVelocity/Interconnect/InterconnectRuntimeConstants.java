package com.kaloyandonev.serverFuserVelocity.Interconnect;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InterconnectRuntimeConstants {
    public static final ConcurrentHashMap<UUID, ShardConnection> CONNECTIONS = new ConcurrentHashMap<>();

    static volatile boolean baselineSet = false;
    public static int dataVer = 0;
    public static byte[] modHashSet = null;
}
