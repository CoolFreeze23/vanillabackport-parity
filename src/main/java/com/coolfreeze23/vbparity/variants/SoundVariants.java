package com.coolfreeze23.vbparity.variants;

import com.blackgear.platform.common.data.entity.SyncedDataHolder;
import com.blackgear.platform.common.data.entity.SyncedDataKey;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import org.jetbrains.annotations.Nullable;

/**
 * Native 26.2 cow, pig, chicken and cat sound variants ({@code data/minecraft/*_sound_variant/*.json}).
 * The entries are registered in code on both sides, because VanillaBackport's data-driven variant registries are never
 * sent to clients. The chosen variant is stored as the entity's {@code sound_variant} (same NBT key and values as
 * native) and synced through Platform's entity data.
 */
public final class SoundVariants {
    public static final String NBT_KEY = "sound_variant";
    public static final ResourceLocation CLASSIC = ResourceLocation.withDefaultNamespace("classic");

    public record CowSet(SoundEvent ambient, SoundEvent death, SoundEvent hurt, SoundEvent step) {
    }

    public record PigSet(SoundEvent ambient, SoundEvent death, SoundEvent eat, SoundEvent hurt, SoundEvent step) {
    }

    public record ChickenSet(SoundEvent ambient, SoundEvent death, SoundEvent hurt, SoundEvent step) {
    }

    public record CatSet(
        SoundEvent ambient, SoundEvent begForFood, SoundEvent death, SoundEvent eat, SoundEvent hiss,
        SoundEvent hurt, SoundEvent purr, SoundEvent purreow, SoundEvent strayAmbient
    ) {
    }

    public record Variant<S>(S adult, S baby) {
        public S get(LivingEntity entity) {
            return entity.isBaby() ? this.baby : this.adult;
        }
    }

    /** One family of sound variants: entries in native registry order (sorted by id, as data-driven registries load). */
    public static final class Family<S> {
        private final Map<ResourceLocation, Variant<S>> entries = new LinkedHashMap<>();
        private final List<ResourceLocation> order;
        private final SyncedDataKey<ResourceLocation> key;

        @SafeVarargs
        private Family(String name, Map.Entry<String, Variant<S>>... entries) {
            for (Map.Entry<String, Variant<S>> e : entries) {
                this.entries.put(ResourceLocation.withDefaultNamespace(e.getKey()), e.getValue());
            }
            this.order = List.copyOf(this.entries.keySet());
            this.key = SyncedDataKey.create(
                ResourceLocation.fromNamespaceAndPath("vbparity", name), ResourceLocation.STREAM_CODEC, () -> CLASSIC,
                SyncedDataKey.SyncMode.TRACKING_ONLY
            );
        }

        public ResourceLocation getId(Entity entity) {
            return SyncedDataHolder.get(entity).get(this.key);
        }

        public void setId(Entity entity, ResourceLocation id) {
            if (this.entries.containsKey(id)) {
                SyncedDataHolder.get(entity).set(this.key, id);
            }
        }

        public Variant<S> get(Entity entity) {
            Variant<S> variant = this.entries.get(this.getId(entity));
            return variant != null ? variant : this.entries.get(CLASSIC);
        }

        public S sounds(LivingEntity entity) {
            return this.get(entity).get(entity);
        }

        /** Native {@code *SoundVariants.pickRandomSoundVariant}: a uniform pick over the registry. */
        public void pickRandom(Entity entity, RandomSource random) {
            this.setId(entity, this.order.get(random.nextInt(this.order.size())));
        }

        public void save(Entity entity, CompoundTag tag) {
            tag.putString(NBT_KEY, this.getId(entity).toString());
        }

        public void load(Entity entity, CompoundTag tag) {
            if (tag.contains(NBT_KEY, 8)) {
                ResourceLocation id = ResourceLocation.tryParse(tag.getString(NBT_KEY));
                if (id != null) {
                    this.setId(entity, id);
                }
            }
        }

        public List<ResourceLocation> ids() {
            return this.order;
        }
    }

    private static <S> Map.Entry<String, Variant<S>> entry(String id, S adult, S baby) {
        return Map.entry(id, new Variant<>(adult, baby));
    }

    // Cow sound sets have no baby split natively.
    private static final CowSet COW_CLASSIC = new CowSet(SoundEvents.COW_AMBIENT, SoundEvents.COW_DEATH, SoundEvents.COW_HURT, SoundEvents.COW_STEP);
    private static final CowSet COW_MOODY = new CowSet(
        VariantSounds.COW_MOODY_AMBIENT, VariantSounds.COW_MOODY_DEATH, VariantSounds.COW_MOODY_HURT, VariantSounds.COW_MOODY_STEP
    );
    public static final Family<CowSet> COW = new Family<>(
        "cow_sound_variant", entry("classic", COW_CLASSIC, COW_CLASSIC), entry("moody", COW_MOODY, COW_MOODY)
    );

