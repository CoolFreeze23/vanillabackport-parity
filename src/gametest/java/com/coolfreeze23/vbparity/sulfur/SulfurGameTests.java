package com.coolfreeze23.vbparity.sulfur;

import com.blackgear.vanillabackport.client.registries.ModSoundTypes;
import com.blackgear.vanillabackport.common.level.block_entities.PotentSulfurBlockEntity;
import com.blackgear.vanillabackport.common.level.blocks.PotentSulfurBlock;
import com.blackgear.vanillabackport.common.level.blocks.PotentSulfurState;
import com.blackgear.vanillabackport.common.level.blocks.SpeleothemBlock;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCube;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCubeArchetype;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCubeArchetypes;
import com.blackgear.vanillabackport.common.level.worldgen.features.SulfurRootSystemConfiguration;
import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.blackgear.vanillabackport.common.registries.entities.ModAttributes;
import com.blackgear.vanillabackport.common.registries.entities.ModDamageTypes;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.blackgear.vanillabackport.common.registries.worldgen.ModBiomes;
import com.blackgear.vanillabackport.common.worldgen.surface.ModSurfaceRuleData;
import com.blackgear.vanillabackport.core.VanillaBackport;
import com.blackgear.vanillabackport.core.data.tags.ModDamageTypeTags;
import com.coolfreeze23.vbparity.VBParity;
import com.coolfreeze23.vbparity.sulfur.mixin.SequenceRuleSourceAccessor;
import com.coolfreeze23.vbparity.sulfur.mixin.SpeleothemBlockInvoker;
import com.coolfreeze23.vbparity.sulfur.mixin.SulfurCubeInvoker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(VBParity.MOD_ID)
@PrefixGameTestTemplate(false)
public class SulfurGameTests {
    private static final String BOX = "sulfur_box";
    private static final String LAKE = "sulfur_lake";
    // The templates' stone floor ends up at relative y = 1; everything above it is air.

