package com.kaloyandonev.serverfuser;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class Transfers {
    private static final ConcurrentHashMap<Long, CompletableFuture<Byte>> PENDING = new ConcurrentHashMap<>();

    public static CompletableFuture<Byte> register(long txId) {
        var f = new CompletableFuture<Byte>().orTimeout(45, TimeUnit.SECONDS);
        PENDING.put(txId, f);
        f.whenComplete((r, t) -> PENDING.remove(txId));
        return f;
    }

    public static void complete(long txId, byte status) {
        var f = PENDING.get(txId);
        if (f != null) f.complete(status);
    }
}
