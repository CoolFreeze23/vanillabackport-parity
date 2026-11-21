package com.coolfreeze23.vbparity.mayhem;

import com.blackgear.vanillabackport.common.api.extensions.entity.movement.MotionAwareEntity;
import com.blackgear.vanillabackport.common.api.extensions.entity.spear.MobSpearHandler;
import com.blackgear.vanillabackport.common.api.extensions.entity.spear.SpearSwingTracker;
import com.blackgear.vanillabackport.common.level.components.PiercingWeapon;
import com.blackgear.vanillabackport.common.level.components.SwingAnimation;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.camel.CamelHusk;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.Nautilus;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCube;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.blackgear.vanillabackport.core.VanillaBackport;
import com.coolfreeze23.vbparity.mayhem.mixin.MeleeAttackAccessor;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Pass-2 GameTests for Mounts of Mayhem (spears, nautilus, camel husk, zombie horse). */
@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public class MayhemPass2Tests {
    private static final String SMALL = "mayhem_small";

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    private static <T extends Mob> T still(GameTestHelper helper, EntityType<T> type, Vec3 pos) {
        T mob = helper.spawn(type, pos);
        mob.setNoAi(true);
        return mob;
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
    }

    private static boolean advancementDone(ServerPlayer player, String path) {
        AdvancementHolder holder = player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("vbparity", path));
        check(holder != null, "test advancement vbparity:" + path + " did not load (unknown trigger?)");
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** A spear swing after a right-click is still a stab: the swing lasts the spear's swing_animation duration. */
    @GameTest(template = SMALL)
    public static void mayhem_p2_use_swing_is_a_stab(GameTestHelper helper) {
        Zombie zombie = still(helper, EntityType.ZOMBIE, new Vec3(2.5, 2, 2.5));
        ItemStack spear = new ItemStack(ModItems.IRON_SPEAR.get());
        zombie.setItemSlot(EquipmentSlot.MAINHAND, spear);
        ((SpearSwingTracker) zombie).vb$setAttackSwing(false);
        zombie.swing(InteractionHand.MAIN_HAND);
        int expected = SwingAnimation.get(spear).duration();
        check(expected != 6, "test setup: spear swing duration is 6");
        check(zombie.getCurrentSwingDuration() == expected, "swing duration " + zombie.getCurrentSwingDuration() + ", spear " + expected);
        helper.succeed();
    }

    /**
     * Native CamelHusk/ZombieHorse/AbstractNautilus.interact: any click makes them persistent, including one that only
     * takes the lead back (Entity.interact handles that before mobInteract is reached).
     */
    @GameTest(template = SMALL, timeoutTicks = 40)
    public static void mayhem_p2_any_click_makes_persistent(GameTestHelper helper) {
        CamelHusk husk = still(helper, ModEntityTypes.CAMEL_HUSK.get(), new Vec3(1.5, 2, 1.5));
        ZombieHorse horse = still(helper, EntityType.ZOMBIE_HORSE, new Vec3(4.5, 2, 1.5));
        Nautilus nautilus = still(helper, ModEntityTypes.NAUTILUS.get(), new Vec3(1.5, 2, 4.5));
        Pig pig = still(helper, EntityType.PIG, new Vec3(4.5, 2, 4.5));
        ServerPlayer player = MayhemTestPlayers.survival(helper, new Vec3(3.0, 2, 3.0), 40);
        try {
            java.lang.reflect.Field persistence = Mob.class.getDeclaredField("persistenceRequired");
            persistence.setAccessible(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            for (Mob mob : List.of(husk, horse, nautilus, pig)) {
                mob.setLeashedTo(player, true);
                persistence.setBoolean(mob, false);
                mob.interact(player, InteractionHand.MAIN_HAND);
                check(!mob.isLeashed(), "test setup: the click did not take the lead back from " + mob);
            }
            check(husk.isPersistenceRequired(), "camel husk not persistent after a click");
            check(horse.isPersistenceRequired(), "zombie horse not persistent after a click");
            check(nautilus.isPersistenceRequired(), "nautilus not persistent after a click");
            check(!pig.isPersistenceRequired(), "a pig became persistent (native leaves it alone)");
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException("reflection: " + e);
        } finally {
            MayhemTestPlayers.remove(player);
        }
        helper.succeed();
    }

    /** MAYHEM-32: native handleClientTickEnd. A tick end with no movement packet before it resets the known movement. */
    @GameTest(template = SMALL, timeoutTicks = 40)
    public static void mayhem_p2_known_movement_tick_end(GameTestHelper helper) {
        ServerPlayer player = MayhemTestPlayers.survival(helper, new Vec3(2.5, 2, 2.5), 40);
        try {
            Vec3 move = new Vec3(0.3, 0.0, 0.0);
            player.setKnownMovement(move);
            KnownMovement.onClientTickEnd(player);
            check(((MotionAwareEntity) player).getKnownSpeed().equals(move), "speed reset on the tick that had movement");
            check(KnownMovement.of(player).equals(move), "known movement reset on the tick that had movement");
            KnownMovement.onClientTickEnd(player);
            check(((MotionAwareEntity) player).getKnownSpeed().equals(Vec3.ZERO), "speed kept after a tick without movement");
            check(KnownMovement.of(player).equals(Vec3.ZERO), "known movement kept after a tick without movement");
            check(player.getKnownMovement().equals(move), "the vanilla field was changed");
            player.setKnownMovement(move);
            check(KnownMovement.of(player).equals(move), "new movement not read");
        } finally {
            MayhemTestPlayers.remove(player);
        }
        helper.succeed();
    }

    /** MAYHEM-20: shearing nautilus armour fires minecraft:player_sheared_equipment with the sheared item. */
    @GameTest(template = SMALL, timeoutTicks = 40)
    public static void mayhem_p2_sheared_equipment_trigger(GameTestHelper helper) {
        Nautilus nautilus = still(helper, ModEntityTypes.NAUTILUS.get(), new Vec3(3.5, 2, 3.5));
        nautilus.setTame(true, false);
        nautilus.setOwnerUUID(UUID.randomUUID());
        nautilus.setBodyArmorItem(new ItemStack(ModItems.IRON_NAUTILUS_ARMOR.get()));
        ServerPlayer player = MayhemTestPlayers.survival(helper, new Vec3(1.5, 2, 3.5), 40);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
            check(!advancementDone(player, "test/shear_nautilus_armor"), "test setup: already done");
            nautilus.mobInteract(player, InteractionHand.MAIN_HAND);
            check(!nautilus.isWearingBodyArmor(), "armour not sheared");
            check(advancementDone(player, "test/shear_nautilus_armor"), "player_sheared_equipment did not fire");
        } finally {
            MayhemTestPlayers.remove(player);
        }
        helper.succeed();
    }

    /** The 26.2 effect type ids used by native enchantment files parse. */
    @GameTest(template = SMALL)
    public static void mayhem_p2_enchantment_effect_ids(GameTestHelper helper) {
        String impulse = "{\"type\":\"minecraft:apply_impulse\",\"direction\":[0.0,0.0,1.0],\"coordinate_scale\":[1.0,0.0,1.0],\"magnitude\":0.458}";
        String exhaustion = "{\"type\":\"minecraft:apply_exhaustion\",\"amount\":4.0}";
        String damage = "{\"type\":\"minecraft:change_item_damage\",\"amount\":1.0}";
        for (String json : List.of(impulse, exhaustion, damage)) {
            check(EnchantmentEntityEffect.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result().isPresent(), "entity effect did not parse: " + json);
            check(EnchantmentLocationBasedEffect.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result().isPresent(), "location effect did not parse: " + json);
        }
        helper.succeed();
    }

    private static List<Integer> weaponRollBounds(GameTestHelper helper) {
        List<Integer> bounds = new ArrayList<>();
        RandomSource random = new LegacyRandomSource(0L) {
            @Override
            public float nextFloat() {
                return 0.0F;
            }

            @Override
            public int nextInt(int bound) {
                bounds.add(bound);
                return 0;
            }
        };
        Zombie zombie = still(helper, EntityType.ZOMBIE, new Vec3(2.5, 2, 2.5));
        try {
            Method m = Zombie.class.getDeclaredMethod("populateDefaultEquipmentSlots", RandomSource.class, DifficultyInstance.class);
            m.setAccessible(true);
            m.invoke(zombie, random, helper.getLevel().getCurrentDifficultyAt(zombie.blockPosition()));
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException("reflection: " + e);
        }
        zombie.discard();
        return bounds;
    }

    /** With has_spears off, the zombie weapon roll is the 1.21.1 nextInt(3) again (VB widened it to 6 regardless). */
    @GameTest(template = SMALL)
    public static void mayhem_p2_zombie_roll_without_spears(GameTestHelper helper) {
        boolean before = VanillaBackport.COMMON_CONFIG.hasSpears.get();
        List<Integer> off;
        List<Integer> on;
        try {
            VanillaBackport.COMMON_CONFIG.hasSpears.set(false);
            off = weaponRollBounds(helper);
            VanillaBackport.COMMON_CONFIG.hasSpears.set(true);
            on = weaponRollBounds(helper);
        } finally {
            VanillaBackport.COMMON_CONFIG.hasSpears.set(before);
        }
        check(off.contains(3) && !off.contains(6), "spears off: roll bounds " + off);
        check(on.contains(6), "spears on: roll bounds " + on);
        helper.succeed();
    }

    /** TPHYS-C02: the second stab knockback is the native halved getKnockback (Knockback I: 0.5, not 1.0). */
    @GameTest(template = SMALL)
    public static void mayhem_p2_stab_knockback_halved(GameTestHelper helper) {
        Zombie zombie = still(helper, EntityType.ZOMBIE, new Vec3(2.5, 2, 1.5));
        ItemStack spear = new ItemStack(ModItems.IRON_SPEAR.get());
        spear.enchant(enchantment(helper, Enchantments.KNOCKBACK), 1);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, spear);
        zombie.setYRot(0.0F);
        Pig pig = still(helper, EntityType.PIG, new Vec3(2.5, 2, 3.5));
        pig.setDeltaMovement(Vec3.ZERO);
        ((MobSpearHandler) zombie).vb$stabAttack(EquipmentSlot.MAINHAND, pig, 0.0F, false, true, false);
        Vec3 v = pig.getDeltaMovement();
        double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
        // knockback(0.4) then knockback(k): |v| = 0.4 / 2 + k
        check(Math.abs(horizontal - 0.7) < 1.0E-4, "horizontal knockback " + horizontal + ", native 0.7 (VB 1.2)");
        helper.succeed();
    }

    /** TPHYS-C03: Lunge works while creative-flying (native checks is_fall_flying only). */
    @GameTest(template = SMALL, timeoutTicks = 40)
    public static void mayhem_p2_lunge_while_flying(GameTestHelper helper) {
        ServerPlayer player = MayhemTestPlayers.survival(helper, new Vec3(2.5, 3, 2.5), 40);
        try {
            player.setGameMode(GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.setYRot(0.0F);
            ItemStack spear = new ItemStack(ModItems.IRON_SPEAR.get());
            spear.enchant(enchantment(helper, ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace("lunge"))), 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, spear);
            player.setDeltaMovement(Vec3.ZERO);
            ((MobSpearHandler) player).vb$postPiercingAttack();
            Vec3 v = player.getDeltaMovement();
            check(Math.sqrt(v.x * v.x + v.z * v.z) > 0.4, "no lunge impulse while flying: " + v);
        } finally {
            MayhemTestPlayers.remove(player);
        }
        helper.succeed();
    }

    /** TPHYS-C04: starting and stopping a spear use makes no vibration (use_effects interact_vibrations = false). */
    @GameTest(template = SMALL, timeoutTicks = 120)
    public static void mayhem_p2_spear_use_no_vibration(GameTestHelper helper) {
        BlockPos sensorPos = new BlockPos(1, 2, 1);
        helper.setBlock(sensorPos, Blocks.SCULK_SENSOR);
        // A husk: it does not burn in daylight (fire damage would itself be a vibration).
        Zombie zombie = still(helper, EntityType.HUSK, new Vec3(3.5, 2, 3.5));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.IRON_SPEAR.get()));
        AtomicBoolean used = new AtomicBoolean();
        helper.runAfterDelay(50, () -> {
            zombie.startUsingItem(InteractionHand.MAIN_HAND);
            check(zombie.isUsingItem(), "test setup: the zombie is not using the spear");
            zombie.stopUsingItem();
            used.set(true);
        });
        AtomicBoolean fired = new AtomicBoolean();
        java.util.concurrent.atomic.AtomicInteger firedFrequency = new java.util.concurrent.atomic.AtomicInteger(-1);
        for (int t = 51; t < 90; t++) {
            helper.runAfterDelay(t, () -> {
                if (helper.getBlockState(sensorPos).getValue(SculkSensorBlock.PHASE) != SculkSensorPhase.INACTIVE) {
                    if (!fired.get()) {
                        SculkSensorBlockEntity be = helper.getBlockEntity(sensorPos);
                        firedFrequency.set(be.getLastVibrationFrequency());
                    }
                    fired.set(true);
                }
            });
        }
        helper.runAfterDelay(95, () -> {
            check(used.get(), "not used");
            check(!fired.get(), "spear use made the sculk sensor fire, frequency " + firedFrequency.get());
            helper.succeed();
        });
    }

    /** TPHYS-C07: a primed sulfur cube can be hit by a spear even when invulnerable (isInvulnerableToPiercingWeapon). */
    @GameTest(template = SMALL)
    public static void mayhem_p2_spear_hits_primed_invulnerable_cube(GameTestHelper helper) {
        Zombie zombie = still(helper, EntityType.ZOMBIE, new Vec3(1.5, 2, 1.5));
        SulfurCube cube = still(helper, ModEntityTypes.SULFUR_CUBE.get(), new Vec3(3.5, 2, 3.5));
        cube.setInvulnerable(true);
        check(!PiercingWeapon.canHitEntity(zombie, cube), "an unprimed invulnerable cube can be hit");
        cube.setFuse(40);
        check(PiercingWeapon.canHitEntity(zombie, cube), "a primed invulnerable cube cannot be hit");
        helper.succeed();
    }

    /** TPHYS-C08: post-attack enchantment effects of a player stab act on the hit dragon part, as natively. */
    @GameTest(template = SMALL, timeoutTicks = 40)
    public static void mayhem_p2_stab_post_attack_on_dragon_part(GameTestHelper helper) {
        EnderDragon dragon = still(helper, EntityType.ENDER_DRAGON, new Vec3(3.5, 3, 3.5));
        EnderDragonPart part = dragon.getSubEntities()[2];
        ServerPlayer player = MayhemTestPlayers.survival(helper, new Vec3(1.5, 2, 1.5), 40);
        try {
            ItemStack spear = new ItemStack(ModItems.IRON_SPEAR.get());
            spear.enchant(enchantment(helper, Enchantments.FIRE_ASPECT), 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, spear);
            boolean hit = ((MobSpearHandler) player).vb$stabAttack(EquipmentSlot.MAINHAND, part, 4.0F, true, false, false);
            check(hit, "test setup: the stab did not hit the part");
            check(part.getRemainingFireTicks() > 0, "the hit part was not set on fire (fire " + part.getRemainingFireTicks()
                + ", dragon " + dragon.getRemainingFireTicks() + ")");
            check(dragon.getRemainingFireTicks() <= 0, "the dragon body was set on fire instead of the part");
        } finally {
            MayhemTestPlayers.remove(player);
            dragon.discard();
        }
        helper.succeed();
    }

    /** TPHYS-C09: nautilus knockback resistance is the native float literal 0.3F. */
    @GameTest(template = SMALL)
    public static void mayhem_p2_nautilus_knockback_resistance_float(GameTestHelper helper) {
        Nautilus nautilus = still(helper, ModEntityTypes.NAUTILUS.get(), new Vec3(2.5, 2, 2.5));
        double base = nautilus.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getBaseValue();
        check(base == (double) 0.3F, "knockback resistance " + base + ", native " + (double) 0.3F);
        helper.succeed();
    }

    /** TPHYS-CE01: a piglin holding a spear does not also use the plain melee attack. */
    @GameTest(template = SMALL)
    public static void mayhem_p2_spear_piglin_no_melee(GameTestHelper helper) {
        Piglin piglin = still(helper, EntityType.PIGLIN, new Vec3(2.5, 2, 2.5));
        piglin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GOLDEN_SPEAR.get()));
        check(MeleeAttackAccessor.vbp$isHoldingUsableProjectileWeapon(piglin), "a spear piglin still melees");
        piglin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        check(!MeleeAttackAccessor.vbp$isHoldingUsableProjectileWeapon(piglin), "a sword piglin no longer melees");
        Zombie zombie = still(helper, EntityType.ZOMBIE, new Vec3(4.5, 2, 4.5));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.IRON_SPEAR.get()));
        check(!MeleeAttackAccessor.vbp$isHoldingUsableProjectileWeapon(zombie), "the change reached a non-piglin");
        helper.succeed();
    }

    /** TPHYS-A03: a baby nautilus carries its passenger at 0.5 (native BABY_DIMENSIONS), not 0.56875. */
    @GameTest(template = SMALL)
    public static void mayhem_p2_baby_nautilus_passenger_point(GameTestHelper helper) {
        Nautilus nautilus = still(helper, ModEntityTypes.NAUTILUS.get(), new Vec3(2.5, 2, 2.5));
        nautilus.setAge(-24000);
        nautilus.refreshDimensions();
        double y = nautilus.getDimensions(nautilus.getPose()).attachments().get(EntityAttachment.PASSENGER, 0, 0.0F).y;
        check(Math.abs(y - 0.5) < 1.0E-6, "baby passenger point " + y);
        check(Math.abs(nautilus.getBbWidth() - 0.4375F) < 1.0E-6 && Math.abs(nautilus.getBbHeight() - 0.475F) < 1.0E-6,
            "baby size " + nautilus.getBbWidth() + " x " + nautilus.getBbHeight());
        helper.succeed();
    }
}
