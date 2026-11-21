package com.coolfreeze23.vbparity.mayhem;

import com.blackgear.vanillabackport.common.api.extensions.entity.movement.MotionAwareEntity;
import com.blackgear.vanillabackport.common.api.extensions.entity.spear.MobSpearHandler;
import com.blackgear.vanillabackport.common.level.components.AttackRange;
import com.blackgear.vanillabackport.common.level.components.KineticWeapon;
import com.blackgear.vanillabackport.common.level.entities.ai.behavior.ChargeAttack;
import com.blackgear.vanillabackport.common.level.entities.ai.goal.SpearUseGoal;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.AbstractNautilus;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.Nautilus;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.ZombieNautilus;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.skeleton.Parched;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.blackgear.vanillabackport.common.registries.items.ModDataComponents;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.blackgear.vanillabackport.common.triggers.SpearMobsTrigger;
import com.coolfreeze23.vbparity.mayhem.mixin.ParrotAccessor;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public class MayhemGameTests {
    private static final String SMALL = "mayhem_small";

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    private static Object field(Object owner, Class<?> type, String name) {
        try {
            Field f = type.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(owner);
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException("reflection " + type.getSimpleName() + "." + name + ": " + e);
        }
    }

    private static Object call(Object owner, Class<?> type, String name, Class<?>[] params, Object... args) {
        try {
            Method m = type.getDeclaredMethod(name, params);
            m.setAccessible(true);
            return m.invoke(owner, args);
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException("reflection " + type.getSimpleName() + "." + name + ": " + e);
        }
    }

    /** A random source with fixed answers, for equipment and attribute rolls. */
    private static RandomSource fixedRandom(float nextFloat, int nextInt, double nextDouble) {
        return new LegacyRandomSource(0L) {
            @Override
            public float nextFloat() {
                return nextFloat;
            }

            @Override
            public int nextInt(int bound) {
                return Math.min(nextInt, bound - 1);
            }

            @Override
            public double nextDouble() {
                return nextDouble;
            }
        };
    }

    private static <T extends Mob> T still(GameTestHelper helper, EntityType<T> type, Vec3 pos) {
        T mob = helper.spawn(type, pos);
        mob.setNoAi(true);
        return mob;
    }

    private static Holder<DamageType> spearType(GameTestHelper helper) {
        return helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(MayhemParity.SPEAR_DAMAGE);
    }

    private static double horizontal(Vec3 v) {
        return Math.sqrt(v.x * v.x + v.z * v.z);
    }

    // ---- MAYHEM-01 -------------------------------------------------------------------------------------------

    @GameTest(template = SMALL)
    public static void mayhem_spear_dismount_thresholds(GameTestHelper helper) {
        for (MayhemParity.SpearThreshold entry : MayhemParity.dismountThresholds()) {
            KineticWeapon kinetic = new ItemStack(entry.item().get()).get(ModDataComponents.KINETIC_WEAPON.get());
            check(kinetic != null && kinetic.dismountConditions().isPresent(), "no kinetic dismount condition on " + entry.item().get());
            float speed = kinetic.dismountConditions().get().minSpeed();
            check(speed == entry.dismountThreshold(), entry.item().get() + " dismount threshold " + speed + ", expected " + entry.dismountThreshold());
        }
        helper.succeed();
    }

    // ---- MAYHEM-02 -------------------------------------------------------------------------------------------

    @GameTest(template = SMALL)
    public static void mayhem_spear_damage_type_data(GameTestHelper helper) {
        Holder<DamageType> spear = spearType(helper);
        check("spear".equals(spear.value().msgId()), "spear damage message id " + spear.value().msgId());
        check(spear.is(DamageTypeTags.NO_KNOCKBACK), "spear damage not in #no_knockback");
        check(spear.is(DamageTypeTags.IS_PLAYER_ATTACK), "spear damage not in #is_player_attack");
        for (Supplier<Item> item : MayhemParity.spears()) {
            check(new ItemStack(item.get()).has(ModDataComponents.DAMAGE_TYPE.get()), item.get() + " has no damage_type component");
        }
        helper.succeed();
    }

    /** A mob's damaging stab uses minecraft:spear and adds no hurt knockback (dealsKnockback=false). */
    @GameTest(template = SMALL)
    public static void mayhem_mob_stab_uses_spear_damage(GameTestHelper helper) {
        Zombie zombie = still(helper, EntityType.ZOMBIE, new Vec3(1.5, 2, 2.5));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.IRON_SPEAR.get()));
        Pig pig = still(helper, EntityType.PIG, new Vec3(3.5, 2, 2.5));
        pig.setDeltaMovement(Vec3.ZERO);
        boolean hit = ((MobSpearHandler) zombie).vb$stabAttack(EquipmentSlot.MAINHAND, pig, 2.0F, true, false, false);
        check(hit, "stab did not hit");
        DamageSource source = pig.getLastDamageSource();
        check(source != null && source.is(MayhemParity.SPEAR_DAMAGE), "stab damage source " + (source == null ? null : source.typeHolder()));
        check(horizontal(pig.getDeltaMovement()) < 0.01, "stab added hurt knockback " + pig.getDeltaMovement());
        helper.succeed();
    }

    /** Mob melee with a spear (Mob.doHurtTarget) uses minecraft:spear and so no default hurt knockback. */
    @GameTest(template = SMALL)
    public static void mayhem_mob_melee_uses_spear_damage(GameTestHelper helper) {
        Zombie zombie = still(helper, EntityType.ZOMBIE, new Vec3(1.5, 2, 2.5));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.IRON_SPEAR.get()));
        Pig pig = still(helper, EntityType.PIG, new Vec3(3.5, 2, 2.5));
        pig.setDeltaMovement(Vec3.ZERO);
        check(zombie.doHurtTarget(pig), "melee did not hit");
        DamageSource source = pig.getLastDamageSource();
        check(source != null && source.is(MayhemParity.SPEAR_DAMAGE), "melee damage source " + (source == null ? null : source.typeHolder()));
        check(horizontal(pig.getDeltaMovement()) < 0.01, "melee added hurt knockback " + pig.getDeltaMovement());
        helper.succeed();
    }

    /** A player's damaging stab uses minecraft:spear and adds no hurt knockback. */
    @GameTest(template = SMALL, timeoutTicks = 40)
    public static void mayhem_player_stab_uses_spear_damage(GameTestHelper helper) {
        ServerPlayer player = MayhemTestPlayers.survival(helper, new Vec3(1.5, 2, 2.5), 40);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.IRON_SPEAR.get()));
        Pig pig = still(helper, EntityType.PIG, new Vec3(3.5, 2, 2.5));
        helper.runAfterDelay(2, () -> {
            pig.setDeltaMovement(Vec3.ZERO);
            boolean hit = ((MobSpearHandler) player).vb$stabAttack(EquipmentSlot.MAINHAND, pig, 4.0F, true, false, false);
            DamageSource source = pig.getLastDamageSource();
            check(hit, "stab did not hit: pig " + pig.getHealth() + ", strength " + player.getAttackStrengthScale(0.5F) + ", attackable " + pig.isAttackable()
                + ", source " + (source == null ? null : source.getMsgId()) + ", invulnerable " + pig.invulnerableTime + ", mode " + player.gameMode.getGameModeForPlayer());
            check(source != null && source.is(MayhemParity.SPEAR_DAMAGE), "stab damage source " + (source == null ? null : source.typeHolder()));
            check(horizontal(pig.getDeltaMovement()) < 0.01, "stab added hurt knockback " + pig.getDeltaMovement());
            MayhemTestPlayers.remove(player);
            helper.succeed();
        });
    }

    // ---- MAYHEM-07 -------------------------------------------------------------------------------------------

    private static ItemStack zombieWeapon(GameTestHelper helper, int roll) {
        Zombie zombie = still(helper, EntityType.ZOMBIE, new Vec3(2.5, 2, 2.5));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            zombie.setItemSlot(slot, ItemStack.EMPTY);
        }
        call(zombie, Zombie.class, "populateDefaultEquipmentSlots",
            new Class<?>[] {RandomSource.class, net.minecraft.world.DifficultyInstance.class},
            fixedRandom(0.0F, roll, 0.5), helper.getLevel().getCurrentDifficultyAt(zombie.blockPosition()));
        ItemStack weapon = zombie.getMainHandItem();
        zombie.discard();
        return weapon;
    }

    @GameTest(template = SMALL)
    public static void mayhem_zombie_spawns_with_iron_spear(GameTestHelper helper) {
        check(zombieWeapon(helper, 0).is(Items.IRON_SWORD), "roll 0 should give an iron sword");
        check(zombieWeapon(helper, 1).is(ModItems.IRON_SPEAR.get()), "roll 1 should give an iron spear, got " + zombieWeapon(helper, 1));
        check(zombieWeapon(helper, 2).is(Items.IRON_SHOVEL), "roll 2 should give an iron shovel");
        helper.succeed();
    }

    // ---- MAYHEM-08 -------------------------------------------------------------------------------------------

    private static long spearGoals(Mob mob) {
        return mob.goalSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal).filter(goal -> goal instanceof SpearUseGoal).count();
    }

    @GameTest(template = SMALL)
    public static void mayhem_spear_use_goal_owners(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(1.5, 2, 1.5));
        Drowned drowned = helper.spawn(EntityType.DROWNED, new Vec3(3.5, 2, 3.5));
        ZombifiedPiglin piglin = helper.spawn(EntityType.ZOMBIFIED_PIGLIN, new Vec3(5.5, 2, 5.5));
        helper.runAfterDelay(1, () -> {
            check(spearGoals(zombie) == 1, "zombie spear goals " + spearGoals(zombie));
            check(spearGoals(drowned) == 0, "drowned spear goals " + spearGoals(drowned));
            check(spearGoals(piglin) == 1, "zombified piglin spear goals " + spearGoals(piglin));
            helper.succeed();
        });
    }

    // ---- MAYHEM-12 / 13 / 14 / 15 ----------------------------------------------------------------------------

    private static ChargeAttack newCharge() {
        return new ChargeAttack(80, TargetingConditions.forCombat(), 0.6F, 2.0F, 12.0, 11.0, SoundEvents.GENERIC_HURT);
    }

    @GameTest(template = SMALL)
    public static void mayhem_charge_attack_distances(GameTestHelper helper) {
        ChargeAttack charge = newCharge();
        double chargeDistance = (double) field(charge, ChargeAttack.class, "maxChargeDistance");
        double detection = (double) field(charge, ChargeAttack.class, "maxTargetDetectionDistance");
        check(chargeDistance == 12.0, "max charge distance " + chargeDistance);
        check(detection == 11.0, "max target detection distance " + detection);
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void mayhem_charge_knockback_keeps_vertical_speed(GameTestHelper helper) {
        Nautilus nautilus = still(helper, ModEntityTypes.NAUTILUS.get(), new Vec3(1.5, 2, 2.5));
        Pig pig = still(helper, EntityType.PIG, new Vec3(3.5, 2, 2.5));
        nautilus.setDeltaMovement(0.2, 0.1, 0.0);
        call(newCharge(), ChargeAttack.class, "dealKnockback",
            new Class<?>[] {net.minecraft.server.level.ServerLevel.class, Animal.class, LivingEntity.class}, helper.getLevel(), nautilus, pig);
        Vec3 after = nautilus.getDeltaMovement();
        check(Math.abs(after.y - 0.1) < 1.0E-9, "charger vertical speed after hit " + after.y + " (expected 0.1)");
        check(Math.abs(after.x - 0.12) < 1.0E-9, "charger horizontal speed after hit " + after.x + " (expected 0.12)");
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> behaviours(Brain<?> brain) {
        Map<Integer, Map<?, Set<?>>> byPriority = (Map<Integer, Map<?, Set<?>>>) field(brain, Brain.class, "availableBehaviorsByPriority");
        List<Object> all = new ArrayList<>();
        byPriority.values().forEach(byActivity -> byActivity.values().forEach(all::addAll));
        return all;
    }

    @GameTest(template = SMALL)
    @SuppressWarnings("unchecked")
    public static void mayhem_zombie_nautilus_speeds(GameTestHelper helper) {
        ZombieNautilus nautilus = still(helper, ModEntityTypes.ZOMBIE_NAUTILUS.get(), new Vec3(2.5, 2, 2.5));
        boolean sawCharge = false;
        boolean sawTempt = false;
        for (Object behaviour : behaviours(nautilus.getBrain())) {
            if (behaviour instanceof ChargeAttack charge) {
                float speed = (float) field(charge, ChargeAttack.class, "speed");
                check(speed == 0.5F, "zombie nautilus charge speed " + speed);
                sawCharge = true;
            } else if (behaviour instanceof FollowTemptation tempt) {
                Function<LivingEntity, Float> modifier = (Function<LivingEntity, Float>) field(tempt, FollowTemptation.class, "speedModifier");
                float speed = modifier.apply(nautilus);
                check(speed == 0.9F, "zombie nautilus tempted speed " + speed);
                sawTempt = true;
            }
        }
        check(sawCharge && sawTempt, "charge or tempt behaviour not found (" + sawCharge + ", " + sawTempt + ")");
        helper.succeed();
    }

    // ---- MAYHEM-17 -------------------------------------------------------------------------------------------

    /** The dash's game event is ENTITY_ACTION, which a sculk sensor reads as frequency 4 (ENTITY_INTERACT is 6). */
    @GameTest(template = SMALL, timeoutTicks = 140)
    public static void mayhem_nautilus_dash_game_event(GameTestHelper helper) {
        BlockPos sensorPos = new BlockPos(1, 2, 1);
        helper.setBlock(sensorPos, Blocks.SCULK_SENSOR);
        Nautilus nautilus = still(helper, ModEntityTypes.NAUTILUS.get(), new Vec3(4.5, 2, 4.5));
        AtomicBoolean dashed = new AtomicBoolean();
        helper.runAfterDelay(50, () -> {
            nautilus.handleStartJump(0);
            dashed.set(true);
        });
        helper.succeedWhen(() -> {
            check(dashed.get(), "not dashed yet");
            SculkSensorBlockEntity sensor = helper.getBlockEntity(sensorPos);
            check(sensor.getLastVibrationFrequency() == 4, "sensor frequency " + sensor.getLastVibrationFrequency());
        });
    }

    // ---- MAYHEM-19 / 20 --------------------------------------------------------------------------------------

    @GameTest(template = SMALL)
    public static void mayhem_nautilus_armour_needs_tame_adult(GameTestHelper helper) {
        Nautilus wild = still(helper, ModEntityTypes.NAUTILUS.get(), new Vec3(1.5, 2, 1.5));
        check(!wild.canUseSlot(EquipmentSlot.BODY), "an untamed adult nautilus accepts body armour");
        Nautilus tame = still(helper, ModEntityTypes.NAUTILUS.get(), new Vec3(4.5, 2, 4.5));
        tame.setTame(true, false);
        check(tame.canUseSlot(EquipmentSlot.BODY), "a tame adult nautilus refuses body armour");
        tame.setAge(-24000);
        check(!tame.canUseSlot(EquipmentSlot.BODY), "a baby nautilus accepts body armour");
        helper.succeed();
    }

    @GameTest(template = SMALL, timeoutTicks = 40)
    public static void mayhem_nautilus_shearing(GameTestHelper helper) {
        Nautilus nautilus = still(helper, ModEntityTypes.NAUTILUS.get(), new Vec3(3.5, 2, 3.5));
        nautilus.setTame(true, false);
        nautilus.setOwnerUUID(UUID.randomUUID());
        nautilus.setBodyArmorItem(new ItemStack(ModItems.IRON_NAUTILUS_ARMOR.get()));
        nautilus.getSlot(400).set(new ItemStack(Items.SADDLE));
        ServerPlayer player = MayhemTestPlayers.survival(helper, new Vec3(1.5, 2, 3.5), 40);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        helper.runAfterDelay(2, () -> {
            check(nautilus.isSaddled(), "saddle was not put on");
            nautilus.mobInteract(player, InteractionHand.MAIN_HAND);
            check(!nautilus.isWearingBodyArmor(), "a non-owner could not shear the armour off");
            check(nautilus.isSaddled(), "the saddle came off before the armour");
            check(!player.isPassenger(), "the player mounted instead of shearing");
            nautilus.mobInteract(player, InteractionHand.MAIN_HAND);
            check(!nautilus.isSaddled(), "the saddle was not sheared off");
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, nautilus.getBoundingBox().inflate(3.0));
            check(drops.stream().anyMatch(e -> e.getItem().is(ModItems.IRON_NAUTILUS_ARMOR.get())), "armour not dropped");
            check(drops.stream().anyMatch(e -> e.getItem().is(Items.SADDLE)), "saddle not dropped");
            ItemEntity armourDrop = drops.stream().filter(e -> e.getItem().is(ModItems.IRON_NAUTILUS_ARMOR.get())).findFirst().orElseThrow();
            double height = armourDrop.getY() - nautilus.getY();
            check(Math.abs(height - 1.1375) < 0.05, "armour dropped " + height + " above the nautilus (passenger point 1.1375)");
            MayhemTestPlayers.remove(player);
            helper.succeed();
        });
    }

    // ---- MAYHEM-21 -------------------------------------------------------------------------------------------

    @GameTest(template = SMALL, timeoutTicks = 80)
    public static void mayhem_noai_nautilus_does_not_dry_out(GameTestHelper helper) {
        Nautilus nautilus = still(helper, ModEntityTypes.NAUTILUS.get(), new Vec3(2.5, 2, 2.5));
        nautilus.setAirSupply(0);
        helper.runAfterDelay(60, () -> {
            check(nautilus.getHealth() == nautilus.getMaxHealth(), "NoAI nautilus on land was hurt: " + nautilus.getHealth() + "/" + nautilus.getMaxHealth()
                + " by " + (nautilus.getLastDamageSource() == null ? null : nautilus.getLastDamageSource().getMsgId()) + ", air " + nautilus.getAirSupply());
            helper.succeed();
        });
    }

    // ---- MAYHEM-24 / 25 / 27 ---------------------------------------------------------------------------------

    private static boolean hasPanic(Mob mob) {
        return mob.goalSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof PanicGoal);
    }

    @GameTest(template = SMALL)
    public static void mayhem_zombie_horse_never_panics(GameTestHelper helper) {
        ZombieHorse zombieHorse = helper.spawn(EntityType.ZOMBIE_HORSE, new Vec3(1.5, 2, 1.5));
        Horse horse = helper.spawn(EntityType.HORSE, new Vec3(4.5, 2, 4.5));
        check(!hasPanic(zombieHorse), "zombie horse has a panic goal");
        check(hasPanic(horse), "horse lost its panic goal");
        helper.succeed();
    }

    @GameTest(template = SMALL, timeoutTicks = 60)
    public static void mayhem_baby_zombie_horse_stays_baby(GameTestHelper helper) {
        ZombieHorse horse = helper.spawn(EntityType.ZOMBIE_HORSE, new Vec3(2.5, 2, 2.5));
        horse.setAge(-24000);
        float expectedWidth = EntityType.ZOMBIE_HORSE.getWidth() * 0.7F;
        float expectedHeight = EntityType.ZOMBIE_HORSE.getHeight() * 0.7F;
        check(Math.abs(horse.getBbWidth() - expectedWidth) < 1.0E-4, "baby width " + horse.getBbWidth() + ", expected " + expectedWidth);
        check(Math.abs(horse.getBbHeight() - expectedHeight) < 1.0E-4, "baby height " + horse.getBbHeight() + ", expected " + expectedHeight);
        double passenger = horse.getDimensions(horse.getPose()).attachments().get(EntityAttachment.PASSENGER, 0, 0.0F).y;
        double expectedPassenger = (EntityType.ZOMBIE_HORSE.getHeight() - 0.25F) * 0.7F;
        check(Math.abs(passenger - expectedPassenger) < 1.0E-4, "baby passenger point " + passenger + ", expected " + expectedPassenger);
        helper.runAfterDelay(40, () -> {
            check(horse.getAge() == -24000, "baby zombie horse aged to " + horse.getAge());
            helper.succeed();
        });
    }

    @GameTest(template = SMALL)
    public static void mayhem_zombie_horse_speed_divisor(GameTestHelper helper) {
        ZombieHorse horse = helper.spawn(EntityType.ZOMBIE_HORSE, new Vec3(2.5, 2, 2.5));
        call(horse, AbstractHorse.class, "randomizeAttributes", new Class<?>[] {RandomSource.class}, fixedRandom(0.5F, 0, 0.5));
        double speed = horse.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
        double expected = (9.0 + 0.5 * 1.0 + 0.5 * 1.0 + 0.5 * 1.0) / 42.16F;
        check(speed == expected, "zombie horse speed " + speed + ", expected " + expected);
        helper.succeed();
    }

    // ---- MAYHEM-28 / 29 --------------------------------------------------------------------------------------

    @GameTest(template = SMALL)
    public static void mayhem_zombie_horse_biomes_and_parrot(GameTestHelper helper) {
        TagKey<Biome> spawns = TagKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("spawns_zombie_horses"));
        Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        check(biomes.getHolderOrThrow(Biomes.SUNFLOWER_PLAINS).is(spawns), "sunflower plains not in #spawns_zombie_horses");
        check(biomes.getHolderOrThrow(Biomes.PLAINS).is(spawns), "plains left #spawns_zombie_horses");
        check(ParrotAccessor.vbp$mobSoundMap().get(EntityType.ZOMBIE_HORSE) == MayhemParity.PARROT_IMITATE_ZOMBIE_HORSE,
            "parrots do not imitate zombie horses");
        check(BuiltInRegistries.SOUND_EVENT.containsKey(MayhemParity.SADDLE_UNEQUIP_ID), "item.saddle.unequip not registered");
        helper.succeed();
    }

    // ---- MAYHEM-30 -------------------------------------------------------------------------------------------

    @GameTest(template = SMALL)
    public static void mayhem_parched_weakness_immune(GameTestHelper helper) {
        Parched parched = still(helper, ModEntityTypes.PARCHED.get(), new Vec3(2.5, 2, 2.5));
        check(!parched.canBeAffected(new MobEffectInstance(MobEffects.WEAKNESS, 200)), "parched accept Weakness");
        check(!parched.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200)), "Weakness was added to a parched");
        check(parched.canBeAffected(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200)), "parched refuse Slowness");
        helper.succeed();
    }

    // ---- MAYHEM-32 / 33 --------------------------------------------------------------------------------------

    @GameTest(template = SMALL, timeoutTicks = 40)
    public static void mayhem_player_known_speed_resets(GameTestHelper helper) {
        ServerPlayer player = MayhemTestPlayers.survival(helper, new Vec3(2.5, 2, 2.5), 40);
        helper.runAfterDelay(1, () -> {
            player.setKnownMovement(new Vec3(0.3, 0.0, 0.0));
            Vec3 fresh = ((MotionAwareEntity) player).getKnownSpeed();
            check(fresh.x == 0.3, "fresh known speed " + fresh);
        });
        helper.runAfterDelay(8, () -> {
            Vec3 stale = ((MotionAwareEntity) player).getKnownSpeed();
            check(stale.equals(Vec3.ZERO), "known speed kept " + stale + " with no movement packets");
            check(player.getKnownMovement().x == 0.3, "vanilla known movement was changed");
            MayhemTestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** Native minecarts compute their known speed before moving, so after two ticks it is the first tick's motion. */
    @GameTest(template = SMALL)
    public static void mayhem_minecart_known_speed_lags_one_tick(GameTestHelper helper) {
        AbstractMinecart cart = helper.spawn(EntityType.MINECART, new Vec3(2.5, 4, 2.5));
        cart.setDeltaMovement(0.2, 0.0, 0.0);
        Vec3 p0 = cart.position();
        cart.tick();
        Vec3 p1 = cart.position();
        cart.tick();
        Vec3 p2 = cart.position();
        Vec3 known = ((MotionAwareEntity) cart).getKnownSpeed();
        Vec3 first = p1.subtract(p0);
        Vec3 second = p2.subtract(p1);
        check(first.distanceTo(second) > 1.0E-4, "test setup: both ticks moved the same " + first);
        check(known.distanceTo(first) < 1.0E-9, "known speed " + known + ", expected the first tick's motion " + first + " (second " + second + ")");
        helper.succeed();
    }

    // ---- MAYHEM-10 / 11 / 42 ---------------------------------------------------------------------------------

    @GameTest(template = SMALL)
    public static void mayhem_sweeping_edge_not_on_spears(GameTestHelper helper) {
        Holder<Enchantment> sweeping = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SWEEPING_EDGE);
        check(!sweeping.value().isSupportedItem(new ItemStack(ModItems.IRON_SPEAR.get())), "Sweeping Edge supports spears");
        check(sweeping.value().isSupportedItem(new ItemStack(Items.IRON_SWORD)), "Sweeping Edge no longer supports swords");
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void mayhem_lunge_tooltip_order(GameTestHelper helper) {
        Registry<Enchantment> enchantments = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        HolderSet.Named<Enchantment> order = enchantments.getTag(EnchantmentTags.TOOLTIP_ORDER).orElseThrow();
        List<ResourceKey<Enchantment>> keys = order.stream().map(holder -> holder.unwrapKey().orElseThrow()).toList();
        ResourceKey<Enchantment> lunge = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace("lunge"));
        int frost = keys.indexOf(Enchantments.FROST_WALKER);
        check(frost >= 0 && keys.indexOf(lunge) == frost + 1, "lunge at " + keys.indexOf(lunge) + ", frost walker at " + frost);
        check(keys.indexOf(Enchantments.SHARPNESS) == frost + 2, "sharpness does not follow lunge");
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void mayhem_blasting_time(GameTestHelper helper) {
        int found = 0;
        for (RecipeHolder<BlastingRecipe> holder : helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.BLASTING)) {
            BlastingRecipe recipe = holder.value();
            if (recipe.getIngredients().get(0).test(new ItemStack(ModItems.GOLDEN_SPEAR.get()))
                || recipe.getIngredients().get(0).test(new ItemStack(ModItems.IRON_SPEAR.get()))) {
                check(recipe.getCookingTime() == 100, holder.id() + " blasts for " + recipe.getCookingTime());
                found++;
            }
        }
        check(found >= 2, "spear blasting recipes found: " + found);
        helper.succeed();
    }

    // ---- MAYHEM-39 / 41 / MISC-33 ----------------------------------------------------------------------------

    @GameTest(template = SMALL)
    public static void mayhem_attack_range_defaults(GameTestHelper helper) {
        AttackRange range = AttackRange.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
        check(range.minReach() == 0.0F && range.maxReach() == 3.0F, "reach defaults " + range.minReach() + "/" + range.maxReach());
        check(range.minCreativeReach() == 0.0F && range.maxCreativeReach() == 5.0F, "creative reach defaults " + range.minCreativeReach() + "/" + range.maxCreativeReach());
        check(range.hitboxMargin() == 0.3F && range.mobFactor() == 1.0F, "margin/mob factor defaults " + range.hitboxMargin() + "/" + range.mobFactor());
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void mayhem_spear_mobs_trigger_player_predicate(GameTestHelper helper) {
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var result = SpearMobsTrigger.TriggerInstance.CODEC.parse(ops,
            JsonParser.parseString("{\"player\":{\"type\":\"minecraft:player\"},\"count\":2}"));
        check(result.isSuccess(), "plain entity predicate rejected: " + result.error().map(Object::toString).orElse(""));
        var list = SpearMobsTrigger.TriggerInstance.CODEC.parse(ops, JsonParser.parseString(
            "{\"player\":[{\"condition\":\"minecraft:entity_properties\",\"entity\":\"this\",\"predicate\":{\"type\":\"minecraft:player\"}}]}"));
        check(list.isSuccess(), "condition list rejected: " + list.error().map(Object::toString).orElse(""));
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void mayhem_minimum_attack_charge_id(GameTestHelper helper) throws Exception {
        ResourceLocation id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(ModDataComponents.MINIMUM_ATTACK_CHARGE.get());
        check(MayhemParity.MINIMUM_ATTACK_CHARGE.equals(id), "component id " + id);
        check(BuiltInRegistries.DATA_COMPONENT_TYPE.get(MayhemParity.OLD_MINIMUM_ATTACK_CHARGE) == ModDataComponents.MINIMUM_ATTACK_CHARGE.get(),
            "old id does not resolve");
        ItemStack old = ItemStack.parse(helper.getLevel().registryAccess(),
            TagParser.parseTag("{id:\"minecraft:iron_spear\",count:1,components:{\"minecraft:minimum_attack_range\":0.5f}}")).orElseThrow();
        check(Float.valueOf(0.5F).equals(old.get(ModDataComponents.MINIMUM_ATTACK_CHARGE.get())), "old-id stack lost its value: " + old.get(ModDataComponents.MINIMUM_ATTACK_CHARGE.get()));
        ItemStack fresh = ItemStack.parse(helper.getLevel().registryAccess(),
            TagParser.parseTag("{id:\"minecraft:iron_spear\",count:1,components:{\"minecraft:minimum_attack_charge\":0.25f}}")).orElseThrow();
        check(Float.valueOf(0.25F).equals(fresh.get(ModDataComponents.MINIMUM_ATTACK_CHARGE.get())), "native-id stack value " + fresh.get(ModDataComponents.MINIMUM_ATTACK_CHARGE.get()));
        helper.succeed();
    }
}
