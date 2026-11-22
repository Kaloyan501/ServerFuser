package com.kaloyandonev.serverfuser.Messages.Helpers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public final class Wire {
    private Wire() {}

    public static void writeUuid(DataOutputStream o, UUID u) throws IOException {
        o.writeLong(u.getMostSignificantBits());
        o.writeLong(u.getLeastSignificantBits());
    }

    public static UUID readUuid(DataInputStream d) throws IOException {
        return new UUID(d.readLong(), d.readLong());
    }

    /** Reads a length-prefixed byte array, rejecting bad lengths. */
    public static byte[] readBlob(DataInputStream d, int max) throws IOException {
        int len = d.readInt();
        if (len < 0 || len > max || len > d.available())
            throw new IOException("bad blob length " + len);
        byte[] b = new byte[len];
        d.readFully(b);
        return b;
    }

    public static void writeBlob(DataOutputStream o, byte[] b) throws IOException {
        o.writeInt(b.length);
        o.write(b);
    }
}
