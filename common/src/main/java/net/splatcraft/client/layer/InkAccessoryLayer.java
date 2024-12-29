package net.splatcraft.client.layer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.splatcraft.data.capabilities.playerinfo.EntityInfo;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

public class InkAccessoryLayer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>
{
	BipedEntityModel<AbstractClientPlayerEntity> MODEL;
	public InkAccessoryLayer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> renderer, BipedEntityModel<AbstractClientPlayerEntity> model)
	{
		super(renderer);
		MODEL = model;
	}
	@Override
	public void render(@NotNull MatrixStack matrixStack, @NotNull VertexConsumerProvider iRenderTypeBuffer, int i, @NotNull AbstractClientPlayerEntity entity, float v, float v1, float v2, float v3, float v4, float v5)
	{
		if (!EntityInfoCapability.hasCapability(entity))
			return;
		EntityInfo info = EntityInfoCapability.get(entity);
		ItemStack inkBand = info.getInkBand();
		
		if (!inkBand.isEmpty() && (ItemStack.areItemsEqual(entity.getMainHandStack(), inkBand) || ItemStack.areItemsEqual(entity.getOffHandStack(), inkBand)))
			return;
		
		boolean isFoil = inkBand.hasGlint();
		Identifier stackLoc = Registries.ITEM.getId(inkBand.getItem());
		
		String customModelData = "";
		
		if (inkBand.contains(DataComponentTypes.CUSTOM_MODEL_DATA) && MinecraftClient.getInstance().getResourceManager().getResource(Identifier.of(stackLoc.getNamespace(), "textures/models/" + stackLoc.getPath() + "_" + inkBand.get(DataComponentTypes.CUSTOM_MODEL_DATA) + ".png")).isPresent())
		{
			customModelData = "_" + inkBand.get(DataComponentTypes.CUSTOM_MODEL_DATA);
		}
		
		Identifier texture = Identifier.of(stackLoc.getNamespace(), "textures/models/" + stackLoc.getPath() + customModelData + ".png");
		Identifier coloredTexture = Identifier.of(stackLoc.getNamespace(), "textures/models/" + stackLoc.getPath() + customModelData + "_colored.png");
		
		MODEL.leftArm.visible = entity.getMainArm() == Arm.LEFT;
		MODEL.leftLeg.visible = entity.getMainArm() == Arm.LEFT;
		MODEL.rightArm.visible = entity.getMainArm() == Arm.RIGHT;
		MODEL.rightLeg.visible = entity.getMainArm() == Arm.RIGHT;
		
		InkColor color = ColorUtils.getColorLockedIfConfig(ColorUtils.getEntityColor(entity));
		
		if (MinecraftClient.getInstance().getResourceManager().getResource(texture).isPresent())
		{
			getContextModel().copyBipedStateTo(MODEL);
			render(matrixStack, iRenderTypeBuffer, i, isFoil, MODEL, -1, texture);
			if (MinecraftClient.getInstance().getResourceManager().getResource(coloredTexture).isPresent())
				render(matrixStack, iRenderTypeBuffer, i, isFoil, MODEL, color.getColorWithAlpha(255), coloredTexture);
		}
	}
	private void render(MatrixStack p_241738_1_, VertexConsumerProvider p_241738_2_, int p_241738_3_, boolean isFoil, BipedEntityModel<AbstractClientPlayerEntity> p_241738_6_, int color, Identifier armorResource)
	{
		VertexConsumer ivertexbuilder = ItemRenderer.getArmorGlintConsumer(p_241738_2_, RenderLayer.getArmorCutoutNoCull(armorResource), isFoil);
		p_241738_6_.render(p_241738_1_, ivertexbuilder, p_241738_3_, OverlayTexture.DEFAULT_UV, color);
	}
}
