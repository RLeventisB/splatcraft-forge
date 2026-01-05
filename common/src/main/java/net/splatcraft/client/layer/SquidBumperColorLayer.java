package net.splatcraft.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.SquidBumperModel;
import net.splatcraft.entities.SquidBumperEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class SquidBumperColorLayer extends RenderLayer<SquidBumperEntity, SquidBumperModel>
{
	private static final ResourceLocation TEXTURE = Splatcraft.identifierOf("textures/entity/squid_bumper.png");
	private final SquidBumperModel model;
	private final Predicate<SquidBumperEntity> isBodyVisible;
	public SquidBumperColorLayer(RenderLayerParent<SquidBumperEntity, SquidBumperModel> renderer, EntityModelSet modelSet, Predicate<SquidBumperEntity> isBodyVisible)
	{
		super(renderer);
		model = new SquidBumperModel(modelSet.bakeLayer(SquidBumperModel.LAYER_LOCATION));
		this.isBodyVisible = isBodyVisible;
	}
	@Override
	public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, @NotNull SquidBumperEntity entity, float limbSwing, float limbSwingAmount, float partialTickTime, float ageInTicks, float netHeadYaw, float headPitch)
	{
		if (isBodyVisible != null && !isBodyVisible.test(entity)) return;
		
		InkColor color = ColorUtils.getColorLockedIfConfig(ColorUtils.getEntityColor(entity));
		
		getParentModel().copyPropertiesTo(model);
		model.prepareMobModel(entity, limbSwing, limbSwingAmount, headPitch);
		model.setupAnim(entity, limbSwing, limbSwingAmount, partialTickTime, ageInTicks, netHeadYaw);
		
		VertexConsumer ivertexbuilder = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
		model.renderToBuffer(poseStack, ivertexbuilder, packedLight, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), color.getColor());
	}
}
