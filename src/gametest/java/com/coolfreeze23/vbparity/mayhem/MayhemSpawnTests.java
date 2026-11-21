package com.coolfreeze23.vbparity.mayhem;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.camel.CamelHusk;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Pass-2 spawning GameTests for Mounts of Mayhem and the happy ghast. */
@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public class MayhemSpawnTests {
    private static final String SMALL = "mayhem_small";

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    /** The test level, but reporting the given difficulty and handing out the given random. */
    private static ServerLevelAccessor levelWith(ServerLevel level, Difficulty difficulty, RandomSource random) {
        return (ServerLevelAccessor) Proxy.newProxyInstance(ServerLevelAccessor.class.getClassLoader(), new Class<?>[] {ServerLevelAccessor.class},
            (proxy, method, args) -> {
                if (difficulty != null && method.getName().equals("getDifficulty") && method.getParameterCount() == 0) {
                    return difficulty;
                }
                if (random != null && method.getName().equals("getRandom") && method.getParameterCount() == 0) {
                    return random;
                }
                try {
                    return method.invoke(level, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            });
    }

    /** A random source that records, for each draw, the class and method that asked for it. */
    private static class TracingRandom extends LegacyRandomSource {
        final List<String> callers = new ArrayList<>();
        final List<Integer> intBounds = new ArrayList<>();
        final float floatValue;
        final int intValue;

        TracingRandom(long seed, float floatValue, int intValue) {
            super(seed);
            this.floatValue = floatValue;
            this.intValue = intValue;
        }

        private void record(String kind) {
            String caller = StackWalker.getInstance().walk(frames -> frames
                .filter(f -> !f.getClassName().equals(TracingRandom.class.getName()) && !f.getClassName().startsWith("net.minecraft.world.level.levelgen.")
                    && !f.getClassName().startsWith("net.minecraft.util.RandomSource"))
                .findFirst().map(f -> f.getClassName() + "#" + f.getMethodName()).orElse("?"));
            this.callers.add(kind + " " + caller);
        }

        @Override
        public float nextFloat() {
            record("float");
            return this.floatValue >= 0.0F ? this.floatValue : super.nextFloat();
        }

        @Override
        public int nextInt(int bound) {
            record("int");
            this.intBounds.add(bound);
            return this.intValue >= 0 ? Math.min(this.intValue, bound - 1) : super.nextInt(bound);
        }

        @Override
        public double nextDouble() {
            record("double");
            return super.nextDouble();
        }

        boolean stackHas(String needle) {
            return StackWalker.getInstance().walk(frames -> frames.anyMatch(f -> f.getClassName().contains(needle)));
        }
    }

    private static boolean monsterRule(EntityType<?> type, ServerLevelAccessor level, BlockPos pos) {
        try {
            Class<?> spawns = Class.forName("com.blackgear.vanillabackport.common.integrations.MobSpawns");
            Method m = spawns.getDeclaredMethod("checkMonsterSpawnRules", EntityType.class, ServerLevelAccessor.class, MobSpawnType.class, BlockPos.class,
                RandomSource.class);
            m.setAccessible(true);
            return (boolean) m.invoke(null, type, level, MobSpawnType.TRIAL_SPAWNER, pos, RandomSource.create(1L));
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException("reflection: " + e);
        }
    }

    /** SPAWN-06: VB's monster spawn rule does not refuse a Peaceful-allowed type (zombie horse) on Peaceful. */
    @GameTest(template = SMALL)
    public static void mayhem_p2_zombie_horse_spawn_rule_on_peaceful(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        ServerLevelAccessor peaceful = levelWith(helper.getLevel(), Difficulty.PEACEFUL, null);
        check(monsterRule(EntityType.ZOMBIE_HORSE, peaceful, pos), "zombie horse refused on Peaceful");
        check(!monsterRule(ModEntityTypes.CREAKING.get(), peaceful, pos), "a creaking passed on Peaceful");
        check(monsterRule(ModEntityTypes.CREAKING.get(), levelWith(helper.getLevel(), Difficulty.HARD, null), pos), "test setup: creaking refused on Hard");
        helper.succeed();
    }

    /** SPAWN-07: a spawner only places a happy ghast on an animal-spawnable block (Animal.checkAnimalSpawnRules). */
    @GameTest(template = SMALL)
    public static void mayhem_p2_happy_ghast_spawn_placement(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.GRASS_BLOCK);
        ServerLevel level = helper.getLevel();
        boolean onStone = SpawnPlacements.checkSpawnRules(ModEntityTypes.HAPPY_GHAST.get(), level, MobSpawnType.SPAWNER,
            helper.absolutePos(new BlockPos(1, 2, 1)), level.getRandom());
        boolean onGrass = SpawnPlacements.checkSpawnRules(ModEntityTypes.HAPPY_GHAST.get(), level, MobSpawnType.SPAWNER,
            helper.absolutePos(new BlockPos(4, 2, 4)), level.getRandom());
        check(!onStone, "a happy ghast may spawn on stone");
        check(onGrass, "a happy ghast may not spawn on lit grass");
        helper.succeed();
    }

    /** SPAWN-09: a leashed camel husk / zombie horse needs no despawn (native requiresCustomPersistence || isLeashed). */
    @GameTest(template = SMALL, timeoutTicks = 40)
    public static void mayhem_p2_leashed_mounts_do_not_despawn(GameTestHelper helper) {
        CamelHusk husk = helper.spawn(ModEntityTypes.CAMEL_HUSK.get(), new Vec3(1.5, 2, 1.5));
        ZombieHorse horse = helper.spawn(EntityType.ZOMBIE_HORSE, new Vec3(4.5, 2, 1.5));
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(4.5, 2, 4.5));
        Zombie holder = helper.spawn(EntityType.ZOMBIE, new Vec3(2.5, 2, 4.5));
        holder.setNoAi(true);
        for (var mob : List.of(husk, horse, pig)) {
            mob.setNoAi(true);
            mob.setLeashedTo(holder, true);
        }
        check(husk.requiresCustomPersistence(), "a leashed camel husk can despawn");
        check(horse.requiresCustomPersistence(), "a leashed zombie horse can despawn");
        check(!pig.requiresCustomPersistence(), "the pig changed (native 1.21.1 vanilla left alone)");
        helper.succeed();
    }

    /** SPAWN-10: the zombie rider of a natural zombie horse is rolled before the horse's own draws. */
    @GameTest(template = SMALL)
    public static void mayhem_p2_zombie_horse_rider_rolled_first(GameTestHelper helper) {
        ZombieHorse horse = EntityType.ZOMBIE_HORSE.create(helper.getLevel());
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        horse.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        List<Boolean> riderFirst = new ArrayList<>();
        TracingRandom random = new TracingRandom(7L, -1.0F, -1) {
            @Override
            public double nextDouble() {
                if (riderFirst.isEmpty()) {
                    riderFirst.add(stackHas("monster.Zombie"));
                }
                return super.nextDouble();
            }

            @Override
            public float nextFloat() {
                if (riderFirst.isEmpty()) {
                    riderFirst.add(stackHas("monster.Zombie"));
                }
                return super.nextFloat();
            }
        };
        horse.finalizeSpawn(levelWith(helper.getLevel(), null, random), helper.getLevel().getCurrentDifficultyAt(horse.blockPosition()), MobSpawnType.NATURAL, null);
        check(horse.getFirstPassenger() instanceof Zombie zombie && zombie.getMainHandItem().is(ModItems.IRON_SPEAR.get()), "no spear zombie rider");
        check(!riderFirst.isEmpty() && riderFirst.get(0), "the first spawn draw was the horse's, not the rider's");
        horse.getFirstPassenger().discard();
        horse.discard();
        helper.succeed();
    }

    /** SPAWN-13: a conversion (villager to zombie villager) never gets VB's spear or its widened weapon roll. */
    @GameTest(template = SMALL)
    public static void mayhem_p2_conversion_gets_no_spear(GameTestHelper helper) {
        for (MobSpawnType reason : List.of(MobSpawnType.CONVERSION, MobSpawnType.NATURAL)) {
            Zombie zombie = EntityType.ZOMBIE_VILLAGER.create(helper.getLevel());
            Vec3 at = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
            zombie.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
            TracingRandom random = new TracingRandom(3L, 0.0F, 1);
            zombie.finalizeSpawn(levelWith(helper.getLevel(), null, random), helper.getLevel().getCurrentDifficultyAt(zombie.blockPosition()), reason,
                new Zombie.ZombieGroupData(false, false));
            boolean spear = zombie.getMainHandItem().is(ModItems.IRON_SPEAR.get());
            if (reason == MobSpawnType.CONVERSION) {
                check(!spear, "a converted zombie villager got an iron spear");
                check(random.intBounds.contains(3) && !random.intBounds.contains(6), "conversion weapon roll bounds " + random.intBounds);
            } else {
                check(spear, "test setup: a natural zombie villager with roll 1 has no spear (" + zombie.getMainHandItem() + ")");
            }
            zombie.discard();
        }
        helper.succeed();
    }

    private static long zombieFloats(GameTestHelper helper, EntityType<? extends Zombie> type) {
        Zombie zombie = type.create(helper.getLevel());
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        zombie.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        TracingRandom random = new TracingRandom(5L, 0.99F, 0);
        zombie.finalizeSpawn(levelWith(helper.getLevel(), null, random), helper.getLevel().getCurrentDifficultyAt(zombie.blockPosition()), MobSpawnType.NATURAL,
            new Zombie.ZombieGroupData(false, false));
        zombie.discard();
        return random.callers.stream()
            .filter(c -> c.startsWith("float net.minecraft.world.entity.monster.Zombie#") && !c.endsWith("#populateDefaultEquipmentSlots"))
            .count();
    }

    /** SPAWN-14: Zombie.finalizeSpawn draws the door float for drowned too, so they make as many draws as a zombie there. */
    @GameTest(template = SMALL)
    public static void mayhem_p2_drowned_draws_door_roll(GameTestHelper helper) {
        long zombie = zombieFloats(helper, EntityType.ZOMBIE);
        long drowned = zombieFloats(helper, EntityType.DROWNED);
        check(zombie > 0 && drowned == zombie, "Zombie.finalizeSpawn floats: zombie " + zombie + ", drowned " + drowned);
        helper.succeed();
    }
}
