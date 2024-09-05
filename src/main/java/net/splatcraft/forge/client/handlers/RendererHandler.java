package net.splatcraft.forge.client.handlers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.SplatcraftConfig;
import net.splatcraft.forge.client.layer.PlayerInkColoredSkinLayer;
import net.splatcraft.forge.client.renderer.InkSquidRenderer;
import net.splatcraft.forge.data.SplatcraftTags;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.forge.items.InkTankItem;
import net.splatcraft.forge.items.weapons.IChargeableWeapon;
import net.splatcraft.forge.items.weapons.SubWeaponItem;
import net.splatcraft.forge.items.weapons.WeaponBaseItem;
import net.splatcraft.forge.items.weapons.settings.CommonRecords;
import net.splatcraft.forge.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.forge.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.UUID;

import static net.splatcraft.forge.items.weapons.WeaponBaseItem.enoughInk;

@SuppressWarnings("deprecation")
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Splatcraft.MODID)
public class RendererHandler
{
    private static final ResourceLocation WIDGETS = new ResourceLocation(Splatcraft.MODID, "textures/gui/widgets.png");
    private static float oldCooldown = 0;
    private static float tickTime = 0;
    private static InkSquidRenderer squidRenderer;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void playerRender(RenderPlayerEvent event)
    {
        Player player = event.getEntity();
        if (player.isSpectator()) return;

        if (PlayerInfoCapability.isSquid(player))
        {
            event.setCanceled(true);
            if (squidRenderer == null)
                squidRenderer = new InkSquidRenderer(InkSquidRenderer.getContext());
            if (!InkBlockUtils.canSquidHide(player))
            {
                squidRenderer.render(player, player.yHeadRot, event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Post<>(player, squidRenderer, event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight()));
            }
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event)
    {
        Player player = Minecraft.getInstance().player;
        if (PlayerCooldown.hasPlayerCooldown(player) && !player.isSpectator() && PlayerCooldown.getPlayerCooldown(player).getSlotIndex() >= 0)
        {
            player.getInventory().selected = PlayerCooldown.getPlayerCooldown(player).getSlotIndex();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void playerRenderPost(RenderPlayerEvent.Post event)
    {
    }

    @SubscribeEvent
    public static void renderArm(RenderArmEvent event)
    {
        PlayerRenderer playerrenderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(event.getPlayer());

        PlayerModel<AbstractClientPlayer> playerModel = playerrenderer.getModel();
        PlayerInkColoredSkinLayer.renderHand(playerModel, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getPlayer(),
                event.getArm().equals(HumanoidArm.LEFT) ? playerModel.leftArm : playerModel.rightArm, event.getArm().equals(HumanoidArm.LEFT) ? playerModel.leftSleeve : playerModel.rightSleeve);
    }

    @SubscribeEvent
    public static void renderHand(RenderHandEvent event)
    {
        Player player = Minecraft.getInstance().player;
        if (PlayerInfoCapability.isSquid(player))
        {
            event.setCanceled(true);
            return;
        }

        if (PlayerCooldown.hasPlayerCooldown(player) && PlayerCooldown.getPlayerCooldown(player).getHand().equals(event.getHand()))
        {
            PlayerCooldown cooldown = PlayerCooldown.getPlayerCooldown(player);
            float time = (float) cooldown.getTime();
            float maxTime = (float) cooldown.getMaxTime();
            if (time != oldCooldown)
            {
                oldCooldown = time;
                tickTime = 0;
            }
            tickTime = (tickTime + 1) % 10;
            float yOff = -0.5f * ((time - event.getPartialTick()) / maxTime);// - (tickTime/20f));

            if (player.getItemInHand(event.getHand()).getItem() instanceof WeaponBaseItem<?> weaponBaseItem)
            {
                switch (weaponBaseItem.getPose(player.getItemInHand(event.getHand())))
                {
                    case ROLL:
                        yOff = -((time - event.getPartialTick()) / maxTime) + 0.5f;
                        break;
                    case BRUSH:
                        event.getPoseStack().mulPose(Axis.YN.rotation(yOff * ((player.getMainArm() == HumanoidArm.RIGHT ? event.getHand().equals(InteractionHand.MAIN_HAND) : event.getHand().equals(InteractionHand.OFF_HAND)) ? 1 : -1)));
                        yOff = 0;
                        break;
                }
            }

            event.getPoseStack().translate(0, yOff, 0);
        }
        else
        {
            tickTime = 0;
        }
    }

    public static boolean renderSubWeapon(ItemStack stack, PoseStack poseStack, MultiBufferSource source, int light, float partialTicks)
    {
        if (stack.getItem() instanceof SubWeaponItem subWeaponItem)
        {
            AbstractSubWeaponEntity sub = subWeaponItem.entityType.get().create(Minecraft.getInstance().player.level());
            sub.setColor(ColorUtils.getInkColor(stack));
            sub.setItem(stack);
            sub.readItemData(stack.getOrCreateTag().getCompound("EntityData"));

            sub.isItem = true;

            poseStack.translate(.5f, .55f, .5f);

            Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(sub).render(sub, 0, partialTicks, poseStack, source, light);
            return true;
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    private static void applyItemArmTransform(PoseStack p_228406_1_, HumanoidArm p_228406_2_, float p_228406_3_)
    {
        int i = p_228406_2_ == HumanoidArm.RIGHT ? 1 : -1;
        p_228406_1_.translate((float) i * 0.56F, -0.52F + p_228406_3_ * -0.6F, -0.72F);
    }

    @OnlyIn(Dist.CLIENT)
    private static void applyItemArmAttackTransform(PoseStack p_228399_1_, HumanoidArm p_228399_2_, float p_228399_3_)
    {
        int i = p_228399_2_ == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.sin(p_228399_3_ * p_228399_3_ * (float) Math.PI);
        p_228399_1_.mulPose(Axis.YP.rotationDegrees(((float) i * (45.0F + f * -20.0F))));
        float f1 = Mth.sin(Mth.sqrt(p_228399_3_) * (float) Math.PI);
        p_228399_1_.mulPose(Axis.ZP.rotationDegrees((float) i * f1 * -20.0F));
        p_228399_1_.mulPose(Axis.XP.rotationDegrees((f1 * -80.0F)));
        p_228399_1_.mulPose(Axis.YP.rotationDegrees(((float) i * -45.0F)));
    }

    public static void renderItem(ItemStack itemStackIn, ItemDisplayContext context, boolean leftHand, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn, BakedModel modelIn)
    {
        if (!itemStackIn.isEmpty())
        {
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

            matrixStackIn.pushPose();
            boolean flag = context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED;
            if (itemStackIn.getItem() == Items.TRIDENT && flag)
            {
                modelIn = itemRenderer.getItemModelShaper().getModelManager().getModel(new ModelResourceLocation("minecraft", "trident", "inventory"));
            }

            modelIn = modelIn.applyTransform(context, matrixStackIn, leftHand);
            matrixStackIn.translate(-0.5D, -0.5D, -0.5D);
            if (!modelIn.isCustomRenderer() && (itemStackIn.getItem() != Items.TRIDENT || flag))
            {
                boolean flag1;
                if (context != ItemDisplayContext.GUI && !context.firstPerson() && itemStackIn.getItem() instanceof BlockItem blockItem)
                {
                    Block block = blockItem.getBlock();
                    flag1 = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
                }
                else
                {
                    flag1 = true;
                }
/* code got nuked
				if (modelIn.isLayered())
				{
					net.minecraftforge.client.ForgeHooksClient.drawItemLayered(itemRenderer, modelIn, itemStackIn, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn, flag1);
				}
				else
*/
                {
                    RenderType rendertype = ItemBlockRenderTypes.getRenderType(itemStackIn, flag1);//getItemEntityTranslucent(InventoryMenu.BLOCK_ATLAS);
                    VertexConsumer ivertexbuilder;
                    if (itemStackIn.getItem() == Items.COMPASS && itemStackIn.hasFoil())
                    {
                        matrixStackIn.pushPose();
                        PoseStack.Pose matrixstack$entry = matrixStackIn.last();
                        if (context == ItemDisplayContext.GUI)
                        {
                            matrixstack$entry.pose().scale(0.5F);
                        }
                        else if (context.firstPerson())
                        {
                            matrixstack$entry.pose().scale(0.75F);
                        }

                        if (flag1)
                        {
                            ivertexbuilder = ItemRenderer.getCompassFoilBufferDirect(bufferIn, rendertype, matrixstack$entry);
                        }
                        else
                        {
                            ivertexbuilder = ItemRenderer.getCompassFoilBuffer(bufferIn, rendertype, matrixstack$entry);
                        }

                        matrixStackIn.popPose();
                    }
                    else if (flag1)
                    {
                        ivertexbuilder = ItemRenderer.getFoilBufferDirect(bufferIn, rendertype, true, itemStackIn.hasFoil());
                    }
                    else
                    {
                        ivertexbuilder = ItemRenderer.getFoilBuffer(bufferIn, rendertype, true, itemStackIn.hasFoil());
                    }

                    itemRenderer.renderModelLists(modelIn, itemStackIn, combinedLightIn, combinedOverlayIn, matrixStackIn, ivertexbuilder);
                }
            }
            else
            {
                IClientItemExtensions.of(itemStackIn).getCustomRenderer().renderByItem(itemStackIn, context, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);
            }

            matrixStackIn.popPose();
        }
    }

    /*
    protected static RenderType getItemEntityTranslucent(ResourceLocation locationIn)
    {
        RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder().setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .target(locationIn);/*.setDiffuseLightingState(new RenderState.DiffuseLightingState(true)).setAlphaState(new RenderStateShard.AlphaState(0.003921569F)).setLightmapState(new RenderStateShard.LightmapState(true))
                .setOverlayState(new RenderState.OverlayState(true)).createCompositeState(true);

        return RenderType.create("item_entity_translucent", VertexFormat.Mode.NEW_ENTITY, 7, 256, true, false, rendertype$state);
    }
    */

    @SubscribeEvent
    public static void onChatMessage(ClientChatReceivedEvent event)
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && SplatcraftConfig.Client.coloredPlayerNames.get())
        {
            HashMap<String, UUID> players = new HashMap<>();
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            if (connection != null)
            {
                for (PlayerInfo info : connection.getOnlinePlayers())
                {
                    players.put(getDisplayName(info).getString(), info.getProfile().getId());
                }
            }

            if (!(event.getMessage().getContents() instanceof TranslatableContents translatableContents))
            {
                return;
            }

            for (Object arg : translatableContents.getArgs())
            {
                if (!(arg instanceof MutableComponent msgChildren))
                    continue;
                String key = msgChildren.getString();

                if (players.containsKey(key))
                    msgChildren.setStyle(msgChildren.getStyle().withColor(TextColor.fromRgb(ClientUtils.getClientPlayerColor(players.get(key)))));
            }
        }
    }

    @SubscribeEvent
    public static void renderNameplate(RenderNameTagEvent event)
    {
        if (SplatcraftConfig.Client.coloredPlayerNames.get() && event.getEntity() instanceof LivingEntity)
        {
            int color = ColorUtils.getEntityColor(event.getEntity());
            if (SplatcraftConfig.Client.getColorLock())
            {
                color = ColorUtils.getLockedColor(color);
            }
            if (color != -1)
            {
                event.setContent(((MutableComponent) event.getContent()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));
            }
        }
    }

    public static Component getDisplayName(PlayerInfo info)
    {
        return info.getTabListDisplayName() != null ? info.getTabListDisplayName().copy() : PlayerTeam.formatNameForTeam(info.getTeam(), Component.literal(info.getProfile().getName()));
    }

    //Render Player HUD elements
    private static float squidTime = 0;
    private static float prevInkPctg = 0;
    private static float inkFlash = 0;

    @SubscribeEvent
    public static void renderGui(RenderGuiOverlayEvent.Pre event)
    {
        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        renderGuiInternal(null, event.getGuiGraphics(), event.getPartialTick(), width, height);
    }

    public static void renderGuiInternal(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height)
    {
        Player player = Minecraft.getInstance().player;
        boolean hasCapability = PlayerInfoCapability.hasCapability(player);
        if (player == null || player.isSpectator() || !hasCapability)
        {
            return;
        }
        net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfo info = PlayerInfoCapability.get(player);
        //if (event.getType().equals(RenderGameOverlayEvent.ElementType.LAYER))
        {
            if (player.getMainHandItem().getItem() instanceof IChargeableWeapon || player.getOffhandItem().getItem() instanceof IChargeableWeapon)
            {
                PoseStack matrixStack = graphics.pose();
                matrixStack.pushPose();
                RenderSystem.enableBlend();
                RenderSystem.setShaderTexture(0, WIDGETS);
                RenderSystem.setShaderColor(1, 1, 1, 1);

                graphics.blit(WIDGETS, width / 2 - 15, height / 2 + 14, 30, 9, 88, 0, 30, 9, 256, 256);
                if (info.getPlayerCharge() != null)
                {
                    PlayerCharge playerCharge = info.getPlayerCharge();
                    float charge = lerp(playerCharge.prevCharge, playerCharge.charge, partialTicks);

                    if (charge > 1)
                    {
                        RenderSystem.setShaderColor(1, 1, 1, playerCharge.getDischargeValue(partialTicks) * 0.05f);
                        graphics.blit(WIDGETS, width / 2 - 15, height / 2 + 14, 30, 9, 88, 9, 30, 9, 256, 256);

                        if (Math.floor(charge) != charge)
                            charge = charge % 1f;
                    }

                    RenderSystem.setShaderColor(1, 1, 1, playerCharge.getDischargeValue(partialTicks));
                    graphics.blit(WIDGETS, width / 2 - 15, height / 2 + 14, (int) (30 * charge), 9, 88, 9, (int) (30 * charge), 9, 256, 256);
                }
                RenderSystem.setShaderColor(1, 1, 1, 1);

                matrixStack.popPose();
            }
        }

        float[] playerColor = ColorUtils.hexToRGB(info.getColor());
        if (player.getMainHandItem().getItem() instanceof WeaponBaseItem<?> weaponBaseItem)
        {
            PoseStack matrixStack = graphics.pose();
            matrixStack.pushPose();
            RenderSystem.setShaderTexture(0, WIDGETS);
            RenderSystem.enableBlend();

            CommonRecords.ShotDeviationDataRecord data = weaponBaseItem.getSettings(player.getMainHandItem()).getShotDeviationData(player.getMainHandItem(), player);
            ShotDeviationHelper.DeviationData deviationData = ShotDeviationHelper.getDeviationData(player.getMainHandItem());

            CommonUtils.Result actualChanceResult = CommonUtils.tickValue(deviationData.chanceDecreaseDelay(), deviationData.chance(), data.chanceDecreasePerTick(), data.minDeviateChance(), partialTicks);
            CommonUtils.Result airInfluenceResult = CommonUtils.tickValue(deviationData.airborneDecreaseDelay(), deviationData.airborneInfluence(), data.airborneContractTimeToDecrease() == 0 ? Float.NaN : 1f / data.airborneContractTimeToDecrease(), 0, partialTicks);

            Minecraft mc = Minecraft.getInstance();

            Matrix4f projectionMatrix = mc.gameRenderer.getProjectionMatrix(
                    mc.gameRenderer.getFov(
                            mc.gameRenderer.getMainCamera(),
                            partialTicks,
                            true));

//            float aspectRatio = Mth.lerp(1 / (1 + data.getMaximumDeviation() * 100), (float) height / width, 1f);
            float aspectRatio = 0.5625f;

            float currentAirInfluence = airInfluenceResult.value();
            float currentDeviationChance = actualChanceResult.value();

            float currentDeviation = Mth.lerp(ShotDeviationHelper.getModifiedAirInfluence(currentAirInfluence), data.airborneShotDeviation(), data.groundShotDeviation()) * Mth.DEG_TO_RAD / 2f * 1.34f;

            float value = Math.min(0.71428573f, currentDeviationChance / data.maxDeviateChance() / 1.4f);
            float[] rgb = new float[]
                    {
                            Mth.lerp(value, 0.6f, playerColor[0]),
                            Mth.lerp(value, 0.6f, playerColor[1]),
                            Mth.lerp(value, 0.6f, playerColor[2])
                    };
            RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], 0.4f);

            Vec3 relativePos = new Vec3(0, 0, 1);
            for (int x = -1; x <= 1; x += 2)
            {
                for (int y = -1; y <= 1; y += 2)
                {
                    Vec3 rotatedPos = relativePos.xRot(currentDeviation * x).yRot(currentDeviation * y);

                    Vector3f camSpace = rotatedPos.toVector3f();

                    Vector4f projectiveCamSpace = new Vector4f(camSpace, 1f);
                    projectionMatrix.transform(projectiveCamSpace);
                    float w = projectiveCamSpace.w();

                    Vector4f screenPos = new Vector4f(projectiveCamSpace.x() / w * width, projectiveCamSpace.y() / w * height, w, (float) Math.sqrt(relativePos.dot(relativePos)));

                    GraphicsUtils.blit(graphics, WIDGETS, width / 2f - 3 + screenPos.x - 3 * y, height / 2f - 3 + (screenPos.y * aspectRatio) - 3 * x, 6, 6, 64 - 7 * y, 8 - 7 * x, 4, 4, 256, 256);
                }
            }

//            GraphicsUtils.blit(graphics, WIDGETS, width / 2f - 3 + currentDeviation, height / 2f - 3 - (currentDeviation * aspectRatio), 6, 6, 71, 1, 4, 4, 256, 256);
//            GraphicsUtils.blit(graphics, WIDGETS, width / 2f - 3 + currentDeviation, height / 2f - 3 + (currentDeviation * aspectRatio), 6, 6, 71, 15, 4, 4, 256, 256);
//            GraphicsUtils.blit(graphics, WIDGETS, width / 2f - 3 - currentDeviation, height / 2f - 3 + (currentDeviation * aspectRatio), 6, 6, 57, 15, 4, 4, 256, 256);

            RenderSystem.setShaderColor(1, 1, 1, 1);
            matrixStack.popPose();
        }

        boolean showCrosshairInkIndicator = SplatcraftConfig.Client.inkIndicator.get().equals(SplatcraftConfig.InkIndicator.BOTH) || SplatcraftConfig.Client.inkIndicator.get().equals(SplatcraftConfig.InkIndicator.CROSSHAIR);
        boolean isHoldingMatchItem = player.getMainHandItem().is(SplatcraftTags.Items.MATCH_ITEMS) || player.getOffhandItem().is(SplatcraftTags.Items.MATCH_ITEMS);
        boolean showLowInkWarning = showCrosshairInkIndicator && SplatcraftConfig.Client.lowInkWarning.get() && (isHoldingMatchItem || info.isSquid()) && !enoughInk(player, null, 10f, 0, false);

        boolean canUse = true;
        boolean hasTank = player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof InkTankItem;
        float inkPctg = 0;
        if (hasTank)
        {
            ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
            inkPctg = InkTankItem.getInkAmount(stack) / ((InkTankItem) stack.getItem()).capacity;
            if (isHoldingMatchItem)
                canUse = ((InkTankItem) stack.getItem()).canUse(player.getMainHandItem().getItem()) || ((InkTankItem) stack.getItem()).canUse(player.getOffhandItem().getItem());
        }
        if (info.isSquid() || showLowInkWarning || !canUse)
        {
            //if (event.getType().equals(RenderGameOverlayEvent.ElementType.LAYER))
            {
                squidTime += partialTicks;

                if (showCrosshairInkIndicator)
                {
                    float speed = 0.15f;
                    int heightAnim = Math.min(14, (int) (squidTime * speed));
                    int glowAnim = Math.max(0, Math.min(18, (int) (squidTime * speed) - 16));
                    float[] rgb = playerColor;

                    PoseStack matrixStack = graphics.pose();
                    matrixStack.pushPose();
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderTexture(0, WIDGETS);

                    if (enoughInk(player, null, 220, 0, false))
                    { // checks if you have unlimited ink
                        graphics.blit(WIDGETS, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 2, 0, 131, 18, 2, 256, 256);
                        graphics.blit(WIDGETS, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 4 + heightAnim, 0, 131, 18, 4 + heightAnim, 256, 256);

                        RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], 1);

                        graphics.blit(WIDGETS, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 4 + heightAnim, 18, 131, 18, 4 + heightAnim, 256, 256);
                        graphics.blit(WIDGETS, width / 2 + 9 + 18 - glowAnim, height / 2 - 9, glowAnim, 18, 18 - glowAnim, 149, glowAnim, 18, 256, 256);
                    }
                    else
                    {
                        graphics.blit(WIDGETS, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 2, 0, 95, 18, 2, 256, 256);
                        graphics.blit(WIDGETS, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 4 + heightAnim, 0, 95, 18, 4 + heightAnim, 256, 256);

                        if (inkPctg != prevInkPctg && inkPctg == 1)
                        {
                            inkFlash = 0.2f;
                        }
                        inkFlash = Math.max(0, inkFlash - 0.0004f);

                        float inkPctgLerp = lerp(prevInkPctg, inkPctg, 0.05f);
                        float inkSize = (1 - inkPctg) * 18;

                        RenderSystem.setShaderColor(rgb[0] + inkFlash, rgb[1] + inkFlash, rgb[2] + inkFlash, 1);
                        matrixStack.translate(0, inkSize - Math.floor(inkSize), 0);
                        graphics.blit(WIDGETS, width / 2 + 9, (int) (height / 2 - 9 + (14 - heightAnim) + (1 - inkPctgLerp) * 18), 18, (int) ((4 + heightAnim) * inkPctgLerp), 18, 95 + inkSize, 18, (int) ((4 + heightAnim) * inkPctg), 256, 256);
                        matrixStack.translate(0, -(inkSize - Math.floor(inkSize)), 0);

                        if (SplatcraftConfig.Client.vanillaInkDurability.get())
                        {
                            float[] durRgb = ColorUtils.hexToRGB(Mth.hsvToRgb(Math.max(0.0F, inkPctgLerp) / 3.0F, 1.0F, 1.0F));
                            RenderSystem.setShaderColor(durRgb[0], durRgb[1], durRgb[2], 1);
                        }
                        else
                        {
                            RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], 1);
                        }

                        graphics.blit(WIDGETS, width / 2 + 9 + 18 - glowAnim, height / 2 - 9, glowAnim, 18, 18 - glowAnim, 113, glowAnim, 18, 256, 256);

                        RenderSystem.setShaderColor(1, 1, 1, 1);
                        if (glowAnim == 18)
                        {
                            if (!canUse)
                            {
                                graphics.blit(WIDGETS, width / 2 + 9, height / 2 - 9, 36, 112, 18, 18, 256, 256);
                            }
                            else if (showLowInkWarning)
                            {
                                graphics.blit(WIDGETS, width / 2 + 9, height / 2 - 9, 18, 112, 18, 18, 256, 256);
                            }
                        }
                    }
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                    matrixStack.popPose();
                }
                prevInkPctg = inkPctg;
            }
        }
        else
        {
            squidTime = 0;
        }
    }

    private static float lerp(float a, float b, float f)
    {
        return a + f * (b - a);
    }
}