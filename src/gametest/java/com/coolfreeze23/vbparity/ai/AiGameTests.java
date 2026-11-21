package com.coolfreeze23.vbparity.ai;

import static com.coolfreeze23.vbparity.ai.AiTestUtil.check;
import static com.coolfreeze23.vbparity.ai.AiTestUtil.near;
import static com.coolfreeze23.vbparity.ai.AiTestUtil.still;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.camel.CamelHusk;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.AbstractNautilus;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.Nautilus;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.ZombieNautilus;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.skeleton.Parched;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCube;
import com.blackgear.vanillabackport.common.api.modules.sound_variant.SoundVariantHolder;
import com.blackgear.vanillabackport.common.api.modules.sound_variant.WolfSoundVariants;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.coolfreeze23.vbparity.VBParity;
import com.coolfreeze23.vbparity.ai.mixin.BrainAccessor;
import com.coolfreeze23.vbparity.ai.mixin.SensorAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.CatVariantTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Mob-behaviour parity (tech-ai findings): brains, sensors, mounts, sulfur cube knockback, spawn and breeding draws. */
@GameTestHolder(VBParity.MOD_ID)
@PrefixGameTestTemplate(false)
public class AiGameTests {
    private static final String SMALL = "mayhem_small";
    private static final String BIG = "ghast_big";
    private static final String BOX = "sulfur_box";

    // ------------------------------------------------------------------ AI-MNT-01

    @GameTest(template = SMALL)
    public static void ai_nautilus_brains_register_is_tempted(GameTestHelper helper) {
        Nautilus nautilus = still(helper, ModEntityTypes.NAUTILUS.get(), new Vec3(2.5, 2, 2.5));
        ZombieNautilus zombie = still(helper, ModEntityTypes.ZOMBIE_NAUTILUS.get(), new Vec3(4.5, 2, 4.5));
        check(nautilus.getBrain().checkMemory(MemoryModuleType.IS_TEMPTED, MemoryStatus.REGISTERED), "nautilus brain lacks IS_TEMPTED");
        check(zombie.getBrain().checkMemory(MemoryModuleType.IS_TEMPTED, MemoryStatus.REGISTERED), "zombie nautilus brain lacks IS_TEMPTED");
        helper.succeed();
    }

