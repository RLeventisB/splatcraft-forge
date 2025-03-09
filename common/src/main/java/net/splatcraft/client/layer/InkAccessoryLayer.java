package net.splatcraft.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

public class InkAccessoryLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
{
	HumanoidModel<AbstractClientPlayer> MODEL;
	public InkAccessoryLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer, HumanoidModel<AbstractClientPlayer> model)
	{
		super(renderer);
		MODEL = model;
	}
	@Override
	public void render(@NotNull PoseStack matrixStack, @NotNull MultiBufferSource iRenderTypeBuffer, int i, @NotNull AbstractClientPlayer entity, float v, float v1, float v2, float v3, float v4, float v5)
	{
		if (!EntityInfoCapability.hasCapability(entity))
			return;
		EntityInfo info = EntityInfoCapability.get(entity);
		ItemStack inkBand = info.getInkBand();
		
		if (!inkBand.isEmpty() && (ItemStack.isSameItem(entity.getMainHandItem(), inkBand) || ItemStack.isSameItem(entity.getOffhandItem(), inkBand)))
			return;
		
		boolean isFoil = inkBand.hasFoil();
		ResourceLocation stackLoc = BuiltInRegistries.ITEM.getKey(inkBand.getItem());
		
		String customModelData = "";
		
		if (inkBand.has(DataComponents.CUSTOM_MODEL_DATA) && Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(stackLoc.getNamespace(), "textures/models/" + stackLoc.getPath() + "_" + inkBand.get(DataComponents.CUSTOM_MODEL_DATA) + ".png")).isPresent())
		{
			customModelData = "_" + inkBand.get(DataComponents.CUSTOM_MODEL_DATA);
		}
		
		ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(stackLoc.getNamespace(), "textures/models/" + stackLoc.getPath() + customModelData + ".png");
		ResourceLocation coloredTexture = ResourceLocation.fromNamespaceAndPath(stackLoc.getNamespace(), "textures/models/" + stackLoc.getPath() + customModelData + "_colored.png");
		
		MODEL.leftArm.visible = entity.getMainArm() == HumanoidArm.LEFT;
		MODEL.leftLeg.visible = entity.getMainArm() == HumanoidArm.LEFT;
		MODEL.rightArm.visible = entity.getMainArm() == HumanoidArm.RIGHT;
		MODEL.rightLeg.visible = entity.getMainArm() == HumanoidArm.RIGHT;
		
		InkColor color = ColorUtils.getColorLockedIfConfig(ColorUtils.getEntityColor(entity));
		
		if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent())
		{
			getParentModel().copyPropertiesTo(MODEL);
			render(matrixStack, iRenderTypeBuffer, i, isFoil, MODEL, -1, texture);
			if (Minecraft.getInstance().getResourceManager().getResource(coloredTexture).isPresent())
				render(matrixStack, iRenderTypeBuffer, i, isFoil, MODEL, color.getColorWithAlpha(255), coloredTexture);
		}
	}
	private void render(PoseStack p_241738_1_, MultiBufferSource p_241738_2_, int p_241738_3_, boolean isFoil, HumanoidModel<AbstractClientPlayer> p_241738_6_, int color, ResourceLocation armorResource)
	{
		VertexConsumer ivertexbuilder = ItemRenderer.getArmorFoilBuffer(p_241738_2_, RenderType.armorCutoutNoCull(armorResource), isFoil);
		p_241738_6_.renderToBuffer(p_241738_1_, ivertexbuilder, p_241738_3_, OverlayTexture.NO_OVERLAY, color);
	}
}
