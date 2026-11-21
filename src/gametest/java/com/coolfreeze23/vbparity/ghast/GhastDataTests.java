package com.coolfreeze23.vbparity.ghast;

import com.blackgear.platform.common.data.neoforge.LootPoolAccess;
import com.blackgear.platform.core.mixin.neoforge.access.LootTableAccessor;
import com.blackgear.vanillabackport.common.level.blocks.DriedGhastBlock;
import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.coolfreeze23.vbparity.ghast.mixin.LootItemAccessor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.NetherFossilPieces;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public final class GhastDataTests {
    /** GHAST-21: the dried ghast is a legacy-solid block (forceSolidOn). */
    @GameTest(template = "ghast_small")
    public static void ghast_driedGhastIsSolid(GameTestHelper helper) {
        BlockState state = ModBlocks.DRIED_GHAST.get().defaultBlockState();
        helper.assertTrue(state.isSolid(), "Dried ghast isSolid()");
        helper.assertTrue(state.blocksMotion(), "Dried ghast blocksMotion()");
        helper.succeed();
    }

    /** GHAST-24: placing into falling flowing water does not waterlog the block; a water source does. */
    @GameTest(template = "ghast_small")
    public static void ghast_driedGhastWaterlogging(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        DriedGhastBlock block = (DriedGhastBlock) ModBlocks.DRIED_GHAST.get();
        BlockPos falling = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos source = helper.absolutePos(new BlockPos(5, 2, 5));
        level.setBlock(falling, Fluids.FLOWING_WATER.getFlowing(8, true).createLegacyBlock(), 2);
        level.setBlock(source, Blocks.WATER.defaultBlockState(), 2);
        helper.assertTrue(level.getFluidState(falling).getAmount() == 8 && !level.getFluidState(falling).isSource(), "The test water is falling flowing water");

        BlockState inFalling = block.getStateForPlacement(context(level, falling));
        BlockState inSource = block.getStateForPlacement(context(level, source));
        helper.assertFalse(inFalling.getValue(DriedGhastBlock.WATERLOGGED), "Falling flowing water does not waterlog a placed dried ghast");
        helper.assertTrue(inSource.getValue(DriedGhastBlock.WATERLOGGED), "A water source waterlogs a placed dried ghast");
        level.setBlock(falling, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(source, Blocks.AIR.defaultBlockState(), 2);
        helper.succeed();
    }

    private static BlockPlaceContext context(ServerLevel level, BlockPos pos) {
        return new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.DRIED_GHAST.get()),
            new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    /** GHAST-16: ghastlings follow adult armadillos too. */
    @GameTest(template = "ghast_small")
    public static void ghast_armadilloIsFollowable(GameTestHelper helper) {
        TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("followable_friendly_mobs"));
        helper.assertTrue(EntityType.ARMADILLO.is(tag), "Armadillo is in followable_friendly_mobs");
        helper.assertTrue(ModEntityTypes.HAPPY_GHAST.get().is(tag), "Happy ghast is still in followable_friendly_mobs");
        helper.succeed();
    }

    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
        "purple", "blue", "brown", "green", "red", "black"};

    /** GHAST-27 and GHAST-28: harness recipes are in the Equipment tab, and dyeing never takes a harness of the target colour. */
    @GameTest(template = "ghast_small")
    public static void ghast_harnessRecipes(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        for (String color : COLORS) {
            Item target = helper.getLevel().registryAccess().registryOrThrow(Registries.ITEM).get(ResourceLocation.withDefaultNamespace(color + "_harness"));
            CraftingRecipe craft = crafting(helper, manager.byKey(ResourceLocation.withDefaultNamespace(color + "_harness")), color + "_harness");
            CraftingRecipe dye = crafting(helper, manager.byKey(ResourceLocation.withDefaultNamespace("dye_" + color + "_harness")), "dye_" + color + "_harness");
            helper.assertTrue(craft.category() == CraftingBookCategory.EQUIPMENT, color + "_harness is in the Equipment tab, saw " + craft.category());
            helper.assertTrue(dye.category() == CraftingBookCategory.EQUIPMENT, "dye_" + color + "_harness is in the Equipment tab, saw " + dye.category());
            Ingredient harnesses = dye.getIngredients().get(1);
            helper.assertFalse(harnesses.test(new ItemStack(target)), "dye_" + color + "_harness does not take a " + color + " harness");
            for (String other : COLORS) {
                if (!other.equals(color)) {
                    Item otherItem = helper.getLevel().registryAccess().registryOrThrow(Registries.ITEM).get(ResourceLocation.withDefaultNamespace(other + "_harness"));
                    helper.assertTrue(harnesses.test(new ItemStack(otherItem)), "dye_" + color + "_harness takes a " + other + " harness");
                }
            }
        }

        helper.succeed();
    }

    private static CraftingRecipe crafting(GameTestHelper helper, Optional<RecipeHolder<?>> holder, String name) {
        helper.assertTrue(holder.isPresent() && holder.get().value() instanceof CraftingRecipe, "Recipe " + name + " exists");
        return (CraftingRecipe) holder.get().value();
    }

    /** GHAST-29: the dried ghast and happy ghast loot tables have their random sequences. */
    @GameTest(template = "ghast_small")
    public static void ghast_lootRandomSequences(GameTestHelper helper) throws ReflectiveOperationException {
        Field field = LootTable.class.getDeclaredField("randomSequence");
        field.setAccessible(true);
        var registries = helper.getLevel().getServer().reloadableRegistries();
        LootTable block = registries.getLootTable(ModBlocks.DRIED_GHAST.get().getLootTable());
        LootTable entity = registries.getLootTable(ModEntityTypes.HAPPY_GHAST.get().getDefaultLootTable());
        helper.assertTrue(Optional.of(ResourceLocation.withDefaultNamespace("blocks/dried_ghast")).equals(field.get(block)), "Dried ghast table sequence, saw " + field.get(block));
        helper.assertTrue(Optional.of(ResourceLocation.withDefaultNamespace("entities/happy_ghast")).equals(field.get(entity)), "Happy ghast table sequence, saw " + field.get(entity));
        helper.succeed();
    }

    /** GHAST-30: in the piglin barter pool the dried ghast comes right after the ender pearl. */
    @GameTest(template = "ghast_small")
    public static void ghast_barterOrder(GameTestHelper helper) {
        LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.PIGLIN_BARTERING);
        List<LootPoolEntryContainer> entries = ((LootPoolAccess) ((LootTableAccessor) table).getPools().get(0)).platform$getEntries();
        int pearl = -1;
        int ghast = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i) instanceof LootItem item) {
                Item it = ((LootItemAccessor) item).vbp$getItem().value();
                if (it == Items.ENDER_PEARL) {
                    pearl = i;
                } else if (it == ModBlocks.DRIED_GHAST.get().asItem()) {
                    ghast = i;
                }
            }
        }

        helper.assertTrue(pearl == 6 && ghast == 7, "Ender pearl at 6 and dried ghast at 7, saw " + pearl + " and " + ghast + " of " + entries.size());
        helper.succeed();
    }

    /** GHAST-31: a nether fossil outside a nether biome still gets its dried ghast roll. */
    @GameTest(template = "ghast_big", batch = "ghast_fossil")
    public static void ghast_fossilDriedGhastAnyBiome(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(4, 1, 4));
        helper.assertFalse(level.getBiome(origin).is(net.minecraft.tags.BiomeTags.IS_NETHER), "The test area is not a nether biome");
        BoundingBox area = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(23, 23, 23)));
        int placedRolls = 0;
        for (int attempt = 0; attempt < 40 && placedRolls < 3; attempt++) {
            clear(level, area);
            BlockPos at = origin.offset(attempt % 8, 0, attempt / 8);
            NetherFossilPieces.NetherFossilPiece piece = new NetherFossilPieces.NetherFossilPiece(level.getStructureManager(),
                ResourceLocation.withDefaultNamespace("nether_fossils/fossil_" + (1 + attempt % 4)), at, Rotation.NONE);
            BoundingBox fossil = piece.template().getBoundingBox(piece.placeSettings(), piece.templatePosition());
            RandomSource random = RandomSource.create(level.getSeed()).forkPositional().at(fossil.getCenter());
            if (!(random.nextFloat() < 0.5F)) {
                continue;
            }

            BlockPos expected = new BlockPos(fossil.minX() + random.nextInt(fossil.getXSpan()), fossil.minY(), fossil.minZ() + random.nextInt(fossil.getZSpan()));
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), copy(area), new ChunkPos(at), at);
            BlockState there = level.getBlockState(expected);
            helper.assertFalse(there.isAir(), "Attempt " + attempt + ": the rolled dried ghast spot " + expected + " was left empty");
            if (there.is(ModBlocks.DRIED_GHAST.get())) {
                placedRolls++;
            }
        }

        clear(level, area);
        helper.assertTrue(placedRolls > 0, "At least one fossil outside the nether placed its dried ghast");
        helper.succeed();
    }

    private static BoundingBox copy(BoundingBox box) {
        return new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    private static void clear(ServerLevel level, BoundingBox box) {
        BlockPos.betweenClosedStream(box).forEach(pos -> {
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        });
    }
}
