package com.kaloyandonev.serverfuser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class ModSetHash {
    private ModSetHash() {}

    /** SHA-256 over the sorted SHA-256 hashes of every .jar directly inside modsDir. 32 bytes. */
    public static byte[] compute(Path modsDir) throws IOException {
        List<byte[]> hashes = new ArrayList<>();

        if (Files.isDirectory(modsDir)) {
            List<Path> jars;
            try (Stream<Path> s = Files.list(modsDir)) {
                jars = s.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                        .toList();
            }
            for (Path jar : jars) hashes.add(hashFile(jar));
        }

        hashes.sort(Arrays::compare);          // order-independent: filesystem listing order doesn't matter

        MessageDigest outer = sha256();
        for (byte[] h : hashes) outer.update(h);
        return outer.digest();
    }

    private static byte[] hashFile(Path file) throws IOException {
        MessageDigest md = sha256();
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
        }
        return md.digest();
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);   // every JVM is required to have SHA-256
        }
    }
}