    @GameTest(template = SMALL, timeoutTicks = 140)
    public static void ai_nautilus_follows_tempting_player(GameTestHelper helper) {
        AiTestUtil.water(helper, 1, 2, 1, 5, 4, 5);
        Nautilus nautilus = helper.spawn(ModEntityTypes.NAUTILUS.get(), new Vec3(2.5, 2.5, 2.5));
        ServerPlayer player = AiTestUtil.survival(helper, new Vec3(4.5, 2, 4.5), 140);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD));
        helper.succeedWhen(() -> {
            check(nautilus.getBrain().getMemory(MemoryModuleType.IS_TEMPTED).orElse(false), "the nautilus is not tempted by a player holding cod");
            AiTestUtil.remove(player);
        });
    }

    // ------------------------------------------------------------------ AI-MNT-02

    @GameTest(template = SMALL, timeoutTicks = 100)
    public static void ai_camel_husk_tempted_by_rabbit_foot(GameTestHelper helper) {
        CamelHusk husk = helper.spawn(ModEntityTypes.CAMEL_HUSK.get(), new Vec3(2.5, 2, 2.5));
        ServerPlayer player = AiTestUtil.survival(helper, new Vec3(5.5, 2, 5.5), 100);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RABBIT_FOOT));
        helper.succeedWhen(() -> {
            check(husk.getBrain().getMemory(MemoryModuleType.TEMPTING_PLAYER).isPresent(), "a rabbit foot does not tempt the camel husk");
            AiTestUtil.remove(player);
        });
    }

    @GameTest(template = SMALL, timeoutTicks = 80)
    public static void ai_camel_husk_ignores_cactus(GameTestHelper helper) {
        CamelHusk husk = helper.spawn(ModEntityTypes.CAMEL_HUSK.get(), new Vec3(2.5, 2, 2.5));
        ServerPlayer player = AiTestUtil.survival(helper, new Vec3(5.5, 2, 5.5), 80);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CACTUS));
        helper.runAfterDelay(60, () -> {
            check(husk.getBrain().getMemory(MemoryModuleType.TEMPTING_PLAYER).isEmpty(), "cactus tempts the camel husk");
            AiTestUtil.remove(player);
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ AI-MNT-06

    /** The sun-burn roll comes inside Mob.aiStep, before AbstractHorse's heal (nextInt(900)) roll. */
    @GameTest(template = SMALL, timeoutTicks = 40)
    public static void ai_zombie_horse_burn_roll_before_heal_roll(GameTestHelper helper) {
        ZombieHorse horse = still(helper, EntityType.ZOMBIE_HORSE, new Vec3(3.5, 2, 3.5));
        helper.runAfterDelay(2, () -> {
            ServerLevel level = helper.getLevel();
            long time = level.getDayTime();
            level.setDayTime(6000L);
            level.updateSkyBrightness();
            TraceRandom random = new TraceRandom(11L);
            AiTestUtil.setEntityRandom(horse, random);
            try {
                horse.aiStep();
            } finally {
                level.setDayTime(time);
                level.updateSkyBrightness();
            }
            int burn = random.calls.indexOf("f");
            int heal = random.calls.indexOf("i900");
            check(burn >= 0, "test setup: no sun-burn roll (not day or no sky?) " + random.calls);
            check(heal >= 0, "test setup: no heal roll " + random.calls);
            check(burn < heal, "burn roll after the heal roll: " + random.calls);
            horse.clearFire();
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ AI-MNT-07

    private static BlockSource dispenser(GameTestHelper helper, BlockPos rel, Direction facing) {
        helper.setBlock(rel, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, facing));
        BlockPos abs = helper.absolutePos(rel);
        BlockState state = helper.getLevel().getBlockState(abs);
        return new BlockSource(helper.getLevel(), abs, state, (DispenserBlockEntity) helper.getLevel().getBlockEntity(abs));
    }

    @GameTest(template = SMALL)
    public static void ai_dispenser_equips_tame_nautilus(GameTestHelper helper) {
        BlockSource source = dispenser(helper, new BlockPos(1, 2, 3), Direction.EAST);
        Nautilus nautilus = still(helper, ModEntityTypes.NAUTILUS.get(), new Vec3(2.5, 2, 3.5));
        ItemStack armor = new ItemStack(ModItems.IRON_NAUTILUS_ARMOR.get(), 2);
        check(!ArmorItem.dispenseArmor(source, armor), "a wild nautilus took dispensed armour");
        nautilus.setTame(true, false);
        check(ArmorItem.dispenseArmor(source, armor), "the dispenser did not equip a tame nautilus");
        check(nautilus.getItemBySlot(EquipmentSlot.BODY).is(ModItems.IRON_NAUTILUS_ARMOR.get()), "no armour in the BODY slot");
        check(armor.getCount() == 1, "one armour piece used, " + armor.getCount() + " left");
        check(nautilus.isPersistenceRequired(), "equipping did not make the nautilus persistent");
        helper.succeed();
    }

    // ------------------------------------------------------------------ AI-MNT-08

    @GameTest(template = SMALL, timeoutTicks = 40)
    public static void ai_nautilus_water_travel_animates(GameTestHelper helper) {
        AiTestUtil.water(helper, 1, 2, 1, 5, 4, 5);
        Nautilus nautilus = helper.spawn(ModEntityTypes.NAUTILUS.get(), new Vec3(3.5, 2.5, 3.5));
        helper.runAfterDelay(5, () -> {
            check(nautilus.isInWater(), "test setup: nautilus not in water");
            nautilus.xo = nautilus.getX();
            nautilus.yo = nautilus.getY();
            nautilus.zo = nautilus.getZ();
            nautilus.walkAnimation.setSpeed(0.0F);
            nautilus.setDeltaMovement(Vec3.ZERO);
            nautilus.setSpeed(0.5F);
            nautilus.travel(new Vec3(0.0, 0.0, 1.0));
            check(nautilus.walkAnimation.speed() > 0.0F, "swimming does not update the walk animation");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ AI-MNT-09

    @GameTest(template = SMALL)
    public static void ai_camel_husk_and_zombie_nautilus_never_babies(GameTestHelper helper) {
        CamelHusk husk = still(helper, ModEntityTypes.CAMEL_HUSK.get(), new Vec3(2.5, 2, 2.5));
        ZombieNautilus zombie = still(helper, ModEntityTypes.ZOMBIE_NAUTILUS.get(), new Vec3(4.5, 2, 4.5));
        Cow cow = still(helper, EntityType.COW, new Vec3(2.5, 2, 4.5));
        for (AgeableMob mob : List.<AgeableMob>of(husk, zombie, cow)) {
            CompoundTag tag = new CompoundTag();
            mob.addAdditionalSaveData(tag);
            tag.putInt("Age", -24000);
            mob.readAdditionalSaveData(tag);
        }
        check(!husk.isBaby(), "a camel husk loaded with Age -24000 is a baby");
        check(!zombie.isBaby(), "a zombie nautilus loaded with Age -24000 is a baby");
        check(cow.isBaby(), "control: a cow with Age -24000 should be a baby");
        CompoundTag saved = new CompoundTag();
        husk.addAdditionalSaveData(saved);
        check(!saved.contains("Age") && !saved.contains("ForcedAge"), "camel husk saves Age: " + saved.getAllKeys());
        helper.succeed();
    }

    // ------------------------------------------------------------------ AI-MNT-10

    @GameTest(template = SMALL)
    public static void ai_drowned_turns_with_zombie_nautilus(GameTestHelper helper) {
        ZombieNautilus mount = helper.spawn(ModEntityTypes.ZOMBIE_NAUTILUS.get(), new Vec3(3.5, 2, 3.5));
        Drowned drowned = helper.spawn(EntityType.DROWNED, new Vec3(3.5, 2, 3.5));
        drowned.startRiding(mount, true);
        check(drowned.getControlledVehicle() == mount, "test setup: the drowned does not control the zombie nautilus");
        mount.yBodyRot = 90.0F;
        drowned.yBodyRot = 0.0F;
        drowned.rideTick();
        near(90.0, drowned.yBodyRot, 1.0E-4, "rider body rotation");
        helper.succeed();
    }

    // ------------------------------------------------------------------ AI-SLF-01

    private static SulfurCube blockCube(GameTestHelper helper, Vec3 pos) {
        SulfurCube cube = helper.spawn(ModEntityTypes.SULFUR_CUBE.get(), pos);
        cube.setNoAi(true);
        cube.setSize(2, true);
        cube.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.DIRT));
        return cube;
    }

    /** A knockback after a hit that native passes with comesFromEffect = false (ram, charge) is not scaled. */
    @GameTest(template = BOX, timeoutTicks = 60)
    public static void ai_sulfur_plain_knockback_after_hit_not_scaled(GameTestHelper helper) {
        SulfurCube cube = blockCube(helper, new Vec3(4.5, 2.0, 4.5));
        Zombie attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(2.5, 2.0, 4.5));
        attacker.setYRot(-90.0F);
        attacker.setYHeadRot(-90.0F);
        helper.runAfterDelay(3, () -> {
            check(cube.hasBodyItem(), "cube holds a block");
            DamageSource source = helper.getLevel().damageSources().mobAttack(attacker);
            cube.setDeltaMovement(Vec3.ZERO);
            cube.hurt(source, 4.0F);
            Vec3 first = cube.getDeltaMovement();
            cube.setDeltaMovement(Vec3.ZERO);
            cube.knockback(0.5, attacker.getX() - cube.getX(), attacker.getZ() - cube.getZ());
            Vec3 second = cube.getDeltaMovement();
            check(first.length() > 1.0E-4, "the hit itself knocks the cube back");
            near(1.0, second.length() / first.length(), 1.0E-3, "plain knockback / hit knockback ratio");
            helper.succeed();
        });
    }

    /** Native sweep: the cube is hurt first, then pushed with the archetype knockback (twice the hit's own push). */
    @GameTest(template = BOX, timeoutTicks = 60, attempts = 3, requiredSuccesses = 1)
    public static void ai_sulfur_sweep_hurts_then_pushes(GameTestHelper helper) {
        Zombie target = still(helper, EntityType.ZOMBIE, new Vec3(3.5, 2.0, 4.5));
        SulfurCube cube = blockCube(helper, new Vec3(4.6, 2.0, 4.5));
        ServerPlayer player = AiTestUtil.survival(helper, new Vec3(2.2, 2.0, 4.5), 60);
        helper.runAfterDelay(3, () -> {
            Vec3 p = helper.absoluteVec(new Vec3(2.2, 2.0, 4.5));
            player.moveTo(p.x, p.y, p.z, -90.0F, 0.0F);
            player.setYHeadRot(-90.0F);
            player.setOnGround(true);
            player.setSpeed(0.1F); // the sweep needs walked distance < speed; a mock player never ran aiStep
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            AiTestUtil.set(player, LivingEntity.class, "attackStrengthTicker", 1000);
            cube.setDeltaMovement(Vec3.ZERO);
            player.attack(target);
            Vec3 swept = cube.getDeltaMovement();
            float damage = (float) AiTestUtil.get(cube, SulfurCube.class, "pendingKnockbackDamage");
            check(damage > 0.0F, "test setup: the cube was not swept (" + damage + ")");
            cube.setDeltaMovement(Vec3.ZERO);
            cube.hurt(player.damageSources().playerAttack(player), damage);
            Vec3 single = cube.getDeltaMovement();
            AiTestUtil.remove(player);
            check(single.length() > 1.0E-4, "test setup: the hit alone does not push the cube");
            near(2.0 * single.x, swept.x, 1.0E-4, "swept x");
            near(2.0 * single.y, swept.y, 1.0E-4, "swept y");
            near(2.0 * single.z, swept.z, 1.0E-4, "swept z");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ AI-SLF-03 / 04 / 05

    @GameTest(template = BOX, timeoutTicks = 40)
    public static void ai_sulfur_bucket_keeps_no_persistence(GameTestHelper helper) {
        // GameTestHelper.spawn always marks a mob persistent, so this cube is spawned the plain way.
        SulfurCube cube = ModEntityTypes.SULFUR_CUBE.get().create(helper.getLevel());
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 2.0, 4.5));
        cube.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cube);
        cube.setNoAi(true);
        cube.setSize(2, true);
        check(!cube.isPersistenceRequired(), "test setup: the cube started persistent");
        ServerPlayer player = AiTestUtil.survival(helper, new Vec3(2.5, 2.0, 4.5), 40);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        player.interactOn(cube, InteractionHand.MAIN_HAND);
        ItemStack bucket = player.getItemInHand(InteractionHand.MAIN_HAND);
        check(bucket.is(ModItems.SULFUR_CUBE_BUCKET.get()), "test setup: the cube was not bucketed, hand holds " + bucket);
        CompoundTag data = bucket.getOrDefault(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        AiTestUtil.remove(player);
        check(!data.getBoolean("PersistenceRequired"), "bucketing made the cube persistent: " + data);
        helper.succeed();
    }

    @GameTest(template = BOX)
    public static void ai_sulfur_cube_takes_no_dispensed_armor(GameTestHelper helper) {
        BlockSource source = dispenser(helper, new BlockPos(3, 2, 4), Direction.EAST);
        SulfurCube cube = still(helper, ModEntityTypes.SULFUR_CUBE.get(), new Vec3(4.5, 2.0, 4.5));
        cube.setSize(2, true);
        check(!ArmorItem.dispenseArmor(source, new ItemStack(Items.DIAMOND_CHESTPLATE)), "a chestplate was dispensed onto the cube");
        check(!ArmorItem.dispenseArmor(source, new ItemStack(Items.IRON_HELMET)), "a helmet was dispensed onto the cube");
        check(!cube.hasBodyItem(), "the cube now holds " + cube.getItemBySlot(EquipmentSlot.CHEST));
        helper.succeed();
    }

    @GameTest(template = BOX, timeoutTicks = 40)
    public static void ai_sulfur_dispenser_shears_drop_at_attachment(GameTestHelper helper) {
        BlockSource source = dispenser(helper, new BlockPos(3, 2, 4), Direction.EAST);
        SulfurCube cube = blockCube(helper, new Vec3(4.5, 2.0, 4.5));
        helper.runAfterDelay(2, () -> {
            new ShearsDispenseItemBehavior().dispense(source, new ItemStack(Items.SHEARS));
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, cube.getBoundingBox().inflate(3.0),
                e -> e.getItem().is(Items.DIRT));
            check(!drops.isEmpty(), "test setup: the dispenser did not shear the block off");
            near(cube.getY() + cube.getBbHeight(), drops.get(0).getY(), 1.0E-6, "drop height");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ AI-GHS-01

    @GameTest(template = BIG)
    public static void ai_ghast_wander_target_home_centre(GameTestHelper helper) {
        HappyGhast ghast = still(helper, ModEntityTypes.HAPPY_GHAST.get(), new Vec3(4.5, 4, 4.5));
        BlockPos home = helper.absolutePos(new BlockPos(4, 4, 4));
        ghast.restrictTo(home, 32);
        Vec3 origin = new Vec3(home.getX() + 32.2, home.getY() + 0.5, home.getZ() + 0.5);
        RandomSource random = new TraceRandom(1L).forceFloat(0.5F);
        try {
            Class<?> goal = Class.forName("com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast$RandomFloatAroundGoal");
            Object target = AiTestUtil.invoke(null, goal, "chooseRandomPositionWithRestriction",
                new Class<?>[] {Mob.class, Vec3.class, RandomSource.class}, ghast, origin, random);
            check(target != null, "a target 31.7 from the home centre (inside 32) was rejected");
            Vec3 outside = new Vec3(home.getX() + 32.6, home.getY() + 0.5, home.getZ() + 0.5);
            Object rejected = AiTestUtil.invoke(null, goal, "chooseRandomPositionWithRestriction",
                new Class<?>[] {Mob.class, Vec3.class, RandomSource.class}, ghast, outside, random);
            check(rejected == null, "a target 32.1 from the home centre was kept");
        } catch (ClassNotFoundException e) {
            throw new net.minecraft.gametest.framework.GameTestAssertException("no goal class: " + e);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ AI-GHS-D1

    @GameTest(template = BIG, timeoutTicks = 40)
    public static void ai_ridden_diagonal_input_is_square_fitted(GameTestHelper helper) {
        HappyGhast ghast = still(helper, ModEntityTypes.HAPPY_GHAST.get(), new Vec3(6.5, 4, 6.5));
        Nautilus nautilus = still(helper, ModEntityTypes.NAUTILUS.get(), new Vec3(12.5, 2, 12.5));
        ServerPlayer player = AiTestUtil.survival(helper, new Vec3(2.5, 2, 2.5), 40);
        try {
            for (Object[] mount : new Object[][] {{ghast, HappyGhast.class}, {nautilus, AbstractNautilus.class}}) {
                player.xxa = 0.98F;
                player.zza = 0.0F;
                Vec3 pure = (Vec3) AiTestUtil.invoke(mount[0], (Class<?>) mount[1], "getRiddenInput", new Class<?>[] {Player.class, Vec3.class}, player, Vec3.ZERO);
                player.zza = 0.98F;
                Vec3 diagonal = (Vec3) AiTestUtil.invoke(mount[0], (Class<?>) mount[1], "getRiddenInput", new Class<?>[] {Player.class, Vec3.class}, player, Vec3.ZERO);
                check(Math.abs(pure.x) > 1.0E-6, "test setup: no strafe for " + mount[0]);
                near(0.70710677 / 0.98, diagonal.x / pure.x, 1.0E-5, "diagonal / pure strafe for " + mount[0]);
            }
        } finally {
            AiTestUtil.remove(player);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ AI-GHS-D1b

    /** Native applyInput decays xxa/zza before the AI, so the move control's input reaches travel in full. */
    @GameTest(template = SMALL, timeoutTicks = 40)
    public static void ai_vb_mob_input_decay_before_ai(GameTestHelper helper) {
        Parched parched = helper.spawn(ModEntityTypes.PARCHED.get(), new Vec3(2.5, 2, 2.5));
        Skeleton skeleton = helper.spawn(EntityType.SKELETON, new Vec3(4.5, 2, 4.5));
        helper.runAfterDelay(2, () -> {
            for (Mob mob : List.<Mob>of(parched, skeleton)) {
                mob.getMoveControl().setWantedPosition(mob.getX() + 30.0, mob.getY(), mob.getZ() + 30.0, 1.0);
                mob.aiStep();
            }
            check(parched.getSpeed() > 0.0F, "test setup: the parched's move control did not run");
            near(parched.getSpeed(), parched.zza, 1.0E-7, "parched forward input after aiStep");
            near(skeleton.getSpeed() * 0.98F, skeleton.zza, 1.0E-7, "control: skeleton keeps the 1.21.1 decay");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ AI-GHS-D2

    @GameTest(template = BIG, timeoutTicks = 40)
    public static void ai_ghast_runs_no_ai_while_steered(GameTestHelper helper) {
        HappyGhast ghast = helper.spawn(ModEntityTypes.HAPPY_GHAST.get(), new Vec3(6.5, 4, 6.5));
        ghast.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.WHITE_HARNESS.get()));
        ServerPlayer player = AiTestUtil.survival(helper, new Vec3(6.5, 9, 6.5), 40);
        helper.runAfterDelay(2, () -> {
            player.startRiding(ghast, true);
            AiTestUtil.invoke(ghast, HappyGhast.class, "setServerStillTimeout", new Class<?>[] {int.class}, 0);
            boolean steered = ghast.getControllingPassenger() == player;
            int before = ghast.getNoActionTime();
            ghast.aiStep();
            int after = ghast.getNoActionTime();
            player.stopRiding();
            AiTestUtil.remove(player);
            check(steered, "test setup: the player does not control the ghast");
            check(after == before, "the ghast's AI ran while steered (noActionTime " + before + " -> " + after + ")");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ AI-GHS-D3

    @GameTest(template = BIG)
    public static void ai_home_bias_is_continuous(GameTestHelper helper) {
        HappyGhast ghastling = still(helper, ModEntityTypes.HAPPY_GHAST.get(), new Vec3(6.5, 4, 6.5));
        ghastling.setBaby(true);
        Cow cow = still(helper, EntityType.COW, new Vec3(12.5, 2, 12.5));
        ghastling.restrictTo(helper.absolutePos(new BlockPos(2, 4, 2)), 32);
        cow.restrictTo(helper.absolutePos(new BlockPos(2, 2, 2)), 32);
        TraceRandom vb = new TraceRandom(5L);
        RandomPos.generateRandomPosTowardDirection(ghastling, 10, vb, new BlockPos(3, 0, 3));
        TraceRandom vanilla = new TraceRandom(5L);
        RandomPos.generateRandomPosTowardDirection(cow, 10, vanilla, new BlockPos(3, 0, 3));
        check(vb.calls.equals(List.of("d", "d")), "ghastling home bias draws " + vb.calls);
        check(vanilla.calls.equals(List.of("i5", "i5")), "control: cow home bias draws " + vanilla.calls);
        helper.succeed();
    }

    // ------------------------------------------------------------------ AI-GHS-D4 / AI-MNT-D2

    @GameTest(template = SMALL, timeoutTicks = 40)
    public static void ai_panic_start_stops_the_path(GameTestHelper helper) {
        CamelHusk husk = helper.spawn(ModEntityTypes.CAMEL_HUSK.get(), new Vec3(1.5, 2, 1.5));
        Cow cow = helper.spawn(EntityType.COW, new Vec3(1.5, 2, 5.5));
        helper.runAfterDelay(2, () -> {
            check(husk.getNavigation().moveTo(husk.getX() + 4.0, husk.getY(), husk.getZ(), 1.0), "test setup: no path for the camel husk");
            check(cow.getNavigation().moveTo(cow.getX() + 4.0, cow.getY(), cow.getZ(), 1.0), "test setup: no path for the cow");
            AnimalPanic<PathfinderMob> panic = new AnimalPanic<>(2.0F);
            for (PathfinderMob mob : List.<PathfinderMob>of(husk, cow)) {
                AiTestUtil.invoke(panic, AnimalPanic.class, "start", new Class<?>[] {ServerLevel.class, PathfinderMob.class, long.class},
                    helper.getLevel(), mob, helper.getLevel().getGameTime());
            }
            check(husk.getNavigation().isDone(), "the camel husk kept its path when panic started");
            check(!cow.getNavigation().isDone(), "control: the cow keeps the 1.21.1 behaviour");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ AI-GHS-D5 / AI-MNT-D4

    @GameTest(template = SMALL)
    public static void ai_sensor_delays_from_the_mob_random(GameTestHelper helper) {
        Nautilus nautilus = still(helper, ModEntityTypes.NAUTILUS.get(), new Vec3(3.5, 2, 3.5));
        CompoundTag tag = nautilus.saveWithoutId(new CompoundTag());
        nautilus.getRandom().setSeed(1234L);
        nautilus.load(tag);
        RandomSource expected = RandomSource.create(1234L);
        Map<SensorType<?>, Sensor<?>> sensors = ((BrainAccessor) nautilus.getBrain()).vbp$sensors();
        check(sensors.size() == 5, "test setup: nautilus has " + sensors.size() + " sensors");
        List<Long> got = new ArrayList<>();
        List<Long> want = new ArrayList<>();
        for (Sensor<?> sensor : sensors.values()) {
            got.add(((SensorAccessor) sensor).vbp$timeToTick());
            want.add((long) expected.nextInt(((SensorAccessor) sensor).vbp$scanRate()));
        }
        check(got.equals(want), "sensor start delays " + got + ", native " + want);
        helper.succeed();
    }

    // ------------------------------------------------------------------ AI-MNT-D3

    @GameTest(template = BIG, timeoutTicks = 40)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void ai_sensors_use_follow_range(GameTestHelper helper) {
        Nautilus nautilus = still(helper, ModEntityTypes.NAUTILUS.get(), new Vec3(2.5, 2, 2.5));
        nautilus.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(17.5);
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(19.5, 2, 2.5));
        ServerPlayer player = AiTestUtil.survival(helper, new Vec3(2.5, 2, 19.5), 40);
        helper.runAfterDelay(2, () -> {
            Map<SensorType<?>, Sensor<?>> sensors = ((BrainAccessor) nautilus.getBrain()).vbp$sensors();
            for (SensorType<?> type : List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS)) {
                Sensor sensor = sensors.get(type);
                ((SensorAccessor) sensor).vbp$setTimeToTick(1L);
                sensor.tick(helper.getLevel(), nautilus);
            }
            List<LivingEntity> living = nautilus.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).orElse(List.of());
            List<Player> players = nautilus.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS).orElse(List.of());
            AiTestUtil.remove(player);
            check(living.contains(stand), "an entity 17 blocks away is outside the 17.5 follow range scan");
            check(players.contains(player), "a player 17 blocks away is outside the 17.5 follow range scan");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ AI-MNT-D5

    @GameTest(template = SMALL, timeoutTicks = 40)
    public static void ai_nautilus_waypoint_half_block(GameTestHelper helper) {
        AiTestUtil.water(helper, 1, 2, 1, 5, 4, 5);
        Nautilus nautilus = still(helper, ModEntityTypes.NAUTILUS.get(), new Vec3(3.5, 2.5, 3.5));
        helper.runAfterDelay(2, () -> {
            BlockPos node = helper.absolutePos(new BlockPos(3, 3, 3));
            nautilus.setPos(node.getX() + 0.5, node.getY() - 0.7, node.getZ() + 0.5);
            Path path = new Path(new ArrayList<>(List.of(new Node(node.getX(), node.getY(), node.getZ()))), node, true);
            PathNavigation nav = nautilus.getNavigation();
            nav.moveTo(path, 1.0);
            check(nav.getPath() == path && path.getNextNodeIndex() == 0, "test setup: path not taken");
            AiTestUtil.invoke(nav, PathNavigation.class, "followThePath", new Class<?>[0]);
            check(path.getNextNodeIndex() == 0, "the nautilus advanced past a node 0.7 above it");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ AI-SPR-D1 / D2

    private static boolean canReplace(Mob mob, ItemStack candidate, ItemStack existing) {
        Class<?> owner = mob instanceof Piglin ? Piglin.class : Mob.class;
        return (boolean) AiTestUtil.invoke(mob, owner, "canReplaceCurrentItem", new Class<?>[] {ItemStack.class, ItemStack.class}, candidate, existing);
    }

    @GameTest(template = SMALL)
    public static void ai_spear_pickups_compare_attack_damage(GameTestHelper helper) {
        Zombie zombie = still(helper, EntityType.ZOMBIE, new Vec3(3.5, 2, 3.5));
        ItemStack iron = new ItemStack(ModItems.IRON_SPEAR.get());
        ItemStack diamond = new ItemStack(ModItems.DIAMOND_SPEAR.get());
        ItemStack netherite = new ItemStack(ModItems.NETHERITE_SPEAR.get());
        check(canReplace(zombie, diamond, iron), "a diamond spear does not replace an iron spear");
        check(!canReplace(zombie, iron, diamond), "an iron spear replaces a diamond spear");
        check(canReplace(zombie, netherite, new ItemStack(Items.IRON_SHOVEL)), "a netherite spear (7) does not replace an iron shovel (6.5)");
        check(!canReplace(zombie, new ItemStack(Items.WOODEN_SWORD), netherite), "a wooden sword (6) replaces a netherite spear (7)");
        check(canReplace(zombie, new ItemStack(Items.IRON_SHOVEL), iron), "an iron shovel (6.5) does not replace an iron spear (5)");
        check(canReplace(zombie, new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.IRON_SWORD)), "control: sword upgrade");
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void ai_piglin_golden_spear_vs_crossbow(GameTestHelper helper) {
        Piglin piglin = still(helper, EntityType.PIGLIN, new Vec3(3.5, 2, 3.5));
        piglin.setBaby(false);
        ItemStack worn = new ItemStack(Items.CROSSBOW);
        worn.setDamageValue(10);
        check(canReplace(piglin, new ItemStack(ModItems.GOLDEN_SPEAR.get()), worn), "a new golden spear does not replace a worn crossbow");
        ItemStack wornSpear = new ItemStack(ModItems.GOLDEN_SPEAR.get());
        wornSpear.setDamageValue(10);
        check(canReplace(piglin, new ItemStack(Items.CROSSBOW), wornSpear), "a new crossbow does not replace a worn golden spear");
        check(!canReplace(piglin, new ItemStack(ModItems.GOLDEN_SPEAR.get()), new ItemStack(Items.CROSSBOW)), "equal golden spear replaced the crossbow");
        helper.succeed();
    }

    // ------------------------------------------------------------------ AI-VAR-01 / 02 / 03 / 04 / 05 / SPAWN-11

    private static List<String> spawnTrace(GameTestHelper helper, Mob mob, long seed) {
        ServerLevel level = helper.getLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        mob.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        TraceRandom random = new TraceRandom(seed);
        AiTestUtil.withLevelRandom(level, random, () -> mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null));
        return random.calls;
    }

    @GameTest(template = SMALL)
    public static void ai_farm_animal_variants_drawn_before_super(GameTestHelper helper) {
        List<String> cow = spawnTrace(helper, EntityType.COW.create(helper.getLevel()), 3L);
        List<String> pig = spawnTrace(helper, EntityType.PIG.create(helper.getLevel()), 3L);
        check(cow.equals(List.of("i1", "i2", "d", "d", "f")), "cow spawn draws " + cow);
        check(pig.equals(List.of("i1", "i3", "d", "d", "f")), "pig spawn draws " + pig);
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void ai_mooshroom_has_no_cow_variant(GameTestHelper helper) {
        MushroomCow mooshroom = EntityType.MOOSHROOM.create(helper.getLevel());
        List<String> draws = spawnTrace(helper, mooshroom, 3L);
        check(draws.equals(List.of("d", "d", "f")), "mooshroom spawn draws " + draws);
        CompoundTag tag = new CompoundTag();
        mooshroom.addAdditionalSaveData(tag);
        check(!tag.contains("variant"), "mooshroom saves a cow variant: " + tag.getString("variant"));
        helper.succeed();
    }

    @GameTest(template = SMALL)
    @SuppressWarnings("unchecked")
    public static void ai_wolf_spawn_draws(GameTestHelper helper) {
        Wolf wolf = EntityType.WOLF.create(helper.getLevel());
        ServerLevel level = helper.getLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        wolf.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        TraceRandom random = new TraceRandom(9L);
        AiTestUtil.withLevelRandom(level, random, () -> wolf.finalizeSpawn(level, level.getCurrentDifficultyAt(wolf.blockPosition()), MobSpawnType.COMMAND, null));
        check(random.calls.equals(List.of("i1", "i7", "d", "d", "f")), "wolf spawn draws " + random.calls);
        List<ResourceLocation> ids = new ArrayList<>(WolfSoundVariants.REGISTRIES.entries().keySet());
        ids.sort(null);
        Object picked = ((SoundVariantHolder<Object>) (Object) wolf).vb$getSoundVariant();
        ResourceLocation id = WolfSoundVariants.REGISTRIES.getKey((com.blackgear.vanillabackport.common.api.modules.sound_variant.WolfSoundVariant) picked);
        check(id.equals(ids.get(random.valueOf(7))), "draw " + random.valueOf(7) + " gave " + id + ", native " + ids.get(random.valueOf(7)));
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void ai_frog_variant_draw_before_memories(GameTestHelper helper) {
        List<String> draws = spawnTrace(helper, EntityType.FROG.create(helper.getLevel()), 4L);
        check(!draws.isEmpty() && draws.get(0).equals("i1"), "frog spawn draws " + draws);
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void ai_cat_breed_in_id_order(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        boolean discriminating = false;
        for (long seed = 0; seed < 200 && !discriminating; seed++) {
            Cat cat = EntityType.CAT.create(level);
            Vec3 pos = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
            cat.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
            TraceRandom random = new TraceRandom(seed);
            AiTestUtil.withLevelRandom(level, random, () -> cat.finalizeSpawn(level, level.getCurrentDifficultyAt(cat.blockPosition()), MobSpawnType.COMMAND, null));
            int bound = random.valueOf(11) >= 0 ? 11 : 10;
            int value = random.valueOf(bound);
            check(value >= 0, "test setup: no breed draw in " + random.calls);
            TagKey<CatVariant> tag = bound == 11 ? CatVariantTags.FULL_MOON_SPAWNS : CatVariantTags.DEFAULT_SPAWNS;
            List<Holder<CatVariant>> tagOrder = new ArrayList<>();
            BuiltInRegistries.CAT_VARIANT.getTag(tag).ifPresent(set -> set.forEach(tagOrder::add));
            List<Holder<CatVariant>> idOrder = new ArrayList<>(tagOrder);
            idOrder.sort((a, b) -> a.unwrapKey().orElseThrow().location().compareTo(b.unwrapKey().orElseThrow().location()));
            check(cat.getVariant().equals(idOrder.get(value)), "seed " + seed + ": draw " + value + " gave " + cat.getVariant() + ", native " + idOrder.get(value));
            discriminating = !tagOrder.get(value).equals(idOrder.get(value));
        }
        check(discriminating, "test setup: no seed where tag order and id order differ");
        helper.succeed();
    }

    // ------------------------------------------------------------------ AI-VAR-06

    @GameTest(template = SMALL)
    public static void ai_breeding_draws_from_the_parent(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Cow father = still(helper, EntityType.COW, new Vec3(2.5, 2, 2.5));
        Cow mother = still(helper, EntityType.COW, new Vec3(4.5, 2, 2.5));
        TraceRandom cowRandom = new TraceRandom(2L);
        AiTestUtil.setEntityRandom(father, cowRandom);
        father.getBreedOffspring(level, mother);
        check(cowRandom.calls.equals(List.of("b")), "cow parent draws " + cowRandom.calls);

        Wolf dad = still(helper, EntityType.WOLF, new Vec3(2.5, 2, 4.5));
        Wolf mum = still(helper, EntityType.WOLF, new Vec3(4.5, 2, 4.5));
        dad.setTame(true, false);
        mum.setTame(true, false);
        TraceRandom wolfRandom = new TraceRandom(2L);
        AiTestUtil.setEntityRandom(dad, wolfRandom);
        dad.getBreedOffspring(level, mum);
        check(wolfRandom.calls.equals(List.of("b", "i7")), "tame wolf parent draws " + wolfRandom.calls);
        helper.succeed();
    }
}
