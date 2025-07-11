package net.splatcraft.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.inktanks.AbstractInkTankModel;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.items.InkTankItem;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class InkTankFeature<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M>
{
	private static final Map<InkTankItem, Tuple<ModelLayerLocation, Function<ModelPart, AbstractInkTankModel>>> MAP = new HashMap<>();
	private static final Map<InkTankItem, AbstractInkTankModel> MODEL_CACHE = new HashMap<>();
	private final EntityModelSet modelLoader;
	private String id;
	public InkTankFeature(RenderLayerParent<T, M> context, EntityModelSet modelLoader)
	{
		super(context);
		this.modelLoader = modelLoader;
	}
	public static void register(InkTankItem item, ModelLayerLocation layer, Function<ModelPart, AbstractInkTankModel> constructor)
	{
		MAP.put(item, new Tuple<>(layer, constructor));
	}
	@Override
	public void render(@NotNull PoseStack matrixStack, @NotNull MultiBufferSource provider, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch)
	{
		ItemStack itemStack = entity.getItemBySlot(EquipmentSlot.CHEST);
		if (itemStack.getItem() instanceof InkTankItem item)
		{
			AbstractInkTankModel model = MODEL_CACHE.getOrDefault(item, createModel(item));
			matrixStack.pushPose();
			
			getParentModel().copyPropertiesTo((EntityModel<T>) model);
			model.setInkLevels(InkTankItem.getInkPercentage(itemStack));
			model.setupAnim(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
			model.notifyState(getParentModel());
			
			VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(provider, RenderType.entityTranslucent(
				Splatcraft.identifierOf("textures/item/tanks/" + id + "_layer_1_overlay.png")
			), itemStack.hasFoil());
			model.renderToBuffer(matrixStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, -1);
			
			matrixStack.popPose();
			matrixStack.pushPose();
			
			vertexConsumer = provider.getBuffer(RenderType.entityTranslucent(
				Splatcraft.identifierOf("textures/item/tanks/" + id + "_layer_1.png")
			));
			model.renderToBuffer(matrixStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, EntityInfoCapability.get(entity).getColor().getColorWithAlpha(255));
			matrixStack.popPose();
		}
	}
	private AbstractInkTankModel createModel(InkTankItem item)
	{
		Tuple<ModelLayerLocation, Function<ModelPart, AbstractInkTankModel>> data = MAP.get(item);
		id = data.getA().getModel().getPath();
		AbstractInkTankModel model = data.getB().apply(modelLoader.bakeLayer(data.getA()));
		MODEL_CACHE.put(item, model);
		return model;
	}
}
