package com.coolfreeze23.vbparity.ghast;

import com.blackgear.vanillabackport.common.api.modules.leash_behavior.LeashPhysics;
import com.blackgear.vanillabackport.common.api.modules.leash_behavior.LeashableCallback;
import com.blackgear.vanillabackport.common.api.modules.leash_behavior.InterpolationHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public final class GhastLeashTests {
    private static LeashFenceKnotEntity knot(GameTestHelper helper, BlockPos fence) {
        helper.setBlock(fence, Blocks.OAK_FENCE);
        LeashFenceKnotEntity knot = new LeashFenceKnotEntity(helper.getLevel(), helper.absolutePos(fence));
        helper.getLevel().addFreshEntity(knot);
        return knot;
    }

    /** GHAST-38: the lead drops when its holder becomes a spectator. */
    @GameTest(template = "ghast_big", timeoutTicks = 60)
    public static void ghast_spectatorDropsLeash(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, new Vec3(10.5, 1, 12.5));
        GhastTestUtil.TestPlayer player = GhastTestUtil.player(helper, new Vec3(12.5, 1, 12.5), false);
        cow.setLeashedTo(player, true);
        helper.assertTrue(cow.getLeashHolder() == player, "The cow is leashed to the player");
        player.spectator = true;
        helper.succeedWhen(() -> {
            helper.assertFalse(cow.isLeashed(), "The cow's lead dropped when its holder became a spectator");
            GhastTestUtil.remove(helper, player);
        });
    }

    /** GHAST-33: clicking a knot with empty hands takes its mobs back, and the empty knot goes away. */
    @GameTest(template = "ghast_big")
    public static void ghast_knotHandsMobsBack(GameTestHelper helper) {
        LeashFenceKnotEntity knot = knot(helper, new BlockPos(12, 1, 12));
        Cow cow = helper.spawn(EntityType.COW, new Vec3(14.5, 1, 12.5));
        cow.setLeashedTo(knot, true);
        GhastTestUtil.TestPlayer player = GhastTestUtil.player(helper, new Vec3(12.5, 1, 14.5), false);
        try {
            knot.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(cow.getLeashHolder() == player, "The knot handed the cow back to the player, holder is " + cow.getLeashHolder());
            helper.assertTrue(knot.isRemoved(), "The knot with nothing left on it was removed");
        } finally {
            cow.dropLeash(true, false);
            GhastTestUtil.remove(helper, player);
        }

        helper.succeed();
    }

    /** GHAST-33: a knot removes itself once its last mob is untied. */
    @GameTest(template = "ghast_big")
    public static void ghast_emptyKnotIsRemoved(GameTestHelper helper) {
        LeashFenceKnotEntity knot = knot(helper, new BlockPos(12, 1, 12));
        Cow first = helper.spawn(EntityType.COW, new Vec3(14.5, 1, 12.5));
        Cow second = helper.spawn(EntityType.COW, new Vec3(10.5, 1, 12.5));
        first.setLeashedTo(knot, true);
        second.setLeashedTo(knot, true);
        first.dropLeash(true, false);
        helper.assertFalse(knot.isRemoved(), "The knot stays while a mob is still tied to it");
        second.dropLeash(true, false);
        helper.assertTrue(knot.isRemoved(), "The knot goes away when its last mob is untied");
        helper.succeed();
    }

    /** GHAST-34: tying to a fence finds mobs in a 32-block box (10 blocks away here) but only ties those within snap distance (12). */
    @GameTest(template = "ghast_big")
    public static void ghast_fenceTieSearchBox(GameTestHelper helper) {
        BlockPos fence = new BlockPos(16, 1, 12);
        helper.setBlock(fence, Blocks.OAK_FENCE);
        GhastTestUtil.TestPlayer player = GhastTestUtil.player(helper, new Vec3(14.5, 1, 12.5), false);
        Cow near = helper.spawn(EntityType.COW, new Vec3(6.5, 1, 12.5));
        Cow far = helper.spawn(EntityType.COW, new Vec3(2.5, 1, 12.5));
        try {
            near.setLeashedTo(player, true);
            far.setLeashedTo(player, true);
            LeadItem.bindPlayerMobs(player, helper.getLevel(), helper.absolutePos(fence));
            Entity nearHolder = near.getLeashHolder();
            helper.assertTrue(nearHolder instanceof LeashFenceKnotEntity, "The cow 10 blocks from the fence is tied to it, holder is " + nearHolder);
            helper.assertTrue(far.getLeashHolder() == player, "The cow 14 blocks away is out of snap distance and stays with the player");
        } finally {
            near.dropLeash(true, false);
            far.dropLeash(true, false);
            GhastTestUtil.remove(helper, player);
        }

        helper.succeed();
    }

    /** GHAST-35 and GHAST-36: llamas use a single lead; zombie horses use the native quad offsets. */
    @GameTest(template = "ghast_big")
    public static void ghast_leadAttachments(GameTestHelper helper) {
        Llama llama = helper.spawn(EntityType.LLAMA, new Vec3(4.5, 1, 4.5));
        TraderLlama trader = helper.spawn(EntityType.TRADER_LLAMA, new Vec3(8.5, 1, 4.5));
        Horse horse = helper.spawn(EntityType.HORSE, new Vec3(12.5, 1, 4.5));
        ZombieHorse zombieHorse = helper.spawn(EntityType.ZOMBIE_HORSE, new Vec3(16.5, 1, 4.5));
        helper.assertFalse(((LeashableCallback) llama).vb$supportsQuadLeash(), "A llama gets one lead");
        helper.assertFalse(((LeashableCallback) trader).vb$supportsQuadLeash(), "A trader llama gets one lead");
        helper.assertTrue(((LeashableCallback) horse).vb$supportsQuadLeash(), "A horse still gets four leads");
        Vec3[] expected = LeashPhysics.createQuadOffsets(zombieHorse, 0.04, 0.41, 0.18, 0.73);
        Vec3[] actual = ((LeashableCallback) zombieHorse).vb$getQuadLeashOffsets();
        for (int i = 0; i < 4; i++) {
            helper.assertTrue(actual[i].distanceTo(expected[i]) < 1.0E-9, "Zombie horse lead point " + i + " is " + expected[i] + ", saw " + actual[i]);
        }

        helper.succeed();
    }

    /** GHAST-37: an elastic lead pull stops a horse grazing and stands a sitting camel up. */
    @GameTest(template = "ghast_big", timeoutTicks = 120)
    public static void ghast_elasticPullReactions(GameTestHelper helper) {
        Horse horse = helper.spawn(EntityType.HORSE, new Vec3(6.5, 1, 6.5));
        Camel camel = helper.spawn(EntityType.CAMEL, new Vec3(14.5, 1, 14.5));
        horse.setNoAi(true);
        camel.setNoAi(true);
        camel.sitDown();
        helper.runAfterDelay(60, () -> {
            horse.setEating(true);
            ((LeashableCallback) horse).vb$onElasticLeashPull();
            helper.assertFalse(horse.isEating(), "A lead pull stops the horse grazing");
            helper.assertTrue(camel.isCamelSitting() && !camel.isInPoseTransition(), "The camel has finished sitting down");
            ((LeashableCallback) camel).vb$onElasticLeashPull();
            helper.assertFalse(camel.isCamelSitting(), "A lead pull stands the camel up");
            helper.succeed();
        });
    }

    /** GHAST-41: a repeated identical target does not restart a running boat interpolation. */
    @GameTest(template = "ghast_small")
    public static void ghast_interpolationIgnoresSameTarget(GameTestHelper helper) {
        Boat boat = helper.spawn(EntityType.BOAT, new Vec3(4.5, 2, 4.5));
        boat.setNoGravity(true);
        InterpolationHandler handler = new InterpolationHandler(boat, 3);
        Vec3 target = boat.position().add(0.0, 0.0, 1.0);
        handler.interpolateTo(target, 0.0F, 0.0F);
        handler.interpolate();
        handler.interpolateTo(target, 0.0F, 0.0F);
        handler.interpolate();
        handler.interpolate();
        helper.assertFalse(handler.hasActiveInterpolation(), "The same target did not restart the three-step interpolation");
        boat.discard();
        helper.succeed();
    }
}
