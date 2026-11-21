package com.coolfreeze23.vbparity.ghast;

import com.blackgear.vanillabackport.common.integrations.interactions.GhastHarnessInteraction;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public final class GhastHarnessTests {
    private static ItemStack redHarness() {
        return new ItemStack(ModItems.RED_HARNESS.get());
    }

    /** GHAST-01: a harnessed ghast that dies without a player kill still drops its harness. */
    @GameTest(template = "ghast_big")
    public static void ghast_harnessAlwaysDrops(GameTestHelper helper) {
        for (int i = 0; i < 5; i++) {
            HappyGhast ghast = GhastTestUtil.ghast(helper, new Vec3(12.5, 2, 12.5), false);
            ghast.setNoAi(true);
            ghast.setItemSlot(EquipmentSlot.CHEST, redHarness());
            ghast.kill();
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, ghast.getBoundingBox().inflate(2.0),
                item -> item.getItem().is(ModItems.RED_HARNESS.get()));
            helper.assertTrue(drops.size() == 1, "A dead harnessed ghast drops its harness every time, run " + i + " saw " + drops.size());
            drops.forEach(ItemEntity::discard);
        }

        helper.succeed();
    }

    /** GHAST-02: the harness the ghast wears is the player's own stack, with its components. */
    @GameTest(template = "ghast_big")
    public static void ghast_harnessKeepsComponents(GameTestHelper helper) {
        HappyGhast ghast = GhastTestUtil.ghast(helper, new Vec3(12.5, 2, 12.5), false);
        ghast.setNoAi(true);
        GhastTestUtil.TestPlayer player = GhastTestUtil.player(helper, new Vec3(5.5, 1, 5.5), false);
        try {
            ItemStack harness = redHarness();
            harness.set(DataComponents.CUSTOM_NAME, Component.literal("Cloud Rider"));
            player.setItemInHand(InteractionHand.MAIN_HAND, harness);
            new GhastHarnessInteraction().onInteract(player, ghast, InteractionHand.MAIN_HAND);
            ItemStack worn = ghast.getItemBySlot(EquipmentSlot.CHEST);
            helper.assertTrue(worn.is(ModItems.RED_HARNESS.get()), "The ghast wears the red harness");
            helper.assertTrue(Component.literal("Cloud Rider").equals(worn.get(DataComponents.CUSTOM_NAME)), "The worn harness keeps its custom name, saw " + worn.get(DataComponents.CUSTOM_NAME));
            helper.assertTrue(player.getMainHandItem().isEmpty(), "A survival player's harness is used up");
        } finally {
            GhastTestUtil.remove(helper, player);
        }

        helper.succeed();
    }

    /** GHAST-04: a dispenser puts a harness on the happy ghast in front of it. */
    @GameTest(template = "ghast_big", timeoutTicks = 60)
    public static void ghast_dispenserHarnesses(GameTestHelper helper) {
        BlockPos dispenser = new BlockPos(12, 1, 12);
        helper.setBlock(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.UP));
        DispenserBlockEntity be = helper.getBlockEntity(dispenser);
        be.setItem(0, redHarness());
        HappyGhast ghast = GhastTestUtil.ghast(helper, new Vec3(12.5, 2, 12.5), false);
        ghast.setNoAi(true);
        ghast.setNoGravity(true);
        helper.setBlock(dispenser.east(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            helper.assertTrue(ghast.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.RED_HARNESS.get()), "The dispenser harnessed the ghast");
            helper.assertTrue(be.isEmpty(), "The harness left the dispenser");
        });
    }

    /** GHAST-05: shears respect sneaking and Curse of Binding, and the harness drops at seat height (y + 4). */
    @GameTest(template = "ghast_big")
    public static void ghast_shearingRules(GameTestHelper helper) {
        HappyGhast ghast = GhastTestUtil.ghast(helper, new Vec3(12.5, 2, 12.5), false);
        ghast.setNoAi(true);
        ghast.setNoGravity(true);
        GhastTestUtil.TestPlayer player = GhastTestUtil.player(helper, new Vec3(5.5, 1, 5.5), false);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));

            ghast.setItemSlot(EquipmentSlot.CHEST, redHarness());
            player.setShiftKeyDown(true);
            ghast.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(ghast.isHarnessed(), "Sneaking with shears does not remove the harness");
            helper.assertFalse(player.isPassenger(), "Sneaking does not mount either");
            player.setShiftKeyDown(false);

            ItemStack bound = redHarness();
            Holder<Enchantment> binding = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE);
            bound.enchant(binding, 1);
            ghast.setItemSlot(EquipmentSlot.CHEST, bound);
            ghast.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(ghast.isHarnessed(), "Curse of Binding keeps the harness on for a survival player");
            // The click then falls through to mounting, as in the current game.
            player.stopRiding();

            ghast.setItemSlot(EquipmentSlot.CHEST, redHarness());
            ghast.interact(player, InteractionHand.MAIN_HAND);
            helper.assertFalse(ghast.isHarnessed(), "Shears remove the harness");
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, ghast.getBoundingBox().inflate(3.0),
                item -> item.getItem().is(ModItems.RED_HARNESS.get()));
            helper.assertTrue(drops.size() == 1, "The sheared harness drops, saw " + drops.size());
            double dy = drops.get(0).getY() - ghast.getY();
            helper.assertTrue(GhastTestUtil.near(dy, 4.0), "The harness drops at the average seat height (+4.0), saw +" + dy);
            drops.forEach(ItemEntity::discard);
        } finally {
            GhastTestUtil.remove(helper, player);
        }

        helper.succeed();
    }

    /** GHAST-06: sneak-right-click on a harnessed ghast is not used up by the ride branch (it falls through); a normal click mounts. */
    @GameTest(template = "ghast_big")
    public static void ghast_sneakDoesNotMount(GameTestHelper helper) {
        HappyGhast ghast = GhastTestUtil.ghast(helper, new Vec3(12.5, 2, 12.5), false);
        ghast.setNoAi(true);
        ghast.setNoGravity(true);
        ghast.setItemSlot(EquipmentSlot.CHEST, redHarness());
        GhastTestUtil.TestPlayer player = GhastTestUtil.player(helper, new Vec3(5.5, 1, 5.5), false);
        try {
            player.setShiftKeyDown(true);
            InteractionResult sneaking = ghast.interact(player, InteractionHand.MAIN_HAND);
            helper.assertFalse(player.isPassenger(), "A sneaking player does not mount");
            helper.assertFalse(sneaking.consumesAction(), "A sneak-click falls through to the animal interaction instead of being used up, saw " + sneaking);
            player.setShiftKeyDown(false);
            ghast.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getVehicle() == ghast, "A normal click mounts");
            player.stopRiding();
        } finally {
            GhastTestUtil.remove(helper, player);
        }

        helper.succeed();
    }

    /** GHAST-07: the four seats are 1.7 blocks from the centre at height 4. */
    @GameTest(template = "ghast_small")
    public static void ghast_seatPositions(GameTestHelper helper) {
        EntityType<HappyGhast> type = ModEntityTypes.HAPPY_GHAST.get();
        Vec3[] expected = {new Vec3(0.0, 4.0, 1.7), new Vec3(-1.7, 4.0, 0.0), new Vec3(0.0, 4.0, -1.7), new Vec3(1.7, 4.0, 0.0)};
        for (int i = 0; i < expected.length; i++) {
            Vec3 seat = type.getDimensions().attachments().get(EntityAttachment.PASSENGER, i, 0.0F);
            helper.assertTrue(seat.distanceTo(expected[i]) < 1.0E-6, "Seat " + i + " is at " + expected[i] + ", saw " + seat);
        }

        helper.succeed();
    }

    /** GHAST-09: a ridden ghast is solid only to other happy ghasts. */
    @GameTest(template = "ghast_big")
    public static void ghast_riddenCollision(GameTestHelper helper) {
        HappyGhast ghast = GhastTestUtil.ghast(helper, new Vec3(12.5, 2, 12.5), false);
        ghast.setNoAi(true);
        ghast.setNoGravity(true);
        Zombie rider = helper.spawn(EntityType.ZOMBIE, new Vec3(12.5, 7, 12.5));
        rider.startRiding(ghast, true);
        Zombie other = helper.spawn(EntityType.ZOMBIE, new Vec3(4.5, 1, 4.5));
        HappyGhast otherGhast = GhastTestUtil.ghast(helper, new Vec3(4.5, 10, 18.5), false);
        otherGhast.setNoAi(true);
        helper.assertTrue(ghast.isVehicle(), "The ghast is ridden");
        helper.assertFalse(ghast.isOnStillTimeout(), "The ghast is not holding still");
        helper.assertFalse(other.canCollideWith(ghast), "A ridden ghast is not solid to a zombie");
        helper.assertTrue(otherGhast.canCollideWith(ghast), "A ridden ghast is solid to another happy ghast");
        helper.assertFalse(ghast.canBeCollidedWith(), "With no other entity known, a moving ghast is not solid");
        rider.stopRiding();
        helper.succeed();
    }
}
