package com.coolfreeze23.vbparity.variants;

import com.blackgear.vanillabackport.common.api.modules.mob_variant.VariantDataHolder;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.chicken.ChickenVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.chicken.ChickenVariants;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.blackgear.vanillabackport.core.data.ModBuiltInLootTables;
import com.blackgear.vanillabackport.core.data.tags.ModBiomeTags;
import com.coolfreeze23.vbparity.variants.mixin.access.LivingEntitySoundInvoker;
import com.coolfreeze23.vbparity.variants.mixin.access.MobSoundInvoker;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public final class VariantsGameTests {
    private static final String TEMPLATE = "variants_platform";
    private static final BlockPos MOB_POS = new BlockPos(2, 1, 2);

    private VariantsGameTests() {
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) < 1.0E-5F;
    }

    // VARIANTS-07: the farm-animal biome tags hold exactly the native biomes (no NeoForge c: tags).
    @GameTest(template = TEMPLATE)
    public static void variants_biome_tags(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        TagKey<Biome> cold = ModBiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS;
        TagKey<Biome> warm = ModBiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS;
        Holder<Biome> stonyShore = biomes.getHolderOrThrow(Biomes.STONY_SHORE);
        Holder<Biome> mushroom = biomes.getHolderOrThrow(Biomes.MUSHROOM_FIELDS);
        Holder<Biome> stonyPeaks = biomes.getHolderOrThrow(Biomes.STONY_PEAKS);
        check(helper, !stonyShore.is(cold) && !stonyShore.is(warm), "stony_shore must be temperate");
        check(helper, !mushroom.is(cold) && !mushroom.is(warm), "mushroom_fields must be temperate");
        check(helper, stonyPeaks.is(cold) && !stonyPeaks.is(warm), "stony_peaks must be cold only");
        check(helper, biomes.getHolderOrThrow(Biomes.WINDSWEPT_HILLS).is(cold), "windswept_hills must be cold");
        check(helper, biomes.getHolderOrThrow(Biomes.SNOWY_PLAINS).is(cold), "snowy_plains must be cold");
        check(helper, biomes.getHolderOrThrow(Biomes.THE_END).is(cold), "the_end must be cold (#is_end)");
        check(helper, biomes.getHolderOrThrow(Biomes.DESERT).is(warm), "desert must be warm");
        check(helper, biomes.getHolderOrThrow(Biomes.BAMBOO_JUNGLE).is(warm), "bamboo_jungle must be warm (#is_jungle)");
        check(helper, biomes.getHolderOrThrow(Biomes.NETHER_WASTES).is(warm), "nether_wastes must be warm (#is_nether)");
        check(helper, !biomes.getHolderOrThrow(Biomes.PLAINS).is(cold) && !biomes.getHolderOrThrow(Biomes.PLAINS).is(warm), "plains must be temperate");
        check(helper, biomes.getTag(cold).map(set -> set.size()).orElse(0) >= 21, "cold tag lost native entries");
        helper.succeed();
    }

    private static void checkBaby(GameTestHelper helper, EntityType<? extends AgeableMob> type, float width, float height, float eye, float passengerY) {
        AgeableMob mob = helper.spawn(type, MOB_POS);
        mob.setBaby(true);
        EntityDimensions dims = mob.getDimensions(Pose.STANDING);
        String name = EntityType.getKey(type).getPath();
        check(helper, near(mob.getBbWidth(), width) && near(mob.getBbHeight(), height),
            name + " baby size " + mob.getBbWidth() + "x" + mob.getBbHeight() + ", expected " + width + "x" + height);
        check(helper, near(mob.getEyeHeight(), eye), name + " baby eye height " + mob.getEyeHeight() + ", expected " + eye);
        float y = (float) dims.attachments().get(EntityAttachment.PASSENGER, 0, 0.0F).y;
        check(helper, near(y, passengerY), name + " baby passenger y " + y + ", expected " + passengerY);
        mob.discard();
    }

    // VARIANTS-13: native baby hitboxes, eye heights and passenger attachments.
    @GameTest(template = TEMPLATE)
    public static void variants_baby_dimensions(GameTestHelper helper) {
        checkBaby(helper, EntityType.CHICKEN, 0.3F, 0.4F, 0.28125F, 0.375F);
        checkBaby(helper, EntityType.COW, 0.45F, 0.7F, 0.69F, 0.75F);
        checkBaby(helper, EntityType.MOOSHROOM, 0.45F, 0.7F, 0.69F, 0.75F);
        checkBaby(helper, EntityType.PIG, 0.45F, 0.45F, 0.40625F, 0.5F);
        checkBaby(helper, EntityType.SHEEP, 0.45F, 0.65F, 0.65625F, 0.5625F);
        checkBaby(helper, EntityType.WOLF, 0.3F, 0.425F, 0.34375F, 0.4375F);
        checkBaby(helper, EntityType.CAT, 0.3F, 0.35F, 0.34375F, 0.3125F);
        // Adults keep their normal size.
        Cow cow = helper.spawn(EntityType.COW, MOB_POS);
        check(helper, near(cow.getBbWidth(), 0.9F) && near(cow.getBbHeight(), 1.4F), "adult cow size changed");
        helper.succeed();
    }

    // VARIANTS-01: armor-absorbed damage plays the wolf armor sound even with wolf sound variants on.
    @GameTest(template = TEMPLATE)
    public static void variants_wolf_armor_hurt_sound(GameTestHelper helper) {
        Wolf wolf = helper.spawn(EntityType.WOLF, MOB_POS);
        wolf.setBodyArmorItem(new ItemStack(Items.WOLF_ARMOR));
        SoundEvent armored = ((LivingEntitySoundInvoker) wolf).vbp$getHurtSound(helper.getLevel().damageSources().generic());
        check(helper, armored == SoundEvents.WOLF_ARMOR_DAMAGE, "armored wolf hurt sound was " + armored.getLocation());
        wolf.setBodyArmorItem(ItemStack.EMPTY);
        SoundEvent bare = ((LivingEntitySoundInvoker) wolf).vbp$getHurtSound(helper.getLevel().damageSources().generic());
        check(helper, bare != SoundEvents.WOLF_ARMOR_DAMAGE, "unarmored wolf played the armor sound");
        helper.succeed();
    }

    // VARIANTS-02: puppies use the shared baby wolf sound set.
    @GameTest(template = TEMPLATE)
    public static void variants_baby_wolf_sounds(GameTestHelper helper) {
        Wolf wolf = helper.spawn(EntityType.WOLF, MOB_POS);
        wolf.setBaby(true);
        SoundEvent hurt = ((LivingEntitySoundInvoker) wolf).vbp$getHurtSound(helper.getLevel().damageSources().generic());
        SoundEvent death = ((LivingEntitySoundInvoker) wolf).vbp$getDeathSound();
        check(helper, hurt == VariantSounds.BABY_WOLF_HURT, "puppy hurt sound was " + hurt.getLocation());
        check(helper, death == VariantSounds.BABY_WOLF_DEATH, "puppy death sound was " + death.getLocation());
        Set<SoundEvent> ambient = new HashSet<>();
        for (int i = 0; i < 64; i++) {
            ambient.add(((MobSoundInvoker) wolf).vbp$getAmbientSound());
        }
        check(helper, ambient.contains(VariantSounds.BABY_WOLF_AMBIENT) && ambient.contains(VariantSounds.BABY_WOLF_PANT)
            && ambient.stream().allMatch(s -> s == VariantSounds.BABY_WOLF_AMBIENT || s == VariantSounds.BABY_WOLF_PANT), "puppy ambient sounds " + ambient);
        helper.succeed();
    }

    // VARIANTS-03: moody cows sound moody, the variant is saved as sound_variant, mooshrooms stay classic.
    @GameTest(template = TEMPLATE)
    public static void variants_cow_sound_variant(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, MOB_POS);
        check(helper, ((MobSoundInvoker) cow).vbp$getAmbientSound() == SoundEvents.COW_AMBIENT, "default cow is not classic");
        SoundVariants.COW.setId(cow, ResourceLocation.withDefaultNamespace("moody"));
        check(helper, ((MobSoundInvoker) cow).vbp$getAmbientSound() == VariantSounds.COW_MOODY_AMBIENT, "moody cow ambient");
        check(helper, ((LivingEntitySoundInvoker) cow).vbp$getHurtSound(helper.getLevel().damageSources().generic()) == VariantSounds.COW_MOODY_HURT, "moody cow hurt");
        check(helper, ((LivingEntitySoundInvoker) cow).vbp$getDeathSound() == VariantSounds.COW_MOODY_DEATH, "moody cow death");
        CompoundTag tag = cow.saveWithoutId(new CompoundTag());
        check(helper, "minecraft:moody".equals(tag.getString("sound_variant")), "saved sound_variant was '" + tag.getString("sound_variant") + "'");
        Cow copy = helper.spawn(EntityType.COW, MOB_POS);
        copy.load(tag);
        check(helper, SoundVariants.COW.getId(copy).getPath().equals("moody"), "loaded cow is not moody");
        MushroomCow mooshroom = helper.spawn(EntityType.MOOSHROOM, MOB_POS);
        SoundVariants.COW.setId(mooshroom, ResourceLocation.withDefaultNamespace("moody"));
        check(helper, ((MobSoundInvoker) mooshroom).vbp$getAmbientSound() == SoundEvents.COW_AMBIENT, "mooshroom must stay classic");
        check(helper, !mooshroom.saveWithoutId(new CompoundTag()).contains("sound_variant"), "mooshroom saved a sound_variant");
        helper.succeed();
    }

    // VARIANTS-04/05/06: pig, chicken and cat variants and baby sets.
    @GameTest(template = TEMPLATE)
    public static void variants_pig_chicken_cat_sounds(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, MOB_POS);
        SoundVariants.PIG.setId(pig, ResourceLocation.withDefaultNamespace("big"));
        check(helper, ((LivingEntitySoundInvoker) pig).vbp$getHurtSound(helper.getLevel().damageSources().generic()) == VariantSounds.PIG_BIG_HURT, "big pig hurt");
        SoundVariants.PIG.setId(pig, ResourceLocation.withDefaultNamespace("mini"));
        check(helper, ((MobSoundInvoker) pig).vbp$getAmbientSound() == VariantSounds.PIG_MINI_AMBIENT, "mini pig ambient");
        pig.setBaby(true);
        check(helper, ((MobSoundInvoker) pig).vbp$getAmbientSound() == VariantSounds.BABY_PIG_AMBIENT, "piglet ambient");
        check(helper, ((LivingEntitySoundInvoker) pig).vbp$getDeathSound() == VariantSounds.BABY_PIG_DEATH, "piglet death");

        Chicken chicken = helper.spawn(EntityType.CHICKEN, MOB_POS);
        SoundVariants.CHICKEN.setId(chicken, ResourceLocation.withDefaultNamespace("picky"));
        check(helper, ((MobSoundInvoker) chicken).vbp$getAmbientSound() == VariantSounds.CHICKEN_PICKY_AMBIENT, "picky chicken ambient");
        chicken.setBaby(true);
        check(helper, ((LivingEntitySoundInvoker) chicken).vbp$getHurtSound(helper.getLevel().damageSources().generic()) == VariantSounds.BABY_CHICKEN_HURT, "chick hurt");

        Cat cat = helper.spawn(EntityType.CAT, MOB_POS);
        SoundVariants.CAT.setId(cat, ResourceLocation.withDefaultNamespace("royal"));
        check(helper, ((MobSoundInvoker) cat).vbp$getAmbientSound() == VariantSounds.CAT_ROYAL_STRAY_AMBIENT, "royal stray cat ambient");
        check(helper, ((LivingEntitySoundInvoker) cat).vbp$getDeathSound() == VariantSounds.CAT_ROYAL_DEATH, "royal cat death");
        cat.setBaby(true);
        check(helper, ((LivingEntitySoundInvoker) cat).vbp$getHurtSound(helper.getLevel().damageSources().generic()) == VariantSounds.BABY_CAT_HURT, "kitten hurt");
        check(helper, "minecraft:royal".equals(cat.saveWithoutId(new CompoundTag()).getString("sound_variant")), "cat sound_variant not saved");
        helper.succeed();
    }

    // VARIANTS-03..06: spawning picks sound variants uniformly; breeding-style creation keeps classic.
    @GameTest(template = TEMPLATE)
    public static void variants_sound_variant_spawn_pick(GameTestHelper helper) {
        Set<String> cows = new HashSet<>();
        Set<String> pigs = new HashSet<>();
        for (int i = 0; i < 48; i++) {
            Cow cow = helper.spawn(EntityType.COW, MOB_POS);
            cow.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(cow.blockPosition()), MobSpawnType.SPAWN_EGG, null);
            cows.add(SoundVariants.COW.getId(cow).getPath());
            cow.discard();
            Pig pig = helper.spawn(EntityType.PIG, MOB_POS);
            pig.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(pig.blockPosition()), MobSpawnType.SPAWN_EGG, null);
            pigs.add(SoundVariants.PIG.getId(pig).getPath());
            pig.discard();
        }
        check(helper, cows.equals(Set.of("classic", "moody")), "spawned cow sound variants " + cows);
        check(helper, pigs.equals(Set.of("big", "classic", "mini")), "spawned pig sound variants " + pigs);
        Chicken plain = helper.spawn(EntityType.CHICKEN, MOB_POS);
        check(helper, SoundVariants.CHICKEN.getId(plain).getPath().equals("classic"), "a chicken created without finalizeSpawn must be classic");
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static String randomSequence(LootTable table) {
        try {
            java.lang.reflect.Field field = LootTable.class.getDeclaredField("randomSequence");
            field.setAccessible(true);
            return ((java.util.Optional<ResourceLocation>) field.get(table)).map(ResourceLocation::toString).orElse("");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // VARIANTS-18: the chicken_lay table (with its native random sequence) decides every egg.
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void variants_chicken_lay(GameTestHelper helper) {
        LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(ModBuiltInLootTables.CHICKEN_LAY);
        check(helper, randomSequence(table).equals("minecraft:gameplay/chicken_lay"), "chicken_lay has no native random_sequence");
        Chicken temperate = helper.spawn(EntityType.CHICKEN, new BlockPos(0, 1, 0));
        Chicken cold = helper.spawn(EntityType.CHICKEN, new BlockPos(4, 1, 4));
        ChickenVariant coldVariant = ChickenVariants.REGISTRIES.getOrThrow(ChickenVariants.COLD);
        VariantDataHolder.<ChickenVariant>getHolder(cold).orElseThrow().setVariantData(coldVariant);
        temperate.eggTime = 1;
        cold.eggTime = 1;
        helper.runAfterDelay(5, () -> {
            helper.assertItemEntityCountIs(Items.EGG, new BlockPos(0, 1, 0), 1.5, 1);
            helper.assertItemEntityCountIs(ModItems.BLUE_EGG.get(), new BlockPos(4, 1, 4), 1.5, 1);
            helper.assertItemEntityCountIs(Items.EGG, new BlockPos(4, 1, 4), 1.0, 0);
            check(helper, temperate.eggTime >= 5990 && cold.eggTime >= 5990, "egg timers were not reset: " + temperate.eggTime + ", " + cold.eggTime);
            helper.succeed();
        });
    }
}
