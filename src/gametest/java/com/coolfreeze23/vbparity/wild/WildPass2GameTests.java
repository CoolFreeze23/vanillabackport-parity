package com.coolfreeze23.vbparity.wild;

import com.blackgear.vanillabackport.common.level.blocks.ShelfMushroomBlock;
import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.coolfreeze23.vbparity.VBParity;
import com.coolfreeze23.vbparity.sulfur.SulfurPass2GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Pass-2 Wilderness Bound fixes: shelf mushroom bounce (WILD-24, TBLK-303/351, TPHYS-B01), poplar bone meal (TBLK-352). */
@GameTestHolder(VBParity.MOD_ID)
@PrefixGameTestTemplate(false)
public class WildPass2GameTests {
    private static final String T = "wild_platform";

    private static AABB shelf(GameTestHelper h) {
        h.setBlock(new BlockPos(4, 1, 5), Blocks.STONE);
        h.setBlock(new BlockPos(4, 2, 5), Blocks.STONE);
        h.setBlock(new BlockPos(4, 2, 4), ModBlocks.SHELF_MUSHROOM.get().defaultBlockState().setValue(ShelfMushroomBlock.FACING, Direction.NORTH));
        BlockPos pos = h.absolutePos(new BlockPos(4, 2, 4));
        return h.getLevel().getBlockState(pos).getCollisionShape(h.getLevel(), pos).bounds().move(pos);
    }

    private static void near(GameTestHelper h, double expected, double actual, String what) {
        h.assertTrue(Math.abs(expected - actual) <= 1.0E-6, what + ": expected " + expected + " but was " + actual);
    }

    /** A pig standing on a shelf mushroom stays put (no hop); a falling pig bounces with native compensation. */
    @GameTest(template = T, timeoutTicks = 60)
    public static void wild_shelf_mushroom_rest_and_bounce(GameTestHelper h) {
        AABB top = shelf(h);
        Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new Vec3(1.5, 3, 1.5));
        Vec3 c = top.getCenter();
        pig.setPos(c.x, top.maxY, c.z);
        pig.setDeltaMovement(0.0, -0.0784, 0.0);
        pig.move(MoverType.SELF, pig.getDeltaMovement());
        near(h, 0.0, pig.getDeltaMovement().y, "resting pig vertical speed");
        pig.setPos(c.x, top.maxY + 0.1, c.z);
        pig.setOnGround(false);
        pig.setDeltaMovement(0.0, -0.5, 0.0);
        pig.move(MoverType.SELF, pig.getDeltaMovement());
        double p = 0.1 / 0.5;
        double expected = (p * pig.getGravity() + 0.5) * Mth.lerp(p, 1.0, 0.98) * 0.75;
        near(h, expected, pig.getDeltaMovement().y, "bounce off the shelf mushroom");
        pig.discard();
        h.succeed();
    }

    /** A shelf-mushroom bounce is a vibration (native GameEvent.BOUNCE, frequency 2; HIT_GROUND here). */
    @GameTest(template = T, timeoutTicks = 120)
    public static void wild_shelf_mushroom_bounce_vibration(GameTestHelper h) {
        AABB top = shelf(h);
        h.setBlock(new BlockPos(1, 2, 2), Blocks.SCULK_SENSOR);
        Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new Vec3(1.5, 2, 4.5));
        h.runAfterDelay(50, () -> {
            h.assertTrue(SculkSensorBlock.getPhase(h.getBlockState(new BlockPos(1, 2, 2))) == SculkSensorPhase.INACTIVE, "setup: sensor idle");
            Vec3 c = top.getCenter();
            pig.setPos(c.x, top.maxY + 0.1, c.z);
            pig.setOnGround(false);
            pig.setDeltaMovement(0.0, -0.5, 0.0);
            pig.move(MoverType.SELF, pig.getDeltaMovement());
            pig.setNoGravity(true);
            pig.setDeltaMovement(Vec3.ZERO);
            h.runAfterDelay(8, () -> {
                BlockState sensor = h.getBlockState(new BlockPos(1, 2, 2));
                h.assertTrue(SculkSensorBlock.getPhase(sensor) != SculkSensorPhase.INACTIVE, "sculk sensor heard the bounce");
                pig.discard();
                h.succeed();
            });
        });
    }

    /** Native poplar bone meal: one nextInt(3) from the level random first, refused near the build limit. */
    @GameTest(template = T)
    public static void wild_poplar_sapling_bonemeal_target(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockState sapling = ModBlocks.POPLAR_SAPLING.get().defaultBlockState();
        BonemealableBlock block = (BonemealableBlock) sapling.getBlock();
        BlockPos low = h.absolutePos(new BlockPos(2, 2, 2));
        long before = SulfurPass2GameTests.seedOf(level.random);
        h.assertTrue(block.isValidBonemealTarget(level, low, sapling), "valid target low down");
        long after = SulfurPass2GameTests.seedOf(level.random);
        h.assertTrue(before != after, "the tree type roll draws from the level random");
        BlockPos high = new BlockPos(low.getX(), level.getMaxBuildHeight() - 3, low.getZ());
        h.assertFalse(block.isValidBonemealTarget(level, high, sapling), "refused when the trunk base would pass the build limit");
        h.succeed();
    }
}
