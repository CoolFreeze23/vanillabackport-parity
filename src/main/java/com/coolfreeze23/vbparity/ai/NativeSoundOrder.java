package com.coolfreeze23.vbparity.ai;

import com.blackgear.platform.core.BuiltInCoreRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * AI-VAR-03 / AI-VAR-06: native WolfSoundVariants.pickRandomSoundVariant draws one nextInt(n) over the data-driven
 * registry, whose entries are in id order (angry, big, classic, cute, grumpy, puglin, sad). VB's registry keeps code
 * registration order, so the same draw gave a different set.
 */
public final class NativeSoundOrder {
    private NativeSoundOrder() {
    }

    public static <T> T pick(BuiltInCoreRegistry<T> registry, RandomSource random) {
        List<Map.Entry<ResourceLocation, T>> entries = new ArrayList<>(registry.entries().entrySet());
        entries.sort(Map.Entry.comparingByKey());
        return entries.get(random.nextInt(entries.size())).getValue();
    }
}
