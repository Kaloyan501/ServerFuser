package com.kaloyandonev.serverFuserVelocity.PacketHandler;

import com.kaloyandonev.serverfuser.Messages.PlayerData;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;

public class PlayerHandler {
    public static void handle(PlayerData pd){
        try {
            byte[] calcSha = MessageDigest.getInstance("SHA-256").digest(pd.nbt());

            if (!MessageDigest.isEqual(calcSha, pd.sha())) {
                throw new IOException("SHA Checksum invalid. Throwing data out.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        System.out.println(String.format("Got player data for playerUUID: %s with length %d and sha %s", pd.playerUUID().toString(), pd.nbtLen(), HexFormat.of().formatHex(pd.sha())));
        System.out.println(HexFormat.of().formatHex(pd.nbt()));
    }
}
