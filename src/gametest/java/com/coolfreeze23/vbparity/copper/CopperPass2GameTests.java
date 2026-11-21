package com.coolfreeze23.vbparity.copper;

import com.blackgear.vanillabackport.common.level.block_entities.ShelfBlockEntity;
import com.blackgear.vanillabackport.common.level.blocks.ShelfBlock;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.golem.copper_golem.CopperGolem;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.golem.copper_golem.container.ContainerManager;
import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.blackgear.vanillabackport.core.VanillaBackport;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.OfferFlowerGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Pass 2: copper classes nobody audited, the partial items and the technical audits. */
@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public final class CopperPass2GameTests {
    private static final String SMALL = "copper_small";

    // ---------------------------------------------------------------- COPPER-22 / COPPER-25: antenna and flower goal

    /** The vanilla flower goal offers to a copper golem (no villager near) and puts the poppy on the antenna (BODY). */
    @GameTest(template = SMALL, batch = "vbp_copper_flower2", timeoutTicks = 40, attempts = 3, requiredSuccesses = 1)
    public static void copper_iron_golem_gives_copper_golem_poppy(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();
        level.setDayTime(1000L);
        level.updateSkyBrightness(); // isDay() reads skyDarken, which otherwise updates only on the next tick
        IronGolem iron = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(3, 2, 4));
        iron.setNoAi(true);
        CopperGolem copper = helper.spawn(ModEntityTypes.COPPER_GOLEM.get(), new BlockPos(5, 2, 4));
        copper.setNoAi(true);
        copper.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        OfferFlowerGoal goal = null;
        for (WrappedGoal wrapped : iron.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof OfferFlowerGoal offer) {
                goal = offer;
            }
        }
        helper.assertTrue(goal != null, "iron golem has no OfferFlowerGoal");
        boolean started = false;
        for (int i = 0; i < 400_000 && !started; i++) {
            started = goal.canUse();
        }
        helper.assertTrue(started, "the flower goal never chose the copper golem (native candidates include #accepts_iron_golem_gift)");
        goal.start();
        Field tick = OfferFlowerGoal.class.getDeclaredField("tick");
        tick.setAccessible(true);
        for (int i = 0; i < 1000 && tick.getInt(goal) > 0; i++) {
            goal.tick();
        }
        goal.stop();
        helper.assertTrue(copper.getItemBySlot(CopperAntenna.SLOT).is(Items.POPPY), "antenna (native SADDLE, here BODY) got " + copper.getItemBySlot(CopperAntenna.SLOT));
        helper.assertTrue(copper.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN), "head item was replaced: " + copper.getItemBySlot(EquipmentSlot.HEAD));
        helper.succeed();
    }

    /** Shearing takes the antenna item and leaves a head item; VB kept the antenna in HEAD and sheared the pumpkin. */
    @GameTest(template = SMALL)
    public static void copper_golem_shear_keeps_head_item(GameTestHelper helper) {
        CopperGolem golem = helper.spawn(ModEntityTypes.COPPER_GOLEM.get(), new BlockPos(4, 2, 4));
        golem.setNoAi(true);
        golem.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        helper.assertFalse(golem.readyForShearing(), "a golem with only a head item is shearable");
        golem.setItemSlot(CopperAntenna.SLOT, new ItemStack(Items.POPPY));
        helper.assertTrue(golem.readyForShearing(), "golem with an antenna poppy is not shearable");
        golem.shear(SoundSource.PLAYERS);
        helper.assertTrue(golem.getItemBySlot(CopperAntenna.SLOT).isEmpty(), "antenna kept " + golem.getItemBySlot(CopperAntenna.SLOT));
        helper.assertTrue(golem.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN), "shearing removed the head item");
        helper.succeed();
    }

    /** A VB-era save (antenna in HEAD, no marker) loads with the antenna in BODY; marked saves load as saved. */
    @GameTest(template = SMALL)
    public static void copper_golem_antenna_save_migration(GameTestHelper helper) {
        CopperGolem old = helper.spawn(ModEntityTypes.COPPER_GOLEM.get(), new BlockPos(4, 2, 4));
        old.setNoAi(true);
        old.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.POPPY));
        old.setGuaranteedDrop(EquipmentSlot.HEAD);
        CompoundTag vbSave = old.saveWithoutId(new CompoundTag());
        helper.assertTrue(vbSave.getBoolean(CopperAntenna.SAVE_MARKER), "saves carry no antenna marker");
        vbSave.remove(CopperAntenna.SAVE_MARKER);
        CopperGolem loaded = ModEntityTypes.COPPER_GOLEM.get().create(helper.getLevel());
        loaded.load(vbSave);
        helper.assertTrue(loaded.getItemBySlot(CopperAntenna.SLOT).is(Items.POPPY), "VB save: antenna not moved to BODY");
        helper.assertTrue(loaded.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "VB save: HEAD still holds " + loaded.getItemBySlot(EquipmentSlot.HEAD));

        CompoundTag marked = old.saveWithoutId(new CompoundTag());
        CopperGolem kept = ModEntityTypes.COPPER_GOLEM.get().create(helper.getLevel());
        kept.load(marked);
        helper.assertTrue(kept.getItemBySlot(EquipmentSlot.HEAD).is(Items.POPPY), "marked save: HEAD item was moved");
        loaded.discard();
        kept.discard();
        helper.succeed();
    }

    // ---------------------------------------------------------------- Job 1: bars, rods, shelves

    /** Native IronBarsBlock.skipRendering culls faces between different #bars blocks. */
    @GameTest(template = SMALL)
    public static void copper_bars_cull_other_bars(GameTestHelper helper) {
        BlockState copper = ModBlocks.COPPER_BARS.unaffected().get().defaultBlockState().setValue(IronBarsBlock.EAST, true);
        BlockState iron = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true);
        helper.assertTrue(copper.skipRendering(iron, Direction.EAST), "copper bars do not cull the iron bars they touch");
        helper.assertTrue(iron.skipRendering(copper, Direction.WEST), "iron bars do not cull the copper bars they touch");
        helper.assertFalse(copper.skipRendering(Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.WEST, true), Direction.EAST), "bars cull a glass pane");
        helper.succeed();
    }

    /** TBLK-101: a waxed rod is no oxidation neighbour; an exposed copper block beside it still ages (native chance 1). */
    @GameTest(template = SMALL)
    public static void copper_waxed_rod_does_not_stop_oxidation(GameTestHelper helper) {
        BlockPos copperRel = new BlockPos(4, 2, 4);
        helper.setBlock(copperRel, Blocks.EXPOSED_COPPER);
        helper.setBlock(copperRel.east(), ModBlocks.WAXED_LIGHTNING_ROD.get());
        BlockPos copperPos = helper.absolutePos(copperRel);
        BlockState state = helper.getLevel().getBlockState(copperPos);
        Optional<BlockState> next = ((WeatheringCopper) state.getBlock()).getNextState(state, helper.getLevel(), copperPos, RandomSource.create(1L));
        helper.assertTrue(next.isPresent() && next.get().is(Blocks.WEATHERED_COPPER), "exposed copper beside a waxed rod did not age: " + next);
        helper.succeed();
    }

    /** TBLK-102: a powered rod replaced by another rod (strike cleaning) stays powered and unpowers after 8 ticks. */
    @GameTest(template = SMALL, timeoutTicks = 40)
    public static void copper_cleaned_rod_keeps_pulse(GameTestHelper helper) {
        BlockPos rodRel = new BlockPos(4, 2, 4);
        helper.setBlock(rodRel, ModBlocks.WEATHERED_LIGHTNING_ROD.get().defaultBlockState().setValue(LightningRodBlock.POWERED, true));
        BlockPos rod = helper.absolutePos(rodRel);
        ServerLevel level = helper.getLevel();
        level.setBlockAndUpdate(rod, WeatheringCopper.getFirst(level.getBlockState(rod)));
        BlockState cleaned = level.getBlockState(rod);
        helper.assertTrue(cleaned.is(Blocks.LIGHTNING_ROD) && cleaned.getValue(LightningRodBlock.POWERED), "cleaned rod is " + cleaned + " (native keeps it powered)");
        helper.assertTrue(level.getBlockTicks().hasScheduledTick(rod, Blocks.LIGHTNING_ROD), "no 8-tick unpower tick scheduled");
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(level.getBlockState(rod).getValue(LightningRodBlock.POWERED), "rod still powered after 10 ticks");
            helper.succeed();
        });
    }

    /** Native shelves are all ignitedByLava; crimson and warped keep their stem colours. */
    @GameTest(template = SMALL)
    public static void copper_nether_shelves_properties(GameTestHelper helper) {
        for (Block shelf : List.of(ModBlocks.CRIMSON_SHELF.get(), ModBlocks.WARPED_SHELF.get(), ModBlocks.OAK_SHELF.get())) {
            helper.assertTrue(shelf.defaultBlockState().ignitedByLava(), shelf + " is not ignited by lava");
        }
        MapColor crimson = ModBlocks.CRIMSON_SHELF.get().defaultBlockState().getMapColor(helper.getLevel(), BlockPos.ZERO);
        MapColor warped = ModBlocks.WARPED_SHELF.get().defaultBlockState().getMapColor(helper.getLevel(), BlockPos.ZERO);
        helper.assertTrue(crimson == MapColor.CRIMSON_STEM && warped == MapColor.WARPED_STEM, "shelf map colours " + crimson.id + " / " + warped.id);
        helper.succeed();
    }

    /** COPPER-27: the native ON_SHELF display context exists (NeoForge enum extension). */
    @GameTest(template = SMALL)
    public static void copper_on_shelf_display_context(GameTestHelper helper) {
        ItemDisplayContext onShelf = CopperShelf.onShelf();
        helper.assertTrue(onShelf != null && onShelf.isModded() && onShelf.fallback() == null, "ON_SHELF context missing or with a fallback");
        helper.assertTrue(ItemDisplayContext.BY_ID.apply(onShelf.getId()) == onShelf, "ON_SHELF has no id");
        helper.succeed();
    }

    // ---------------------------------------------------------------- TBLK-103: double chest scrape and wax

    private static int partnerEvents(GameTestHelper helper, Block chest, ItemStack tool, int levelEvent) {
        BlockPos leftRel = new BlockPos(3, 2, 3);
        BlockPos rightRel = new BlockPos(4, 2, 3);
        helper.setBlock(leftRel, chest.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
        helper.setBlock(rightRel, chest.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        BlockPos clicked = helper.absolutePos(leftRel);
        try (PacketCapture capture = new PacketCapture(helper.getLevel(), Vec3.atCenterOf(clicked))) {
            tool.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(clicked), Direction.NORTH, clicked, false)));
            return (int) capture.of(ClientboundLevelEventPacket.class).stream().filter(p -> p.getType() == levelEvent).count();
        }
    }

    @GameTest(template = SMALL, batch = "vbp_copper_chest_events")
    public static void copper_double_chest_scrape_both_halves(GameTestHelper helper) {
        int events = partnerEvents(helper, ModBlocks.EXPOSED_COPPER_CHEST.get(), new ItemStack(Items.IRON_AXE), 3005);
        helper.assertTrue(events == 2, "scrape particles sent for " + events + " halves (native 2)");
        helper.succeed();
    }

    @GameTest(template = SMALL, batch = "vbp_copper_chest_events")
    public static void copper_double_chest_wax_both_halves(GameTestHelper helper) {
        int events = partnerEvents(helper, ModBlocks.COPPER_CHEST.get(), new ItemStack(Items.HONEYCOMB), 3003);
        helper.assertTrue(events == 2, "wax particles sent for " + events + " halves (native 2)");
        helper.succeed();
    }

    // ---------------------------------------------------------------- TBLK-104: spear off a shelf

    @GameTest(template = SMALL, timeoutTicks = 60)
    public static void copper_shelf_spear_swap_is_silent(GameTestHelper helper) throws Exception {
        BlockPos shelfRel = new BlockPos(4, 2, 4);
        helper.setBlock(shelfRel, ModBlocks.OAK_SHELF.get().defaultBlockState().setValue(ShelfBlock.FACING, Direction.NORTH));
        BlockPos sensorRel = new BlockPos(6, 2, 4);
        helper.setBlock(sensorRel, Blocks.SCULK_SENSOR);
        helper.runAfterDelay(15, () -> {
            try {
                ShelfBlockEntity shelf = (ShelfBlockEntity) helper.getBlockEntity(shelfRel);
                shelf.swapItemNoUpdate(1, new ItemStack(ModItems.IRON_SPEAR.get()));
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                Method swap = ShelfBlock.class.getDeclaredMethod("swapSingleItem", ItemStack.class, Player.class, ShelfBlockEntity.class, int.class, net.minecraft.world.entity.player.Inventory.class);
                swap.setAccessible(true);
                swap.invoke(ModBlocks.OAK_SHELF.get(), ItemStack.EMPTY, player, shelf, 1, player.getInventory());
                helper.assertTrue(player.getMainHandItem().is(ModItems.IRON_SPEAR.get()), "player did not get the spear");
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
        helper.runAfterDelay(35, () -> {
            SculkSensorPhase phase = helper.getBlockState(sensorRel).getValue(SculkSensorBlock.PHASE);
            helper.assertTrue(phase == SculkSensorPhase.INACTIVE, "sculk sensor heard the spear swap (" + phase + "); native sends no vibration");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- SPAWN-08: golem pattern order

    @GameTest(template = SMALL, timeoutTicks = 20)
    public static void copper_snow_golem_beats_copper_golem(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.SNOW_BLOCK);
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.SNOW_BLOCK);
        helper.setBlock(new BlockPos(5, 4, 4), Blocks.COPPER_BLOCK);
        helper.setBlock(new BlockPos(4, 4, 4), Blocks.CARVED_PUMPKIN);
        AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12);
        List<SnowGolem> snow = helper.getLevel().getEntitiesOfClass(SnowGolem.class, box);
        List<CopperGolem> copper = helper.getLevel().getEntitiesOfClass(CopperGolem.class, box);
        helper.assertTrue(snow.size() == 1 && copper.isEmpty(), snow.size() + " snow golems and " + copper.size() + " copper golems (native: the snow golem)");
        helper.assertBlockPresent(Blocks.COPPER_BLOCK, new BlockPos(5, 4, 4));
        snow.forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }

    @GameTest(template = SMALL, timeoutTicks = 20)
    public static void copper_golem_still_builds(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.COPPER_BLOCK);
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.CARVED_PUMPKIN);
        AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12);
        List<CopperGolem> copper = helper.getLevel().getEntitiesOfClass(CopperGolem.class, box);
        helper.assertTrue(copper.size() == 1, copper.size() + " copper golems from a copper block and a pumpkin");
        copper.forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }

    // ---------------------------------------------------------------- Job 3: COPPER-03 gate

    /** With has_copper_golems off, golems (eggs, /summon, old worlds) still open chests; the recheck must count them. */
    @GameTest(template = SMALL, batch = "vbp_copper_config_off", timeoutTicks = 40)
    public static void copper_chest_count_with_golems_config_off(GameTestHelper helper) {
        boolean before = VanillaBackport.COMMON_CONFIG.hasCopperGolems.get();
        VanillaBackport.COMMON_CONFIG.hasCopperGolems.set(false);
        try {
            BlockPos chestRel = new BlockPos(3, 2, 3);
            helper.setBlock(chestRel, ModBlocks.COPPER_CHEST.get().defaultBlockState());
            CopperGolem golem = helper.spawn(ModEntityTypes.COPPER_GOLEM.get(), chestRel.east(2));
            golem.setNoAi(true);
            BlockPos chest = helper.absolutePos(chestRel);
            golem.setOpenedChestPos(chest);
            ContainerManager.startOpen(helper.getLevel().getBlockEntity(chest), golem);
        } catch (RuntimeException e) {
            VanillaBackport.COMMON_CONFIG.hasCopperGolems.set(before);
            throw e;
        }
        helper.runAfterDelay(12, () -> {
            try {
                BlockPos chest = helper.absolutePos(new BlockPos(3, 2, 3));
                CopperGolem golem = helper.getLevel().getEntitiesOfClass(CopperGolem.class, new AABB(chest).inflate(4)).get(0);
                int held = ChestBlockEntity.getOpenCount(helper.getLevel(), chest);
                helper.assertTrue(held == 1, "recheck dropped the golem: count " + held);
                ContainerManager.stopOpen(helper.getLevel().getBlockEntity(chest), golem);
                golem.setOpenedChestPos(null);
                int after = ChestBlockEntity.getOpenCount(helper.getLevel(), chest);
                helper.assertTrue(after == 0, "count after the golem closed it: " + after);
                helper.succeed();
            } finally {
                VanillaBackport.COMMON_CONFIG.hasCopperGolems.set(before);
            }
        });
    }

    // ---------------------------------------------------------------- AI-CPG-01 / 02: which chests a golem opens and closes

    /** A single chest's neighbour (here a trapped chest at its counter-clockwise side) is not opened or closed. */
    @GameTest(template = SMALL, timeoutTicks = 30)
    public static void copper_golem_single_chest_leaves_neighbour(GameTestHelper helper) {
        BlockPos chestRel = new BlockPos(4, 2, 4);
        BlockPos trappedRel = chestRel.west();
        helper.setBlock(chestRel, ModBlocks.COPPER_CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
        helper.setBlock(trappedRel, Blocks.TRAPPED_CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
        CopperGolem golem = helper.spawn(ModEntityTypes.COPPER_GOLEM.get(), chestRel.south(2));
        golem.setNoAi(true);
        BlockPos chest = helper.absolutePos(chestRel);
        BlockPos trapped = helper.absolutePos(trappedRel);
        golem.setOpenedChestPos(chest);
        ContainerManager.startOpen(helper.getLevel().getBlockEntity(chest), golem);
        helper.assertTrue(ChestBlockEntity.getOpenCount(helper.getLevel(), chest) == 1, "golem did not open its chest");
        helper.assertTrue(ChestBlockEntity.getOpenCount(helper.getLevel(), trapped) == 0, "the neighbouring trapped chest was opened too");
        helper.runAfterDelay(12, () -> {
            ContainerManager.stopOpen(helper.getLevel().getBlockEntity(chest), golem);
            golem.clearOpenedChestPos();
            int neighbour = ChestBlockEntity.getOpenCount(helper.getLevel(), trapped);
            helper.assertTrue(neighbour == 0, "neighbour opener count " + neighbour);
            helper.succeed();
        });
    }

    /** A double chest is not closed by the golem at tick 60; the opener recheck closes it after the golem lets go. */
    @GameTest(template = SMALL, timeoutTicks = 30)
    public static void copper_golem_double_chest_closes_on_recheck(GameTestHelper helper) {
        BlockPos leftRel = new BlockPos(3, 2, 3);
        BlockPos rightRel = new BlockPos(4, 2, 3);
        helper.setBlock(leftRel, ModBlocks.COPPER_CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
        helper.setBlock(rightRel, ModBlocks.COPPER_CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
        CopperGolem golem = helper.spawn(ModEntityTypes.COPPER_GOLEM.get(), leftRel.south(2));
        golem.setNoAi(true);
        BlockPos left = helper.absolutePos(leftRel);
        BlockPos right = helper.absolutePos(rightRel);
        golem.setOpenedChestPos(left);
        ContainerManager.startOpen(helper.getLevel().getBlockEntity(left), golem);
        helper.assertTrue(ChestBlockEntity.getOpenCount(helper.getLevel(), left) == 1 && ChestBlockEntity.getOpenCount(helper.getLevel(), right) == 1, "both halves not opened");
        ContainerManager.stopOpen(helper.getLevel().getBlockEntity(left), golem);
        helper.assertTrue(ChestBlockEntity.getOpenCount(helper.getLevel(), left) == 1, "the golem closed the double chest itself (native leaves it to the recheck)");
        golem.clearOpenedChestPos();
        helper.runAfterDelay(7, () -> {
            helper.assertTrue(ChestBlockEntity.getOpenCount(helper.getLevel(), left) == 0 && ChestBlockEntity.getOpenCount(helper.getLevel(), right) == 0, "recheck did not close the double chest");
            helper.succeed();
        });
    }
}
