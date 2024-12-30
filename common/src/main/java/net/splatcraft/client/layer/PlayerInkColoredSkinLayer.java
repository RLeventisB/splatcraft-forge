package net.splatcraft.client.layer;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public class PlayerInkColoredSkinLayer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>
{
	public static final HashMap<UUID, Identifier> TEXTURES = new HashMap<>();
	public static final String PATH = "config/skins/";
	PlayerEntityModel<AbstractClientPlayerEntity> MODEL;
	public PlayerInkColoredSkinLayer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> renderer, PlayerEntityModel<AbstractClientPlayerEntity> model)
	{
		super(renderer);
		MODEL = model;
	}
	@Override
	public void render(@NotNull MatrixStack matrixStack, @NotNull VertexConsumerProvider iRenderTypeBuffer, int i, AbstractClientPlayerEntity entity, float v, float v1, float v2, float v3, float v4, float v5)
	{
		if (entity.isSpectator() || entity.isInvisible() || !EntityInfoCapability.hasCapability(entity) || !TEXTURES.containsKey(entity.getUuid()))
		{
			return;
		}
		
		InkColor color = ColorUtils.getEntityColor(entity);
		
		copyPropertiesFrom(getContextModel(), MODEL);
		render(matrixStack, iRenderTypeBuffer, i, MODEL, color, TEXTURES.get(entity.getUuid()));
	}
	private void render(MatrixStack p_241738_1_, VertexConsumerProvider buffer, int p_241738_3_, PlayerEntityModel<AbstractClientPlayerEntity> p_241738_6_, InkColor color, Identifier armorResource)
	{
		VertexConsumer ivertexbuilder = buffer.getBuffer(RenderLayer.getEntityTranslucent(armorResource));
		p_241738_6_.render(p_241738_1_, ivertexbuilder, p_241738_3_, OverlayTexture.DEFAULT_UV, color.getColorWithAlpha(255));
	}
	private void copyPropertiesFrom(PlayerEntityModel<AbstractClientPlayerEntity> from, PlayerEntityModel<AbstractClientPlayerEntity> to)
	{
		from.copyStateTo(to);
		
		to.jacket.copyTransform(from.jacket);
		to.rightSleeve.copyTransform(from.rightSleeve);
		to.leftSleeve.copyTransform(from.leftSleeve);
		to.rightPants.copyTransform(from.rightPants);
		to.leftPants.copyTransform(from.leftPants);
		
		to.jacket.visible = from.jacket.visible;
		to.rightSleeve.visible = from.rightSleeve.visible;
		to.leftSleeve.visible = from.leftSleeve.visible;
		to.rightPants.visible = from.rightPants.visible;
		to.leftPants.visible = from.leftPants.visible;
	}
}
