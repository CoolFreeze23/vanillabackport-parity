package com.coolfreeze23.vbparity.copper.mixin.client;

import com.blackgear.vanillabackport.client.level.layer.SimpleEquipmentLayer;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Native SimpleEquipmentLayer renders through EquipmentLayerRenderer: RenderTypes.armorCutoutNoCull (VB: entityCutoutNoCull)
 * and, for an enchanted item, a second pass with armorEntityGlint. The layer's item is the happy ghast's harness (CHEST)
 * or the nautilus' body armour; the saddle texture layer has no item stack of its own here and gets no glint.
 */
@Mixin(SimpleEquipmentLayer.class)
public abstract class SimpleEquipmentLayerMixin {
    @WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;entityCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"))
    private RenderType vbparity$armorRenderType(ResourceLocation texture, Operation<RenderType> original) {
        return RenderType.armorCutoutNoCull(texture);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private void vbparity$foil(
        EntityModel model, PoseStack pose, VertexConsumer vertices, int light, int overlay, Operation<Void> original,
        @Local(argsOnly = true) MultiBufferSource buffer, @Local(argsOnly = true) LivingEntity entity, @Local Optional<ResourceLocation> texture
    ) {
        original.call(model, pose, vertices, light, overlay);
        ItemStack stack = ItemStack.EMPTY;
        if (entity instanceof HappyGhast) {
            stack = entity.getItemBySlot(EquipmentSlot.CHEST);
        } else if (entity instanceof Mob mob && texture.isPresent() && !texture.get().getPath().contains("saddle")) {
            stack = mob.getBodyArmorItem();
        }
        if (stack.hasFoil()) {
            original.call(model, pose, buffer.getBuffer(RenderType.armorEntityGlint()), light, overlay);
        }
    }
}