    private static final PigSet PIG_BABY = new PigSet(
        VariantSounds.BABY_PIG_AMBIENT, VariantSounds.BABY_PIG_DEATH, VariantSounds.BABY_PIG_EAT, VariantSounds.BABY_PIG_HURT, VariantSounds.BABY_PIG_STEP
    );
    public static final Family<PigSet> PIG = new Family<>(
        "pig_sound_variant",
        entry("big", new PigSet(VariantSounds.PIG_BIG_AMBIENT, VariantSounds.PIG_BIG_DEATH, VariantSounds.PIG_BIG_EAT, VariantSounds.PIG_BIG_HURT, SoundEvents.PIG_STEP), PIG_BABY),
        entry("classic", new PigSet(SoundEvents.PIG_AMBIENT, SoundEvents.PIG_DEATH, VariantSounds.PIG_EAT, SoundEvents.PIG_HURT, SoundEvents.PIG_STEP), PIG_BABY),
        entry("mini", new PigSet(VariantSounds.PIG_MINI_AMBIENT, VariantSounds.PIG_MINI_DEATH, VariantSounds.PIG_MINI_EAT, VariantSounds.PIG_MINI_HURT, SoundEvents.PIG_STEP), PIG_BABY)
    );

    private static final ChickenSet CHICKEN_BABY = new ChickenSet(
        VariantSounds.BABY_CHICKEN_AMBIENT, VariantSounds.BABY_CHICKEN_DEATH, VariantSounds.BABY_CHICKEN_HURT, VariantSounds.BABY_CHICKEN_STEP
    );
    public static final Family<ChickenSet> CHICKEN = new Family<>(
        "chicken_sound_variant",
        entry("classic", new ChickenSet(SoundEvents.CHICKEN_AMBIENT, SoundEvents.CHICKEN_DEATH, SoundEvents.CHICKEN_HURT, SoundEvents.CHICKEN_STEP), CHICKEN_BABY),
        entry("picky", new ChickenSet(VariantSounds.CHICKEN_PICKY_AMBIENT, VariantSounds.CHICKEN_PICKY_DEATH, VariantSounds.CHICKEN_PICKY_HURT, SoundEvents.CHICKEN_STEP), CHICKEN_BABY)
    );

    private static final CatSet CAT_BABY = new CatSet(
        VariantSounds.BABY_CAT_AMBIENT, VariantSounds.BABY_CAT_BEG_FOR_FOOD, VariantSounds.BABY_CAT_DEATH, VariantSounds.BABY_CAT_EAT,
        VariantSounds.BABY_CAT_HISS, VariantSounds.BABY_CAT_HURT, VariantSounds.BABY_CAT_PURR, VariantSounds.BABY_CAT_PURREOW,
        VariantSounds.BABY_CAT_STRAY_AMBIENT
    );
    public static final Family<CatSet> CAT = new Family<>(
        "cat_sound_variant",
        entry("classic", new CatSet(
            SoundEvents.CAT_AMBIENT, SoundEvents.CAT_BEG_FOR_FOOD, SoundEvents.CAT_DEATH, SoundEvents.CAT_EAT, SoundEvents.CAT_HISS,
            SoundEvents.CAT_HURT, SoundEvents.CAT_PURR, SoundEvents.CAT_PURREOW, SoundEvents.CAT_STRAY_AMBIENT
        ), CAT_BABY),
        entry("royal", new CatSet(
            VariantSounds.CAT_ROYAL_AMBIENT, VariantSounds.CAT_ROYAL_BEG_FOR_FOOD, VariantSounds.CAT_ROYAL_DEATH, VariantSounds.CAT_ROYAL_EAT,
            VariantSounds.CAT_ROYAL_HISS, VariantSounds.CAT_ROYAL_HURT, VariantSounds.CAT_ROYAL_PURR, VariantSounds.CAT_ROYAL_PURREOW,
            VariantSounds.CAT_ROYAL_STRAY_AMBIENT
        ), CAT_BABY)
    );

    private SoundVariants() {
    }

    /** Native mooshrooms always use the classic cow set ({@code AbstractCow.getSoundSet}); only plain cows vary. */
    public static boolean isPlainCow(Entity entity) {
        return entity instanceof Cow && entity.getType() == EntityType.COW;
    }

    /** The sound-variant family an entity uses, or null. */
    @Nullable
    public static Family<?> familyOf(Entity entity) {
        if (isPlainCow(entity)) {
            return COW;
        } else if (entity instanceof Pig) {
            return PIG;
        } else if (entity instanceof Chicken) {
            return CHICKEN;
        } else {
            return entity instanceof Cat ? CAT : null;
        }
    }

    /** Forces class loading so the synced-data keys exist on both sides before any entity data arrives. */
    public static void init() {
    }
}
