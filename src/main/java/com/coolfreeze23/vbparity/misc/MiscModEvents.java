package com.coolfreeze23.vbparity.misc;

import com.blackgear.vanillabackport.common.registries.entities.ModAttributes;
import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.blackgear.vanillabackport.core.ModChecker;
import com.blackgear.vanillabackport.core.VanillaBackport;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;

@EventBusSubscriber(modid = "vbparity", bus = EventBusSubscriber.Bus.MOD)
public final class MiscModEvents {
    private MiscModEvents() {}

    @SubscribeEvent
    public static void registerItems(RegisterEvent event) {
        event.register(Registries.ITEM, helper -> {
            MiscContent.fieldMasonedBannerPattern = MiscContent.newPatternItem(MiscContent.PATTERN_ITEM_FIELD_MASONED);
            MiscContent.bordureIndentedBannerPattern = MiscContent.newPatternItem(MiscContent.PATTERN_ITEM_BORDURE_INDENTED);
            helper.register(MiscContent.FIELD_MASONED_ID, MiscContent.fieldMasonedBannerPattern);
            helper.register(MiscContent.BORDURE_INDENTED_ID, MiscContent.bordureIndentedBannerPattern);
        });
        // MISC-02: the native transmute recipe type (bundle dyeing), and the condition that follows VB's option.
        event.register(Registries.RECIPE_SERIALIZER, helper ->
                helper.register(ResourceLocation.withDefaultNamespace("crafting_transmute"), TransmuteRecipe.SERIALIZER));
        event.register(NeoForgeRegistries.Keys.CONDITION_CODECS, helper ->
                helper.register(ResourceLocation.fromNamespaceAndPath("vbparity", "dyeable_bundles"), TransmuteRecipe.DyeableBundlesCondition.CODEC));
    }

    /**
     * The native lead/shears sound events. Registered last and only when nothing else registered the same id,
     * so another module that adds the same native events does not cause a duplicate registration.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerSounds(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.SOUND_EVENT)) {
            return;
        }
        Registry<SoundEvent> registry = event.getRegistry(Registries.SOUND_EVENT);
        for (ResourceLocation id : MiscContent.SOUND_IDS) {
            if (!registry.containsKey(id)) {
                event.register(Registries.SOUND_EVENT, id, () -> SoundEvent.createVariableRangeEvent(id));
            }
        }
    }

    /** Native {@code Waypoint.addHideAttribute}: carved pumpkin and the seven heads hide their wearer from the locator bar. */
    @SubscribeEvent
    public static void modifyComponents(ModifyDefaultComponentsEvent event) {
        ItemAttributeModifiers hide = ItemAttributeModifiers.builder()
                .add(ModAttributes.WAYPOINT_TRANSMIT_RANGE,
                        new AttributeModifier(ResourceLocation.withDefaultNamespace("waypoint_transmit_range_hide"), -1.0,
                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        EquipmentSlotGroup.HEAD)
                .build()
                .withTooltip(false);
        for (Item item : List.of(Items.CARVED_PUMPKIN, Items.SKELETON_SKULL, Items.WITHER_SKELETON_SKULL, Items.PLAYER_HEAD,
                Items.ZOMBIE_HEAD, Items.CREEPER_HEAD, Items.DRAGON_HEAD, Items.PIGLIN_HEAD)) {
            event.modify(item, builder -> builder.set(DataComponents.ATTRIBUTE_MODIFIERS, hide));
        }
    }

    /**
     * Creative tabs: native contents and order for every backported item (CreativeTabOrder). Native also lists the
     * two new banner pattern items, and the waxed lightning rod and waxed copper chest in Redstone Blocks, which VB
     * leaves out; they are added first and then placed with the rest.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void reorderCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        try {
            if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
                addIfMissing(event, MiscContent.fieldMasonedBannerPattern);
                addIfMissing(event, MiscContent.bordureIndentedBannerPattern);
            } else if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
                if (VanillaBackport.COMMON_CONFIG.hasWeatheringLightningRods.get() || !ModChecker.CAVERNS_AND_CHASMS) {
                    addIfMissing(event, ModBlocks.WAXED_LIGHTNING_ROD.get().asItem());
                }
                addIfMissing(event, ModBlocks.WAXED_COPPER_CHEST.get().asItem());
            }
            CreativeTabOrder.apply(event);
        } catch (IllegalArgumentException e) {
            com.coolfreeze23.vbparity.VBParity.LOG.warn("Could not reorder creative tab {}", event.getTabKey().location(), e);
        }
    }

    private static void addIfMissing(BuildCreativeModeTabContentsEvent event, Item item) {
        ItemStack stack = new ItemStack(item);
        if (!stack.isEmpty() && !contains(event, stack)) {
            event.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    private static boolean contains(BuildCreativeModeTabContentsEvent event, ItemStack stack) {
        return event.getParentEntries().stream().anyMatch(s -> ItemStack.isSameItemSameComponents(s, stack));
    }
}
