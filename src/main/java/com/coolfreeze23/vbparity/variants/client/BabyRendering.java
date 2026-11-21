package com.coolfreeze23.vbparity.variants.client;

import com.blackgear.platform.core.BuiltInCoreRegistry;
import com.blackgear.vanillabackport.common.api.modules.mob_variant.VariantDataHolder;
import com.blackgear.vanillabackport.common.api.modules.mob_variant.ClientAsset;
import com.blackgear.vanillabackport.common.api.modules.mob_variant.ModelAndTexture;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.chicken.ChickenVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.chicken.ChickenVariants;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.cow.CowVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.pig.PigVariant;
import com.coolfreeze23.vbparity.variants.BabyAssets;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.cow.CowVariants;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.pig.PigVariants;
import com.coolfreeze23.vbparity.variants.client.model.BabyCatModel;
import com.coolfreeze23.vbparity.variants.client.model.BabyChickenModel;
import com.coolfreeze23.vbparity.variants.client.model.BabyCowModel;
import com.coolfreeze23.vbparity.variants.client.model.BabyPigModel;
import com.coolfreeze23.vbparity.variants.client.model.BabySheepModels;
import com.coolfreeze23.vbparity.variants.client.model.BabyWolfModel;
import com.coolfreeze23.vbparity.variants.client.model.TemperateCowModel;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MushroomCowRenderer;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.Nullable;

/**
 * VARIANTS-08/10/11/12: native 26.2 baby models and textures for calves, piglets, chicks, puppies, kittens and lambs,
 * and the Spring to Life model for temperate adult cows.
 * <p>Baby textures are only known for the native variants (code-driven, so they also work on dedicated servers, where
 * VanillaBackport's data-driven variants never reach the client). A baby whose variant has no known baby texture keeps
 * VanillaBackport's rendering.
 */
public final class BabyRendering {
    public static final ModelLayerLocation TEMPERATE_COW = layer("temperate_cow");
    public static final ModelLayerLocation COW_BABY = layer("cow_baby");
    public static final ModelLayerLocation PIG_BABY = layer("pig_baby");
    public static final ModelLayerLocation CHICKEN_BABY = layer("chicken_baby");
    public static final ModelLayerLocation SHEEP_BABY = layer("sheep_baby");
    public static final ModelLayerLocation SHEEP_BABY_WOOL = layer("sheep_baby_wool");
    public static final ModelLayerLocation WOLF_BABY = layer("wolf_baby");
    public static final ModelLayerLocation CAT_BABY = layer("cat_baby");
    public static final ModelLayerLocation CAT_BABY_COLLAR = layer("cat_baby_collar");

    public static final ResourceLocation SHEEP_BABY_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/sheep/sheep_baby.png");
    public static final ResourceLocation SHEEP_BABY_WOOL_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/sheep/sheep_wool_baby.png");
    public static final ResourceLocation WOLF_COLLAR_BABY_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_collar_baby.png");
    public static final ResourceLocation CAT_COLLAR_BABY_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/cat/cat_collar_baby.png");

    private static final Set<String> FARM_VARIANTS = Set.of("temperate", "warm", "cold");
    private static final Set<String> CAT_TEXTURES = Set.of(
        "tabby", "black", "red", "siamese", "british_shorthair", "calico", "persian", "ragdoll", "white", "jellie", "all_black"
    );
    private static final Set<String> WOLF_TEXTURES = Set.of(
        "wolf", "wolf_tame", "wolf_angry",
        "wolf_ashen", "wolf_ashen_tame", "wolf_ashen_angry",
        "wolf_black", "wolf_black_tame", "wolf_black_angry",
        "wolf_chestnut", "wolf_chestnut_tame", "wolf_chestnut_angry",
        "wolf_rusty", "wolf_rusty_tame", "wolf_rusty_angry",
        "wolf_snowy", "wolf_snowy_tame", "wolf_snowy_angry",
        "wolf_spotted", "wolf_spotted_tame", "wolf_spotted_angry",
        "wolf_striped", "wolf_striped_tame", "wolf_striped_angry",
        "wolf_woods", "wolf_woods_tame", "wolf_woods_angry"
    );

