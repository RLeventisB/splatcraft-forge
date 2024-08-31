package net.splatcraft.forge.client.handlers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.SplatcraftConfig;
import net.splatcraft.forge.VectorUtils;
import net.splatcraft.forge.client.layer.PlayerInkColoredSkinLayer;
import net.splatcraft.forge.client.renderer.InkSquidRenderer;
import net.splatcraft.forge.data.SplatcraftTags;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.forge.items.InkTankItem;
import net.splatcraft.forge.items.weapons.IChargeableWeapon;
import net.splatcraft.forge.items.weapons.SubWeaponItem;
import net.splatcraft.forge.items.weapons.WeaponBaseItem;
import net.splatcraft.forge.registries.SplatcraftGameRules;
import net.splatcraft.forge.util.*;

import java.util.ArrayList;
import java.util.List;

import static net.splatcraft.forge.items.weapons.WeaponBaseItem.enoughInk;

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
		if (PlayerCooldown.hasPlayerCooldown(player) && !player.isSpectator())
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
			
			if (player.getItemInHand(event.getHand()).getItem() instanceof WeaponBaseItem weaponBaseItem)
			{
				switch (weaponBaseItem.getPose(player.getItemInHand(event.getHand())))
				{
					case ROLL:
						yOff = -((time - event.getPartialTick()) / maxTime) + 0.5f;
						break;
					case BRUSH:
						event.getPoseStack().mulPose(VectorUtils.rotation(VectorUtils.YN, yOff * ((player.getMainArm() == HumanoidArm.RIGHT ? event.getHand().equals(InteractionHand.MAIN_HAND) : event.getHand().equals(InteractionHand.OFF_HAND)) ? 1 : -1)));
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
			
			float i = Minecraft.getInstance().player.getInventory().selected / 4f;
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
		p_228399_1_.mulPose(VectorUtils.rotationDegrees(VectorUtils.YP, ((float) i * (45.0F + f * -20.0F))));
		float f1 = Mth.sin(Mth.sqrt(p_228399_3_) * (float) Math.PI);
		p_228399_1_.mulPose(VectorUtils.rotationDegrees(VectorUtils.ZP, (float) i * f1 * -20.0F));
		p_228399_1_.mulPose(VectorUtils.rotationDegrees(VectorUtils.XP, (f1 * -80.0F)));
		p_228399_1_.mulPose(VectorUtils.rotationDegrees(VectorUtils.YP, ((float) i * -45.0F)));
	}
	public static void renderItem(ItemStack itemStackIn, ItemDisplayContext pDisplayContext, boolean leftHand, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn, BakedModel modelIn)
	{
		if (!itemStackIn.isEmpty())
		{
			ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
			
			matrixStackIn.pushPose();
			boolean flag = pDisplayContext == ItemDisplayContext.GUI || pDisplayContext == ItemDisplayContext.GROUND || pDisplayContext == ItemDisplayContext.FIXED;
			if (itemStackIn.getItem() == Items.TRIDENT && flag)
			{
				modelIn = itemRenderer.getItemModelShaper().getModelManager().getModel(new ModelResourceLocation("minecraft", "trident", "inventory"));
			}
			
			modelIn = modelIn.applyTransform(pDisplayContext, matrixStackIn, leftHand);
			matrixStackIn.translate(-0.5D, -0.5D, -0.5D);
			if (!modelIn.isCustomRenderer() && (itemStackIn.getItem() != Items.TRIDENT || flag))
			{
				boolean flag1;
				if (pDisplayContext != ItemDisplayContext.GUI && !pDisplayContext.firstPerson() && itemStackIn.getItem() instanceof BlockItem blockItem)
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
						if (pDisplayContext == ItemDisplayContext.GUI)
						{
							matrixstack$entry.pose().scale(0.5F);
						}
						else if (pDisplayContext.firstPerson())
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
				IClientItemExtensions.of(itemStackIn).getCustomRenderer().renderByItem(itemStackIn, pDisplayContext, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);
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
		if (level != null && SplatcraftGameRules.getBooleanRuleValue(level, SplatcraftGameRules.COLORED_PLAYER_NAMES) && event.getMessage() instanceof MutableComponent component)
		{
			
			List<String> players = new ArrayList<>();
			ClientPacketListener connection = Minecraft.getInstance().getConnection();
			if (connection != null)
				connection.getOnlinePlayers().forEach(info -> players.add(getDisplayName(info).getString()));
			
			if (component.getContents() instanceof TranslatableContents translatableContents
//				&& translatableContents.getKey().equals("chat.type.text")
			)
			{
				for (Object arg : translatableContents.getArgs())
				{
					if (!(arg instanceof MutableComponent msgChildren))
						continue;
					
					String key = msgChildren.getString();
					
					if (players.contains(key))
						msgChildren.setStyle(msgChildren.getStyle().withColor(TextColor.fromRgb(ClientUtils.getClientPlayerColor(key))));
				}
			}
//			for (Component obj : component.getContents())
//			{
//				if (!(obj instanceof MutableComponent msgChildren))
//					continue;
//
//				String key = msgChildren.getString();
//
//				if (players.contains(key))
//					msgChildren.setStyle(msgChildren.getStyle().withColor(TextColor.fromRgb(ClientUtils.getClientPlayerColor(key))));
//			}
		}
	}
	@SubscribeEvent
	public static void renderNameplate(RenderNameTagEvent event)
	{
		if (SplatcraftGameRules.getBooleanRuleValue(event.getEntity().level(), SplatcraftGameRules.COLORED_PLAYER_NAMES) && event.getEntity() instanceof LivingEntity)
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
		return info.getTabListDisplayName() != null ? info.getTabListDisplayName().copy() : PlayerTeam.formatNameForTeam(info.getTeam(), Component.translatable(info.getProfile().getName()));
	}
	//Render Player HUD elements
	private static float squidTime = 0;
	private static float prevInkPctg = 0;
	private static float inkFlash = 0;
	@SubscribeEvent
	public static void renderGui(RenderGuiOverlayEvent.Pre event)
	{
//		if (!event.getWindow().equals(RenderGuiOverlayEvent.ElementType.LAYER))
//			return;
		
		Player player = Minecraft.getInstance().player;
		if (player == null || player.isSpectator() || !PlayerInfoCapability.hasCapability(player))
		{
			return;
		}
		net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfo info = PlayerInfoCapability.get(player);
		
		int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		GuiGraphics graphics = event.getGuiGraphics();
		
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
				if (PlayerInfoCapability.hasCapability(player) && PlayerInfoCapability.get(player).getPlayerCharge() != null)
				{
					PlayerCharge playerCharge = PlayerInfoCapability.get(player).getPlayerCharge();
					float charge = lerp(playerCharge.prevCharge, playerCharge.charge, event.getPartialTick());
					
					if (charge > 1)
					{
						RenderSystem.setShaderColor(1, 1, 1, playerCharge.getDischargeValue(event.getPartialTick()) * 0.05f);
						graphics.blit(WIDGETS, width / 2 - 15, height / 2 + 14, 30, 9, 88, 9, 30, 9, 256, 256);
						
						if (Math.floor(charge) != charge)
							charge = charge % 1f;
					}
					
					RenderSystem.setShaderColor(1, 1, 1, playerCharge.getDischargeValue(event.getPartialTick()));
					graphics.blit(WIDGETS, width / 2 - 15, height / 2 + 14, (int) (30 * charge), 9, 88, 9, (int) (30 * charge), 9, 256, 256);
				}
				RenderSystem.setShaderColor(1, 1, 1, 1);
				
				matrixStack.popPose();
			}
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
				squidTime += event.getPartialTick();
				
				if (showCrosshairInkIndicator)
				{
					float speed = 0.15f;
					int heightAnim = Math.min(14, (int) (squidTime * speed));
					int glowAnim = Math.max(0, Math.min(18, (int) (squidTime * speed) - 16));
					float[] rgb = ColorUtils.hexToRGB(info.getColor());
					
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
