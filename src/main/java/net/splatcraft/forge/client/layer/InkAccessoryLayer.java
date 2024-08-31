package net.splatcraft.forge.client.layer;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.util.ColorUtils;
import org.jetbrains.annotations.NotNull;

public class InkAccessoryLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
{
    HumanoidModel MODEL;

    public InkAccessoryLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer, HumanoidModel model)
    {
        super(renderer);
        this.MODEL = model;
    }



    @Override
    public void render(@NotNull PoseStack matrixStack, @NotNull MultiBufferSource iRenderTypeBuffer, int i, @NotNull AbstractClientPlayer entity, float v, float v1, float v2, float v3, float v4, float v5)
    {
        if(!PlayerInfoCapability.hasCapability(entity))
            return;
        PlayerInfo info = PlayerInfoCapability.get(entity);
        ItemStack inkBand = info.getInkBand();

        if(!inkBand.isEmpty() && ((entity.getMainHandItem().equals(inkBand, false) || entity.getOffhandItem().equals(inkBand, false))))
            return;

        boolean isFoil = inkBand.hasFoil();
        ResourceLocation stackLoc = ForgeRegistries.ITEMS.getKey(inkBand.getItem());

        String customModelData = "";

        if(inkBand.getOrCreateTag().contains("CustomModelData") && Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation(stackLoc.getNamespace(), "textures/models/" + stackLoc.getPath() + "_" + inkBand.getOrCreateTag().getInt("CustomModelData") + ".png")).isPresent())
        {
            customModelData = "_" + inkBand.getOrCreateTag().getInt("CustomModelData");
        }

        ResourceLocation texture = new ResourceLocation(stackLoc.getNamespace(), "textures/models/" + stackLoc.getPath() + customModelData + ".png");
        ResourceLocation coloredTexture = new ResourceLocation(stackLoc.getNamespace(), "textures/models/" + stackLoc.getPath() + customModelData + "_colored.png");



        MODEL.leftArm.visible = entity.getMainArm() == HumanoidArm.LEFT;
        MODEL.leftLeg.visible = entity.getMainArm() == HumanoidArm.LEFT;
        MODEL.rightArm.visible = entity.getMainArm() == HumanoidArm.RIGHT;
        MODEL.rightLeg.visible = entity.getMainArm() == HumanoidArm.RIGHT;

        int color = ColorUtils.getPlayerColor(entity);
        float r = ((color & 16711680) >> 16) / 255.0f;
        float g = ((color & '\uff00') >> 8) / 255.0f;
        float b = (color & 255) / 255.0f;


        if(Minecraft.getInstance().getResourceManager().getResource(texture).isPresent())
        {
            this.getParentModel().copyPropertiesTo(MODEL);
            this.render(matrixStack, iRenderTypeBuffer, i, isFoil, MODEL, 1.0F, 1.0F, 1.0F, texture);
            if(Minecraft.getInstance().getResourceManager().getResource(coloredTexture).isPresent())
                this.render(matrixStack, iRenderTypeBuffer, i, isFoil, MODEL, r, g, b, coloredTexture);
        }
    }

    private void render(PoseStack p_241738_1_, MultiBufferSource p_241738_2_, int p_241738_3_, boolean isFoil, HumanoidModel p_241738_6_, float p_241738_8_, float p_241738_9_, float p_241738_10_, ResourceLocation armorResource) {
        VertexConsumer ivertexbuilder = ItemRenderer.getArmorFoilBuffer(p_241738_2_, RenderType.armorCutoutNoCull(armorResource), false, isFoil);
        p_241738_6_.renderToBuffer(p_241738_1_, ivertexbuilder, p_241738_3_, OverlayTexture.NO_OVERLAY, p_241738_8_, p_241738_9_, p_241738_10_, 1.0F);
    }
}
