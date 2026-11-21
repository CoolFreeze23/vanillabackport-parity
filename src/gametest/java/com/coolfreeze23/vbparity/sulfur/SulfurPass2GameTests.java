package com.coolfreeze23.vbparity.sulfur;

import com.blackgear.vanillabackport.common.api.extensions.entity.spear.MobSpearHandler;
import com.blackgear.vanillabackport.common.level.block_entities.PotentSulfurBlockEntity;
import com.blackgear.vanillabackport.common.level.blocks.PotentSulfurBlock;
import com.blackgear.vanillabackport.common.level.blocks.ShelfMushroomBlock;
import com.blackgear.vanillabackport.common.level.blocks.SpeleothemBlock;
import com.blackgear.vanillabackport.common.level.blocks.PotentSulfurState;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCube;
import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.blackgear.vanillabackport.common.registries.entities.ModAttributes;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.coolfreeze23.vbparity.VBParity;
import com.coolfreeze23.vbparity.sulfur.mixin.SulfurCubeInvoker;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Pass-2 sulfur fixes: TBLK-301/302, TPHYS-A02/AE01/B02/B03/C05, SULFUR-14/18, creative fire charge. */
@GameTestHolder(VBParity.MOD_ID)
@PrefixGameTestTemplate(false)
public class SulfurPass2GameTests {
    private static final String BOX = "sulfur_box";

    private static SulfurCube cube(GameTestHelper helper, Vec3 pos, int size) {
        SulfurCube cube = helper.spawn(ModEntityTypes.SULFUR_CUBE.get(), pos);
        cube.setNoAi(true);
        cube.setSize(size, true);
        return cube;
    }

    private static void near(GameTestHelper helper, double expected, double actual, double tolerance, String what) {
        helper.assertTrue(Math.abs(expected - actual) <= tolerance, what + ": expected " + expected + " but was " + actual);
    }