    private static Block block(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(path));
    }

    private static SulfurCube cube(GameTestHelper helper, Vec3 pos, int size) {
        SulfurCube cube = helper.spawn(ModEntityTypes.SULFUR_CUBE.get(), pos);
        cube.setNoAi(true);
        cube.setSize(size, true);
        return cube;
    }

    private static Holder<DamageType> damageType(GameTestHelper helper, ResourceKey<DamageType> key) {
        return helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key);
    }

    private static void near(GameTestHelper helper, double expected, double actual, double tolerance, String what) {
        helper.assertTrue(Math.abs(expected - actual) <= tolerance, what + ": expected " + expected + " but was " + actual);
    }

    // ---------------------------------------------------------------- blocks: SULFUR-19, 20, 30 (+ COPPER-14)

    @GameTest(template = BOX)
    public static void sulfur_block_sound_types(GameTestHelper helper) {
        String[] sulfurFamily = {
            "sulfur", "sulfur_slab", "sulfur_stairs", "sulfur_wall", "polished_sulfur", "polished_sulfur_slab", "polished_sulfur_stairs",
            "polished_sulfur_wall", "sulfur_bricks", "sulfur_brick_slab", "sulfur_brick_stairs", "sulfur_brick_wall", "chiseled_sulfur"
        };
        for (String id : sulfurFamily) {
            helper.assertTrue(block(id).defaultBlockState().getSoundType() == ModSoundTypes.SULFUR, id + " should use the sulfur sound type");
        }
        helper.assertTrue(ModBlocks.POTENT_SULFUR.get().defaultBlockState().getSoundType() == ModSoundTypes.POTENT_SULFUR, "potent_sulfur sound type");
        String[] cinnabarFamily = {"cinnabar", "cinnabar_slab", "cinnabar_stairs", "polished_cinnabar", "cinnabar_bricks", "chiseled_cinnabar"};
        for (String id : cinnabarFamily) {
            helper.assertTrue(block(id).defaultBlockState().getSoundType() == ModSoundTypes.CINNABAR, id + " should use the cinnabar sound type");
        }
        helper.assertTrue(ModBlocks.SULFUR_SPIKE.get().defaultBlockState().getSoundType() == SulfurSounds.SULFUR_SPIKE, "sulfur_spike sound type");
        helper.assertTrue(
            BuiltInRegistries.SOUND_EVENT.getKey(SulfurSounds.SULFUR_SPIKE_BREAK).equals(ResourceLocation.withDefaultNamespace("block.sulfur_spike.break")),
            "block.sulfur_spike.break registered"
        );
        helper.succeed();
    }

    @GameTest(template = BOX)
    public static void sulfur_map_colours(GameTestHelper helper) {
        helper.assertTrue(ModBlocks.POTENT_SULFUR.get().defaultMapColor() == MapColor.GOLD, "potent_sulfur map colour should be GOLD");
        helper.assertTrue(ModBlocks.SULFUR.get().defaultMapColor() == MapColor.COLOR_YELLOW, "sulfur map colour should be COLOR_YELLOW");
        helper.assertTrue(block("polished_sulfur_wall").defaultMapColor() == MapColor.COLOR_YELLOW, "polished_sulfur_wall map colour");
        helper.assertTrue(ModBlocks.CINNABAR.get().defaultMapColor() == MapColor.COLOR_RED, "cinnabar map colour unchanged");
        helper.assertTrue(block("pale_oak_planks").defaultMapColor() == MapColor.QUARTZ, "pale_oak_planks map colour unchanged");
        for (String id : List.of("copper_chest", "exposed_copper_chest", "weathered_copper_chest", "oxidized_copper_chest")) {
            Block chest = block(id);
            helper.assertTrue(chest.defaultMapColor() == chest.defaultBlockState().getMapColor(helper.getLevel(), BlockPos.ZERO),
                id + " keeps its own properties (no later shared-builder changes leak in)");
        }
        helper.assertTrue(block("exposed_copper_chest").defaultBlockState().getMapColor(helper.getLevel(), BlockPos.ZERO) == MapColor.TERRACOTTA_LIGHT_GRAY, "exposed_copper_chest map colour unchanged");

        // COPPER-14: native shelf map colours (Blocks.java 1071-1190), poplar from 26.3 (POPLAR_PLANKS = COLOR_LIGHT_GRAY).
        Map<String, MapColor> shelves = Map.ofEntries(
            Map.entry("acacia_shelf", MapColor.COLOR_ORANGE),
            Map.entry("bamboo_shelf", MapColor.COLOR_YELLOW),
            Map.entry("birch_shelf", MapColor.SAND),
            Map.entry("cherry_shelf", MapColor.TERRACOTTA_WHITE),
            Map.entry("crimson_shelf", MapColor.CRIMSON_STEM),
            Map.entry("dark_oak_shelf", MapColor.COLOR_BROWN),
            Map.entry("jungle_shelf", MapColor.DIRT),
            Map.entry("mangrove_shelf", MapColor.COLOR_RED),
            Map.entry("oak_shelf", MapColor.WOOD),
            Map.entry("pale_oak_shelf", MapColor.QUARTZ),
            Map.entry("spruce_shelf", MapColor.PODZOL),
            Map.entry("warped_shelf", MapColor.WARPED_STEM),
            Map.entry("poplar_shelf", MapColor.COLOR_LIGHT_GRAY)
        );
        shelves.forEach((id, colour) -> {
            BlockState state = block(id).defaultBlockState();
            helper.assertTrue(state.getMapColor(helper.getLevel(), BlockPos.ZERO) == colour, id + " map colour should be " + colour.id + " but is " + state.getMapColor(helper.getLevel(), BlockPos.ZERO).id);
            helper.assertTrue(state.ignitedByLava(), id + " should be ignited by lava (native)");
            helper.assertTrue(state.getSoundType() == block("oak_shelf").defaultBlockState().getSoundType(), id + " keeps the shelf sound type");
        });
        helper.succeed();
    }

    // ---------------------------------------------------------------- sulfur cube: SULFUR-01..08

    @GameTest(template = BOX, timeoutTicks = 100)
    public static void sulfur_split_inherits(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SulfurCube parent = cube(helper, new Vec3(4.0, 2.0, 4.0), 2);
        parent.setNoGravity(true);
        parent.setSilent(true);
        parent.addTag("vbp_split_tag");
        parent.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2400, 1));
        String teamName = "vbp_split_" + Integer.toHexString(parent.getId());
        PlayerTeam team = level.getScoreboard().addPlayerTeam(teamName);
        level.getScoreboard().addPlayerToTeam(parent.getScoreboardName(), team);
        float offset = parent.getDimensions(parent.getPose()).width() / 2.0F;
        double parentZ = parent.getZ();
        parent.kill();

        helper.succeedWhen(() -> {
            List<SulfurCube> children = level.getEntitiesOfClass(
                SulfurCube.class, new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 12, 8), c -> c != parent && c.isAlive()
            );
            helper.assertTrue(children.size() == 2, "expected 2 split children, found " + children.size());
            for (SulfurCube child : children) {
                helper.assertTrue(child.isBaby() && child.getSize() == 1, "child should be a size-1 baby");
                helper.assertTrue(child.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "child should inherit slowness");
                helper.assertTrue(child.getTags().contains("vbp_split_tag"), "child should inherit command tags");
                helper.assertTrue(child.isSilent(), "child should inherit silence");
                helper.assertTrue(child.isNoGravity(), "child should inherit no-gravity");
                helper.assertTrue(child.getTeam() == team, "child should join the parent's team");
                // SULFUR-02: native z offset is (i / 2 - 0.5) * offset = -0.5 * offset for both children.
                near(helper, parentZ - 0.5 * offset, child.getZ(), 0.05, "child z");
            }
            near(helper, 0.0, children.get(0).getZ() - children.get(1).getZ(), 0.05, "both children in one z row");
            level.getScoreboard().removePlayerTeam(team);
        });
    }

    @GameTest(template = BOX)
    public static void sulfur_baby_spawn_size(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AgeableMob.AgeableMobGroupData group = new AgeableMob.AgeableMobGroupData(1.0F);
        SulfurCube first = cube(helper, new Vec3(2.5, 2.0, 2.5), 2);
        first.finalizeSpawn(level, level.getCurrentDifficultyAt(first.blockPosition()), MobSpawnType.NATURAL, group);
        SulfurCube second = cube(helper, new Vec3(5.5, 2.0, 5.5), 2);
        second.finalizeSpawn(level, level.getCurrentDifficultyAt(second.blockPosition()), MobSpawnType.NATURAL, group);
        helper.assertTrue(!first.isBaby() && first.getSize() == 2, "first of the group is an adult of size 2");
        helper.assertTrue(second.isBaby(), "second of the group is made a baby (chance 1.0)");
        helper.assertTrue(second.getSize() == 1, "a baby spawns with size 1, was " + second.getSize());
        near(helper, 4.0, second.getMaxHealth(), 1.0E-6, "baby max health");
        helper.succeed();
    }

    @GameTest(template = BOX, timeoutTicks = 60)
    public static void sulfur_extra_knockback_scaled(GameTestHelper helper) {
        SulfurCube cube = cube(helper, new Vec3(4.5, 2.0, 4.5), 2);
        cube.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.DIRT));
        Zombie attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(2.5, 2.0, 4.5));
        attacker.setYRot(-90.0F);
        attacker.setYHeadRot(-90.0F);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(cube.hasBodyItem(), "cube holds a block");
            DamageSource source = helper.getLevel().damageSources().mobAttack(attacker);
            cube.setDeltaMovement(Vec3.ZERO);
            cube.hurt(source, 4.0F);
            Vec3 first = cube.getDeltaMovement();
            double dx = attacker.getX() - cube.getX();
            double dz = attacker.getZ() - cube.getZ();
            cube.setDeltaMovement(Vec3.ZERO);
            // Second knockback of the same hit, as Mob#doHurtTarget / Player#attack do for the Knockback enchantment or sprinting
            // (native causeExtraKnockback(..., comesFromEffect = true)).
            com.coolfreeze23.vbparity.sulfur.KnockbackEffect.run(true, () -> cube.knockback(0.5, dx, dz));
            Vec3 second = cube.getDeltaMovement();
            helper.assertTrue(first.length() > 1.0E-4, "the hit itself knocks the cube back");
            near(helper, 0.125, second.length() / first.length(), 1.0E-3, "extra knockback / hit knockback ratio");
            helper.succeed();
        });
    }

    @GameTest(template = BOX, timeoutTicks = 60)
    public static void sulfur_drop_at_attachment(GameTestHelper helper) {
        SulfurCube cube = cube(helper, new Vec3(4.5, 2.0, 4.5), 2);
        cube.setNoGravity(true);
        cube.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.DIRT));
        helper.runAfterDelay(3, () -> {
            boolean swapped = ((SulfurCubeInvoker) cube).vbp$equipItem(new ItemStack(Items.COBBLESTONE));
            helper.assertTrue(swapped, "the cube swaps its block");
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, cube.getBoundingBox().inflate(3), e -> e.getItem().is(Items.DIRT));
            helper.assertTrue(drops.size() == 1, "one ejected dirt item");
            near(helper, cube.getY() + cube.getBbHeight(), drops.get(0).getY(), 1.0E-6, "ejected item height");
            drops.get(0).discard();
            helper.succeed();
        });
    }

    @GameTest(template = BOX)
    public static void sulfur_named_not_persistent(GameTestHelper helper) {
        SulfurCube cube = ModEntityTypes.SULFUR_CUBE.get().create(helper.getLevel());
        helper.assertTrue(cube != null, "created");
        cube.setCustomName(Component.literal("Cubey"));
        helper.assertTrue(!cube.isPersistenceRequired(), "a custom name alone does not make the cube persistent");
        cube.setPersistenceRequired();
        helper.assertTrue(cube.isPersistenceRequired(), "explicit persistence still works");
        helper.succeed();
    }

    // ---------------------------------------------------------------- SULFUR-09 dispensers

    private static SulfurCube dispenserSetup(GameTestHelper helper, ItemStack load) {
        BlockPos dispenser = new BlockPos(2, 2, 4);
        helper.setBlock(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity be = helper.getBlockEntity(dispenser);
        be.setItem(0, load);
        SulfurCube cube = cube(helper, new Vec3(3.5, 2.0, 4.5), 2);
        helper.runAfterDelay(3, () -> helper.setBlock(new BlockPos(1, 2, 4), Blocks.REDSTONE_BLOCK));
        return cube;
    }

    @GameTest(template = BOX, timeoutTicks = 60)
    public static void sulfur_dispenser_feeds_cube(GameTestHelper helper) {
        // The cube already holds dirt, so it cannot pick a dropped item up by itself: only native feeding swaps the block.
        SulfurCube cube = dispenserSetup(helper, new ItemStack(Items.COARSE_DIRT, 2));
        cube.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.DIRT));
        helper.succeedWhen(() -> {
            helper.assertTrue(cube.getContainedBlock().is(Items.COARSE_DIRT), "the dispenser fed coarse dirt into the cube");
            DispenserBlockEntity be = helper.getBlockEntity(new BlockPos(2, 2, 4));
            helper.assertTrue(be.getItem(0).getCount() == 1, "one coarse dirt was used");
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, cube.getBoundingBox().inflate(3)).forEach(ItemEntity::discard);
        });
    }

    @GameTest(template = BOX, timeoutTicks = 60)
    public static void sulfur_dispenser_tnt_swallowed(GameTestHelper helper) {
        SulfurCube cube = dispenserSetup(helper, new ItemStack(Items.TNT));
        helper.succeedWhen(() -> {
            helper.assertTrue(cube.getContainedBlock().is(Items.TNT), "the cube swallowed the TNT");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(PrimedTnt.class, cube.getBoundingBox().inflate(4)).isEmpty(), "no primed TNT");
            cube.discard();
        });
    }

    // ---------------------------------------------------------------- SULFUR-10, 11, 12 tags

    @GameTest(template = BOX, timeoutTicks = 40)
    public static void sulfur_damage_and_entity_tags(GameTestHelper helper) {
        for (ResourceKey<DamageType> key : List.of(DamageTypes.PLAYER_ATTACK, DamageTypes.SPIT, DamageTypes.FIREWORKS, DamageTypes.BAD_RESPAWN_POINT, DamageTypes.EXPLOSION)) {
            helper.assertTrue(damageType(helper, key).is(ModDamageTypeTags.SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO), key.location() + " in sulfur_cube_with_block_immune_to");
        }
        Holder<DamageType> hot = damageType(helper, ModDamageTypes.SULFUR_CUBE_HOT);
        helper.assertTrue(hot.is(DamageTypeTags.BURN_FROM_STEPPING), "sulfur_cube_hot in burn_from_stepping");
        helper.assertTrue(hot.is(DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES), "sulfur_cube_hot in panic_environmental_causes");
        helper.assertTrue(ModEntityTypes.SULFUR_CUBE.get().is(EntityTypeTags.NON_CONTROLLING_RIDER), "sulfur_cube in non_controlling_rider");

        SulfurCube rider = cube(helper, new Vec3(4.5, 2.0, 4.5), 1);
        helper.assertTrue(!rider.canControlVehicle(), "a sulfur cube cannot control a vehicle");

        // Frost Walker grants immunity to burn_from_stepping damage, so also to the hot cube's contact damage.
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(2.5, 2.0, 2.5));
        ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
        boots.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FROST_WALKER), 1);
        zombie.setItemSlot(EquipmentSlot.FEET, boots);
        helper.assertTrue(zombie.isInvulnerableTo(new DamageSource(hot)), "Frost Walker makes the wearer immune to sulfur_cube_hot");
        helper.succeed();
    }

    // ---------------------------------------------------------------- SULFUR-13 bounce

    private static void bounceTest(GameTestHelper helper, double floorTop, double blockBounciness) {
        SulfurCube cube = cube(helper, new Vec3(4.5, floorTop + 0.5, 4.5), 2);
        cube.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.DIRT));
        helper.runAfterDelay(3, () -> {
            double attribute = cube.getAttributeValue(ModAttributes.BOUNCINESS);
            helper.assertTrue(attribute > 0.0 && attribute < blockBounciness, "regular archetype bounciness " + attribute);
            Vec3 start = helper.absoluteVec(new Vec3(4.5, floorTop + 0.1, 4.5));
            cube.setPos(start.x, start.y, start.z);
            cube.setOnGround(false);
            cube.setDeltaMovement(0.0, -0.5, 0.0);
            cube.move(MoverType.SELF, cube.getDeltaMovement());
            double gravity = cube.getGravity();
            double portion = 0.1 / 0.5;
            double airDrag = Mth.clamp(1.0F - (1.0F - 0.91F) * (float) cube.getAttributeValue(ModAttributes.AIR_DRAG_MODIFIER), 0.0F, 1.0F);
            double expected = (portion * gravity + 0.5) * Mth.lerp(portion, 1.0, airDrag) * Math.max(attribute, blockBounciness);
            near(helper, expected, cube.getDeltaMovement().y, 1.0E-4, "upward speed after landing");
            helper.succeed();
        });
    }

    @GameTest(template = BOX, timeoutTicks = 60)
    public static void sulfur_bounce_on_slime(GameTestHelper helper) {
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.SLIME_BLOCK);
            }
        }
        bounceTest(helper, 2.0, 1.0);
    }

    @GameTest(template = BOX, timeoutTicks = 60)
    public static void sulfur_bounce_on_bed(GameTestHelper helper) {
        BlockState foot = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT);
        helper.setBlock(new BlockPos(4, 1, 4), foot);
        helper.setBlock(new BlockPos(4, 1, 3), foot.setValue(BedBlock.PART, BedPart.HEAD));
        bounceTest(helper, 1.5625, 0.75);
    }

    // ---------------------------------------------------------------- SULFUR-16

    @GameTest(template = BOX)
    public static void sulfur_archetype_modifier_ids(GameTestHelper helper) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (SulfurCubeArchetype archetype : SulfurCubeArchetypes.REGISTRIES.values()) {
            for (SulfurCubeArchetype.AttributeEntry entry : archetype.attributeModifiers()) {
                AttributeModifier modifier = entry.modifier();
                ids.add(modifier.id());
                helper.assertTrue(!modifier.id().getPath().contains("minecraft_") && !modifier.id().getPath().contains("generic."), "native modifier id: " + modifier.id());
            }
        }
        helper.assertTrue(ids.contains(ResourceLocation.withDefaultNamespace("regular_add_knockback_resistance")), "regular_add_knockback_resistance present in " + ids);
        helper.assertTrue(ids.contains(ResourceLocation.withDefaultNamespace("regular_mul_air_drag_modifier")), "regular_mul_air_drag_modifier present");
        helper.succeed();
    }

    // ---------------------------------------------------------------- speleothems: SULFUR-21..24

    private static BlockState spike(Direction tip, DripstoneThickness thickness) {
        return ModBlocks.SULFUR_SPIKE.get().defaultBlockState().setValue(SpeleothemBlock.TIP_DIRECTION, tip).setValue(SpeleothemBlock.THICKNESS, thickness);
    }

    private static void place(GameTestHelper helper, BlockPos relative, BlockState state) {
        helper.getLevel().setBlock(helper.absolutePos(relative), state, 2);
    }

    @GameTest(template = BOX)
    public static void sulfur_spike_max_length(GameTestHelper helper) {
        place(helper, new BlockPos(3, 9, 3), ModBlocks.SULFUR.get().defaultBlockState());
        place(helper, new BlockPos(3, 8, 3), spike(Direction.DOWN, DripstoneThickness.BASE));
        place(helper, new BlockPos(3, 7, 3), spike(Direction.DOWN, DripstoneThickness.FRUSTUM));
        place(helper, new BlockPos(3, 6, 3), spike(Direction.DOWN, DripstoneThickness.TIP));
        BlockPos start = helper.absolutePos(new BlockPos(3, 8, 3));
        SpeleothemBlock block = (SpeleothemBlock) ModBlocks.SULFUR_SPIKE.get();
        RandomSource random = RandomSource.create(42L);
        for (int i = 0; i < 32; i++) {
            block.tryGrowStalactiteOrStalagmiteIfPossible(helper.getLevel().getBlockState(start), helper.getLevel(), start, random);
        }
        helper.assertTrue(!helper.getBlockState(new BlockPos(3, 5, 3)).is(ModBlocks.SULFUR_SPIKE.get()), "a 3-long sulfur stalactite does not grow (max growth length 2)");

        // Native SulfurSpikeBlock still grows a stalactite whose tip is within the 2-block search.
        place(helper, new BlockPos(5, 9, 5), ModBlocks.SULFUR.get().defaultBlockState());
        place(helper, new BlockPos(5, 8, 5), spike(Direction.DOWN, DripstoneThickness.TIP));
        BlockPos shortStart = helper.absolutePos(new BlockPos(5, 8, 5));
        for (int i = 0; i < 32 && !helper.getBlockState(new BlockPos(5, 7, 5)).is(ModBlocks.SULFUR_SPIKE.get()); i++) {
            block.tryGrowStalactiteOrStalagmiteIfPossible(helper.getLevel().getBlockState(shortStart), helper.getLevel(), shortStart, random);
        }
        helper.assertTrue(helper.getBlockState(new BlockPos(5, 7, 5)).is(ModBlocks.SULFUR_SPIKE.get()), "a 1-long sulfur stalactite still grows");
        helper.succeed();
    }

    private static String column(GameTestHelper helper, int x, int z) {
        StringBuilder out = new StringBuilder();
        for (int y = 0; y <= 9; y++) {
            out.append(y).append('=').append(helper.getBlockState(new BlockPos(x, y, z))).append(' ');
        }
        return out.toString();
    }

    @GameTest(template = BOX)
    public static void sulfur_stalagmite_floor_search(GameTestHelper helper) {
        place(helper, new BlockPos(3, 8, 3), ModBlocks.SULFUR.get().defaultBlockState());
        place(helper, new BlockPos(3, 7, 3), spike(Direction.DOWN, DripstoneThickness.TIP));
        SpeleothemBlockInvoker block = (SpeleothemBlockInvoker) ModBlocks.SULFUR_SPIKE.get();
        block.vbp$growStalagmiteBelow(helper.getLevel(), helper.absolutePos(new BlockPos(3, 7, 3)));
        BlockState grown = helper.getBlockState(new BlockPos(3, 2, 3));
        helper.assertTrue(
            grown.is(ModBlocks.SULFUR_SPIKE.get()) && grown.getValue(SpeleothemBlock.TIP_DIRECTION) == Direction.UP,
            "a stalagmite grows on the floor 5 blocks below the tip; column: " + column(helper, 3, 3)
        );
        helper.succeed();
    }

    @GameTest(template = BOX)
    public static void sulfur_spike_trident_gamerule(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(3, 1, 3);
        place(helper, rel, spike(Direction.UP, DripstoneThickness.TIP));
        BlockPos pos = helper.absolutePos(rel);
        ThrownTrident trident = new ThrownTrident(EntityType.TRIDENT, level);
        trident.setPos(Vec3.atCenterOf(pos).add(-1.0, 0.0, 0.0));
        trident.setDeltaMovement(1.0, 0.0, 0.0);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.WEST, pos, false);
        GameRules.BooleanValue rule = level.getGameRules().getRule(GameRules.RULE_PROJECTILESCANBREAKBLOCKS);
        boolean before = rule.get();
        try {
            rule.set(false, level.getServer());
            BlockState state = level.getBlockState(pos);
            state.onProjectileHit(level, state, hit, trident);
            helper.assertTrue(level.getBlockState(pos).is(ModBlocks.SULFUR_SPIKE.get()), "projectilesCanBreakBlocks=false: the spike survives a trident");
            rule.set(true, level.getServer());
            state.onProjectileHit(level, state, hit, trident);
            helper.assertTrue(!level.getBlockState(pos).is(ModBlocks.SULFUR_SPIKE.get()), "projectilesCanBreakBlocks=true: a fast trident breaks the spike");
        } finally {
            rule.set(before, level.getServer());
        }
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).forEach(ItemEntity::discard);
        helper.succeed();
    }

    @GameTest(template = BOX)
    public static void sulfur_spike_tip_merge(GameTestHelper helper) {
        place(helper, new BlockPos(3, 3, 3), Blocks.STONE.defaultBlockState());
        place(helper, new BlockPos(3, 2, 3), Blocks.POINTED_DRIPSTONE.defaultBlockState()
            .setValue(SpeleothemBlock.TIP_DIRECTION, Direction.DOWN).setValue(SpeleothemBlock.THICKNESS, DripstoneThickness.TIP));
        SpeleothemBlockInvoker block = (SpeleothemBlockInvoker) ModBlocks.SULFUR_SPIKE.get();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        helper.assertTrue(block.vbp$calculateSpeleothemThickness(helper.getLevel(), pos, Direction.UP, true) == DripstoneThickness.TIP,
            "a sulfur spike does not merge with an opposing pointed dripstone tip");

        place(helper, new BlockPos(5, 3, 5), Blocks.STONE.defaultBlockState());
        place(helper, new BlockPos(5, 2, 5), spike(Direction.DOWN, DripstoneThickness.TIP));
        BlockPos same = helper.absolutePos(new BlockPos(5, 1, 5));
        helper.assertTrue(block.vbp$calculateSpeleothemThickness(helper.getLevel(), same, Direction.UP, true) == DripstoneThickness.TIP_MERGE,
            "two sulfur spike tips still merge");
        helper.succeed();
    }

    // ---------------------------------------------------------------- geysers: SULFUR-25, 26

    @GameTest(template = BOX)
    public static void sulfur_geyser_boat_rider(GameTestHelper helper) {
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

        Boat boat = helper.spawn(EntityType.BOAT, new Vec3(3.5, 3.6, 3.5));
        boat.setNoGravity(true);
        Zombie rider = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.5, 3.6, 3.5));
        helper.assertTrue(rider.startRiding(boat, true), "zombie rides the boat");
        boat.setDeltaMovement(Vec3.ZERO);
        rider.setDeltaMovement(Vec3.ZERO);

        PotentSulfurBlockEntity.SERVER_LAUNCH_ENTITY_TICKER.tick(level, sulfurPos, level.getBlockState(sulfurPos), be);
        near(helper, 0.2F, boat.getDeltaMovement().y, 1.0E-6, "the boat is launched once");
        near(helper, 0.0, rider.getDeltaMovement().y, 1.0E-9, "the rider itself is not launched");
        helper.assertTrue(boat.hasImpulse && !boat.hurtMarked, "normal impulse sync instead of a forced motion packet");
        rider.stopRiding();
        rider.discard();
        boat.discard();
        helper.succeed();
    }

    @GameTest(template = BOX)
    public static void sulfur_noxious_gas_glass(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.GLASS);
            }
        }
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.WATER);
        helper.setBlock(new BlockPos(4, 2, 2), Blocks.WATER);
        BlockPos source = helper.absolutePos(new BlockPos(2, 3, 2));
        Vec3 eye = helper.absoluteVec(new Vec3(4.5, 3.5, 2.5));
        helper.assertTrue(!PotentSulfurBlockEntity.canBeReachedByNoxiousGas(level, source, eye), "glass between the two water cells blocks the gas");
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.WATER);
        helper.assertTrue(PotentSulfurBlockEntity.canBeReachedByNoxiousGas(level, source, eye), "open water lets the gas through");
        helper.succeed();
    }

    // ---------------------------------------------------------------- worldgen: SULFUR-38..45

    @GameTest(template = LAKE)
    public static void sulfur_pool_avoids_spikes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState spike = spike(Direction.UP, DripstoneThickness.TIP);
        for (int x = 1; x <= 18; x++) {
            for (int y = 1; y <= 10; y++) {
                for (int z = 1; z <= 18; z++) {
                    place(helper, new BlockPos(x, y, z), spike);
                }
            }
        }
        LakeFeature.Configuration pool = new LakeFeature.Configuration(
            BlockStateProvider.simple(Blocks.WATER.defaultBlockState()), BlockStateProvider.simple(ModBlocks.SULFUR.get().defaultBlockState())
        );
        boolean placed = Feature.LAKE.place(pool, level, level.getChunkSource().getGenerator(), RandomSource.create(7L), helper.absolutePos(new BlockPos(10, 6, 10)));
        helper.assertTrue(!placed, "a sulfur pool is not placed where its shell holds sulfur spikes");
        helper.assertTrue(helper.getBlockState(new BlockPos(10, 3, 10)).is(ModBlocks.SULFUR_SPIKE.get()), "spikes untouched");
        helper.succeed();
    }

    @GameTest(template = BOX)
    public static void sulfur_air_predicates(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ConfiguredFeature<?, ?> spring = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
            .get(ResourceLocation.withDefaultNamespace("rooted_sulfur_spring"));
        helper.assertTrue(spring != null && spring.config() instanceof SulfurRootSystemConfiguration, "rooted_sulfur_spring configured feature");
        SulfurRootSystemConfiguration config = (SulfurRootSystemConfiguration) spring.config();
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.CAVE_AIR);
        helper.assertTrue(config.allowedTreePosition().test(level, helper.absolutePos(new BlockPos(3, 2, 3))), "cave_air is an allowed tree position (#air)");
        helper.succeed();
    }

    @GameTest(template = BOX)
    public static void sulfur_caves_biome(GameTestHelper helper) {
        Biome biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).get(ModBiomes.SULFUR_CAVES);
        helper.assertTrue(biome != null, "sulfur_caves biome");
        helper.assertTrue(biome.getFogColor() == 9222193, "fog colour #8CB831, was " + Integer.toHexString(biome.getFogColor()));
        helper.assertTrue(biome.getAmbientMood().isPresent(), "cave mood sound");
        helper.assertTrue(biome.getAmbientMood().get().getTickDelay() == 6000, "mood tick delay 6000");

        List<MobSpawnSettings.SpawnerData> monsters = biome.getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap();
        for (MobSpawnSettings.SpawnerData data : monsters) {
            if (data.type == EntityType.ZOMBIE_VILLAGER) {
                helper.assertTrue(data.getWeight().asInt() == 5, "zombie_villager weight 5");
            }
        }
        if (VanillaBackport.COMMON_CONFIG.hasSulfurCubes.get()) {
            helper.assertTrue(monsters.get(0).type == ModEntityTypes.SULFUR_CUBE.get(), "sulfur_cube is the first monster entry");
        }

        HolderSet<PlacedFeature> lakes = biome.getGenerationSettings().features().get(GenerationStep.Decoration.LAKES.ordinal());
        int spring = -1;
        int pool = -1;
        for (int i = 0; i < lakes.size(); i++) {
            if (lakes.get(i).is(ResourceLocation.withDefaultNamespace("rooted_sulfur_spring"))) {
                spring = i;
            } else if (lakes.get(i).is(ResourceLocation.withDefaultNamespace("sulfur_pool"))) {
                pool = i;
            }
        }
        helper.assertTrue(pool >= 0, "sulfur_pool in LAKES");
        if (VanillaBackport.COMMON_CONFIG.hasSulfurSprings.get()) {
            helper.assertTrue(spring >= 0 && spring < pool, "rooted_sulfur_spring comes before sulfur_pool (spring " + spring + ", pool " + pool + ")");
        }
        helper.succeed();
    }

    @GameTest(template = BOX)
    public static void sulfur_surface_rule_shape(GameTestHelper helper) {
        if (SulfurSurfaceRules.terraBlenderLoaded()) {
            helper.succeed();
            return;
        }
        NoiseGeneratorSettings overworld = helper.getLevel().registryAccess().registryOrThrow(Registries.NOISE_SETTINGS).get(NoiseGeneratorSettings.OVERWORLD);
        SurfaceRules.RuleSource merged = ModSurfaceRuleData.merge(overworld.surfaceRule(), List.of(ModSurfaceRuleData.makeRules()));
        helper.assertTrue(merged instanceof SequenceRuleSourceAccessor, "merged rule is a sequence");
        List<SurfaceRules.RuleSource> sequence = ((SequenceRuleSourceAccessor) merged).vbp$sequence();
        helper.assertTrue(sequence.size() == 6, "dappled, bedrock_floor, sulfur near surface, main, sulfur bands, deepslate; was " + sequence.size());
        String text = merged.toString();
        helper.assertTrue(!text.contains("randomName=minecraft:bedrock,"), "no extra 'bedrock' gradient");
        helper.assertTrue(sequence.get(1).toString().contains("minecraft:bedrock_floor"), "bedrock floor comes before the sulfur rules");
        helper.assertTrue(sequence.get(5).toString().contains("minecraft:deepslate"), "deepslate gradient stays last");
        helper.succeed();
    }
}
