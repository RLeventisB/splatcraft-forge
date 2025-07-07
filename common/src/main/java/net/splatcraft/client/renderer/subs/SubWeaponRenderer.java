package net.splatcraft.client.renderer.subs;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.splatcraft.client.models.AbstractSubWeaponModel;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.items.weapons.subs.SubWeaponItem;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SubWeaponRenderer<E extends AbstractSubWeaponEntity, M extends AbstractSubWeaponModel<E>> extends EntityRenderer<E>
{
	protected SubWeaponRenderer(EntityRendererProvider.Context context)
	{
		super(context);
	}
	@Override
	public void render(E entity, float entityYaw, float partialTicks, @NotNull PoseStack matrixStackIn, @NotNull MultiBufferSource bufferIn, int packedLightIn)
	{
		InkColor color = ColorUtils.getColorLockedIfConfig(entity.getColor());
		
		int rgba = color.getColor();
		
		M model = getModel();
		ResourceLocation texture = getTextureLocation(entity);
		ResourceLocation inkTexture = getInkTextureLocation(entity);
		ResourceLocation overlay = getOverlayTexture(entity);
		
		ItemStack stack = entity.getItem();
		if (stack.getItem() instanceof SubWeaponItem sub && entity.getType().equals(sub.getEntityType(stack)))
		{
			ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(sub);
			String customModelData = "";
			
			if (stack.has(DataComponents.CUSTOM_MODEL_DATA))
			{
				CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
				if (Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(registryName.getNamespace(),
					"textures/models/" + registryName.getPath() + "_" + modelData.value() + ".png")).isPresent())
				{
					customModelData = "_" + modelData.value();
				}
			}
			
			texture = ResourceLocation.fromNamespaceAndPath(registryName.getNamespace(), "textures/item/weapons/sub/" + registryName.getPath() + customModelData + ".png");
			inkTexture = ResourceLocation.fromNamespaceAndPath(registryName.getNamespace(), "textures/item/weapons/sub/" + registryName.getPath() + customModelData + "_ink.png");
			
			if (overlay != null)
				overlay = ResourceLocation.fromNamespaceAndPath(registryName.getNamespace(), "textures/item/weapons/sub/" + registryName.getPath() + customModelData + "_overlay.png");
		}
		
		model.prepareMobModel(entity, 0, 0, partialTicks);
		int i = OverlayTexture.pack(OverlayTexture.u(getOverlayProgress(entity, partialTicks)), OverlayTexture.v(false));
		model.renderToBuffer(matrixStackIn, bufferIn.getBuffer(model.renderType(inkTexture)), packedLightIn, i, rgba);
		model.renderToBuffer(matrixStackIn, bufferIn.getBuffer(model.renderType(texture)), packedLightIn, i, 0xFFFFFF);
		
		if (overlay != null)
		{
			int overlayRgb = getOverlayColor(entity, partialTicks);
			model.renderToBuffer(matrixStackIn, bufferIn.getBuffer(model.renderType(overlay)), packedLightIn, i, overlayRgb);
		}
		
		super.render(entity, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
	}
	protected float getOverlayProgress(E entity, float partialTicks)
	{
		return 0;
	}
	public abstract M getModel();
	public abstract ResourceLocation getInkTextureLocation(E entity);
	@Nullable
	public ResourceLocation getOverlayTexture(E entity)
	{
		return null;
	}
	public int getOverlayColor(E entity, float partialTicks)
	{
		return 0xFFFFFFFF;
	}
	protected float handleRotationFloat(E livingBase, float partialTicks)
	{
		return (float) livingBase.tickCount + partialTicks;
	}
}
