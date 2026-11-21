package com.coolfreeze23.vbparity.mayhem.mixin.client;

import com.blackgear.vanillabackport.core.data.tags.ModItemTags;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** MAYHEM-37: native spear item models have swap_animation_scale 1.95, which scales the first-person equip dip. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererSpearMixin {
    @ModifyArg(
        method = "renderHandsWithItems",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
        index = 6
    )
    private float vbp$spearSwapScale(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack stack,
                                     float equippedProgress, PoseStack poseStack, MultiBufferSource buffer, int light) {
        return stack.is(ModItemTags.SPEARS) ? equippedProgress * 1.95F : equippedProgress;
    }
}
