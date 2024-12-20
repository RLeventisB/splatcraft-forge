package net.splatcraft.client.renderer.subs;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.client.models.AbstractSubWeaponModel;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.items.weapons.SubWeaponItem;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SubWeaponRenderer<E extends AbstractSubWeaponEntity, M extends AbstractSubWeaponModel<E>> extends EntityRenderer<E>
{
    protected SubWeaponRenderer(EntityRendererFactory.Context context)
    {
        super(context);
    }

    @Override
    public void render(E entityIn, float entityYaw, float partialTicks, @NotNull MatrixStack matrixStackIn, @NotNull VertexConsumerProvider bufferIn, int packedLightIn)
    {

        InkColor color = entityIn.getColor();
        if (SplatcraftConfig.get("splatcraft.colorLock"))
            color = ColorUtils.getLockedColor(color);

        int rgba = color.getColor();

        M model = getModel();
        Identifier texture = getTexture(entityIn);
        Identifier inkTexture = getInkTextureLocation(entityIn);
        Identifier overlay = getOverlayTexture(entityIn);

        ItemStack stack = entityIn.getItem();
        if (stack.getItem() instanceof SubWeaponItem sub && entityIn.getType().equals(sub.entityType.get()))
        {
            Identifier registryName = Registries.ITEM.getId(sub);
            String customModelData = "";

            if (stack.contains(DataComponentTypes.CUSTOM_MODEL_DATA))
            {
                CustomModelDataComponent modelData = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
                if (MinecraftClient.getInstance().getResourceManager().getResource(Identifier.of(registryName.getNamespace(),
                    "textures/models/" + registryName.getPath() + "_" + modelData.value() + ".png")).isPresent())
                {
                    customModelData = "_" + modelData.value();
                }
            }

            texture = Identifier.of(registryName.getNamespace(), "textures/item/weapons/sub/" + registryName.getPath() + customModelData + ".png");
            inkTexture = Identifier.of(registryName.getNamespace(), "textures/item/weapons/sub/" + registryName.getPath() + customModelData + "_ink.png");

            if (overlay != null)
                overlay = Identifier.of(registryName.getNamespace(), "textures/item/weapons/sub/" + registryName.getPath() + customModelData + "_overlay.png");
        }

        model.setAngles(entityIn, 0, 0, handleRotationFloat(entityIn, partialTicks), entityYaw, entityIn.getPitch());
        model.animateModel(entityIn, 0, 0, partialTicks);
        int i = OverlayTexture.packUv(OverlayTexture.getU(getOverlayProgress(entityIn, partialTicks)), OverlayTexture.getV(false));
        model.render(matrixStackIn, bufferIn.getBuffer(model.getLayer(inkTexture)), packedLightIn, i, 0xFF000000 | rgba);
        model.render(matrixStackIn, bufferIn.getBuffer(model.getLayer(texture)), packedLightIn, i, 0xFFFFFF);

        if (overlay != null)
        {
            int overlayRgb = getOverlayColor(entityIn, partialTicks);
            model.render(matrixStackIn, bufferIn.getBuffer(model.getLayer(overlay)), packedLightIn, i, overlayRgb);
        }

        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    protected float getOverlayProgress(E entity, float partialTicks)
    {
        return 0;
    }

    public abstract M getModel();

    public abstract Identifier getInkTextureLocation(E entity);

    @Nullable
    public Identifier getOverlayTexture(E entity)
    {
        return null;
    }

    public int getOverlayColor(E entity, float partialTicks)
    {
        return 0xFFFFFFFF;
    }

    protected float handleRotationFloat(E livingBase, float partialTicks)
    {
        return (float) livingBase.age + partialTicks;
    }
}
