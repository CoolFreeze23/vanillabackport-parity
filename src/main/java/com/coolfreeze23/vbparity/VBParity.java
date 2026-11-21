package com.coolfreeze23.vbparity;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(VBParity.MOD_ID)
public final class VBParity {
    public static final String MOD_ID = "vbparity";
    public static final Logger LOG = LoggerFactory.getLogger("VBParity");

    public VBParity(IEventBus modBus) {
        LOG.info("VanillaBackport Parity loaded");
    }
}
