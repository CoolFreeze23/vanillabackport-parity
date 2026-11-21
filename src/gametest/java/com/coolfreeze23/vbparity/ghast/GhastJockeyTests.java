package com.coolfreeze23.vbparity.ghast;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.camel.CamelHusk;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.ZombieNautilus;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.blackgear.vanillabackport.core.VanillaBackport;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public final class GhastJockeyTests {
    /**
     * GHAST-42: naturally spawned drowned that get a trident can spawn riding a zombie nautilus. With VB's early roll
     * the trident is not there yet and no natural jockey ever appears. About 3% of drowned qualify (6.25% trident,
     * 50% roll, adults only), so 400 spawns give about 12.
     */
    @GameTest(template = "ghast_big", batch = "ghast_spawn", timeoutTicks = 100)
    public static void ghast_drownedNautilusJockeys(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(VanillaBackport.COMMON_CONFIG.hasNautilus.get(), "has_nautilus is on (default)");
        Vec3 at = helper.absoluteVec(new Vec3(12.5, 2, 12.5));
        DifficultyInstance difficulty = level.getCurrentDifficultyAt(BlockPos.containing(at));
        int jockeys = 0;
        List<Entity> spawned = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            Drowned drowned = EntityType.DROWNED.create(level);
            drowned.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
            drowned.finalizeSpawn(level, difficulty, MobSpawnType.NATURAL, null);
            if (drowned.getVehicle() instanceof ZombieNautilus nautilus) {
                jockeys++;
                helper.assertTrue(drowned.getMainHandItem().is(Items.TRIDENT), "Every nautilus rider holds a trident");
                helper.assertFalse(drowned.isBaby(), "Nautilus riders are adults");
                spawned.add(nautilus);
            }

            drowned.stopRiding();
            drowned.discard();
        }

        level.getEntitiesOfClass(ZombieNautilus.class, new AABB(at, at).inflate(4.0)).forEach(spawned::add);
        spawned.forEach(Entity::discard);
        helper.assertTrue(jockeys > 0, "Natural drowned spawns include zombie nautilus jockeys, saw " + jockeys + " of 400");
        helper.succeed();
    }

    /**
     * GHAST-43: the first husk of a natural pack (no group data yet) can spawn on a camel husk, and the jockey keeps
     * its spear. VB's early roll skips every first husk. About 10% roll a camel, so 300 spawns give about 30.
     */
    @GameTest(template = "ghast_big", batch = "ghast_spawn", timeoutTicks = 100)
    public static void ghast_huskCamelJockeys(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 at = helper.absoluteVec(new Vec3(12.5, 2, 12.5));
        DifficultyInstance difficulty = level.getCurrentDifficultyAt(BlockPos.containing(at));
        int jockeys = 0;
        for (int i = 0; i < 300; i++) {
            Husk husk = EntityType.HUSK.create(level);
            husk.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
            husk.finalizeSpawn(level, difficulty, MobSpawnType.NATURAL, null);
            if (husk.getVehicle() instanceof CamelHusk camel) {
                jockeys++;
                if (VanillaBackport.COMMON_CONFIG.hasSpears.get()) {
                    helper.assertTrue(husk.getMainHandItem().is(ModItems.IRON_SPEAR.get()), "A camel husk rider holds an iron spear, saw " + husk.getMainHandItem());
                }

                camel.getPassengers().forEach(Entity::discard);
                camel.discard();
            }

            husk.discard();
        }

        level.getEntitiesOfClass(CamelHusk.class, new AABB(at, at).inflate(4.0)).forEach(camel -> {
            camel.getPassengers().forEach(Entity::discard);
            camel.discard();
        });
        helper.assertTrue(jockeys > 0, "The first husk of a natural pack can ride a camel husk, saw " + jockeys + " of 300");
        helper.succeed();
    }
}