    public static long seedOf(RandomSource random) {
        try {
            for (Class<?> c = random.getClass(); c != null; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (f.getType() == AtomicLong.class) {
                        f.setAccessible(true);
                        return ((AtomicLong) f.get(random)).get();
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        throw new AssertionError("no seed field in " + random.getClass());
    }

    // ---------------------------------------------------------------- TBLK-301

    /** The landing sound is level event 1052 (client random); the server level random is not touched. */
    @GameTest(template = BOX)
    public static void sulfur_spike_landing_keeps_server_random(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockState spike = ModBlocks.SULFUR_SPIKE.get().defaultBlockState()
            .setValue(SpeleothemBlock.TIP_DIRECTION, Direction.DOWN).setValue(SpeleothemBlock.THICKNESS, DripstoneThickness.TIP);
        FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, spike);
        long before = seedOf(level.random);
        ((SpeleothemBlock) spike.getBlock()).onBrokenAfterFall(level, pos, falling);
        long after = seedOf(level.random);
        falling.discard();
        helper.assertTrue(before == after, "server level random advanced by the landing sound");
        helper.succeed();
    }

    // ---------------------------------------------------------------- TBLK-302 / TPHYS-B04

    /** A regular cube (bounciness 0.5) landing on a shelf mushroom bounces with max(0.5, 0.75). */
    @GameTest(template = BOX, timeoutTicks = 60)
    public static void sulfur_bounce_on_shelf_mushroom(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 1, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 2, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 2, 4), ModBlocks.SHELF_MUSHROOM.get().defaultBlockState().setValue(ShelfMushroomBlock.FACING, Direction.NORTH));
        BlockPos shelf = helper.absolutePos(new BlockPos(4, 2, 4));
        AABB top = helper.getLevel().getBlockState(shelf).getCollisionShape(helper.getLevel(), shelf).bounds().move(shelf);
        SulfurCube cube = cube(helper, new Vec3(0, 0, 0), 1);
        cube.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.DIRT));
        helper.runAfterDelay(3, () -> {
            double attribute = cube.getAttributeValue(ModAttributes.BOUNCINESS);
            helper.assertTrue(attribute > 0.0 && attribute < 0.75, "regular archetype bounciness " + attribute);
            Vec3 c = top.getCenter();
            cube.setPos(c.x, top.maxY + 0.1, c.z);
            cube.setOnGround(false);
            cube.setDeltaMovement(0.0, -0.5, 0.0);
            cube.move(MoverType.SELF, cube.getDeltaMovement());
            double portion = 0.1 / 0.5;
            double airDrag = Mth.clamp(1.0F - (1.0F - 0.91F) * (float) cube.getAttributeValue(ModAttributes.AIR_DRAG_MODIFIER), 0.0F, 1.0F);
            double expected = (portion * cube.getGravity() + 0.5) * Mth.lerp(portion, 1.0, airDrag) * 0.75;
            near(helper, expected, cube.getDeltaMovement().y, 1.0E-4, "upward speed off the shelf mushroom");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- TPHYS-AE01

    /** An empty cube (no bounciness) on slime gets native restitution 1.0 with gravity/drag compensation, not -dy. */
    @GameTest(template = BOX, timeoutTicks = 60)
    public static void sulfur_empty_cube_bounce_on_slime(GameTestHelper helper) {
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.SLIME_BLOCK);
            }
        }
        SulfurCube cube = cube(helper, new Vec3(4.5, 2.5, 4.5), 2);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(cube.getItemBySlot(EquipmentSlot.BODY).isEmpty(), "setup: empty cube");
            Vec3 start = helper.absoluteVec(new Vec3(4.5, 2.1, 4.5));
            cube.setPos(start.x, start.y, start.z);
            cube.setOnGround(false);
            cube.setDeltaMovement(0.0, -0.5, 0.0);
            cube.move(MoverType.SELF, cube.getDeltaMovement());
            double portion = 0.1 / 0.5;
            double airDrag = Mth.clamp(1.0F - (1.0F - 0.98F) * (float) cube.getAttributeValue(ModAttributes.AIR_DRAG_MODIFIER), 0.0F, 1.0F);
            double expected = (portion * cube.getGravity() + 0.5) * Mth.lerp(portion, 1.0, airDrag) * 1.0;
            near(helper, expected, cube.getDeltaMovement().y, 1.0E-4, "upward speed of an empty cube off slime");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- TPHYS-A02

    /** A bouncy cube sliding on slime gets the slime step slow-down (0.4 + |dy| * 0.2) after its bounce. */
    @GameTest(template = BOX, timeoutTicks = 60)
    public static void sulfur_cube_slows_on_slime(GameTestHelper helper) {
        for (int x = 1; x <= 6; x++) {
            for (int z = 1; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.SLIME_BLOCK);
            }
        }
        SulfurCube cube = helper.spawn(ModEntityTypes.SULFUR_CUBE.get(), new Vec3(2.5, 2.0, 3.5));
        cube.setSize(1, true);
        cube.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.DIRT));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(cube.onGround(), "setup: cube rests on the slime");
            cube.setDeltaMovement(0.2, cube.getDeltaMovement().y, 0.0);
            helper.runAfterDelay(1, () -> {
                double dx = cube.getDeltaMovement().x;
                helper.assertTrue(dx < 0.1, "slime step slow-down applied after the bounce (dx " + dx + ")");
                helper.succeed();
            });
        });
    }

    // ---------------------------------------------------------------- TPHYS-B02

    @GameTest(template = BOX)
    public static void sulfur_spike_path_type_cautious(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 1, 3), ModBlocks.SULFUR.get());
        BlockState spike = ModBlocks.SULFUR_SPIKE.get().defaultBlockState()
            .setValue(SpeleothemBlock.TIP_DIRECTION, Direction.UP).setValue(SpeleothemBlock.THICKNESS, DripstoneThickness.TIP);
        helper.setBlock(new BlockPos(3, 2, 3), spike);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.5, 2, 1.5));
        PathType at = WalkNodeEvaluator.getPathTypeStatic(zombie, helper.absolutePos(new BlockPos(3, 2, 3)));
        PathType next = WalkNodeEvaluator.getPathTypeStatic(zombie, helper.absolutePos(new BlockPos(4, 2, 3)));
        zombie.discard();
        helper.assertTrue(at == PathType.DAMAGE_CAUTIOUS, "spike node: " + at);
        helper.assertTrue(next == PathType.DAMAGE_CAUTIOUS, "node next to a spike: " + next);
        helper.succeed();
    }

    // ---------------------------------------------------------------- TPHYS-B03

    /** The server never pushes players in a geyser (client-authoritative); mobs get the push and the sync flag. */
    @GameTest(template = BOX)
    public static void sulfur_geyser_skips_server_players(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sulfurRel = new BlockPos(3, 1, 3);
        helper.setBlock(sulfurRel, ModBlocks.POTENT_SULFUR.get().defaultBlockState().setValue(PotentSulfurBlock.STATE, PotentSulfurState.ERUPTING));
        for (int y = 2; y <= 3; y++) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                helper.setBlock(new BlockPos(3, y, 3).relative(side), Blocks.GLASS);
            }
            helper.setBlock(new BlockPos(3, y, 3), Blocks.WATER);
        }
        BlockPos sulfurPos = helper.absolutePos(sulfurRel);
        PotentSulfurBlockEntity be = helper.getBlockEntity(sulfurRel);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 p = helper.absoluteVec(new Vec3(3.5, 3.2, 3.5));
        player.setPos(p.x, p.y, p.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.hasImpulse = false;
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.5, 3.2, 3.5));
        zombie.setDeltaMovement(Vec3.ZERO);
        zombie.hasImpulse = false;
        PotentSulfurBlockEntity.SERVER_LAUNCH_ENTITY_TICKER.tick(level, sulfurPos, level.getBlockState(sulfurPos), be);
        near(helper, 0.0, player.getDeltaMovement().y, 1.0E-9, "server push of a player");
        helper.assertFalse(player.hasImpulse || player.hurtMarked, "no sync flags on a skipped player");
        near(helper, 0.2F, zombie.getDeltaMovement().y, 1.0E-6, "zombie is launched");
        helper.assertTrue(zombie.hasImpulse && !zombie.hurtMarked, "pushed mob gets the normal sync flag");
        player.discard();
        zombie.discard();
        helper.succeed();
    }

    // ---------------------------------------------------------------- TPHYS-C05

    /** A knockback-only player stab on a block-holding cube uses the archetype push with sqrt(0): it doesn't move. */
    @GameTest(template = BOX, timeoutTicks = 60)
    public static void sulfur_knockback_only_stab(GameTestHelper helper) {
        SulfurCube cube = cube(helper, new Vec3(4.5, 2.0, 4.5), 2);
        cube.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.DIRT));
        helper.runAfterDelay(3, () -> {
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            Vec3 p = helper.absoluteVec(new Vec3(2.5, 2.0, 4.5));
            player.setPos(p.x, p.y, p.z);
            cube.setDeltaMovement(Vec3.ZERO);
            ((MobSpearHandler) player).vb$stabAttack(EquipmentSlot.MAINHAND, cube, 4.0F, false, true, false);
            Vec3 v = cube.getDeltaMovement();
            player.discard();
            helper.assertTrue(v.horizontalDistance() < 1.0E-6, "knockback-only stab moved the cube by " + v);
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- SULFUR-14 / 18

    @GameTest(template = BOX, timeoutTicks = 60)
    public static void sulfur_block_in_body_slot(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.TRIGGER_TYPES.containsKey(ResourceLocation.withDefaultNamespace("player_sheared_equipment")),
            "player_sheared_equipment trigger registered");
        SulfurCube cube = cube(helper, new Vec3(4.5, 2.0, 4.5), 2);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(((SulfurCubeInvoker) cube).vbp$equipItem(new ItemStack(Items.DIRT)), "cube swallows dirt");
            helper.assertTrue(cube.getItemBySlot(EquipmentSlot.BODY).is(Items.DIRT) && cube.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),
                "swallowed block in BODY, not CHEST");
            helper.assertTrue(cube.hasBodyItem(), "hasBodyItem reads BODY");
            // a cube saved by VB alone (block in the chest slot) loads with the block in BODY
            CompoundTag tag = new CompoundTag();
            SulfurCube old = cube(helper, new Vec3(2.5, 2.0, 2.5), 2);
            old.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
            old.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.STONE));
            old.setDropChance(EquipmentSlot.CHEST, 2.0F);
            old.saveWithoutId(tag);
            SulfurCube loaded = ModEntityTypes.SULFUR_CUBE.get().create(helper.getLevel());
            loaded.load(tag);
            old.discard();
            helper.assertTrue(loaded.getItemBySlot(EquipmentSlot.BODY).is(Items.STONE) && loaded.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),
                "VB save migrated from CHEST to BODY");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- creative fire charge

    /** Native consume(1, player): priming an explosive cube with a fire charge in creative keeps the charge. */
    @GameTest(template = BOX, timeoutTicks = 60)
    public static void sulfur_creative_fire_charge_kept(GameTestHelper helper) {
        SulfurCube cube = cube(helper, new Vec3(4.5, 2.0, 4.5), 2);
        cube.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.TNT));
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(cube.canExplode(), "setup: TNT cube can explode");
            Player player = helper.makeMockPlayer(GameType.CREATIVE);
            player.getAbilities().instabuild = true;
            ItemStack charge = new ItemStack(Items.FIRE_CHARGE, 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, charge);
            cube.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(cube.isPrimed(), "cube primed");
            helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 3, "creative keeps the fire charge: "
                + player.getItemInHand(InteractionHand.MAIN_HAND).getCount());
            cube.discard();
            helper.succeed();
        });
    }
}
