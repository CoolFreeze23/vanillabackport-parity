package com.coolfreeze23.vbparity.copper;

import com.blackgear.vanillabackport.common.level.blocks.CopperGolemStatueBlock;
import com.blackgear.vanillabackport.common.level.components.KineticWeapon;
import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.blackgear.vanillabackport.common.registries.items.ModDataComponents;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.blackgear.vanillabackport.core.data.tags.ModItemTags;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;

public final class CopperParityEvents {
    private CopperParityEvents() {
    }

    /** Native copper spear dismount threshold (Items.COPPER_SPEAR spear(..., 4.0F, 12.0F, ...)). */
    public static final float COPPER_SPEAR_DISMOUNT_THRESHOLD = 12.0F;
    /** Native FuelValues: wooden shelves burn for baseUnit * 3 / 2 = 300 ticks. */
    public static final int SHELF_BURN_TIME = 300;

    public static List<Supplier<Block>> statues() {
        return List.of(
            ModBlocks.COPPER_GOLEM_STATUE, ModBlocks.EXPOSED_COPPER_GOLEM_STATUE, ModBlocks.WEATHERED_COPPER_GOLEM_STATUE, ModBlocks.OXIDIZED_COPPER_GOLEM_STATUE,
            ModBlocks.WAXED_COPPER_GOLEM_STATUE, ModBlocks.WAXED_EXPOSED_COPPER_GOLEM_STATUE, ModBlocks.WAXED_WEATHERED_COPPER_GOLEM_STATUE,
            ModBlocks.WAXED_OXIDIZED_COPPER_GOLEM_STATUE
        );
    }

    @EventBusSubscriber(modid = "vbparity", bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        public static void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
            // COPPER-29: statue items carry {copper_golem_pose: standing} by default, so they stack with mined statues.
            BlockItemStateProperties standing = BlockItemStateProperties.EMPTY.with(CopperGolemStatueBlock.POSE, CopperGolemStatueBlock.Pose.STANDING);
            for (Supplier<Block> statue : statues()) {
                Item item = statue.get().asItem();
                event.modify(item, builder -> builder.set(DataComponents.BLOCK_STATE, standing));
            }

            // COPPER-21: copper spear dismount speed threshold 12.0 (VB 9.0). Only the copper spear belongs to this area.
            Item spear = ModItems.COPPER_SPEAR.get();
            KineticWeapon kinetic = spear.components().get(ModDataComponents.KINETIC_WEAPON.get());
            if (kinetic != null && kinetic.dismountConditions().isPresent()) {
                KineticWeapon.Condition dismount = kinetic.dismountConditions().get();
                KineticWeapon fixed = new KineticWeapon(
                    kinetic.contactCooldownTicks(),
                    kinetic.delayTicks(),
                    Optional.of(new KineticWeapon.Condition(dismount.maxDurationTicks(), COPPER_SPEAR_DISMOUNT_THRESHOLD, dismount.minRelativeSpeed())),
                    kinetic.knockbackConditions(),
                    kinetic.damageConditions(),
                    kinetic.forwardMovement(),
                    kinetic.damageMultiplier(),
                    kinetic.sound(),
                    kinetic.hitSound()
                );
                event.modify(spear, builder -> builder.set(ModDataComponents.KINETIC_WEAPON.get(), fixed));
            }
        }
    }

    @EventBusSubscriber(modid = "vbparity")
    public static final class GameEvents {
        /** COPPER-15: overworld-wood shelves are 300-tick fuel; crimson and warped shelves are non-flammable wood. */
        @SubscribeEvent
        public static void furnaceFuel(FurnaceFuelBurnTimeEvent event) {
            ItemStack stack = event.getItemStack();
            if (event.getBurnTime() <= 0
                && stack.is(ModItemTags.WOODEN_SHELVES)
                && !stack.is(ModBlocks.CRIMSON_SHELF.get().asItem())
                && !stack.is(ModBlocks.WARPED_SHELF.get().asItem())) {
                event.setBurnTime(SHELF_BURN_TIME);
            }
        }
    }
}
