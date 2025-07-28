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
import net.splatcraft.data.capabilities.structs.PlayerInfo;
import net.splatcraft.platform.Components;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
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
	public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int light, @NotNull AbstractClientPlayer player, float v, float v1, float v2, float v3, float v4, float v5)
	{
		if (!Components.PLAYER_INFO.has(player))
			return;
		
		PlayerInfo info = Components.PLAYER_INFO.getOrCreate(player);
		ItemStack inkBand = info.inkBand();
		
		if (!inkBand.isEmpty() && (ItemStack.isSameItem(player.getMainHandItem(), inkBand) || ItemStack.isSameItem(player.getOffhandItem(), inkBand)))
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
		
		MODEL.leftArm.visible = player.getMainArm() == HumanoidArm.LEFT;
		MODEL.leftLeg.visible = player.getMainArm() == HumanoidArm.LEFT;
		MODEL.rightArm.visible = player.getMainArm() == HumanoidArm.RIGHT;
		MODEL.rightLeg.visible = player.getMainArm() == HumanoidArm.RIGHT;
		
		InkColor color = ColorUtils.getColorLockedIfConfig(ColorUtils.getEntityColor(player));
		
		if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent())
		{
			getParentModel().copyPropertiesTo(MODEL);
			render(poseStack, buffer, light, isFoil, MODEL, -1, texture);
			if (Minecraft.getInstance().getResourceManager().getResource(coloredTexture).isPresent())
				render(poseStack, buffer, light, isFoil, MODEL, color.getColorWithAlpha(255), coloredTexture);
		}
	}
	private void render(PoseStack poseStack, MultiBufferSource buffer, int light, boolean isFoil, HumanoidModel<AbstractClientPlayer> model, int color, ResourceLocation armorResource)
	{
		VertexConsumer ivertexbuilder = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(armorResource), isFoil);
		model.renderToBuffer(poseStack, ivertexbuilder, light, OverlayTexture.NO_OVERLAY, color);
	}
}
