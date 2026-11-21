package com.coolfreeze23.vbparity.misc.client;

import com.blackgear.vanillabackport.core.VanillaBackport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

/** Native {@code Options.cloudRange()} ("Cloud Distance", 2-128 chunks, default 128). */
public interface CloudRangeAccess {
    OptionInstance<Integer> vbp$cloudRange();

    /** The native cloud renderer stands in for VB's "extended cloud reach" option and follows its switch. */
    static boolean nativeClouds() {
        return VanillaBackport.CLIENT_CONFIG.extendedCloudReach.get();
    }

    static int cloudRange() {
        return ((CloudRangeAccess) Minecraft.getInstance().options).vbp$cloudRange().get();
    }
}
