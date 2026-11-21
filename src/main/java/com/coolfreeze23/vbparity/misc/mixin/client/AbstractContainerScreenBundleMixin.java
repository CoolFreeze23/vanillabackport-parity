package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.common.api.modules.bundle_ui.BundleFeatures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Native AbstractContainerScreen: the bundle tooltip (showTooltipWithItemInHand) stays visible while a stack is carried. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenBundleMixin<T extends AbstractContainerMenu> extends Screen {
    @Shadow
    @Final
    protected T menu;

    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    protected abstract List<Component> getTooltipFromContainerItem(ItemStack stack);

    protected AbstractContainerScreenBundleMixin(Component title) {
        super(title);
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void vbp$bundleTooltipWithItemInHand(GuiGraphics graphics, int x, int y, CallbackInfo ci) {
        if (BundleFeatures.onBundleUpdate() && !this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack stack = this.hoveredSlot.getItem();
            if (stack.getTooltipImage().map(image -> image instanceof BundleTooltip).orElse(false)) {
                graphics.renderTooltip(this.font, this.getTooltipFromContainerItem(stack), stack.getTooltipImage(), stack, x, y);
                ci.cancel();
            }
        }
    }
}
