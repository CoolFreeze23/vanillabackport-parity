package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.api.modules.leash_behavior.LeashPhysics;
import com.blackgear.vanillabackport.core.VanillaBackport;
import com.coolfreeze23.vbparity.ghast.GhastSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GHAST-32: a firework boost that snaps your leashes plays item.lead.break. This runs just before VB's own hook at
 * the same point (a lower priority applies first); VB's hook then finds nothing left to drop and stays silent.
 */
@Mixin(value = FireworkRocketItem.class, priority = 900)
public abstract class FireworkRocketItemMixin {
    @Inject(method = "use", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/projectile/FireworkRocketEntity;<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void vbp$leadBreakSound(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (VanillaBackport.COMMON_CONFIG.leashDropConnections.get() && LeashPhysics.dropAllLeashConnections(player, null)) {
            level.playSound(null, player, GhastSounds.LEAD_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }
}
