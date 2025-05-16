package org.kurva.werlii;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Werlii implements ModInitializer {
    public static final String MOD_ID = "werlii";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Werlii mod");
    }
}

