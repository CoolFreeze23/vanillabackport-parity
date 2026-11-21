package com.coolfreeze23.vbparity.sulfur;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The static, shared {@link BlockBehaviour.Properties} builders of VB's SharedBlockProperties. A block built straight from
 * one of them (VB's supplier-style registrations) gets a private copy, so later {@code .mapColor()} calls on the shared
 * builder for other blocks no longer reach it through {@code BlockBehaviour#properties}. Filled when VB's ModBlocks
 * initialises (before any VB block is built); no other mod's properties are ever in the set.
 */
public final class SharedPropertiesGuard {
    private static final Set<BlockBehaviour.Properties> SHARED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static volatile boolean filled;

    private SharedPropertiesGuard() {
    }

    public static void fill() {
        if (filled) {
            return;
        }
        synchronized (SHARED) {
            if (filled) {
                return;
            }
            try {
                Class<?> shared = Class.forName("com.blackgear.vanillabackport.common.level.blocks.properties.SharedBlockProperties");
                for (Field field : shared.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) && field.getType() == BlockBehaviour.Properties.class) {
                        field.setAccessible(true);
                        SHARED.add((BlockBehaviour.Properties) field.get(null));
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Could not read VB's shared block properties", e);
            }
            filled = true;
        }
    }

    public static BlockBehaviour.Properties privateCopy(BlockBehaviour.Properties properties) {
        if (filled) {
            synchronized (SHARED) {
                if (SHARED.contains(properties)) {
                    return PropertiesCopy.copyOf(properties);
                }
            }
        }
        return properties;
    }
}
