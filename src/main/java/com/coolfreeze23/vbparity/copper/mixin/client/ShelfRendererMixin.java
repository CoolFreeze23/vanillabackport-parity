package com.coolfreeze23.vbparity.copper.mixin.client;

import com.blackgear.vanillabackport.client.level.renderer.block_entity.ShelfRenderer;
import com.blackgear.vanillabackport.common.level.block_entities.ShelfBlockEntity;
import com.blackgear.vanillabackport.common.level.blocks.ShelfBlock;
import com.coolfreeze23.vbparity.copper.CopperShelf;
import com.coolfreeze23.vbparity.copper.client.ShelfItemBounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COPPER-27: native ShelfRenderer.submitItem. Each item is placed at (slot - 1) * 0.3125 across, 0.25 out from the centre
 * (VB 0.27), scaled 0.25, rendered in the ON_SHELF context with the model's own on_shelf transform, and raised so its
 * bounding box sits on the shelf bottom or is centred. VB rendered with FIXED and sized items by its own heuristic.
 * This replaces VB's renderer body (VB's own class; nothing else hooks it).
 */
@Mixin(ShelfRenderer.class)
public abstract class ShelfRendererMixin {
    @Shadow @Final private ItemRenderer itemRenderer;

    @Inject(
        method = "render(Lcom/blackgear/vanillabackport/common/level/block_entities/ShelfBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void vbparity$nativeShelf(ShelfBlockEntity shelf, float partialTick, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay, CallbackInfo ci) {
        ci.cancel();
        Direction facing = shelf.getBlockState().getValue(ShelfBlock.FACING);
        float yRot = facing.getAxis().isHorizontal() ? -facing.toYRot() : 180.0F;
        NonNullList<ItemStack> items = shelf.getItems();
        int seed = HashCommon.long2int(shelf.getBlockPos().asLong());
        boolean alignToBottom = shelf.getAlignItemsToBottom();
        ItemDisplayContext context = CopperShelf.onShelf();

        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            BakedModel model = this.itemRenderer.getModel(stack, shelf.getLevel(), null, seed + slot);
            float itemSlotPosition = (slot - 1) * 0.3125F;
            pose.pushPose();
            pose.translate(0.5F, 0.5F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(yRot));
            pose.translate(itemSlotPosition, alignToBottom ? -0.25 : 0.0, -0.25);
            pose.scale(0.25F, 0.25F, 0.25F);
            AABB box = ShelfItemBounds.of(this.itemRenderer, stack, context, model);
            double offsetY = -box.minY;
            if (!alignToBottom) {
                offsetY += -(box.maxY - box.minY) / 2.0;
            }
            pose.translate(0.0, offsetY, 0.0);
            this.itemRenderer.render(stack, context, false, pose, buffer, packedLight, OverlayTexture.NO_OVERLAY, model);
            pose.popPose();
        }
    }
}
