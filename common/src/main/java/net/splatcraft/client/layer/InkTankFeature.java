package net.splatcraft.client.layer;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.inktanks.AbstractInkTankModel;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.items.InkTankItem;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class InkTankFeature<T extends LivingEntity, M extends EntityModel<T>> extends FeatureRenderer<T, M>
{
	private static final Map<InkTankItem, Pair<EntityModelLayer, Function<ModelPart, AbstractInkTankModel>>> MAP = new HashMap<>();
	private static final Map<InkTankItem, AbstractInkTankModel> MODEL_CACHE = new HashMap<>();
	private final EntityModelLoader modelLoader;
	private String id;
	public InkTankFeature(FeatureRendererContext<T, M> context, EntityModelLoader modelLoader)
	{
		super(context);
		this.modelLoader = modelLoader;
	}
	public static void register(InkTankItem item, EntityModelLayer layer, Function<ModelPart, AbstractInkTankModel> constructor)
	{
		MAP.put(item, new Pair<>(layer, constructor));
	}
	@Override
	public void render(MatrixStack matrixStack, VertexConsumerProvider provider, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch)
	{
		ItemStack itemStack = entity.getEquippedStack(EquipmentSlot.CHEST);
		if (itemStack.getItem() instanceof InkTankItem item)
		{
			AbstractInkTankModel model = MODEL_CACHE.getOrDefault(item, createModel(item));
			matrixStack.push();
			
			getContextModel().copyStateTo((EntityModel<T>) model);
			model.setInkLevels(InkTankItem.getInkAmount(itemStack) / item.capacity);
			model.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
			model.notifyState(getContextModel());
			
			VertexConsumer vertexConsumer = ItemRenderer.getArmorGlintConsumer(provider, RenderLayer.getEntityTranslucent(
				Splatcraft.identifierOf("textures/item/tanks/" + id + "_layer_1_overlay.png")
			), itemStack.hasGlint());
			model.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV, -1);
			
			matrixStack.pop();
			matrixStack.push();
			
			vertexConsumer = provider.getBuffer(RenderLayer.getEntityTranslucent(
				Splatcraft.identifierOf("textures/item/tanks/" + id + "_layer_1.png")
			));
			model.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV, EntityInfoCapability.get(entity).getColor().getColorWithAlpha(255));
			matrixStack.pop();
		}
	}
	private AbstractInkTankModel createModel(InkTankItem item)
	{
		Pair<EntityModelLayer, Function<ModelPart, AbstractInkTankModel>> data = MAP.get(item);
		id = data.getLeft().getId().getPath();
		AbstractInkTankModel model = data.getRight().apply(modelLoader.getModelPart(data.getLeft()));
		MODEL_CACHE.put(item, model);
		return model;
	}
}
