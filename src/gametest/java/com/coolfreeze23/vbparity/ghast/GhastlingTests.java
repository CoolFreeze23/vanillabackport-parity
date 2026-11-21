package com.coolfreeze23.vbparity.ghast;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import com.blackgear.vanillabackport.core.util.WorldUtilities;
import com.coolfreeze23.vbparity.ghast.mixin.PathFinderAccessor;
import com.coolfreeze23.vbparity.ghast.mixin.PathNavigationAccessor;
import java.util.Optional;
import com.blackgear.vanillabackport.common.registries.entities.ModSensorTypes;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public final class GhastlingTests {
    /** GHAST-10: the ghastling's eye height is 0.46875 and its box is 0.95 blocks. */
    @GameTest(template = "ghast_small")
    public static void ghast_ghastlingEyeHeight(GameTestHelper helper) {
        HappyGhast baby = GhastTestUtil.ghast(helper, new Vec3(4.5, 2, 4.5), true);
        baby.setNoAi(true);
        helper.assertTrue(baby.isBaby(), "The ghast is a ghastling");
        helper.assertTrue(GhastTestUtil.near(baby.getEyeHeight(), 0.46875), "Ghastling eye height is 0.46875, saw " + baby.getEyeHeight());
        helper.assertTrue(GhastTestUtil.near(baby.getBbHeight(), 0.95) && GhastTestUtil.near(baby.getBbWidth(), 0.95), "Ghastling box is 0.95, saw " + baby.getBbWidth() + "x" + baby.getBbHeight());
        HappyGhast adult = GhastTestUtil.ghast(helper, new Vec3(4.5, 2, 4.5), false);
        adult.setNoAi(true);
        helper.assertTrue(GhastTestUtil.near(adult.getEyeHeight(), 2.6), "Adult eye height stays 2.6, saw " + adult.getEyeHeight());
        helper.succeed();
    }

    /** GHAST-11: the ghastling path finder may visit floor(max(16, 48) * 16) = 768 nodes. */
    @GameTest(template = "ghast_small")
    public static void ghast_ghastlingPathLength(GameTestHelper helper) {
        HappyGhast baby = GhastTestUtil.ghast(helper, new Vec3(4.5, 2, 4.5), true);
        baby.setNoAi(true);
        int nodes = ((PathFinderAccessor) ((PathNavigationAccessor) baby.getNavigation()).vbp$getPathFinder()).vbp$getMaxVisitedNodes();
        helper.assertTrue(nodes == 768, "Ghastling path finder visits 768 nodes, saw " + nodes);
        helper.succeed();
    }

    /** GHAST-12: a ghastling notices a snowball 13 blocks away (tempt range 16, not 10). The sensor is ticked directly. */
    @GameTest(template = "ghast_big", batch = "ghast_ai_tempt")
    public static void ghast_ghastlingTemptRange(GameTestHelper helper) {
        HappyGhast baby = GhastTestUtil.ghast(helper, new Vec3(2.5, 3, 12.5), true);
        baby.setNoAi(true);
        baby.setNoGravity(true);
        GhastTestUtil.TestPlayer player = GhastTestUtil.player(helper, new Vec3(15.5, 3, 12.5), false);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SNOWBALL));
            Sensor<? super HappyGhast> sensor = ModSensorTypes.HAPPY_GHAST_TEMPTATIONS.get().create();
            for (int i = 0; i < 25; i++) {
                sensor.tick(helper.getLevel(), baby);
            }

            helper.assertTrue(baby.getBrain().getMemory(MemoryModuleType.TEMPTING_PLAYER).filter(p -> p == player).isPresent(),
                "The ghastling is tempted from " + baby.distanceTo(player) + " blocks");
        } finally {
            GhastTestUtil.remove(helper, player);
        }

        helper.succeed();
    }

    /**
     * GHAST-13 and GHAST-14: a tempted ghastling 4 blocks from the player still flies toward it (it stops at 3, not 5),
     * and its walk target is the block at the player's eyes.
     */
    @GameTest(template = "ghast_big", batch = "ghast_ai_close", timeoutTicks = 200)
    public static void ghast_ghastlingTemptCloseDistance(GameTestHelper helper) {
        HappyGhast baby = GhastTestUtil.ghast(helper, new Vec3(8.5, 2, 12.5), true);
        baby.setNoGravity(true);
        GhastTestUtil.TestPlayer player = GhastTestUtil.player(helper, new Vec3(12.5, 1, 12.5), false);
        player.setNoGravity(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SNOWBALL));
        helper.succeedWhen(() -> {
            Optional<WalkTarget> walk = baby.getBrain().getMemory(MemoryModuleType.WALK_TARGET);
            helper.assertTrue(walk.isPresent() && walk.get().getTarget() instanceof EntityTracker tracker && tracker.getEntity() == player,
                "The ghastling flies toward the tempting player");
            BlockPos target = walk.get().getTarget().currentBlockPosition();
            BlockPos eyes = BlockPos.containing(player.getEyePosition());
            helper.assertTrue(target.equals(eyes), "The walk target is the player's eye block " + eyes + ", saw " + target);
            GhastTestUtil.remove(helper, player);
        });
    }

    /** GHAST-13 and GHAST-14: a ghastling 4.5 blocks from an adult follows it (from 3 blocks, not 5), toward its eyes. */
    @GameTest(template = "ghast_big", batch = "ghast_ai_adult", timeoutTicks = 300, attempts = 3, requiredSuccesses = 1)
    public static void ghast_ghastlingFollowsAdult(GameTestHelper helper) {
        HappyGhast adult = GhastTestUtil.ghast(helper, new Vec3(8.5, 2, 12.5), false);
        adult.setNoAi(true);
        adult.setNoGravity(true);
        HappyGhast baby = GhastTestUtil.ghast(helper, new Vec3(13.0, 2, 12.5), true);
        baby.setNoGravity(true);
        helper.succeedWhen(() -> {
            Optional<WalkTarget> walk = baby.getBrain().getMemory(MemoryModuleType.WALK_TARGET);
            helper.assertTrue(walk.isPresent() && walk.get().getTarget() instanceof EntityTracker tracker && tracker.getEntity() == adult,
                "The ghastling follows the adult");
            BlockPos target = walk.get().getTarget().currentBlockPosition();
            BlockPos eyes = BlockPos.containing(adult.getEyePosition());
            helper.assertTrue(target.equals(eyes), "The walk target is the adult's eye block " + eyes + ", saw " + target);
        });
    }

    /**
     * GHAST-15: a panicking ghastling flees to an air position ahead of it (within 90 degrees of where it looks), as
     * with the current game's AnimalPanic(2.0F, 0). VB's land position may lie in any direction. Sampled 40 times
     * through AnimalPanic.getPanicPos with the ghastling facing +Z.
     */
    @GameTest(template = "ghast_big", batch = "ghast_ai_panic")
    public static void ghast_ghastlingPanicsAhead(GameTestHelper helper) throws ReflectiveOperationException {
        HappyGhast baby = GhastTestUtil.ghast(helper, new Vec3(12.5, 12, 12.5), true);
        baby.setNoAi(true);
        baby.setNoGravity(true);
        baby.setYRot(0.0F);
        baby.setXRot(0.0F);
        AnimalPanic<HappyGhast> panic = new AnimalPanic<>(2.0F);
        Method getPanicPos = AnimalPanic.class.getDeclaredMethod("getPanicPos", PathfinderMob.class, ServerLevel.class);
        getPanicPos.setAccessible(true);
        int found = 0;
        for (int i = 0; i < 40; i++) {
            Vec3 target = (Vec3) getPanicPos.invoke(panic, baby, helper.getLevel());
            if (target != null) {
                found++;
                helper.assertTrue(target.z - baby.getZ() >= -1.0, "Flee target " + target + " is ahead of the ghastling (+Z), not behind it");
            }
        }

        helper.assertTrue(found > 0, "The ghastling found flee targets");
        helper.succeed();
    }

    /** GHAST-17: the overworld cloud band is 192.33 to 196.33. */
    @GameTest(template = "ghast_small")
    public static void ghast_cloudBand(GameTestHelper helper) {
        HappyGhast ghast = GhastTestUtil.ghast(helper, new Vec3(4.5, 2, 4.5), false);
        ghast.setNoAi(true);
        ghast.setNoGravity(true);
        double x = ghast.getX();
        double z = ghast.getZ();
        try {
            ghast.setPos(x, 196.2, z);
            helper.assertTrue(WorldUtilities.EnvironmentUtils.isInClouds(ghast), "y 196.2 is inside the clouds (top 196.33)");
            ghast.setPos(x, 188.25, z);
            helper.assertFalse(WorldUtilities.EnvironmentUtils.isInClouds(ghast), "y 188.25 (top of box 192.25) is below the clouds (bottom 192.33)");
        } finally {
            ghast.discard();
        }

        helper.succeed();
    }
}
