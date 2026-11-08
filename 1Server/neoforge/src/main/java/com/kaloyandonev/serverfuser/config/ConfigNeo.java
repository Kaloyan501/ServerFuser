package com.kaloyandonev.serverfuser.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ConfigNeo {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final ModConfigSpec.ConfigValue<String> HOST =
            B.comment("Interconnect host").define("host", "127.0.0.1");
    public static final ModConfigSpec.IntValue PORT =
            B.defineInRange("port", 6666, 1, 65535);
    public static final ModConfigSpec SPEC = B.build();
}
