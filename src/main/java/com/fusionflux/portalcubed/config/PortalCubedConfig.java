package com.fusionflux.portalcubed.config;

import eu.midnightdust.lib.config.MidnightConfig;

@SuppressWarnings("CanBeFinal")
public class PortalCubedConfig extends MidnightConfig {

    @Entry public static boolean enableRoundPortals = false;
    @Entry public static boolean enableAccurateMovement = true;
    @Entry public static int maxBridgeLength = 127;
    @Entry public static float fizzlerDamage = 35;
    @Entry public static float rocketDamage = 35;
    @Entry public static float pelletDamage = 35;
}

