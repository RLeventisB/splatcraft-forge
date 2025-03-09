package net.splatcraft.client.layer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.UUID;

public class PlayerInkColoredSkinLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
{
	public static final HashMap<UUID, ResourceLocation> TEXTURES = new HashMap<>();
	public static final String PATH = "config/skins/";
	PlayerModel<AbstractClientPlayer> MODEL;
	public PlayerInkColoredSkinLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer, PlayerModel<AbstractClientPlayer> model)
	{
		super(renderer);
		MODEL = model;
	}
	@Override
	public void render(@NotNull PoseStack matrixStack, @NotNull MultiBufferSource iRenderTypeBuffer, int i, AbstractClientPlayer entity, float v, float v1, float v2, float v3, float v4, float v5)
	{
		if (entity.isSpectator() || entity.isInvisible() || !EntityInfoCapability.hasCapability(entity) || !TEXTURES.containsKey(entity.getUUID()))
		{
			return;
		}
		
		InkColor color = ColorUtils.getEntityColor(entity);
		
		copyPropertiesFrom(getParentModel(), MODEL);
		render(matrixStack, iRenderTypeBuffer, i, MODEL, color, TEXTURES.get(entity.getUUID()));
	}
	private void render(PoseStack p_241738_1_, MultiBufferSource buffer, int p_241738_3_, PlayerModel<AbstractClientPlayer> p_241738_6_, InkColor color, ResourceLocation armorResource)
	{
		VertexConsumer ivertexbuilder = buffer.getBuffer(RenderType.entityTranslucent(armorResource));
		p_241738_6_.renderToBuffer(p_241738_1_, ivertexbuilder, p_241738_3_, OverlayTexture.NO_OVERLAY, color.getColorWithAlpha(255));
	}
	private void copyPropertiesFrom(PlayerModel<AbstractClientPlayer> from, PlayerModel<AbstractClientPlayer> to)
	{
		from.copyPropertiesTo(to);
		
		to.jacket.copyFrom(from.jacket);
		to.rightSleeve.copyFrom(from.rightSleeve);
		to.leftSleeve.copyFrom(from.leftSleeve);
		to.rightPants.copyFrom(from.rightPants);
		to.leftPants.copyFrom(from.leftPants);
		
		to.jacket.visible = from.jacket.visible;
		to.rightSleeve.visible = from.rightSleeve.visible;
		to.leftSleeve.visible = from.leftSleeve.visible;
		to.rightPants.visible = from.rightPants.visible;
		to.leftPants.visible = from.leftPants.visible;
	}
}
