package com.coolfreeze23.vbparity.copper.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * COPPER-27: native ItemStackRenderState.getModelBoundingBox, the box of every model vertex after the display transform
 * and the -0.5 item offset (native: each layer's extents through applyTransform). In 1.21.1 the vertices are collected by
 * rendering the item once into a recording buffer with an identity pose, which covers baked quads and builtin-entity
 * renderers alike. An empty or failed render gives native's empty box at the origin.
 */
public final class ShelfItemBounds {
    private static final Map<BakedModel, AABB> BAKED = new WeakHashMap<>();

    private ShelfItemBounds() {
    }

    public static AABB of(ItemRenderer renderer, ItemStack stack, ItemDisplayContext context, BakedModel model) {
        if (!model.isCustomRenderer()) {
            AABB cached = BAKED.get(model);
            if (cached != null) {
                return cached;
            }
            AABB box = measure(renderer, stack, context, model);
            BAKED.put(model, box);
            return box;
        }
        return measure(renderer, stack, context, model);
    }

    private static AABB measure(ItemRenderer renderer, ItemStack stack, ItemDisplayContext context, BakedModel model) {
        Recorder recorder = new Recorder();
        try {
            MultiBufferSource source = type -> recorder;
            renderer.render(stack, context, false, new PoseStack(), source, 0xF000F0, OverlayTexture.NO_OVERLAY, model);
        } catch (RuntimeException e) {
            return new AABB(0, 0, 0, 0, 0, 0);
        }
        return recorder.defined ? new AABB(recorder.minX, recorder.minY, recorder.minZ, recorder.maxX, recorder.maxY, recorder.maxZ) : new AABB(0, 0, 0, 0, 0, 0);
    }

    private static final class Recorder implements VertexConsumer {
        boolean defined;
        double minX, minY, minZ, maxX, maxY, maxZ;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (!this.defined) {
                this.minX = this.maxX = x;
                this.minY = this.maxY = y;
                this.minZ = this.maxZ = z;
                this.defined = true;
            } else {
                this.minX = Math.min(this.minX, x);
                this.minY = Math.min(this.minY, y);
                this.minZ = Math.min(this.minZ, z);
                this.maxX = Math.max(this.maxX, x);
                this.maxY = Math.max(this.maxY, y);
                this.maxZ = Math.max(this.maxZ, z);
            }
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            return this;
        }
    }
}
