package com.kaloyandonev.serverFuserVelocity;

import com.google.inject.Inject;
import com.kaloyandonev.serverFuserVelocity.Interconnect.TCP;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.IOException;
import java.nio.file.Path;

@Plugin(id = "serverfuservelocity", name = "ServerFuserVelocity", version = "1.0-SNAPSHOT")
public class ServerFuserVelocity {

    public static Logger logger;
    public static ProxyServer proxy;
    private final Path dataDir;
    private ConfigurationNode config;


    @Inject
    public ServerFuserVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDir) {
        ServerFuserVelocity.proxy = server;
        ServerFuserVelocity.logger = logger;
        this.dataDir = dataDir;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            config = Config.loadConfig(dataDir);
        }catch (IOException e) {
            ServerFuserVelocity.logger.error(e.toString());
        }
        Thread t = new Thread(() -> {
            try {
                TCP.createInterconnectSocket(config.node("serverfuser-proxy", "port").getInt());
                TransferRouter.startSweeper();
            } catch (Exception e) {
                logger.error("Interconnect has blown up.", e);
            }
        }, "ServerFuser-Interconnect-Listener");
        t.setDaemon(true);
        t.start();
    }
}
