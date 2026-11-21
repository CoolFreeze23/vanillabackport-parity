package com.coolfreeze23.vbparity.garden;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import java.lang.reflect.Field;
import com.coolfreeze23.vbparity.garden.mixin.PathFinderAccessor;
import com.coolfreeze23.vbparity.garden.mixin.PathNavigationAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.level.Level;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Pass 2: garden partial items and the technical audits. */
@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public final class GardenPass2GameTests {
    private static final String SMALL = "garden_small";

    @SuppressWarnings("unchecked")
    private static void freeze(Creaking creaking) throws ReflectiveOperationException {
        Field field = Creaking.class.getDeclaredField("CAN_MOVE");
        field.setAccessible(true);
        creaking.getEntityData().set((EntityDataAccessor<Boolean>) field.get(null), false);
    }

    /** TPHYS-C06: a frozen creaking takes extra knockback (sprint, enchantment, stabs); native blocks only the default one. */
    @GameTest(template = SMALL)
    public static void garden_frozen_creaking_takes_extra_knockback(GameTestHelper helper) throws ReflectiveOperationException {
        Creaking creaking = helper.spawn(ModEntityTypes.CREAKING.get(), new Vec3(3.5, 1.0, 3.5));
        creaking.setNoAi(true);
        freeze(creaking);
        helper.assertFalse(creaking.canMove(), "creaking is not frozen");
        creaking.setDeltaMovement(Vec3.ZERO);
        creaking.knockback(0.5, 1.0, 0.0);
        helper.assertTrue(creaking.getDeltaMovement().horizontalDistanceSqr() > 0.0, "extra knockback did not move the frozen creaking");

        creaking.setDeltaMovement(Vec3.ZERO);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(1.5, 1.0, 3.5));
        zombie.setNoAi(true);
        creaking.invulnerableTime = 0;
        creaking.hurt(helper.getLevel().damageSources().mobAttack(zombie), 0.5F);
        helper.assertTrue(creaking.getDeltaMovement().horizontalDistanceSqr() == 0.0, "default hurt knockback moved the frozen creaking: " + creaking.getDeltaMovement());
        zombie.discard();
        creaking.discard();
        helper.succeed();
    }

    /** TPHYS-C09: native MOVEMENT_SPEED is the float 0.4F widened, 0.4000000059604645. */
    @GameTest(template = SMALL)
    public static void garden_creaking_speed_is_float(GameTestHelper helper) {
        Creaking creaking = helper.spawn(ModEntityTypes.CREAKING.get(), new Vec3(3.5, 1.0, 3.5));
        double speed = creaking.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
        helper.assertTrue(speed == (double) 0.4F, "creaking base speed " + speed + ", native " + (double) 0.4F);
        creaking.discard();
        helper.succeed();
    }

    /** AI-CRK-01: native illager flee speeds from creakings (vindicator/pillager/illusioner 1.0/1.2, evoker 0.6/1.0). */
    @GameTest(template = SMALL)
    public static void garden_illagers_flee_creaking_native_speeds(GameTestHelper helper) throws ReflectiveOperationException {
        Field walk = AvoidEntityGoal.class.getDeclaredField("walkSpeedModifier");
        Field sprint = AvoidEntityGoal.class.getDeclaredField("sprintSpeedModifier");
        Field avoid = AvoidEntityGoal.class.getDeclaredField("avoidClass");
        walk.setAccessible(true);
        sprint.setAccessible(true);
        avoid.setAccessible(true);
        for (EntityType<? extends Mob> type : List.<EntityType<? extends Mob>>of(EntityType.VINDICATOR, EntityType.PILLAGER, EntityType.ILLUSIONER, EntityType.EVOKER)) {
            Mob mob = helper.spawn(type, new Vec3(3.5, 1.0, 3.5));
            mob.setNoAi(true);
            AvoidEntityGoal<?> goal = null;
            for (WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
                if (wrapped.getGoal() instanceof AvoidEntityGoal<?> g && avoid.get(g) == Creaking.class) {
                    goal = g;
                }
            }
            helper.assertTrue(goal != null, type + " does not avoid creakings");
            double w = walk.getDouble(goal);
            double s = sprint.getDouble(goal);
            boolean evoker = type == EntityType.EVOKER;
            helper.assertTrue(w == (evoker ? 0.6 : 1.0) && s == (evoker ? 1.0 : 1.2), type + " flees at " + w + "/" + s);
            mob.discard();
        }
        helper.succeed();
    }

    /** AI-CRK-D1: an explosion does not move a frozen creaking (native knockback goes through push). */
    @GameTest(template = SMALL)
    public static void garden_frozen_creaking_ignores_explosion(GameTestHelper helper) throws ReflectiveOperationException {
        Creaking frozen = helper.spawn(ModEntityTypes.CREAKING.get(), new Vec3(2.5, 2.0, 3.5));
        frozen.setNoAi(true);
        freeze(frozen);
        Creaking moving = helper.spawn(ModEntityTypes.CREAKING.get(), new Vec3(4.5, 2.0, 3.5));
        moving.setNoAi(true);
        net.minecraft.world.entity.monster.Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(3.5, 2.0, 4.5));
        zombie.setNoAi(true);
        zombie.setDeltaMovement(Vec3.ZERO);
        frozen.setDeltaMovement(Vec3.ZERO);
        moving.setDeltaMovement(Vec3.ZERO);
        Vec3 at = helper.absoluteVec(new Vec3(3.5, 2.5, 3.5));
        net.minecraft.world.level.ExplosionDamageCalculator noDamage = new net.minecraft.world.level.ExplosionDamageCalculator() {
            @Override
            public boolean shouldDamageEntity(net.minecraft.world.level.Explosion explosion, net.minecraft.world.entity.Entity entity) {
                return false;
            }
        };
        helper.getLevel().explode(null, null, noDamage, at.x, at.y, at.z, 2.0F, false, Level.ExplosionInteraction.NONE);
        helper.assertTrue(frozen.getDeltaMovement().lengthSqr() == 0.0, "explosion moved the frozen creaking: " + frozen.getDeltaMovement());
        helper.assertTrue(moving.getDeltaMovement().lengthSqr() > 0.0, "explosion did not move the free creaking: canMove " + moving.canMove() + " pos " + moving.position() + " at " + at + " frozen " + frozen.getDeltaMovement() + " zombie " + zombie.getDeltaMovement() + " seen " + net.minecraft.world.level.Explosion.getSeenPercent(at, moving));
        frozen.discard();
        moving.discard();
        zombie.discard();
        helper.succeed();
    }

    /** AI-CRK-D3: a follow-range change re-budgets the creaking's pathfinder to floor(max(range, 16) * 16). */
    @GameTest(template = SMALL, timeoutTicks = 20)
    public static void garden_creaking_path_budget_follows_range(GameTestHelper helper) {
        Creaking creaking = helper.spawn(ModEntityTypes.CREAKING.get(), new Vec3(3.5, 1.0, 3.5));
        creaking.setNoAi(true);
        creaking.getAttribute(Attributes.FOLLOW_RANGE).addPermanentModifier(
            new AttributeModifier(ResourceLocation.fromNamespaceAndPath("vbparity", "test_range"), 8.0, AttributeModifier.Operation.ADD_VALUE));
        helper.runAfterDelay(3, () -> {
            int nodes = ((PathFinderAccessor) ((PathNavigationAccessor) creaking.getNavigation()).vbparity$garden$pathFinder()).vbparity$garden$maxVisitedNodes();
            int expected = Mth.floor(Math.max((float) creaking.getAttributeValue(Attributes.FOLLOW_RANGE), 16.0F) * 16.0F);
            helper.assertTrue(nodes == expected, "pathfinder budget " + nodes + ", native " + expected);
            creaking.discard();
            helper.succeed();
        });
    }
}