    private BabyRendering() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("vbparity", name), "main");
    }

    @Nullable
    private static <T> String farmVariant(Entity entity, BuiltInCoreRegistry<T> registry) {
        Optional<T> variant = VariantDataHolder.<T>getHolder(entity).flatMap(VariantDataHolder::getVariantData);
        if (variant.isEmpty()) {
            return "temperate";
        }
        ResourceLocation key = registry.getKey(variant.get());
        return key != null && key.getNamespace().equals("minecraft") && FARM_VARIANTS.contains(key.getPath()) ? key.getPath() : null;
    }

    /** The texture of the variant's {@code baby_asset_id} (VARIANTS-10), when its data sets one. */
    @Nullable
    private static ResourceLocation explicitBabyTexture(Entity entity, Class<?> variantType) {
        Optional<Object> variant = VariantDataHolder.<Object>getHolder(entity).flatMap(VariantDataHolder::getVariantData);
        if (variant.isEmpty() || !variantType.isInstance(variant.get())) {
            return null;
        }
        Object value = variant.get();
        ModelAndTexture<?> modelAndTexture = value instanceof CowVariant cow ? cow.modelAndTexture()
            : value instanceof PigVariant pig ? pig.modelAndTexture()
            : value instanceof ChickenVariant chicken ? chicken.modelAndTexture() : null;
        ClientAsset baby = BabyAssets.get(modelAndTexture);
        return baby != null ? baby.path() : null;
    }

    /** Native baby texture for this baby, or null when the adult rendering must be kept. */
    @Nullable
    public static ResourceLocation babyTexture(LivingEntity entity) {
        if (!entity.isBaby()) {
            return null;
        }
        String variant;
        if (entity instanceof MushroomCow mooshroom) {
            return ResourceLocation.withDefaultNamespace(
                "textures/entity/cow/mooshroom_" + (mooshroom.getVariant() == MushroomCow.MushroomType.BROWN ? "brown" : "red") + "_baby.png"
            );
        } else if (entity instanceof Cow && entity.getType() == EntityType.COW) {
            ResourceLocation explicit = explicitBabyTexture(entity, CowVariant.class);
            if (explicit != null) {
                return explicit;
            }
            variant = farmVariant(entity, CowVariants.REGISTRIES);
            return variant == null ? null : ResourceLocation.withDefaultNamespace("textures/entity/cow/cow_" + variant + "_baby.png");
        } else if (entity instanceof Pig) {
            ResourceLocation explicit = explicitBabyTexture(entity, PigVariant.class);
            if (explicit != null) {
                return explicit;
            }
            variant = farmVariant(entity, PigVariants.REGISTRIES);
            return variant == null ? null : ResourceLocation.withDefaultNamespace("textures/entity/pig/pig_" + variant + "_baby.png");
        } else if (entity instanceof Chicken) {
            ResourceLocation explicit = explicitBabyTexture(entity, ChickenVariant.class);
            if (explicit != null) {
                return explicit;
            }
            variant = farmVariant(entity, ChickenVariants.REGISTRIES);
            return variant == null ? null : ResourceLocation.withDefaultNamespace("textures/entity/chicken/chicken_" + variant + "_baby.png");
        } else if (entity instanceof Wolf || entity instanceof Cat) {
            String prefix = entity instanceof Wolf ? "textures/entity/wolf/" : "textures/entity/cat/";
            ResourceLocation adultTexture = entity instanceof Wolf wolf ? wolf.getTexture() : ((Cat) entity).getTextureId();
            if (adultTexture == null || !adultTexture.getNamespace().equals("minecraft") || !adultTexture.getPath().startsWith(prefix)
                || !adultTexture.getPath().endsWith(".png")) {
                return null;
            }
            String name = adultTexture.getPath().substring(prefix.length(), adultTexture.getPath().length() - 4);
            if (entity instanceof Wolf) {
                return WOLF_TEXTURES.contains(name) ? ResourceLocation.withDefaultNamespace(prefix + name + "_baby.png") : null;
            }
            return CAT_TEXTURES.contains(name) ? ResourceLocation.withDefaultNamespace(prefix + "cat_" + name + "_baby.png") : null;
        }
        return null;
    }

    /** Whether this cow, pig or chicken baby gets the native baby rendering (VanillaBackport's variant swap must stand aside). */
    public static boolean hasFarmBaby(LivingEntity entity) {
        return entity.isBaby() && (entity instanceof Cow || entity instanceof Pig || entity instanceof Chicken) && babyTexture(entity) != null;
    }

    /** The per-renderer model set; one is created for each vanilla renderer this feature covers. */
    public abstract static class Kit {
        @Nullable
        public abstract EntityModel<?> select(LivingEntity entity, EntityModel<?> current, float partialTick);
    }

    @Nullable
    public static Kit create(LivingEntityRenderer<?, ?> renderer, EntityRendererProvider.Context context, EntityModel<?> vanillaModel) {
        Class<?> type = renderer.getClass();
        if (type == CowRenderer.class) {
            TemperateCowModel temperate = new TemperateCowModel(context.bakeLayer(TEMPERATE_COW));
            BabyCowModel baby = new BabyCowModel(context.bakeLayer(COW_BABY));
            return new Kit() {
                @Override
                public EntityModel<?> select(LivingEntity entity, EntityModel<?> current, float partialTick) {
                    if (entity.isBaby()) {
                        return babyTexture(entity) != null ? baby : null;
                    }
                    // VanillaBackport swaps in its own model for warm and cold cows; temperate (or variants off) keeps the vanilla one.
                    return current == vanillaModel ? temperate : null;
                }
            };
        } else if (type == MushroomCowRenderer.class) {
            // Native MushroomCowRenderer: the Spring to Life CowModel for adults (64x64 mooshroom textures), BabyCowModel for calves.
            TemperateCowModel adult = new TemperateCowModel(context.bakeLayer(TEMPERATE_COW));
            BabyCowModel baby = new BabyCowModel(context.bakeLayer(COW_BABY));
            return new Kit() {
                @Override
                public EntityModel<?> select(LivingEntity entity, EntityModel<?> current, float partialTick) {
                    return entity.isBaby() ? baby : adult;
                }
            };
        } else if (type == PigRenderer.class) {
            return simple(new BabyPigModel(context.bakeLayer(PIG_BABY)));
        } else if (type == ChickenRenderer.class) {
            return simple(new BabyChickenModel(context.bakeLayer(CHICKEN_BABY)));
        } else if (type == CatRenderer.class) {
            return simple(new BabyCatModel(context.bakeLayer(CAT_BABY), 1.0F));
        } else if (type == WolfRenderer.class) {
            BabyWolfModel baby = new BabyWolfModel(context.bakeLayer(WOLF_BABY));
            return new Kit() {
                @Override
                public EntityModel<?> select(LivingEntity entity, EntityModel<?> current, float partialTick) {
                    if (babyTexture(entity) == null) {
                        return null;
                    }
                    // WolfRenderer tints its own model before rendering; give the puppy the same wet shade.
                    Wolf wolf = (Wolf) entity;
                    baby.setColor(wolf.isWet() ? FastColor.ARGB32.colorFromFloat(1.0F, wolf.getWetShade(partialTick), wolf.getWetShade(partialTick), wolf.getWetShade(partialTick)) : -1);
                    return baby;
                }
            };
        } else if (type == SheepRenderer.class) {
            BabySheepModels.Body baby = new BabySheepModels.Body(context.bakeLayer(SHEEP_BABY));
            return new Kit() {
                @Override
                public EntityModel<?> select(LivingEntity entity, EntityModel<?> current, float partialTick) {
                    return entity.isBaby() ? baby : null;
                }
            };
        }
        return null;
    }

    private static Kit simple(EntityModel<?> baby) {
        return new Kit() {
            @Override
            public EntityModel<?> select(LivingEntity entity, EntityModel<?> current, float partialTick) {
                return babyTexture(entity) != null ? baby : null;
            }
        };
    }
}
