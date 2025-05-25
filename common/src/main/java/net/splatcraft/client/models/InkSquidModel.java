package net.splatcraft.client.models;// Made with Blockbench 4.7.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class InkSquidModel extends EntityModel<LivingEntity>
{
	// This layer location should be baked with EntityRendererFactory.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Splatcraft.identifierOf("inksquidmodel"), "main");
	private final ModelPart squid;
	private final ModelPart rightLimb;
	private final ModelPart leftLimb;
	public InkSquidModel(ModelPart root)
	{
		squid = root.getChild("squid");
		rightLimb = squid.getChild("RightLimb");
		leftLimb = squid.getChild("LeftLimb");
	}
	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition squid = partdefinition.addOrReplaceChild("squid", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition Body = squid.addOrReplaceChild("Body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
			.texOffs(0, 9).addBox(-6.0F, -5.0F, -6.0F, 12.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
			.texOffs(27, 0).addBox(-5.0F, -4.0F, -8.0F, 10.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
			.texOffs(32, 6).addBox(-4.0F, -3.0F, -10.0F, 8.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
			.texOffs(32, 12).addBox(-2.0F, -2.0F, -12.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		Body.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(18, 19).addBox(-2.5F, -5.0F, -2.0F, 5.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
			.texOffs(0, 19).addBox(-3.0F, -4.5F, -2.25F, 6.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		Body.addOrReplaceChild("tentacles", CubeListBuilder.create().texOffs(56, 0).addBox(-2.6593F, -3.75F, 6.6593F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
			.texOffs(56, 0).addBox(-1.495F, -3.75F, 5.495F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
			.texOffs(56, 0).addBox(-0.1161F, -2.25F, 4.1161F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
			.texOffs(56, 0).addBox(-1.495F, -2.25F, 5.495F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
			.texOffs(56, 0).addBox(-0.1161F, -3.75F, 4.1161F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
			.texOffs(56, 0).addBox(0.9875F, -3.75F, 2.9671F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, 0.0F, -2.25F, 0.0F, -0.7854F, 0.0F));

		squid.addOrReplaceChild("LeftLimb", CubeListBuilder.create().texOffs(0, 23).addBox(0.0F, -3.0F, 0.0F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
			.texOffs(0, 29).addBox(-1.0F, -3.0F, 3.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 0.0F, 2.0F));

		squid.addOrReplaceChild("RightLimb", CubeListBuilder.create().texOffs(10, 23).mirror().addBox(-2.0F, -3.0F, 0.0F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false)
			.texOffs(14, 29).mirror().addBox(-2.0F, -3.0F, 3.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-2.0F, 0.0F, 2.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}
	@Override
	public void setupAnim(@NotNull LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
	{

	}
	@Override
	public void copyPropertiesTo(@NotNull EntityModel<LivingEntity> other)
	{
		super.copyPropertiesTo(other);

		if (other instanceof InkSquidModel otherSquid)
		{
			otherSquid.squid.copyFrom(squid);
			otherSquid.leftLimb.copyFrom(leftLimb);
			otherSquid.rightLimb.copyFrom(rightLimb);
		}
	}
	@Override
	public void prepareMobModel(@NotNull LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTickTime)
	{
		super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTickTime);
		boolean isSwimming = entity.isSwimming();

		if (!entity.isPassenger())
		{
			InkOverlayInfo info = InkOverlayCapability.get(entity);
			Optional<EntityInfo> entityInfoOptional = EntityInfoCapability.getOptional(entity);
			float angle = Float.NaN;
			if (entityInfoOptional.isPresent())
			{
				EntityInfo entityInfo = entityInfoOptional.get();
				if (entityInfo.getClimbedDirection().isPresent())
				{
					Direction climbDirection = entityInfo.getClimbedDirection().get();
					float horizontalMovement = (float) climbDirection.getAxis().choose(entity.getDeltaMovement().z, 0, entity.getDeltaMovement().x) / 10f;
					squid.yRot = climbDirection.toYRot() * Mth.DEG_TO_RAD + horizontalMovement + Mth.PI;
					angle = -Mth.HALF_PI;
				}
				else if (entityInfo.getSquidSurgeState() < 0)
				{
					final float x = EntityInfo.SQUID_SURGE_ENDLAG + entityInfo.getSquidSurgeState() + partialTickTime;
					// i wanted the squid to rotate linearly and then smoothly do one last rotation so thanks google for providing me with https://www.integral-calculator.com/ made by david scherfgen ig
					// yes this only works if linearTime is half of totalTime and finalLinearValue is one third of startValue, dont ask why
					final float startValue = Mth.PI * 3;
					final float finalLinearValue = Mth.PI;
					final float linearTime = EntityInfo.SQUID_SURGE_ENDLAG / 2;
					final float totalTime = EntityInfo.SQUID_SURGE_ENDLAG;
					final float m = (finalLinearValue - startValue) / linearTime;
					if (x < linearTime)
					{
						squid.yRot = startValue - m * x;
					}
					else
					{
						squid.yRot = finalLinearValue + (m * (x - linearTime) * (x - 2 * totalTime + linearTime)) / (2 * (totalTime - linearTime));
					}
					angle = -Mth.HALF_PI * (Mth.sqrt(1 - x / EntityInfo.SQUID_SURGE_ENDLAG));
				}
			}

			if (Float.isNaN(angle))
			{
				angle = isSwimming ? -(entity.getXRot() * Mth.DEG_TO_RAD) : -Mth.lerp(partialTickTime, info.getPreviousSquidPitch(), info.getSquidPitch()) * 1.1f;
				squid.yRot = 0;
			}
			squid.xRot = Mth.clamp(angle, -Mth.HALF_PI, Mth.HALF_PI);
		}

		if (entity.onGround() || isSwimming)
		{
			rightLimb.yRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount / (isSwimming ? 2.2f : 1.5f);
			leftLimb.yRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount / (isSwimming ? 2.2f : 1.5f);
		}
		else
		{
			if (Math.abs(Math.round(rightLimb.yRot * 100)) != 0)
			{
				rightLimb.yRot -= rightLimb.yRot / 8f;
			}
			if (Math.abs(Math.round(leftLimb.yRot * 100)) != 0)
			{
				leftLimb.yRot -= leftLimb.yRot / 8f;
			}
		}
	}
	@Override
	public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color)
	{
		squid.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}
}