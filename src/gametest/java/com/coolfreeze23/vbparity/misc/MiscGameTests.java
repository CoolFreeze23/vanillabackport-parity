package com.coolfreeze23.vbparity.misc;

import com.blackgear.vanillabackport.common.api.modules.waypoints.Waypoint;
import com.blackgear.vanillabackport.common.api.modules.waypoints.WaypointStyleAssets;
import com.blackgear.vanillabackport.common.api.modules.waypoints.WaypointTransmitter;
import com.blackgear.vanillabackport.common.commands.arguments.ColorArgument;
import com.blackgear.vanillabackport.common.level.entities.boat.CustomBoat;
import com.blackgear.vanillabackport.common.registries.entities.ModBoatTypes;
import com.blackgear.vanillabackport.common.registries.entities.ModAttributes;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BannerPatternTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * GameTests for the misc fixes. Template {@code vbparity:misc_platform}: 9x14x9 stone floor and air above. GameTestHelper
 * coordinates are relative to the structure block, one below the template, so the floor is at y=1 and things stand at y=2.
 */
@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public final class MiscGameTests {
    private static final String PLATFORM = "misc_platform";

    private MiscGameTests() {}

    private static int itemCount(ItemStack bundle) {
        BundleContents contents = bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return contents.itemCopyStream().mapToInt(ItemStack::getCount).sum();
    }

    private static ServerPlayer player(GameTestHelper helper, double x, double y, double z) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(x, y, z));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }

    private static void removePlayer(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    // MISC-01: native bundle drops 1 item on the first use tick, then 1 every 2 ticks from tick 11 on, for 200 ticks.
    @GameTest(template = PLATFORM)
    public static void misc_bundle_hold_to_empty(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, 4.5, 2, 4.5);
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        BundleContents.Mutable contents = new BundleContents.Mutable(BundleContents.EMPTY);
        for (Item item : List.of(Items.STONE, Items.DIRT, Items.SAND, Items.GRAVEL, Items.COBBLESTONE)) {
            contents.tryInsert(new ItemStack(item));
        }
        bundle.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
        player.setItemInHand(InteractionHand.MAIN_HAND, bundle);

        bundle.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.isUsingItem(), "using a bundle should start item use (hold to empty)");
        helper.assertTrue(bundle.getUseDuration(player) == 200, "bundle use duration should be 200, was " + bundle.getUseDuration(player));
        helper.assertTrue(itemCount(bundle) == 5, "use() itself should not drop anything");

        int left = 5;
        for (int remaining = 200; remaining >= 180; remaining--) {
            if (remaining == 200 || remaining < 190 && remaining % 2 == 0) {
                left = Math.max(0, left - 1);
            }
            bundle.onUseTick(level, player, remaining);
            int count = itemCount(bundle);
            helper.assertTrue(count == left, "after use tick with " + remaining + " ticks remaining expected " + left + " items, got " + count);
        }
        helper.assertTrue(itemCount(bundle) == 0, "bundle should be empty after 10 drops");
        removePlayer(helper, player);
        helper.succeed();
    }

    // MISC-02: a bundle + dye of its own colour is not a recipe (native crafting_transmute).
    @GameTest(template = PLATFORM)
    public static void misc_bundle_same_colour_dye_rejected(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CraftingInput same = CraftingInput.of(2, 1, List.of(new ItemStack(ModItems.RED_BUNDLE.get()), new ItemStack(Items.RED_DYE)));
        CraftingInput other = CraftingInput.of(2, 1, List.of(new ItemStack(ModItems.RED_BUNDLE.get()), new ItemStack(Items.BLUE_DYE)));
        helper.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, same, level).isEmpty(), "red bundle + red dye should not craft");
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, other, level);
        helper.assertTrue(recipe.isPresent(), "red bundle + blue dye should still craft");
        helper.assertTrue(recipe.get().value().assemble(other, level.registryAccess()).is(ModItems.BLUE_BUNDLE.get()), "result should be a blue bundle");
        helper.succeed();
    }

    // MISC-08: Field Masoned / Bordure Indented need their pattern items in the loom.
    @GameTest(template = PLATFORM)
    public static void misc_banner_pattern_items_and_loom(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Item fieldMasoned = BuiltInRegistries.ITEM.get(MiscContent.FIELD_MASONED_ID);
        Item bordure = BuiltInRegistries.ITEM.get(MiscContent.BORDURE_INDENTED_ID);
        helper.assertTrue(fieldMasoned instanceof BannerPatternItem, "minecraft:field_masoned_banner_pattern should be a banner pattern item");
        helper.assertTrue(bordure instanceof BannerPatternItem, "minecraft:bordure_indented_banner_pattern should be a banner pattern item");
        helper.assertTrue(new ItemStack(fieldMasoned).getMaxStackSize() == 1, "pattern items stack to 1");

        Holder<BannerPattern> bricks = level.registryAccess().registryOrThrow(Registries.BANNER_PATTERN).getHolderOrThrow(BannerPatterns.BRICKS);
        Holder<BannerPattern> curly = level.registryAccess().registryOrThrow(Registries.BANNER_PATTERN).getHolderOrThrow(BannerPatterns.CURLY_BORDER);
        helper.assertFalse(bricks.is(BannerPatternTags.NO_ITEM_REQUIRED), "bricks should not be in no_item_required");
        helper.assertFalse(curly.is(BannerPatternTags.NO_ITEM_REQUIRED), "curly_border should not be in no_item_required");
        helper.assertTrue(bricks.is(MiscContent.PATTERN_ITEM_FIELD_MASONED), "pattern_item/field_masoned should contain bricks");
        helper.assertTrue(curly.is(MiscContent.PATTERN_ITEM_BORDURE_INDENTED), "pattern_item/bordure_indented should contain curly_border");

        ServerPlayer player = player(helper, 4.5, 2, 4.5);
        LoomMenu loom = new LoomMenu(0, player.getInventory());
        loom.getSlot(0).set(new ItemStack(Items.WHITE_BANNER));
        loom.getSlot(1).set(new ItemStack(Items.RED_DYE));
        List<Holder<BannerPattern>> free = loom.getSelectablePatterns();
        helper.assertFalse(free.contains(bricks), "loom offers Field Masoned without the pattern item");
        helper.assertFalse(free.contains(curly), "loom offers Bordure Indented without the pattern item");
        loom.getSlot(2).set(new ItemStack(fieldMasoned));
        helper.assertTrue(loom.getSelectablePatterns().contains(bricks), "loom should offer Field Masoned with the pattern item");
        loom.getSlot(2).set(new ItemStack(bordure));
        helper.assertTrue(loom.getSelectablePatterns().contains(curly), "loom should offer Bordure Indented with the pattern item");
        removePlayer(helper, player);

        helper.assertTrue(level.getRecipeManager().byKey(MiscContent.FIELD_MASONED_ID).isPresent(), "field masoned recipe missing");
        helper.assertTrue(level.getRecipeManager().byKey(MiscContent.BORDURE_INDENTED_ID).isPresent(), "bordure indented recipe missing");
        CraftingInput paperBricks = CraftingInput.of(2, 1, List.of(new ItemStack(Items.PAPER), new ItemStack(Items.BRICKS)));
        Optional<RecipeHolder<CraftingRecipe>> made = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, paperBricks, level);
        helper.assertTrue(made.isPresent() && made.get().value().assemble(paperBricks, level.registryAccess()).is(fieldMasoned), "paper + bricks should make the Field Masoned pattern");
        helper.succeed();
    }

    // MISC-09: the netherite lodestone and slime-ball lead recipes are gone; the modern ones use the minecraft: ids.
    @GameTest(template = PLATFORM)
    public static void misc_old_recipes_removed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            ItemStack result = holder.value().getResultItem(level.registryAccess());
            List<Ingredient> ingredients = holder.value().getIngredients();
            if (result.is(Items.LODESTONE)) {
                helper.assertFalse(ingredients.stream().anyMatch(i -> i.test(new ItemStack(Items.NETHERITE_INGOT))),
                        "lodestone recipe " + holder.id() + " still uses a netherite ingot; dynamic=" + com.blackgear.platform.common.v2.recipe.DynamicRecipeManager.getRecipes().keySet().stream().filter(k -> k.getPath().contains("lodestone") || k.getPath().equals("lead")).toList()
                        + " strategy=" + com.blackgear.vanillabackport.core.VanillaBackport.COMMON_CONFIG.lodestoneRecipe.get()
                        + " packs=" + level.getServer().getResourceManager().getResourceStack(ResourceLocation.withDefaultNamespace("recipe/lodestone.json")).stream().map(r -> r.sourcePackId()).toList());
            }
            if (result.is(Items.LEAD)) {
                helper.assertFalse(ingredients.stream().anyMatch(i -> i.test(new ItemStack(Items.SLIME_BALL))),
                        "lead recipe " + holder.id() + " still uses a slime ball");
            }
        }
        for (String name : List.of("lodestone", "lead")) {
            helper.assertTrue(level.getRecipeManager().byKey(ResourceLocation.withDefaultNamespace(name)).isPresent(), "minecraft:" + name + " recipe missing");
            helper.assertFalse(level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("vanillabackport", name)).isPresent(),
                    "vanillabackport:" + name + " duplicate should not exist");
        }
        ItemStack lodestone = level.getRecipeManager().byKey(ResourceLocation.withDefaultNamespace("lodestone")).orElseThrow().value().getResultItem(level.registryAccess());
        helper.assertTrue(lodestone.is(Items.LODESTONE), "minecraft:lodestone should make a lodestone");
        helper.succeed();
    }

    // MISC-28: boats (vanilla and VB's pale oak) never break from a fall. 1.21.1 only breaks a boat that lands while its
    // status is already ON_LAND, so the boats start resting on the floor carrying a 10-block fall distance.
    @GameTest(template = PLATFORM)
    public static void misc_boat_fall_does_not_break(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 a = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        Vec3 b = helper.absoluteVec(new Vec3(6.5, 2, 6.5));
        Boat oak = new Boat(level, a.x, a.y, a.z);
        CustomBoat pale = new CustomBoat(level, b.x, b.y, b.z, ModBoatTypes.PALE_OAK);
        oak.fallDistance = 10.0F;
        pale.fallDistance = 10.0F;
        level.addFreshEntity(oak);
        level.addFreshEntity(pale);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(oak.isAlive(), "oak boat broke from its fall");
            helper.assertTrue(pale.isAlive(), "pale oak boat broke from its fall");
            helper.assertTrue(oak.fallDistance == 0.0F && pale.fallDistance == 0.0F, "landing should reset the fall distance");
            AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(9, 14, 9);
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, box).isEmpty(), "boats dropped planks/sticks");
            helper.succeed();
        });
    }

    // MISC-29: a renamed VB boat keeps its name when placed by hand or by a dispenser.
    @GameTest(template = PLATFORM)
    public static void misc_boat_custom_name_place_and_dispense(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Look at the floor three blocks east (a boat cannot be placed overlapping the player).
        ServerPlayer player = player(helper, 2.5, 2, 2.5);
        player.setYRot(-90.0F);
        player.setXRot((float) Math.toDegrees(Math.atan2(player.getEyeHeight(), 3.0)));
        ItemStack placed = new ItemStack(ModItems.PALE_OAK_BOAT.get());
        placed.set(DataComponents.CUSTOM_NAME, Component.literal("Placed"));
        player.setItemInHand(InteractionHand.MAIN_HAND, placed);
        var useResult = placed.getItem().use(level, player, InteractionHand.MAIN_HAND).getResult();

        BlockPos dispenserPos = new BlockPos(5, 2, 4);
        helper.setBlock(dispenserPos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(6, 2, 4), Blocks.WATER);
        BlockPos abs = helper.absolutePos(dispenserPos);
        BlockState state = level.getBlockState(abs);
        DispenseItemBehavior behavior = DispenserBlock.DISPENSER_REGISTRY.get(ModItems.PALE_OAK_BOAT.get());
        helper.assertTrue(behavior != null, "no dispenser behaviour for pale oak boats");
        ItemStack dispensed = new ItemStack(ModItems.PALE_OAK_BOAT.get());
        dispensed.set(DataComponents.CUSTOM_NAME, Component.literal("Dispensed"));
        behavior.dispense(new BlockSource(level, abs, state, (DispenserBlockEntity) level.getBlockEntity(abs)), dispensed);

        AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(9, 14, 9);
        List<CustomBoat> boats = level.getEntitiesOfClass(CustomBoat.class, box);
        helper.assertTrue(boats.size() == 2, "expected 2 pale oak boats, found " + boats.size() + ": " + boats.stream().map(b -> b.position() + " " + b.getCustomName()).toList()
                + " hit=" + player.pick(5.0, 1.0F, true).getType() + " eye=" + player.getEyePosition() + " rot=" + player.getXRot() + " use=" + useResult);
        removePlayer(helper, player);
        helper.assertTrue(boats.stream().anyMatch(b -> b.hasCustomName() && "Placed".equals(b.getCustomName().getString())), "hand-placed boat lost its name");
        helper.assertTrue(boats.stream().anyMatch(b -> b.hasCustomName() && "Dispensed".equals(b.getCustomName().getString())), "dispensed boat lost its name");
        helper.succeed();
    }

    // MISC-30: #cannot_be_pushed_onto_boats (creaking) is not picked up; a zombie still is.
    @GameTest(template = PLATFORM)
    public static void misc_boat_scoop_exclusions(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 a = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        Vec3 b = helper.absoluteVec(new Vec3(6.5, 2, 6.5));
        Boat boatA = new Boat(level, a.x, a.y, a.z);
        Boat boatB = new Boat(level, b.x, b.y, b.z);
        level.addFreshEntity(boatA);
        level.addFreshEntity(boatB);
        var creaking = helper.spawnWithNoFreeWill(ModEntityTypes.CREAKING.get(), new Vec3(2.5, 2, 2.5));
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(6.5, 2, 6.5));
        helper.assertTrue(creaking.getType().is(MiscContent.CANNOT_BE_PUSHED_ONTO_BOATS), "creaking should be in #cannot_be_pushed_onto_boats");
        helper.assertTrue(EntityType.ELDER_GUARDIAN.is(MiscContent.CANNOT_BE_PUSHED_ONTO_BOATS), "elder guardian should be in #cannot_be_pushed_onto_boats");
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(zombie.isPassenger(), "control: a zombie should be picked up by the boat");
            helper.assertFalse(creaking.isPassenger(), "a creaking was picked up by a boat");
            helper.succeed();
        });
    }

    // MISC-15: the owner lands where the pearl was at the start of the impact tick (native oldPosition), never inside
    // the block it hit; the pearl's portal cooldown is passed on.
    @GameTest(template = PLATFORM)
    public static void misc_ender_pearl_landing_position(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, 1.5, 2, 1.5);
        ThrownEnderpearl pearl = new ThrownEnderpearl(level, player);
        Vec3 start = helper.absoluteVec(new Vec3(4.5, 4.0, 4.5));
        pearl.setPos(start);
        pearl.setDeltaMovement(0.0, -0.7, 0.0);
        pearl.setPortalCooldown();
        level.addFreshEntity(pearl);
        double floorTop = helper.absoluteVec(new Vec3(0, 2, 0)).y;
        helper.succeedWhen(() -> {
            helper.assertTrue(pearl.isRemoved(), "pearl has not landed yet");
            Vec3 landed = player.position();
            // xo/yo/zo = where the pearl was at the start of its impact tick (native oldPosition()).
            Vec3 old = new Vec3(pearl.xo, pearl.yo, pearl.zo);
            helper.assertTrue(landed.distanceTo(old) < 1.0E-6, "player at " + landed + " but the pearl started its impact tick at " + old);
            helper.assertTrue(landed.y > floorTop + 0.01, "player landed at the impact face (y " + landed.y + ", floor " + floorTop + ")");
            helper.assertTrue(player.isOnPortalCooldown(), "pearl portal cooldown was not passed to the player");
            removePlayer(helper, player);
        });
    }

    // MISC-16b: a pearl whose owner is not an online player keeps flying (VB deleted it on its first tick).
    @GameTest(template = PLATFORM)
    public static void misc_ender_pearl_offline_owner_keeps_flying(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 2, 1);
        ThrownEnderpearl mobOwned = new ThrownEnderpearl(EntityType.ENDER_PEARL, level);
        mobOwned.setOwner(zombie);
        Vec3 a = helper.absoluteVec(new Vec3(3.5, 11.0, 3.5));
        mobOwned.setPos(a);
        level.addFreshEntity(mobOwned);

        ThrownEnderpearl offline = new ThrownEnderpearl(EntityType.ENDER_PEARL, level);
        Vec3 b = helper.absoluteVec(new Vec3(6.5, 11.0, 6.5));
        offline.setPos(b);
        CompoundTag tag = offline.saveWithoutId(new CompoundTag());
        tag.putUUID("Owner", UUID.randomUUID());
        offline.load(tag);
        offline.setPos(b);
        level.addFreshEntity(offline);

        helper.runAfterDelay(4, () -> {
            helper.assertFalse(mobOwned.isRemoved(), "pearl owned by a mob was deleted");
            helper.assertFalse(offline.isRemoved(), "pearl with an offline owner was deleted");
            helper.assertTrue(mobOwned.getY() < a.y && offline.getY() < b.y, "pearls should keep flying (falling)");
            helper.succeed();
        });
    }

    // MISC-16a: a dead owner's pearls stay while the owner is in the End credits (wonGame); otherwise they vanish.
    @GameTest(template = PLATFORM)
    public static void misc_ender_pearl_kept_during_credits(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, 1.5, 2, 1.5);
        ThrownEnderpearl pearl = new ThrownEnderpearl(EntityType.ENDER_PEARL, level);
        pearl.setOwner(player);
        pearl.setPos(helper.absoluteVec(new Vec3(4.5, 12.0, 4.5)));
        level.addFreshEntity(pearl);
        player.wonGame = true;
        player.setHealth(0.0F);
        helper.runAfterDelay(3, () -> {
            helper.assertFalse(pearl.isRemoved(), "pearl vanished while its owner was in the End credits");
            player.wonGame = false;
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(pearl.isRemoved(), "control: a dead owner's pearl should vanish");
            player.setHealth(20.0F);
            removePlayer(helper, player);
            helper.succeed();
        });
    }

    // MISC-10: waypoint style and colour survive respawn (ServerPlayer.restoreFrom).
    @GameTest(template = PLATFORM)
    public static void misc_waypoint_icon_survives_respawn(GameTestHelper helper) {
        ServerPlayer oldPlayer = player(helper, 2.5, 2, 2.5);
        ServerPlayer newPlayer = player(helper, 6.5, 2, 6.5);
        Waypoint.Icon icon = ((WaypointTransmitter) oldPlayer).waypointIcon();
        icon.style = WaypointStyleAssets.BOWTIE;
        icon.color = Optional.of(0x12AB34);
        newPlayer.restoreFrom(oldPlayer, false);
        Waypoint.Icon copied = ((WaypointTransmitter) newPlayer).waypointIcon();
        helper.assertTrue(copied.style.equals(WaypointStyleAssets.BOWTIE), "waypoint style lost on respawn: " + copied.style);
        helper.assertTrue(copied.color.equals(Optional.of(0x12AB34)), "waypoint colour lost on respawn: " + copied.color);
        removePlayer(helper, oldPlayer);
        removePlayer(helper, newPlayer);
        helper.succeed();
    }

    // MISC-31: monster spawning stays on in Peaceful; only types outside #monsters_that_spawn_on_peaceful are refused/despawned.
    @GameTest(template = PLATFORM)
    public static void misc_monsters_on_peaceful(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        helper.assertTrue(EntityType.PIGLIN.is(MiscContent.MONSTERS_THAT_SPAWN_ON_PEACEFUL), "piglin should be in the tag");
        helper.assertTrue(EntityType.ZOMBIE_HORSE.is(MiscContent.MONSTERS_THAT_SPAWN_ON_PEACEFUL), "zombie horse should be in the tag");
        helper.assertTrue(EntityType.SHULKER.is(MiscContent.MONSTERS_THAT_SPAWN_ON_PEACEFUL), "shulker should be in the tag");
        helper.assertFalse(EntityType.HOGLIN.is(MiscContent.MONSTERS_THAT_SPAWN_ON_PEACEFUL), "hoglin should not be in the tag");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2, 2, 2);
        Piglin piglin = helper.spawnWithNoFreeWill(EntityType.PIGLIN, 6, 2, 6);
        Difficulty previous = server.getWorldData().getDifficulty();
        try {
            server.setDifficulty(Difficulty.PEACEFUL, true);
            helper.assertTrue(server.isSpawningMonsters(), "monster spawning should stay on in Peaceful");
            helper.assertTrue(EntityType.ZOMBIE.create(level) == null, "zombies cannot be created in Peaceful");
            helper.assertTrue(EntityType.HOGLIN.create(level) == null, "hoglins cannot be created in Peaceful");
            helper.assertTrue(EntityType.PIGLIN.create(level) != null, "piglins can be created in Peaceful");
            helper.assertTrue(EntityType.SHULKER.create(level) != null, "shulkers can be created in Peaceful");
            zombie.checkDespawn();
            piglin.checkDespawn();
            helper.assertTrue(zombie.isRemoved(), "zombie should despawn in Peaceful");
            helper.assertFalse(piglin.isRemoved(), "piglin should stay in Peaceful");
        } finally {
            server.setDifficulty(previous, true);
        }
        helper.succeed();
    }

    // MISC-14: /waypoint ... color accepts the 16 colours only.
    @GameTest(template = PLATFORM)
    public static void misc_waypoint_color_argument_colours_only(GameTestHelper helper) {
        ColorArgument argument = ColorArgument.color();
        for (String bad : List.of("bold", "italic", "obfuscated", "reset")) {
            try {
                argument.parse(new StringReader(bad));
                helper.fail("'" + bad + "' should not parse as a team colour");
            } catch (CommandSyntaxException expected) {
            }
        }
        try {
            helper.assertTrue(argument.parse(new StringReader("red")).isColor(), "red should parse");
        } catch (CommandSyntaxException e) {
            helper.fail("red should parse: " + e.getMessage());
        }
        helper.succeed();
    }

    // MISC-13: the carved pumpkin and heads carry the native hide modifier, which works for any wearer.
    @GameTest(template = PLATFORM)
    public static void misc_head_items_hide_waypoint(GameTestHelper helper) {
        ItemAttributeModifiers modifiers = new ItemStack(Items.CARVED_PUMPKIN).getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        ResourceLocation hideId = ResourceLocation.withDefaultNamespace("waypoint_transmit_range_hide");
        helper.assertTrue(modifiers.modifiers().stream().anyMatch(e -> e.modifier().id().equals(hideId) && e.attribute().equals(ModAttributes.WAYPOINT_TRANSMIT_RANGE)),
                "carved pumpkin has no waypoint_transmit_range_hide modifier");
        for (Item head : List.of(Items.SKELETON_SKULL, Items.WITHER_SKELETON_SKULL, Items.PLAYER_HEAD, Items.ZOMBIE_HEAD,
                Items.CREEPER_HEAD, Items.DRAGON_HEAD, Items.PIGLIN_HEAD)) {
            helper.assertTrue(new ItemStack(head).getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                    .modifiers().stream().anyMatch(e -> e.modifier().id().equals(hideId)), head + " has no hide modifier");
        }
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 4, 2, 4);
        zombie.getAttribute(ModAttributes.WAYPOINT_TRANSMIT_RANGE).setBaseValue(100.0);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        helper.runAfterDelay(5, () -> {
            double range = zombie.getAttributeValue(ModAttributes.WAYPOINT_TRANSMIT_RANGE);
            helper.assertTrue(range == 0.0, "a mob wearing a carved pumpkin should not transmit (range " + range + ")");
            helper.succeed();
        });
    }

    // MISC-35: ruined portal lodestone pool lists the empty entry first (native entry order).
    @GameTest(template = PLATFORM)
    public static void misc_ruined_portal_lodestone_pool_order(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.RUINED_PORTAL);
        JsonElement json = LootTable.DIRECT_CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, level.registryAccess()), table).getOrThrow();
        boolean found = false;
        for (JsonElement pool : json.getAsJsonObject().getAsJsonArray("pools")) {
            var entries = pool.getAsJsonObject().getAsJsonArray("entries");
            boolean hasLodestone = false;
            for (JsonElement entry : entries) {
                JsonObject e = entry.getAsJsonObject();
                if (e.has("name") && e.get("name").getAsString().equals("minecraft:lodestone")) {
                    hasLodestone = true;
                }
            }
            if (hasLodestone && entries.size() == 2) {
                found = true;
                helper.assertTrue(entries.get(0).getAsJsonObject().get("type").getAsString().equals("minecraft:empty"),
                        "lodestone pool should list the empty entry first: " + pool);
            }
        }
        helper.assertTrue(found, "no lodestone pool in chests/ruined_portal");
        helper.succeed();
    }

    // MISC-32: the native lead / shears sound events exist.
    @GameTest(template = PLATFORM)
    public static void misc_lead_sound_events_registered(GameTestHelper helper) {
        for (ResourceLocation id : List.of(MiscContent.LEAD_TIED, MiscContent.LEAD_UNTIED, MiscContent.SHEARS_SNIP)) {
            helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(id), id + " is not registered");
        }
        helper.succeed();
    }

    // TPHYS-A04: VB's pale oak / poplar boats and chest boats have the native boat eye height 0.5625.
    @GameTest(template = PLATFORM)
    public static void misc_custom_boat_eye_height(GameTestHelper helper) {
        for (EntityType<?> type : List.<EntityType<?>>of(ModEntityTypes.CUSTOM_BOAT.get(), ModEntityTypes.CUSTOM_CHEST_BOAT.get())) {
            float eye = type.getDimensions().eyeHeight();
            helper.assertTrue(Math.abs(eye - 0.5625F) < 1.0E-6F, type + " eye height should be 0.5625, was " + eye);
            helper.assertTrue(Math.abs(type.getDimensions().height() - 0.5625F) < 1.0E-6F, type + " height changed");
        }
        helper.succeed();
    }

    private static final java.util.concurrent.atomic.AtomicInteger LOADS = new java.util.concurrent.atomic.AtomicInteger();
    private static volatile UUID loadWatch;
    private static boolean loadListener;

    // Job 3 (PlayerListMixin): a rejoining player's data is read once (VB read it twice) and the saved pearl comes back.
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void misc_ender_pearl_rejoin_loads_player_once(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        if (!loadListener) {
            loadListener = true;
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.LoadFromFile e) -> {
                if (e.getEntity().getUUID().equals(loadWatch)) {
                    LOADS.incrementAndGet();
                }
            });
        }
        com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "vbp-pearl");
        ServerPlayer first = placePlayer(helper, profile);
        ThrownEnderpearl pearl = new ThrownEnderpearl(EntityType.ENDER_PEARL, level);
        pearl.setOwner(first);
        pearl.setNoGravity(true);
        pearl.setPos(helper.absoluteVec(new Vec3(4.5, 12.0, 4.5)));
        level.addFreshEntity(pearl);
        helper.runAfterDelay(3, () -> {
            server.getPlayerList().remove(first);
            helper.assertTrue(pearl.isRemoved(), "the pearl should unload with its owner");
            loadWatch = profile.getId();
            LOADS.set(0);
            ServerPlayer second = placePlayer(helper, profile);
            loadWatch = null;
            helper.assertTrue(LOADS.get() == 1, "player data should be loaded once on join, was " + LOADS.get());
            List<ThrownEnderpearl> pearls = level.getEntitiesOfClass(ThrownEnderpearl.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16),
                    p -> profile.getId().equals(((com.coolfreeze23.vbparity.misc.mixin.ProjectileOwnerAccessor) p).vbp$getOwnerUUID()));
            helper.assertTrue(pearls.size() == 1, "the saved pearl should be spawned again, found " + pearls.size());
            pearls.forEach(ThrownEnderpearl::discard);
            server.getPlayerList().remove(second);
            helper.succeed();
        });
    }

    private static ServerPlayer placePlayer(GameTestHelper helper, com.mojang.authlib.GameProfile profile) {
        net.minecraft.server.network.CommonListenerCookie cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, cookie.clientInformation());
        net.minecraft.network.Connection connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    // Job 3 (MISC-31 call sites): in Peaceful a monster spawn egg fails (also on a spawner) and /summon gives the native error.
    @GameTest(template = PLATFORM)
    public static void misc_peaceful_spawn_egg_and_summon(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerPlayer player = player(helper, 4.5, 2, 4.5);
        BlockPos spawnerPos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(spawnerPos, Blocks.SPAWNER.defaultBlockState());
        Difficulty previous = server.getWorldData().getDifficulty();
        try {
            server.setDifficulty(Difficulty.PEACEFUL, true);
            ItemStack egg = new ItemStack(Items.ZOMBIE_SPAWN_EGG);
            player.setItemInHand(InteractionHand.MAIN_HAND, egg);
            net.minecraft.world.InteractionResult result = egg.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(spawnerPos), Direction.UP, spawnerPos, false)));
            helper.assertTrue(result == net.minecraft.world.InteractionResult.FAIL, "zombie egg on a spawner in Peaceful should FAIL, was " + result);
            helper.assertTrue(egg.getCount() == 1, "the egg should not be used up");
            ItemStack pigEgg = new ItemStack(Items.PIG_SPAWN_EGG);
            player.setItemInHand(InteractionHand.MAIN_HAND, pigEgg);
            helper.assertTrue(pigEgg.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(spawnerPos), Direction.UP, spawnerPos, false))) != net.minecraft.world.InteractionResult.FAIL,
                    "control: a pig egg still works in Peaceful");
            String error = null;
            try {
                net.minecraft.server.commands.SummonCommand.createEntity(server.createCommandSourceStack().withLevel(level),
                        BuiltInRegistries.ENTITY_TYPE.getHolderOrThrow(BuiltInRegistries.ENTITY_TYPE.getResourceKey(EntityType.ZOMBIE).orElseThrow()),
                        helper.absoluteVec(new Vec3(6.5, 2, 6.5)), new CompoundTag(), true);
            } catch (CommandSyntaxException e) {
                error = e.getRawMessage() instanceof Component c && c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                        ? t.getKey() : String.valueOf(e.getRawMessage());
            }
            helper.assertTrue("commands.summon.failed.peaceful".equals(error), "summoning a zombie in Peaceful should fail with the Peaceful message, got " + error);
        } finally {
            server.setDifficulty(previous, true);
            removePlayer(helper, player);
        }
        helper.succeed();
    }

    // MISC-02: the 16 native bundle dyeing recipes (crafting_transmute, equipment, group bundle_dye) replace VB's special recipe.
    @GameTest(template = PLATFORM)
    public static void misc_bundle_dye_recipes_native(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var manager = level.getRecipeManager();
        for (String colour : List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
                "purple", "blue", "brown", "green", "red", "black")) {
            ResourceLocation id = ResourceLocation.withDefaultNamespace(colour + "_bundle");
            var holder = manager.byKey(id);
            helper.assertTrue(holder.isPresent(), "missing recipe " + id);
            var recipe = holder.get().value();
            helper.assertTrue(recipe instanceof TransmuteRecipe, id + " should be a crafting_transmute recipe, was " + recipe.getClass());
            helper.assertTrue(!recipe.isSpecial() && "bundle_dye".equals(recipe.getGroup())
                    && ((CraftingRecipe) recipe).category() == net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT, id + " book info");
        }
        helper.assertTrue(manager.byKey(ResourceLocation.withDefaultNamespace("bundle_coloring")).isEmpty(), "VB's special bundle_coloring recipe should be gone");
        ItemStack red = new ItemStack(ModItems.RED_BUNDLE.get());
        BundleContents.Mutable contents = new BundleContents.Mutable(BundleContents.EMPTY);
        contents.tryInsert(new ItemStack(Items.STONE, 5));
        red.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
        CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.BLUE_DYE), red));
        var match = manager.getRecipeFor(RecipeType.CRAFTING, input, level);
        helper.assertTrue(match.isPresent() && match.get().id().equals(ResourceLocation.withDefaultNamespace("blue_bundle")),
                "red bundle + blue dye should use minecraft:blue_bundle");
        ItemStack out = match.get().value().assemble(input, level.registryAccess());
        helper.assertTrue(out.is(ModItems.BLUE_BUNDLE.get()) && itemCount(out) == 5, "dyeing keeps the bundle contents");
        helper.succeed();
    }

    // MISC-32: shears snip equipment off mobs (native Mob.attemptToShearEquipment): body slot first, then the saddle.
    @GameTest(template = PLATFORM)
    public static void misc_shears_remove_equipment(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, 4.5, 2, 4.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        net.minecraft.world.entity.animal.Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 2, 2, 2);
        pig.equipSaddle(new ItemStack(Items.SADDLE), null);
        helper.assertTrue(pig.isSaddled(), "setup: pig saddled");
        helper.assertTrue(player.interactOn(pig, InteractionHand.MAIN_HAND).consumesAction(), "shearing a saddled pig should succeed");
        helper.assertFalse(pig.isSaddled(), "the pig's saddle should be sheared off");
        net.minecraft.world.entity.animal.horse.Horse horse = helper.spawnWithNoFreeWill(EntityType.HORSE, 6, 2, 6);
        horse.setTamed(true);
        ((com.coolfreeze23.vbparity.misc.mixin.AbstractHorseInventoryAccessor) horse).vbp$inventory().setItem(0, new ItemStack(Items.SADDLE));
        horse.setBodyArmorItem(new ItemStack(Items.IRON_HORSE_ARMOR));
        helper.assertTrue(horse.isSaddled() && horse.isWearingBodyArmor(), "setup: horse saddled and armored");
        player.interactOn(horse, InteractionHand.MAIN_HAND);
        helper.assertTrue(!horse.isWearingBodyArmor() && horse.isSaddled(), "first snip removes the armor (BODY before SADDLE)");
        player.interactOn(horse, InteractionHand.MAIN_HAND);
        helper.assertFalse(horse.isSaddled(), "second snip removes the saddle");
        helper.assertTrue(player.getVehicle() == null, "shearing should not mount the horse");
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(10));
        helper.assertTrue(drops.stream().filter(e -> e.getItem().is(Items.SADDLE)).count() == 2, "two saddles should drop");
        helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(Items.IRON_HORSE_ARMOR)), "the horse armor should drop");
        for (ResourceLocation id : List.of(MiscContent.SADDLE_UNEQUIP, MiscContent.HORSE_ARMOR_UNEQUIP, MiscContent.LLAMA_CARPET_UNEQUIP)) {
            helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(id), id + " is not registered");
        }
        drops.forEach(ItemEntity::discard);
        removePlayer(helper, player);
        helper.succeed();
    }

    // MISC-20: the End clock advances one per tick while the daylight cycle runs, and not otherwise.
    @GameTest(template = PLATFORM)
    public static void misc_end_clock_advances_with_daylight_cycle(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        EndClock.Data data = EndClock.data(server);
        long start = data.totalTicks;
        boolean daylight = server.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
        helper.runAfterDelay(10, () -> {
            long delta = data.totalTicks - start;
            if (daylight) {
                helper.assertTrue(delta >= 9 && delta <= 11, "the End clock should advance once per tick, advanced " + delta);
            } else {
                helper.assertTrue(delta == 0, "the End clock should stop without the daylight cycle, advanced " + delta);
            }
            EndClock.Data probe = new EndClock.Data();
            EndClock.advance(probe, false, true);
            EndClock.advance(probe, true, false);
            helper.assertTrue(probe.totalTicks == 0, "no advance without advance_time or while the tick rate is frozen");
            EndClock.advance(probe, true, true);
            helper.assertTrue(probe.totalTicks == 1, "one advance per tick");
            helper.succeed();
        });
    }

    // Creative tabs: backported items go to their native place (bundles in dye order after Bundle, Tears after Pigstep).
    @GameTest(template = PLATFORM)
    public static void misc_creative_tab_native_order(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var tabKey = net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES;
        var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(tabKey);
        var parent = new net.neoforged.neoforge.common.util.InsertableLinkedOpenCustomHashSet<ItemStack>(net.minecraft.world.item.ItemStackLinkedSet.TYPE_AND_TAG);
        var search = new net.neoforged.neoforge.common.util.InsertableLinkedOpenCustomHashSet<ItemStack>(net.minecraft.world.item.ItemStackLinkedSet.TYPE_AND_TAG);
        List<Item> vbOrder = List.of(Items.LEAD, Items.BUNDLE, ModItems.WHITE_BUNDLE.get(), ModItems.LIGHT_GRAY_BUNDLE.get(), ModItems.RED_BUNDLE.get(),
                ModItems.ORANGE_BUNDLE.get(), Items.MUSIC_DISC_RELIC, ModItems.MUSIC_DISC_TEARS.get(), Items.MUSIC_DISC_5, Items.MUSIC_DISC_PIGSTEP);
        for (Item item : vbOrder) {
            parent.add(new ItemStack(item));
            search.add(new ItemStack(item));
        }
        var event = new net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent(tab, tabKey,
                new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess()), parent, search);
        CreativeTabOrder.apply(event);
        List<Item> expected = List.of(Items.LEAD, Items.BUNDLE, ModItems.WHITE_BUNDLE.get(), ModItems.ORANGE_BUNDLE.get(), ModItems.LIGHT_GRAY_BUNDLE.get(),
                ModItems.RED_BUNDLE.get(), Items.MUSIC_DISC_RELIC, Items.MUSIC_DISC_5, Items.MUSIC_DISC_PIGSTEP, ModItems.MUSIC_DISC_TEARS.get());
        List<Item> gotParent = parent.stream().map(ItemStack::getItem).toList();
        List<Item> gotSearch = search.stream().map(ItemStack::getItem).toList();
        helper.assertTrue(gotParent.equals(expected), "parent order " + gotParent);
        helper.assertTrue(gotSearch.equals(expected), "search order " + gotSearch);
        helper.succeed();
    }
}
