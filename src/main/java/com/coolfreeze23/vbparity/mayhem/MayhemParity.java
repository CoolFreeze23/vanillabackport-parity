package com.coolfreeze23.vbparity.mayhem;

import com.blackgear.vanillabackport.common.level.components.KineticWeapon;
import com.blackgear.vanillabackport.common.registries.items.ModDataComponents;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.blackgear.vanillabackport.core.VanillaBackport;
import com.coolfreeze23.vbparity.mayhem.mixin.ParrotAccessor;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.camel.CamelHusk;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.AbstractNautilus;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.advancements.critereon.PlayerInteractTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Mounts of Mayhem parity: registrations, default item components and setup. */
public final class MayhemParity {
    private MayhemParity() {
    }

    /** Native {@code minecraft:spear} damage type (data/minecraft/damage_type/spear.json). */
    public static final ResourceKey<DamageType> SPEAR_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.withDefaultNamespace("spear"));

    /** VB registered the minimum-charge component as minimum_attack_range; the native id is minimum_attack_charge. */
    public static final ResourceLocation OLD_MINIMUM_ATTACK_CHARGE = ResourceLocation.withDefaultNamespace("minimum_attack_range");
    public static final ResourceLocation MINIMUM_ATTACK_CHARGE = ResourceLocation.withDefaultNamespace("minimum_attack_charge");

    public static final ResourceLocation PARROT_IMITATE_ZOMBIE_HORSE_ID = ResourceLocation.withDefaultNamespace("entity.parrot.imitate.zombie_horse");
    public static final ResourceLocation SADDLE_UNEQUIP_ID = ResourceLocation.withDefaultNamespace("item.saddle.unequip");
    public static final SoundEvent PARROT_IMITATE_ZOMBIE_HORSE = SoundEvent.createVariableRangeEvent(PARROT_IMITATE_ZOMBIE_HORSE_ID);
    public static final SoundEvent SADDLE_UNEQUIP = SoundEvent.createVariableRangeEvent(SADDLE_UNEQUIP_ID);

    /**
     * Native dismount thresholds (Items.*_SPEAR, Item.Properties.spear arg 5). The copper spear (12.0) is set by the
     * copper area, so it is not listed here.
     */
    public static List<SpearThreshold> dismountThresholds() {
        return List.of(
            new SpearThreshold(ModItems.WOODEN_SPEAR, 14.0F),
            new SpearThreshold(ModItems.STONE_SPEAR, 13.0F),
            new SpearThreshold(ModItems.IRON_SPEAR, 11.0F),
            new SpearThreshold(ModItems.GOLDEN_SPEAR, 13.0F),
            new SpearThreshold(ModItems.DIAMOND_SPEAR, 10.0F),
            new SpearThreshold(ModItems.NETHERITE_SPEAR, 9.0F)
        );
    }

    public record SpearThreshold(Supplier<Item> item, float dismountThreshold) {
    }

    public static List<Supplier<Item>> spears() {
        return List.of(ModItems.WOODEN_SPEAR, ModItems.STONE_SPEAR, ModItems.COPPER_SPEAR, ModItems.IRON_SPEAR, ModItems.GOLDEN_SPEAR,
            ModItems.DIAMOND_SPEAR, ModItems.NETHERITE_SPEAR);
    }

    /**
     * Native ItemStack.getDamageSource: the weapon's {@code damage_type} component when it resolves, else the default.
     */
    public static DamageSource weaponDamageSource(ItemStack weapon, LivingEntity attacker, DamageSource fallback) {
        EitherHolder<DamageType> type = weapon.isEmpty() ? null : weapon.get(ModDataComponents.DAMAGE_TYPE.get());
        if (type == null) {
            return fallback;
        }
        Optional<Holder<DamageType>> holder = type.unwrap(attacker.registryAccess());
        return holder.map(h -> new DamageSource(h, attacker)).orElse(fallback);
    }

    /**
     * Native {@code minecraft:player_sheared_equipment} (Mob.shearItem), fired when a player shears equipment off a
     * nautilus or happy ghast. 1.21.1 has no such trigger; the instance type is the native PlayerInteractTrigger.
     */
    public static final ResourceLocation PLAYER_SHEARED_EQUIPMENT_ID = ResourceLocation.withDefaultNamespace("player_sheared_equipment");
    public static final PlayerInteractTrigger PLAYER_SHEARED_EQUIPMENT = new PlayerInteractTrigger();

    public static void shearedEquipment(Player player, ItemStack sheared, Entity entity) {
        if (player instanceof ServerPlayer serverPlayer) {
            PLAYER_SHEARED_EQUIPMENT.trigger(serverPlayer, sheared, entity);
        }
    }

    /** Native CamelHusk / ZombieHorse / AbstractNautilus.interact: any interaction makes the mob persistent. */
    public static void persistOnInteract(Entity entity) {
        if (isVbMount(entity)) {
            ((Mob) entity).setPersistenceRequired();
        }
    }

    /** The mounts whose native class overrides interact() with setPersistenceRequired(). */
    public static boolean isVbMount(Entity entity) {
        return entity instanceof CamelHusk || entity instanceof ZombieHorse || entity instanceof AbstractNautilus;
    }

    /** 26.2 enchantment effect type ids that 1.21.1 + VB know under other names (datapack compatibility). */
    private static final ResourceLocation VB_IMPULSE = ResourceLocation.fromNamespaceAndPath("vanillabackport", "apply_entity_impulse");
    private static final ResourceLocation VB_EXHAUSTION = ResourceLocation.fromNamespaceAndPath("vanillabackport", "apply_exhaustion");

    @EventBusSubscriber(modid = "vbparity", bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void register(RegisterEvent event) {
            event.register(Registries.SOUND_EVENT, helper -> {
                helper.register(PARROT_IMITATE_ZOMBIE_HORSE_ID, PARROT_IMITATE_ZOMBIE_HORSE);
                helper.register(SADDLE_UNEQUIP_ID, SADDLE_UNEQUIP);
            });
            event.register(Registries.LOOT_CONDITION_TYPE,
                helper -> helper.register(ResourceLocation.fromNamespaceAndPath("vbparity", "not_fall_flying"), NotFallFlyingCondition.TYPE));
            event.register(Registries.TRIGGER_TYPE, helper -> helper.register(PLAYER_SHEARED_EQUIPMENT_ID, PLAYER_SHEARED_EQUIPMENT));
            if (event.getRegistryKey().equals(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE)) {
                var registry = event.getRegistry(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE);
                registry.addAlias(ResourceLocation.withDefaultNamespace("apply_impulse"), VB_IMPULSE);
                registry.addAlias(ResourceLocation.withDefaultNamespace("apply_exhaustion"), VB_EXHAUSTION);
                registry.addAlias(ResourceLocation.withDefaultNamespace("change_item_damage"), ResourceLocation.withDefaultNamespace("damage_item"));
            }
            if (event.getRegistryKey().equals(Registries.ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE)) {
                var registry = event.getRegistry(Registries.ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE);
                registry.addAlias(ResourceLocation.withDefaultNamespace("apply_impulse"), VB_IMPULSE);
                registry.addAlias(ResourceLocation.withDefaultNamespace("apply_exhaustion"), VB_EXHAUSTION);
                registry.addAlias(ResourceLocation.withDefaultNamespace("change_item_damage"), ResourceLocation.withDefaultNamespace("damage_item"));
            }
            if (event.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE)) {
                // MISC-33: the component now registers under the native id; stacks saved with VB's id still load.
                event.getRegistry(Registries.DATA_COMPONENT_TYPE).addAlias(OLD_MINIMUM_ATTACK_CHARGE, MINIMUM_ATTACK_CHARGE);
            }
        }

        @SubscribeEvent
        public static void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
            // MAYHEM-01: native dismount speed thresholds.
            for (SpearThreshold entry : dismountThresholds()) {
                Item spear = entry.item().get();
                KineticWeapon kinetic = spear.components().get(ModDataComponents.KINETIC_WEAPON.get());
                if (kinetic != null && kinetic.dismountConditions().isPresent()) {
                    KineticWeapon.Condition dismount = kinetic.dismountConditions().get();
                    KineticWeapon fixed = new KineticWeapon(
                        kinetic.contactCooldownTicks(),
                        kinetic.delayTicks(),
                        Optional.of(new KineticWeapon.Condition(dismount.maxDurationTicks(), entry.dismountThreshold(), dismount.minRelativeSpeed())),
                        kinetic.knockbackConditions(),
                        kinetic.damageConditions(),
                        kinetic.forwardMovement(),
                        kinetic.damageMultiplier(),
                        kinetic.sound(),
                        kinetic.hitSound()
                    );
                    event.modify(spear, builder -> builder.set(ModDataComponents.KINETIC_WEAPON.get(), fixed));
                }
            }

            // MAYHEM-02: every spear carries damage_type = minecraft:spear, as natively.
            EitherHolder<DamageType> spearType = new EitherHolder<>(SPEAR_DAMAGE);
            for (Supplier<Item> spear : spears()) {
                event.modify(spear.get(), builder -> builder.set(ModDataComponents.DAMAGE_TYPE.get(), spearType));
            }
        }

        @SubscribeEvent
        public static void commonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                // MAYHEM-29: parrots imitate zombie horses (hostile natively).
                if (VanillaBackport.COMMON_CONFIG.hasZombieHorses.get()) {
                    ParrotAccessor.vbp$mobSoundMap().put(EntityType.ZOMBIE_HORSE, PARROT_IMITATE_ZOMBIE_HORSE);
                }
            });
        }
    }
}
