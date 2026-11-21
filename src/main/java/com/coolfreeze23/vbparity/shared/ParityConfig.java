package com.coolfreeze23.vbparity.shared;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * vbparity-common.toml. Every switch here turns on a native mechanic that also changes vanilla content, the same way
 * the current game changed it. All default to on (native behaviour); a pack can turn one off to keep 1.21.1 vanilla.
 */
public final class ParityConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue NATIVE_WANDERING_TRADER;
    public static final ModConfigSpec.BooleanValue NATIVE_WOLF_FOOD;
    public static final ModConfigSpec.BooleanValue GOLDEN_DANDELION;
    public static final ModConfigSpec.BooleanValue NATIVE_COMPOSTER_ROLL;
    public static final ModConfigSpec.BooleanValue NATIVE_MOB_CAP;
    public static final ModConfigSpec.BooleanValue NATIVE_CHUNK_GENERATION_SPAWNS;
    public static final ModConfigSpec.BooleanValue NATIVE_ZOMBIE_VILLAGER_CONVERSION;
    public static final ModConfigSpec.BooleanValue NATIVE_GAMERULES;
    public static final ModConfigSpec.BooleanValue NATIVE_WITCH_HUT_CATS;
    public static final ModConfigSpec.BooleanValue NATIVE_HUGE_MUSHROOMS;
    public static final ModConfigSpec.BooleanValue NATIVE_LUNGE_GRACE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Native mechanics that the current game changed for vanilla content too. On = exactly like the current game.")
            .push("native_vanilla_changes");
        NATIVE_WANDERING_TRADER = b.comment("Wandering trader offers: 2 buying + 2 uncommon + 5 common, from per-world random sequences.")
            .define("native_wandering_trader", true);
        NATIVE_WOLF_FOOD = b.comment("Wolves also eat fish and rabbit stew, as in the current game.")
            .define("native_wolf_food", true);
        GOLDEN_DANDELION = b.comment("The golden dandelion and its baby age lock.")
            .define("golden_dandelion", true);
        NATIVE_COMPOSTER_ROLL = b.comment("Composter chance rolled as an integer out of 100, as in the current game.")
            .define("native_composter_roll", true);
        NATIVE_MOB_CAP = b.comment("Natural spawning checks the global mob cap once per tick, as in the current game.")
            .define("native_mob_cap", true);
        NATIVE_CHUNK_GENERATION_SPAWNS = b.comment("No animals spawn with new chunks while doMobSpawning is off.")
            .define("native_chunk_generation_spawns", true);
        NATIVE_ZOMBIE_VILLAGER_CONVERSION = b.comment("Villagers turned into zombie villagers get no new equipment roll.")
            .define("native_zombie_villager_conversion", true);
        NATIVE_GAMERULES = b.comment("Registers the current game's tntExplodes and spawnMonsters gamerules.")
            .define("native_gamerules", true);
        NATIVE_WITCH_HUT_CATS = b.comment("Witch hut cats behave as in the current game (random colour; vanilla bug MC-147659 that NeoForge fixes).")
            .define("native_witch_hut_cats", true);
        NATIVE_HUGE_MUSHROOMS = b.comment("Huge mushrooms only replace what the current game lets them replace.")
            .define("native_huge_mushrooms", true);
        NATIVE_LUNGE_GRACE = b.comment("The lunge dash's fall-damage grace applies to every mob, not only players.")
            .define("native_lunge_grace", true);
        b.pop();
        SPEC = b.build();
    }

    private ParityConfig() {
    }
}
