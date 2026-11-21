package com.coolfreeze23.vbparity.misc;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BannerPattern;

import java.util.List;

/**
 * Objects and tags shared by the misc fixes: the two native banner pattern items, the native lead and
 * shears sound events, and the tags read by the boat and Peaceful fixes.
 */
public final class MiscContent {
    private MiscContent() {}

    public static final ResourceLocation FIELD_MASONED_ID = ResourceLocation.withDefaultNamespace("field_masoned_banner_pattern");
    public static final ResourceLocation BORDURE_INDENTED_ID = ResourceLocation.withDefaultNamespace("bordure_indented_banner_pattern");

    public static final TagKey<BannerPattern> PATTERN_ITEM_FIELD_MASONED =
            TagKey.create(Registries.BANNER_PATTERN, ResourceLocation.withDefaultNamespace("pattern_item/field_masoned"));
    public static final TagKey<BannerPattern> PATTERN_ITEM_BORDURE_INDENTED =
            TagKey.create(Registries.BANNER_PATTERN, ResourceLocation.withDefaultNamespace("pattern_item/bordure_indented"));

    /** Native {@code minecraft:cannot_be_pushed_onto_boats}. */
    public static final TagKey<EntityType<?>> CANNOT_BE_PUSHED_ONTO_BOATS =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("cannot_be_pushed_onto_boats"));
    /** VB's tag, corrected to the native list of MONSTER-category types that are allowed in Peaceful. */
    public static final TagKey<EntityType<?>> MONSTERS_THAT_SPAWN_ON_PEACEFUL =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("monsters_that_spawn_on_peaceful"));

    public static final ResourceLocation LEAD_TIED = ResourceLocation.withDefaultNamespace("item.lead.tied");
    public static final ResourceLocation LEAD_UNTIED = ResourceLocation.withDefaultNamespace("item.lead.untied");
    public static final ResourceLocation SHEARS_SNIP = ResourceLocation.withDefaultNamespace("item.shears.snip");
    public static final ResourceLocation SADDLE_UNEQUIP = ResourceLocation.withDefaultNamespace("item.saddle.unequip");
    public static final ResourceLocation HORSE_ARMOR_UNEQUIP = ResourceLocation.withDefaultNamespace("item.horse_armor.unequip");
    public static final ResourceLocation LLAMA_CARPET_UNEQUIP = ResourceLocation.withDefaultNamespace("item.llama_carpet.unequip");
    static final List<ResourceLocation> SOUND_IDS = List.of(LEAD_TIED, LEAD_UNTIED, SHEARS_SNIP, SADDLE_UNEQUIP, HORSE_ARMOR_UNEQUIP, LLAMA_CARPET_UNEQUIP);

    public static Item fieldMasonedBannerPattern;
    public static Item bordureIndentedBannerPattern;

    static Item newPatternItem(TagKey<BannerPattern> tag) {
        return new NativeBannerPatternItem(tag, new Item.Properties().stacksTo(1));
    }

    public static SoundEvent sound(ResourceLocation id) {
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(id);
        if (event == null) {
            throw new IllegalStateException("Sound event " + id + " is not registered");
        }
        return event;
    }

    /** Set while code runs that native creates without the Peaceful check (for example {@code new Zombie(level)}). */
    public static final ThreadLocal<Boolean> IGNORE_PEACEFUL_CHECK = ThreadLocal.withInitial(() -> false);

    /** Native {@code EntityType.isAllowedInPeaceful()}: every MONSTER-category type except the tagged ones is not. */
    public static boolean notAllowedInPeaceful(EntityType<?> type) {
        return type.getCategory() == MobCategory.MONSTER && !type.is(MONSTERS_THAT_SPAWN_ON_PEACEFUL);
    }

    /**
     * Native banner pattern items for the two patterns that native moved out of {@code no_item_required}.
     * They have their own full names and no ".desc" line (native has no such lang keys for them).
     */
    public static final class NativeBannerPatternItem extends BannerPatternItem {
        public NativeBannerPatternItem(TagKey<BannerPattern> bannerPattern, Properties properties) {
            super(bannerPattern, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        }
    }
}
