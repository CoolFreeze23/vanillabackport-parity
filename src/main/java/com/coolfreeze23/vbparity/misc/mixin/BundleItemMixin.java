package com.coolfreeze23.vbparity.misc.mixin;

import com.blackgear.vanillabackport.common.api.modules.bundle_ui.BundleFeatures;
import com.blackgear.vanillabackport.common.api.modules.bundle_ui.BundleItemHandler;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Native bundles empty while the use button is held: one item on the first use tick, then one every
 * 2 ticks from the 11th tick on, for up to 200 ticks (BundleItem.use / onUseTick / getUseDuration).
 */
@Mixin(BundleItem.class)
public abstract class BundleItemMixin extends Item {
    private static final int VBP_USE_DURATION = 200;

    public BundleItemMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void vbp$startUsing(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (BundleFeatures.onBundleUpdate()) {
            player.startUsingItem(hand);
            cir.setReturnValue(InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide()));
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return BundleFeatures.onBundleUpdate() ? VBP_USE_DURATION : super.getUseDuration(stack, entity);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!BundleFeatures.onBundleUpdate()) {
            super.onUseTick(level, entity, stack, remainingUseDuration);
            return;
        }
        if (entity instanceof Player player) {
            int useDuration = this.getUseDuration(stack, entity);
            boolean isFirstTick = remainingUseDuration == useDuration;
            if (isFirstTick || remainingUseDuration < useDuration - 10 && remainingUseDuration % 2 == 0) {
                if (BundleItemHandler.dropContents(stack, player)) {
                    level.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS,
                            0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
                    player.awardStat(Stats.ITEM_USED.get(this));
                }
            }
        }
    }
}
