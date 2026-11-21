package com.coolfreeze23.vbparity.variants.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Wolf;

/**
 * VARIANTS-11: port of native 26.2 {@code BabyWolfModel} (32x32) and the {@code WolfModel} animation it inherits
 * (with {@code ageScale} 0.5). The empty real_head, upper_body and real_tail parts only satisfy the 1.21.1
 * {@code WolfModel} constructor. Each frame starts from the rest pose, like native {@code Model.setupAnim}.
 */
public class BabyWolfModel extends WolfModel<Wolf> {
    private static final float AGE_SCALE = 0.5F;
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart tail;
    private int tint = -1;
    private boolean angry;
    private boolean sitting;
    private float headRoll;
    private float bodyRoll;
    private float tailRoll;
    private float limbSwing;
    private float limbSwingAmount;

    public BabyWolfModel(ModelPart root) {
        super(root);
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 12)
                .addBox(-2.99F, -3.25F, -3.0F, 6.0F, 5.0F, 5.0F, new CubeDeformation(0.025F))
                .texOffs(17, 12)
                .addBox(-1.5F, -0.24F, -5.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 18.25F, -4.0F)
        );
        head.addOrReplaceChild(
            "right_ear", CubeListBuilder.create().texOffs(0, 5).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F), PartPose.offset(-2.0F, -4.25F, -0.5F)
        );
        head.addOrReplaceChild(
            "left_ear", CubeListBuilder.create().texOffs(20, 5).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F), PartPose.offset(2.0F, -4.25F, -0.5F)
        );
        head.addOrReplaceChild("real_head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -2.0F, -4.0F, 6.0F, 4.0F, 8.0F), PartPose.offset(0.0F, 19.0F, 0.0F));
        root.addOrReplaceChild("upper_body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(
            "right_hind_leg", CubeListBuilder.create().texOffs(0, 22).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(-1.5F, 21.0F, 3.0F)
        );
        root.addOrReplaceChild(
            "left_hind_leg", CubeListBuilder.create().texOffs(8, 22).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(1.5F, 21.0F, 3.0F)
        );
        root.addOrReplaceChild(
            "right_front_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(-1.5F, 21.0F, -3.0F)
        );
        root.addOrReplaceChild(
            "left_front_leg", CubeListBuilder.create().texOffs(20, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(1.5F, 21.0F, -3.0F)
        );
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 19.0F, 3.0F, -0.5236F, 0.0F, 0.0F));
        tail.addOrReplaceChild(
            "tail_r1",
            CubeListBuilder.create().texOffs(22, 16).addBox(-1.0F, -5.7F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, -0.6F, 0.2F, -3.1F, 0.0F, 0.0F)
        );
        tail.addOrReplaceChild("real_tail", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setColor(int color) {
        super.setColor(color);
        this.tint = color;
    }

    @Override
    public void prepareMobModel(Wolf entity, float limbSwing, float limbSwingAmount, float partialTick) {
        this.angry = entity.isAngry();
        this.sitting = entity.isInSittingPose();
        this.headRoll = entity.getHeadRollAngle(partialTick) + entity.getBodyRollAngle(partialTick, 0.0F);
        this.bodyRoll = entity.getBodyRollAngle(partialTick, -0.16F);
        this.tailRoll = entity.getBodyRollAngle(partialTick, -0.2F);
        this.limbSwing = limbSwing;
        this.limbSwingAmount = limbSwingAmount;
    }

    /** {@code ageInTicks} is the renderer's bob, which {@code WolfRenderer} sets to the tail angle. */
    @Override
    public void setupAnim(Wolf entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        float pos = this.limbSwing;
        float speed = this.limbSwingAmount;
        this.tail.yRot = this.angry ? 0.0F : Mth.cos(pos * 0.6662F) * 1.4F * speed;
        if (this.sitting) {
            this.body.y += 4.0F * AGE_SCALE;
            this.body.z -= 2.0F * AGE_SCALE;
            this.body.xRot = (float) (Math.PI / 4);
            this.tail.y += 9.0F * AGE_SCALE;
            this.tail.z -= 2.0F * AGE_SCALE;
            this.rightHindLeg.y += 6.7F * AGE_SCALE;
            this.rightHindLeg.z -= 5.0F * AGE_SCALE;
            this.rightHindLeg.xRot = (float) (Math.PI * 3.0 / 2.0);
            this.leftHindLeg.y += 6.7F * AGE_SCALE;
            this.leftHindLeg.z -= 5.0F * AGE_SCALE;
            this.leftHindLeg.xRot = (float) (Math.PI * 3.0 / 2.0);
            this.rightFrontLeg.xRot = 5.811947F;
            this.rightFrontLeg.x += 0.01F * AGE_SCALE;
            this.rightFrontLeg.y += 1.0F * AGE_SCALE;
            this.leftFrontLeg.xRot = 5.811947F;
            this.leftFrontLeg.x -= 0.01F * AGE_SCALE;
            this.leftFrontLeg.y += 1.0F * AGE_SCALE;
            this.body.xRot--;
        } else {
            this.rightHindLeg.xRot = Mth.cos(pos * 0.6662F) * 1.4F * speed;
            this.leftHindLeg.xRot = Mth.cos(pos * 0.6662F + (float) Math.PI) * 1.4F * speed;
            this.rightFrontLeg.xRot = Mth.cos(pos * 0.6662F + (float) Math.PI) * 1.4F * speed;
            this.leftFrontLeg.xRot = Mth.cos(pos * 0.6662F) * 1.4F * speed;
        }

        this.body.zRot = this.bodyRoll;
        this.head.zRot = this.headRoll;
        this.tail.zRot = this.tailRoll;
        this.head.xRot = headPitch * (float) (Math.PI / 180.0);
        this.head.yRot = netHeadYaw * (float) (Math.PI / 180.0);
        this.tail.xRot = ageInTicks;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        int tinted = FastColor.ARGB32.multiply(color, this.tint);
        this.headParts().forEach(part -> part.render(poseStack, buffer, packedLight, packedOverlay, tinted));
        this.bodyParts().forEach(part -> part.render(poseStack, buffer, packedLight, packedOverlay, tinted));
    }
}
