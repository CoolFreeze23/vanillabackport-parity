package com.coolfreeze23.vbparity.shared;

import com.blackgear.vanillabackport.core.data.tags.ModItemTags;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;

/**
 * Native cooking fuel for the wooden spear (200 ticks) and for wool slabs and cushions: both use {@code cooking/time_wool_slabs} = 50 ticks
 * (VanillaBackport sets 100 and 200 through its own fuel event, so this runs after it).
 */
@EventBusSubscriber(modid = "vbparity")
public final class SharedFuel {
    public static final int WOOL_SLAB_BURN_TIME = 50;
    public static final int WOODEN_SPEAR_BURN_TIME = 200;

    private SharedFuel() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void furnaceFuel(FurnaceFuelBurnTimeEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(ModItemTags.CUSHIONS) || stack.is(ModItemTags.WOOL_SLABS)) {
            event.setBurnTime(WOOL_SLAB_BURN_TIME);
        } else if (stack.is(com.blackgear.vanillabackport.common.registries.items.ModItems.WOODEN_SPEAR.get())
            && com.blackgear.vanillabackport.core.VanillaBackport.COMMON_CONFIG.hasSpears.get()) {
            // native FuelValues: .add(Items.WOODEN_SPEAR, baseUnit) with baseUnit 200
            event.setBurnTime(WOODEN_SPEAR_BURN_TIME);
        }
    }
}
