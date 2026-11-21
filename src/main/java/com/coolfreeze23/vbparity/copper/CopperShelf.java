package com.coolfreeze23.vbparity.copper;

import net.minecraft.world.item.ItemDisplayContext;

/**
 * COPPER-27: native ItemDisplayContext.ON_SHELF, added to the 1.21.1 enum through NeoForge's enum extension
 * (META-INF/enumextensions.json, constant VBPARITY_ON_SHELF). NeoForge requires the serialised name to carry the mod id,
 * so it is "vbparity:on_shelf"; model JSON is read under native's "on_shelf" key (ItemTransformsDeserializerMixin).
 * It has no fallback, like native: a model without on_shelf uses no transform.
 */
public final class CopperShelf {
    public static final String NATIVE_KEY = "on_shelf";
    private static ItemDisplayContext onShelf;

    private CopperShelf() {
    }

    public static ItemDisplayContext onShelf() {
        ItemDisplayContext context = onShelf;
        if (context == null) {
            context = ItemDisplayContext.valueOf("VBPARITY_ON_SHELF");
            onShelf = context;
        }
        return context;
    }
}
