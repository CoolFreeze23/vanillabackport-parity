package com.coolfreeze23.vbparity.ai;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.camel.CamelHusk;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.golem.copper_golem.CopperGolem;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.AbstractNautilus;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.skeleton.Parched;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCube;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.blackgear.vanillabackport.core.data.tags.ModItemTags;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

/** Shared scoping and native helpers for the mob-behaviour fixes. */
public final class AiParity {
    private AiParity() {
    }

    /**
     * Mobs VanillaBackport adds. Engine-order fixes (sensor start delay, input decay, panic path stop, home bias) are
     * applied to these only, so vanilla mobs keep their 1.21.1 behaviour.
     */
    public static boolean isVbAdded(@Nullable Entity entity) {
        return entity instanceof HappyGhast
            || entity instanceof AbstractNautilus
            || entity instanceof CamelHusk
            || entity instanceof Creaking
            || entity instanceof CopperGolem
            || entity instanceof SulfurCube
            || entity instanceof Parched;
    }

    /** Brain mobs whose sensors use the follow range natively (ranges fixed at 16 in 1.21.1). */
    public static boolean followRangeSensors(@Nullable Entity entity) {
        return entity instanceof HappyGhast || entity instanceof AbstractNautilus || entity instanceof CamelHusk;
    }

    public static double followRange(LivingEntity body) {
        return body.getAttributes().hasAttribute(Attributes.FOLLOW_RANGE) ? body.getAttributeValue(Attributes.FOLLOW_RANGE) : 16.0;
    }

    /**
     * AI-GHS-D1: native keyboard input is normalised, scaled by 0.98 and fitted to the unit square
     * (KeyboardInput, LocalPlayer.modifyInputSpeedForSquareMovement), so with both axes held each component a mount
     * reads is 0.70710677; 1.21.1 passes 0.98 on both. A single axis stays 0.98 in both.
     */
    public static float squareInput(float component, net.minecraft.world.entity.player.Player player) {
        return player.xxa != 0.0F && player.zza != 0.0F && component != 0.0F ? Math.signum(component) * 0.70710677F : component;
    }

    // ------------------------------------------------------------------ weapon comparison (native Mob.compareWeapons)

    public static boolean spearsOn() {
        return com.blackgear.vanillabackport.core.VanillaBackport.COMMON_CONFIG.hasSpears.get();
    }

    public static boolean isSpear(ItemStack stack) {
        return stack.is(ModItemTags.SPEARS);
    }

    /** Native getPreferredWeaponType as an item test, or null when the mob has none. */
    @Nullable
    public static Predicate<ItemStack> preferredWeapon(Mob mob) {
        if (mob instanceof Piglin) {
            return mob.isBaby() ? null : s -> s.is(Items.CROSSBOW) || s.is(ModItems.GOLDEN_SPEAR.get());
        } else if (mob instanceof WitherSkeleton) {
            return null;
        } else if (mob instanceof AbstractSkeleton) {
            return s -> s.is(Items.BOW);
        } else if (mob instanceof Pillager) {
            return s -> s.is(Items.CROSSBOW);
        } else if (mob instanceof Drowned) {
            return s -> s.is(Items.TRIDENT);
        }
        return null;
    }

    /** Native Mob.compareWeapons (main hand). */
    public static boolean compareWeapons(Mob mob, ItemStack candidate, ItemStack existing) {
        Predicate<ItemStack> preferred = preferredWeapon(mob);
        if (preferred != null) {
            if (preferred.test(existing) && !preferred.test(candidate)) {
                return false;
            }
            if (!preferred.test(existing) && preferred.test(candidate)) {
                return true;
            }
        }
        double n = attackDamageWith(mob, candidate);
        double o = attackDamageWith(mob, existing);
        return n != o ? n > o : canReplaceEqualItem(candidate, existing);
    }

    /** Native getApproximateAttributeWith(stack, ATTACK_DAMAGE, MAINHAND): only attack-damage modifiers count. */
    public static double attackDamageWith(Mob mob, ItemStack stack) {
        double base = mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? mob.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) : 0.0;
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double value = base;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().value() != Attributes.ATTACK_DAMAGE.value() || !entry.slot().test(EquipmentSlot.MAINHAND)) {
                continue;
            }
            double amount = entry.modifier().amount();
            AttributeModifier.Operation op = entry.modifier().operation();
            value += switch (op) {
                case ADD_VALUE -> amount;
                case ADD_MULTIPLIED_BASE -> amount * base;
                case ADD_MULTIPLIED_TOTAL -> amount * value;
            };
        }
        return value;
    }

    /** Native Mob.canReplaceEqualItem: more enchantments, then less damage, then a custom name. */
    public static boolean canReplaceEqualItem(ItemStack candidate, ItemStack existing) {
        Set<?> cur = existing.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet();
        Set<?> neu = candidate.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet();
        if (neu.size() != cur.size()) {
            return neu.size() > cur.size();
        }
        int nd = candidate.getDamageValue();
        int cd = existing.getDamageValue();
        return nd != cd ? nd < cd : candidate.has(DataComponents.CUSTOM_NAME) && !existing.has(DataComponents.CUSTOM_NAME);
    }
}
