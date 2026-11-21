package com.coolfreeze23.vbparity;

import com.coolfreeze23.vbparity.shared.ParityConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(VBParity.MOD_ID)
public final class VBParity {
    public static final String MOD_ID = "vbparity";
    public static final Logger LOG = LoggerFactory.getLogger("VBParity");

    public VBParity(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ParityConfig.SPEC);
        LOG.info("VanillaBackport Parity loaded");
    }
}
