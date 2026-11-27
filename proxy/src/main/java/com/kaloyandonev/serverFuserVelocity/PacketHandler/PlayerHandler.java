package com.kaloyandonev.serverFuserVelocity.PacketHandler;

import com.kaloyandonev.serverFuserVelocity.ServerFuserVelocity;
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
            ServerFuserVelocity.logger.error(e.toString());
            return;
        }

        ServerFuserVelocity.logger.info("Got player data for playerUUID: {} with length {} and sha {}\n", pd.playerUUID().toString(), pd.nbtLen(), HexFormat.of().formatHex(pd.sha()));
        ServerFuserVelocity.logger.info(HexFormat.of().formatHex(pd.nbt()));
    }
}
