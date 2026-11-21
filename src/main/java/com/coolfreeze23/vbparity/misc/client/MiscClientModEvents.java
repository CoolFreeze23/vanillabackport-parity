package com.coolfreeze23.vbparity.misc.client;

import com.blackgear.vanillabackport.common.api.modules.bundle_ui.BundleFeatures;
import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import java.io.IOException;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "vbparity", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MiscClientModEvents {
    private MiscClientModEvents() {}

    /**
     * 26.2 renders copper bars and copper chains like iron bars and the iron chain (one layer for all bars). In 1.21.1
     * iron bars and chains are {@code cutout_mipped}; VB registered its copper versions as plain {@code cutout}.
     * Enqueued work runs after every mod's client-setup handler, so this lands after VB's registration.
     */
    /** MISC-27: the native cloud renderer's shader and its clouds.png reader. */
    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath("vbparity", "native_clouds"), DefaultVertexFormat.POSITION_COLOR),
                NativeCloudRenderer.INSTANCE::setShader);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(NativeCloudRenderer.INSTANCE);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModBlocks.COPPER_BARS.forEach(block -> ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutoutMipped()));
            ModBlocks.COPPER_CHAIN.forEach(block -> ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutoutMipped()));
        });
    }

    /**
     * Native ItemUseAnimation.BUNDLE: while a bundle is being used, the first-person arm does the normal arm
     * transform plus the attack swing (ItemInHandRenderer: {@code case BUNDLE -> swingArm(attack, ...)}).
     * 1.21.1 has no BUNDLE use animation, so this is done through the item's client extension.
     */
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        List<Item> bundles = new ArrayList<>();
        bundles.add(Items.BUNDLE);
        bundles.addAll(BundleFeatures.BUNDLES_BY_DYE.values());
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand,
                                                   float partialTick, float equipProcess, float swingProcess) {
                if (!BundleFeatures.onBundleUpdate()) {
                    return false;
                }
                InteractionHand hand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                if (!player.isUsingItem() || player.getUseItemRemainingTicks() <= 0 || player.getUsedItemHand() != hand) {
                    return false;
                }
                int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
                // applyItemArmTransform
                poseStack.translate(invert * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
                // swingArm
                float x = -0.4F * Mth.sin(Mth.sqrt(swingProcess) * (float) Math.PI);
                float y = 0.2F * Mth.sin(Mth.sqrt(swingProcess) * (float) (Math.PI * 2));
                float z = -0.2F * Mth.sin(swingProcess * (float) Math.PI);
                poseStack.translate(invert * x, y, z);
                // applyItemArmAttackTransform
                float f = Mth.sin(swingProcess * swingProcess * (float) Math.PI);
                poseStack.mulPose(Axis.YP.rotationDegrees(invert * (45.0F + f * -20.0F)));
                float f1 = Mth.sin(Mth.sqrt(swingProcess) * (float) Math.PI);
                poseStack.mulPose(Axis.ZP.rotationDegrees(invert * f1 * -20.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(f1 * -80.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(invert * -45.0F));
                return true;
            }
        }, bundles.toArray(Item[]::new));
    }
}
