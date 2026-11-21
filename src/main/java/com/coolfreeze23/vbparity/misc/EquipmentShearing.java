package com.coolfreeze23.vbparity.misc;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.AbstractNautilus;
import com.coolfreeze23.vbparity.misc.mixin.AbstractHorseInventoryAccessor;
import com.coolfreeze23.vbparity.misc.mixin.SteeringAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import javax.annotation.Nullable;

/**
 * Native Entity.interact / Mob.attemptToShearEquipment: right-clicking a mob with shears (not sneaking, no passengers,
 * nothing leashed to shear first) snips off the first shearable piece of equipment, in the order main hand, off hand,
 * feet, legs, chest, head, body, saddle: saddles, horse armor, wolf armor and llama carpets (native
 * {@code Equippable.canBeSheared}). Curse of Binding blocks it unless the player is in creative. The item drops at
 * the average passenger attachment point, the shears take 1 damage, a SHEAR game event and the item's unequip sound
 * play. 1.21.1 has no Equippable component, so the shearable items and their sounds are listed here. Wolves keep the
 * 1.21.1 wolf-armor shearing (already the native rule); VB's happy ghast and nautiluses have their own handling.
 * Runs after VB's lead interaction, which handles shears that cut leashes first (native order).
 */
@EventBusSubscriber(modid = "vbparity")
public final class EquipmentShearing {
    private EquipmentShearing() {}

    private static final EquipmentSlot[] NATIVE_SLOT_ORDER = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.FEET, EquipmentSlot.LEGS,
            EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.BODY};

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack shears = player.getItemInHand(hand);
        if (!shears.is(Items.SHEARS) || !(event.getTarget() instanceof Mob mob) || !mob.isAlive()
                || mob instanceof Wolf || mob instanceof HappyGhast || mob instanceof AbstractNautilus
                || mob.isVehicle() || player.isSecondaryUseActive()) {
            return;
        }
        if (attemptToShearEquipment(mob, player, hand, shears)) {
            event.setCancellationResult(InteractionResult.sidedSuccess(mob.level().isClientSide()));
            event.setCanceled(true);
        }
    }

    public static boolean attemptToShearEquipment(Mob mob, Player player, InteractionHand hand, ItemStack shears) {
        for (EquipmentSlot slot : NATIVE_SLOT_ORDER) {
            ItemStack stack = mob.getItemBySlot(slot);
            SoundEvent sound = shearingSound(stack);
            if (sound != null && canShear(stack, player)) {
                shear(mob, player, hand, shears, stack.copy(), sound, () -> mob.setItemSlot(slot, ItemStack.EMPTY));
                return true;
            }
        }
        // The SADDLE slot: 1.21.1 keeps saddles in the horse inventory or in the pig/strider steering state.
        if (mob instanceof AbstractHorse horse && horse.isSaddled()) {
            ItemStack saddle = ((AbstractHorseInventoryAccessor) horse).vbp$inventory().getItem(AbstractHorse.INV_SLOT_SADDLE);
            if (mob.level().isClientSide() || !saddle.isEmpty() && canShear(saddle, player)) {
                shear(mob, player, hand, shears, saddle.copy(), MiscContent.sound(MiscContent.SADDLE_UNEQUIP),
                        () -> ((AbstractHorseInventoryAccessor) horse).vbp$inventory().setItem(AbstractHorse.INV_SLOT_SADDLE, ItemStack.EMPTY));
                return true;
            }
        } else if ((mob instanceof Pig || mob instanceof Strider) && ((net.minecraft.world.entity.Saddleable) mob).isSaddled()) {
            shear(mob, player, hand, shears, new ItemStack(Items.SADDLE), MiscContent.sound(MiscContent.SADDLE_UNEQUIP),
                    () -> ((SteeringAccessor) mob).vbp$steering().setSaddle(false));
            return true;
        }
        return false;
    }

    /** Native {@code Equippable.shearingSound} of the items that can be sheared off, or null. */
    @Nullable
    public static SoundEvent shearingSound(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(Items.SADDLE)) {
            return MiscContent.sound(MiscContent.SADDLE_UNEQUIP);
        }
        if (stack.getItem() instanceof AnimalArmorItem armor) {
            return armor.getBodyType() == AnimalArmorItem.BodyType.CANINE
                    ? SoundEvents.ARMOR_UNEQUIP_WOLF : MiscContent.sound(MiscContent.HORSE_ARMOR_UNEQUIP);
        }
        if (stack.is(ItemTags.WOOL_CARPETS)) {
            return MiscContent.sound(MiscContent.LLAMA_CARPET_UNEQUIP);
        }
        return null;
    }

    private static boolean canShear(ItemStack stack, Player player) {
        return !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) || player.isCreative();
    }

    /** Native {@code Mob.shearItem} + the unequip sound. Server side only; the client just reports success. */
    private static void shear(Mob mob, Player player, InteractionHand hand, ItemStack shears, ItemStack sheared, SoundEvent sound,
                              Runnable remove) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        shears.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        Vec3 offset = averagePassengerAttachment(mob);
        remove.run();
        mob.gameEvent(GameEvent.SHEAR, player);
        if (!sheared.isEmpty()) {
            ItemEntity item = new ItemEntity(level, mob.getX() + offset.x, mob.getY() + offset.y, mob.getZ() + offset.z, sheared);
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
        mob.playSound(sound);
    }

    /** Native {@code getAttachments().getAverage(EntityAttachment.PASSENGER)} (unrotated). */
    static Vec3 averagePassengerAttachment(Entity entity) {
        Vec3 sum = Vec3.ZERO;
        int count = 0;
        for (Vec3 point; (point = entity.getAttachments().getNullable(EntityAttachment.PASSENGER, count, 0.0F)) != null; count++) {
            sum = sum.add(point);
        }
        return count == 0 ? Vec3.ZERO : sum.scale(1.0F / count);
    }
}
