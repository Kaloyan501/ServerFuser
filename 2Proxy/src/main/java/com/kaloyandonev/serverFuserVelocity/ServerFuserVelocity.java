package com.kaloyandonev.serverFuserVelocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;

@Plugin(id = "serverfuservelocity", name = "ServerFuserVelocity", version = "1.0-SNAPSHOT")
public class ServerFuserVelocity {

    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Plugin initialization logic goes here
    }
}
