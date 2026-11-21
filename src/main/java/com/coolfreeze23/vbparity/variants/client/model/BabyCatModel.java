package com.coolfreeze23.vbparity.variants.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;

/**
 * VARIANTS-11: port of native 26.2 {@code BabyFelineModel}/{@code BabyCatModel} (32x32) with its own animation
 * ({@code ageScale} 0.5). Each frame starts from the rest pose, like native {@code Model.setupAnim}.
 * {@code scale} is 1.0 for the kitten body and 1.01 for the collar ({@code BabyCatModel.COLLAR_TRANSFORMER}).
 */
public class BabyCatModel extends CatModel<Cat> {
    private static final float AGE_SCALE = 0.5F;
    private final ModelPart root;
    private final float scale;
    private boolean crouching;
    private boolean sprinting;
    private boolean sitting;
    private float lieDownAmount;
    private float lieDownAmountTail;
    private float relaxStateOneAmount;

    public BabyCatModel(ModelPart root, float scale) {
        super(root);
        this.root = root;
        this.scale = scale;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.5F, -3.0F, -2.875F, 5.0F, 4.0F, 4.0F)
                .texOffs(18, 0)
                .addBox(-2.0F, -4.0F, -0.875F, 1.0F, 1.0F, 2.0F)
                .texOffs(24, 0)
                .addBox(1.0F, -4.0F, -0.875F, 1.0F, 1.0F, 2.0F)
                .texOffs(18, 3)
                .addBox(-1.5F, -1.0F, -3.875F, 3.0F, 2.0F, 1.0F),
            PartPose.offset(0.0F, 20.0F, -3.125F)
        );
        root.addOrReplaceChild(
            "left_front_leg", CubeListBuilder.create().texOffs(18, 18).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F), PartPose.offset(1.0F, 22.0F, -1.5F)
        );
        root.addOrReplaceChild(
            "right_front_leg", CubeListBuilder.create().texOffs(12, 18).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F), PartPose.offset(-1.0F, 22.0F, -1.5F)
        );
        root.addOrReplaceChild(
            "left_hind_leg", CubeListBuilder.create().texOffs(18, 22).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F), PartPose.offset(1.0F, 22.0F, 2.5F)
        );
        root.addOrReplaceChild(
            "body", CubeListBuilder.create().texOffs(0, 8).addBox(-2.0F, -1.5F, -3.5F, 4.0F, 3.0F, 7.0F), PartPose.offset(0.0F, 20.5F, 0.5F)
        );
        root.addOrReplaceChild(
            "right_hind_leg", CubeListBuilder.create().texOffs(12, 22).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F), PartPose.offset(-1.0F, 22.0F, 2.5F)
        );
        root.addOrReplaceChild(
            "tail1",
            CubeListBuilder.create().texOffs(0, 18).addBox(-0.5F, -0.107F, 0.0849F, 1.0F, 1.0F, 5.0F),
            PartPose.offsetAndRotation(0.0F, 19.107F, 3.9151F, -0.567232F, 0.0F, 0.0F)
        );
        root.addOrReplaceChild("tail2", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void prepareMobModel(Cat entity, float limbSwing, float limbSwingAmount, float partialTick) {
        this.crouching = entity.isCrouching();
        this.sprinting = entity.isSprinting();
        this.sitting = entity.isInSittingPose();
        this.lieDownAmount = entity.getLieDownAmount(partialTick);
        this.lieDownAmountTail = entity.getLieDownAmountTail(partialTick);
        this.relaxStateOneAmount = entity.getRelaxStateOneAmount(partialTick);
    }

    @Override
    public void setupAnim(Cat entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        if (this.crouching) {
            this.body.y += 1.0F * AGE_SCALE;
            this.head.y += 2.0F * AGE_SCALE;
            this.tail1.y += 1.0F * AGE_SCALE;
            this.tail2.y += -4.0F * AGE_SCALE;
            this.tail2.z += 2.0F * AGE_SCALE;
            this.tail1.xRot = (float) (Math.PI / 2);
            this.tail2.xRot = (float) (Math.PI / 2);
        } else if (this.sprinting) {
            this.tail2.y = this.tail1.y;
            this.tail2.z += 2.0F * AGE_SCALE;
            this.tail1.xRot = (float) (Math.PI / 2);
            this.tail2.xRot = (float) (Math.PI / 2);
        }

        this.head.xRot = headPitch * (float) (Math.PI / 180.0);
        this.head.yRot = netHeadYaw * (float) (Math.PI / 180.0);
        if (!this.sitting) {
            float speed = limbSwingAmount;
            float pos = limbSwing;
            if (this.sprinting) {
                this.leftHindLeg.xRot = Mth.cos(pos * 0.6662F) * speed;
                this.rightHindLeg.xRot = Mth.cos(pos * 0.6662F + 0.3F) * speed;
                this.leftFrontLeg.xRot = Mth.cos(pos * 0.6662F + (float) Math.PI + 0.3F) * speed;
                this.rightFrontLeg.xRot = Mth.cos(pos * 0.6662F + (float) Math.PI) * speed;
                this.tail2.xRot = 1.7278761F + (float) (Math.PI / 10) * Mth.cos(pos) * speed;
            } else {
                this.leftHindLeg.xRot = Mth.cos(pos * 0.6662F) * speed;
                this.rightHindLeg.xRot = Mth.cos(pos * 0.6662F + (float) Math.PI) * speed;
                this.leftFrontLeg.xRot = Mth.cos(pos * 0.6662F + (float) Math.PI) * speed;
                this.rightFrontLeg.xRot = Mth.cos(pos * 0.6662F) * speed;
                if (!this.crouching) {
                    this.tail2.xRot = 1.7278761F + (float) (Math.PI / 4) * Mth.cos(pos) * speed;
                } else {
                    this.tail2.xRot = 1.7278761F + 0.47123894F * Mth.cos(pos) * speed;
                }
            }
        } else {
            this.body.xRot += -0.43633232F;
            this.body.y++;
            this.head.z += 0.75F;
            this.tail1.xRot += 0.5454154F;
            this.tail1.y += 4.0F;
            this.tail1.z -= 0.9F;
            this.leftHindLeg.z -= 0.9F;
            this.rightHindLeg.z -= 0.9F;
        }

        if (this.lieDownAmount > 0.0F) {
            this.body.x++;
            this.head.xRot = Mth.rotLerp(this.lieDownAmount, this.head.xRot, (float) (Math.PI / 18));
            this.head.zRot = Mth.rotLerp(this.lieDownAmount, this.head.zRot, (float) (-Math.PI * 5.0 / 12.0));
            this.head.x++;
            this.head.y += 0.75F;
            this.head.z -= 0.5F;
            this.rightFrontLeg.xRot = (float) (-Math.PI / 4);
            this.rightFrontLeg.x += 3.5F;
            this.rightFrontLeg.y -= 0.5F;
            this.rightFrontLeg.z += 0.0F;
            this.leftFrontLeg.xRot = (float) (-Math.PI / 2);
            this.leftFrontLeg.x++;
            this.leftFrontLeg.y--;
            this.leftFrontLeg.z -= 2.0F;
            this.rightHindLeg.xRot = (float) (Math.PI * 2.0 / 9.0);
            this.rightHindLeg.yRot = (float) (Math.PI / 9);
            this.rightHindLeg.zRot = (float) (-Math.PI / 9);
            this.rightHindLeg.x += 2.5F;
            this.rightHindLeg.y -= 0.25F;
            this.rightHindLeg.z += 0.5F;
            this.leftHindLeg.x++;
            this.leftHindLeg.z--;
            this.tail1.xRot = this.tail1.xRot + Mth.rotLerp(this.lieDownAmountTail, this.tail1.xRot, (float) (-Math.PI / 6));
            this.tail1.yRot = this.tail1.yRot + Mth.rotLerp(this.lieDownAmountTail, this.tail1.yRot, 0.0F);
            this.tail1.zRot = this.tail1.zRot + Mth.rotLerp(this.lieDownAmountTail, this.tail1.zRot, (float) (-Math.PI / 18));
            this.tail1.x++;
            this.tail1.y += 0.5F;
            this.tail1.z -= 0.25F;
        }

        if (this.relaxStateOneAmount > 0.0F) {
            this.head.xRot = Mth.rotLerp(this.relaxStateOneAmount, this.head.xRot, -0.58177644F);
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        poseStack.pushPose();
        if (this.scale != 1.0F) {
            // MeshTransformer.scaling: root pose scaled and moved down 24.016 * (1 - factor) pixels.
            poseStack.translate(0.0F, 24.016F * (1.0F - this.scale) / 16.0F, 0.0F);
            poseStack.scale(this.scale, this.scale, this.scale);
        }
        this.headParts().forEach(part -> part.render(poseStack, buffer, packedLight, packedOverlay, color));
        this.bodyParts().forEach(part -> part.render(poseStack, buffer, packedLight, packedOverlay, color));
        poseStack.popPose();
    }
}
