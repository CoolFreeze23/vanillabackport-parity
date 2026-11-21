package com.coolfreeze23.vbparity.devcheck;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * Dev-only client check (never in the jar): forces every mixin to apply, makes a world, stages every backported mob
 * (adult and baby) and block, takes screenshots, then quits. Enabled by -Dvbparity.devcheck=true.
 */
@EventBusSubscriber(modid = "vbparity", value = Dist.CLIENT)
public final class DevCheck {
    static final Logger LOG = LoggerFactory.getLogger("VBP-DEVCHECK");
    static final String WORLD = "vbp_devcheck";
    static final List<String> MOBS = List.of(
        "creaking", "happy_ghast", "copper_golem", "parched", "camel_husk", "nautilus", "zombie_nautilus", "sulfur_cube",
        "cow", "pig", "chicken", "wolf", "cat", "sheep", "zombie_horse", "frog", "zombie", "drowned", "husk", "skeleton");
    static final List<String> BLOCKS = List.of(
        "creaking_heart", "pale_moss_block", "open_eyeblossom", "pale_hanging_moss", "resin_clump", "copper_chest",
        "oak_shelf", "copper_golem_statue", "copper_lantern", "lightning_rod", "potent_sulfur", "leaf_litter",
        "firefly_bush", "bush", "short_dry_grass", "cactus_flower", "dried_ghast", "copper_bars", "sulfur",
        "straw_bed", "shelf_mushroom", "poplar_log");
    static final List<String> ITEMS = List.of("iron_spear", "bundle", "white_harness", "copper_sword", "golden_nautilus_armor");
    static boolean started;
    static int worldTicks;
    static int shots;

    private DevCheck() {
    }

    static boolean on() {
        return Boolean.getBoolean("vbparity.devcheck");
    }

    @SubscribeEvent
    public static void title(ScreenEvent.Opening event) {
        if (!on() || started || !(event.getNewScreen() instanceof TitleScreen)) {
            return;
        }
        started = true;
        try {
            MixinEnvironment.getCurrentEnvironment().audit();
            LOG.info("MIXIN AUDIT OK");
        } catch (Throwable t) {
            LOG.error("MIXIN AUDIT FAILED", t);
        }
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> {
            if (mc.getLevelSource().levelExists(WORLD)) {
                mc.createWorldOpenFlows().openWorld(WORLD, () -> mc.setScreen(new TitleScreen()));
            } else {
                GameRules rules = new GameRules();
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.NORMAL, true, rules,
                    WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(12345L, false, false),
                    WorldPresets::createNormalWorldDimensions, new TitleScreen());
            }
        });
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!on() || mc.player == null || mc.getSingleplayerServer() == null) {
            return;
        }
        worldTicks++;
        if (worldTicks == 60) {
            mc.getSingleplayerServer().execute(DevCheck::stage);
        }
        if (worldTicks == 160 || worldTicks == 220) {
            shot(mc);
        }
        if (worldTicks == 230) {
            mc.player.setYRot(mc.player.getYRot() + 180F);
        }
        if (worldTicks == 280) {
            shot(mc);
        }
        if (worldTicks == 300) {
            LOG.info("DEVCHECK DONE after {} world ticks", worldTicks);
            mc.stop();
        }
    }

    static void shot(Minecraft mc) {
        int n = ++shots;
        Screenshot.grab(mc.gameDirectory, "vbp_devcheck_" + n + ".png", mc.getMainRenderTarget(),
            c -> LOG.info("SCREENSHOT {}: {}", n, c.getString()));
    }

    static void stage() {
        Minecraft mc = Minecraft.getInstance();
        ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
        ServerLevel level = player.serverLevel();
        level.setDayTime(6000);
        BlockPos origin = player.blockPosition();
        // Flat stage so every mob is visible.
        for (int dx = -14; dx <= 14; dx++) {
            for (int dz = -8; dz <= 30; dz++) {
                level.setBlock(origin.offset(dx, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int dy = 0; dy < 12; dy++) {
                    level.setBlock(origin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        int i = 0;
        int spawned = 0;
        for (String id : MOBS) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.withDefaultNamespace(id));
            for (int baby = 0; baby < 2; baby++) {
                BlockPos at = origin.offset(-12 + (i % 10) * 2 + (i >= 10 ? 1 : 0), 0, 8 + (i / 10) * 4 + baby * 10);
                Entity e = type.spawn(level, at, MobSpawnType.COMMAND);
                if (e == null) {
                    LOG.warn("could not spawn {}", id);
                    continue;
                }
                spawned++;
                if (e instanceof Mob mob) {
                    mob.setNoAi(true);
                }
                if (baby == 1) {
                    if (e instanceof AgeableMob ageable) {
                        ageable.setAge(-24000);
                    } else if (e instanceof Mob mob) {
                        mob.setBaby(true);
                    }
                }
            }
            i++;
        }
        int bx = 0;
        for (String id : BLOCKS) {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(id));
            level.setBlock(origin.offset(-11 + bx, 0, 4), block.defaultBlockState(), 3);
            bx++;
        }
        for (String id : ITEMS) {
            player.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(id))));
        }
        player.getInventory().selected = 0;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(level, origin.getX() + 0.5, origin.getY() + 5, origin.getZ() - 3.5, 0F, 25F);
        LOG.info("STAGED {} entities, {} blocks", spawned, bx);
    }
}
