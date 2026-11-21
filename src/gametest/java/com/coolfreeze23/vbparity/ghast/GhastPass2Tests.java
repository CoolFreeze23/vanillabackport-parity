package com.coolfreeze23.vbparity.ghast;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.coolfreeze23.vbparity.ghast.mixin.PathFinderAccessor;
import com.coolfreeze23.vbparity.ghast.mixin.PathNavigationAccessor;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Pass-2 GameTests for the ghast area (happy ghast, leads). */
@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public final class GhastPass2Tests {
    private static int nodes(HappyGhast ghast) {
        return ((PathFinderAccessor) ((PathNavigationAccessor) ghast.getNavigation()).vbp$getPathFinder()).vbp$getMaxVisitedNodes();
    }

    /** GHAST-11: the node budget follows the follow range above 48, as native floor(max(FOLLOW_RANGE, 48) * 16). */
    @GameTest(template = "ghast_big", batch = "ghast_p2")
    public static void ghast_p2_ghastlingNodesFollowRange(GameTestHelper helper) {
        HappyGhast baby = GhastTestUtil.ghast(helper, new Vec3(4.5, 3, 4.5), true);
        baby.setNoAi(true);
        baby.setNoGravity(true);
        baby.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(60.0);
        baby.getNavigation().createPath(helper.absolutePos(new BlockPos(12, 3, 12)), 1);
        helper.assertTrue(nodes(baby) == 960, "Ghastling with follow range 60 visits " + nodes(baby) + " nodes, native 960");
        baby.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16.0);
        baby.getNavigation().stop();
        baby.getNavigation().createPath(helper.absolutePos(new BlockPos(14, 3, 14)), 1);
        helper.assertTrue(nodes(baby) == 768, "Ghastling with follow range 16 visits " + nodes(baby) + " nodes, native 768");
        helper.succeed();
    }

    /** GHAST-34: a lead used on a fence with nothing to tie passes (native), instead of consuming the click. */
    @GameTest(template = "ghast_small", batch = "ghast_p2")
    public static void ghast_p2_leadOnFenceWithNothingPasses(GameTestHelper helper) {
        BlockPos fence = new BlockPos(3, 1, 3);
        helper.setBlock(fence, Blocks.OAK_FENCE);
        GhastTestUtil.TestPlayer player = GhastTestUtil.player(helper, new Vec3(1.5, 1, 1.5), false);
        try {
            ItemStack lead = new ItemStack(Items.LEAD);
            player.setItemInHand(InteractionHand.MAIN_HAND, lead);
            BlockPos abs = helper.absolutePos(fence);
            UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
            InteractionResult result = lead.useOn(context);
            helper.assertTrue(result == InteractionResult.PASS, "A lead on an empty fence returned " + result);
        } finally {
            GhastTestUtil.remove(helper, player);
        }
        helper.succeed();
    }

    /** GHAST-05: shearing a harness off fires minecraft:player_sheared_equipment with the harness. */
    @GameTest(template = "ghast_big", batch = "ghast_p2")
    public static void ghast_p2_shearHarnessTrigger(GameTestHelper helper) {
        HappyGhast ghast = GhastTestUtil.ghast(helper, new Vec3(12.5, 2, 12.5), false);
        ghast.setNoAi(true);
        ghast.setNoGravity(true);
        GhastTestUtil.TestPlayer player = GhastTestUtil.player(helper, new Vec3(5.5, 1, 5.5), false);
        try {
            ghast.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.RED_HARNESS.get()));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
            AdvancementHolder advancement = player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("vbparity", "test/shear_harness"));
            helper.assertTrue(advancement != null, "test advancement did not load (unknown trigger?)");
            ghast.interact(player, InteractionHand.MAIN_HAND);
            helper.assertFalse(ghast.isHarnessed(), "Shears did not remove the harness");
            helper.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(), "player_sheared_equipment did not fire");
        } finally {
            GhastTestUtil.remove(helper, player);
        }
        helper.succeed();
    }
}
