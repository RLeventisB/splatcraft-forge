package net.splatcraft.client.handlers;

import com.google.common.base.Strings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.splatcraft.Splatcraft;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.client.renderer.InkSquidRenderer;
import net.splatcraft.data.PlaySession;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.Stage;
import net.splatcraft.data.StageGameMode;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.weapons.DualieItem;
import net.splatcraft.items.weapons.IChargeableWeapon;
import net.splatcraft.items.weapons.RollerItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.AbstractWeaponSettings;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.items.weapons.settings.DynamicDataRecord;
import net.splatcraft.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.items.weapons.subs.SubWeaponItem;
import net.splatcraft.mixin.accessors.EntityAccessor;
import net.splatcraft.mixin.accessors.GameRendererFovAccessor;
import net.splatcraft.platform.Components;
import net.splatcraft.platform.Services;
import net.splatcraft.platform.event.CompoundEventResult;
import net.splatcraft.platform.event.EventResult;
import net.splatcraft.platform.event.InteractionEvents;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.tileentities.StageMarkerTileEntity;
import net.splatcraft.util.*;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.specials.BaseSpecialAction;
import net.splatcraft.util.structs.InkColor;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.splatcraft.items.weapons.WeaponBaseItem.enoughInk;

public class RendererHandler
{
	private static final ResourceLocation WIDGETS = Splatcraft.identifierOf("textures/gui/widgets.png");
	private static InkSquidRenderer squidRenderer;
	//Render PlayerEntity HUD elements
	private static float squidTime = 0;
	private static float prevInkPctg = 0;
	private static float inkFlash = 0;
	public static void registerEvents()
	{
		Services.PLATFORM.registerListener(InteractionEvents.ClientChatReceive.class, RendererHandler::onChatMessage);
	}
	public static boolean playerRender(AbstractClientPlayer player, float f, float g, PoseStack matrixStack, MultiBufferSource consumerProvider, int color)
	{
		if (player.isSpectator()) return false;
		
		if (CommonUtils.isSquid(player))
		{
			if (squidRenderer == null)
				squidRenderer = new InkSquidRenderer(InkSquidRenderer.getContext());
			if (!InkBlockUtils.canSquidHide(player))
			{
				squidRenderer.render(player, f, g, matrixStack, consumerProvider, color);
				CommonUtils.doRenderLivingAfterEvent(player, squidRenderer, g, matrixStack, consumerProvider, color);
			}
			return true;
		}
		return false;
	}
	public static boolean renderHand(float tickDelta, InteractionHand hand, PoseStack matrices)
	{
		Player player = ClientUtils.getClientPlayer();
		if (CommonUtils.isSquid(player))
		{
			return false;
		}
		
		AtomicBoolean doRender = new AtomicBoolean(true);
		Optional<EntityAction> actionOptional = EntityAction.getEntityActionIf(player, v -> v.getItemSlot().isItemForSlot(player, hand));
		actionOptional.ifPresent(action ->
		{
			float time = action.getTime() - tickDelta;
			float maxTime = action.getMaxTime();
			float yOff = 0;
			float weaponRotation = 0;
			
			if (action instanceof BaseSpecialAction specialAction)
			{
				if (time < 3)
					yOff = -0.5f * time / 3f;
				else
				{
					float[] dataArray = {yOff, weaponRotation};
					specialAction.transformHeldWeaponRender(dataArray, doRender, hand, tickDelta, time, matrices);
					yOff = dataArray[0];
					weaponRotation = dataArray[1];
				}
			}
			else
			{
				yOff = -0.5f * (time / maxTime);
				if (player.getItemInHand(hand).getItem() instanceof WeaponBaseItem<?> weaponBaseItem)
				{
					switch (weaponBaseItem.getPose(player, player.getItemInHand(hand)))
					{
						case ROLLER_SWING:
							time = action.getTime() + tickDelta;
							if (action instanceof RollerItem.InitialSwingAction swingAction)
							{
								float timeUntilSwingFrame = (swingAction.attackFrame + 0.4f) - time;
								float startupTime = swingAction.attackFrame;
								if (timeUntilSwingFrame >= 0)
								{
									yOff = Math.min(1.5f, (1f - Mth.square(timeUntilSwingFrame / startupTime)) * 3f);
								}
								else
								{
									yOff = Math.max(0f, 1.5f + timeUntilSwingFrame * 0.7f);
								}
								
								if (!swingAction.isGrounded())
								{
									weaponRotation = yOff * 0.5f;
									yOff = 0;
								}
							}
							
							break;
						case BRUSH:
							matrices.mulPose(Axis.YN.rotation(yOff * ((player.getMainArm() == HumanoidArm.RIGHT ? hand.equals(InteractionHand.MAIN_HAND) : hand.equals(InteractionHand.OFF_HAND)) ? 1 : -1)));
							yOff = 0;
							break;
						case TURRET_FIRE:
							yOff = 0;
							
							break;
						case DUAL_FIRE:
							if (action instanceof DualieItem.DodgeRollAction dodgeRollAction && dodgeRollAction.preventWeaponUse())
							{
								yOff = -(time - dodgeRollAction.turretModeFrame) / (maxTime - dodgeRollAction.turretModeFrame);
							}
							
							break;
					}
				}
			}
			if (weaponRotation != 0)
				matrices.mulPose(Axis.XP.rotation(weaponRotation));
			matrices.translate(0, yOff, 0);
		});
		return doRender.get();
	}
	public static <T extends DynamicDataRecord<T>> boolean renderSubWeapon(ItemStack stack, SubWeaponItem<T> subWeaponItem, PoseStack poseStack, MultiBufferSource source, int light, float partialTicks, boolean leftHanded)
	{
//		SubWeaponRenderer<?, ?> renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().renderers.get(subWeaponItem.entityType.get());
		// ok i tried to render the sub models via getting their internal model instead of instantiating a whole entity but the entityrenderer thing does a lot of work about colors and those things since these models have 2 layers
		// maybe i will take that approach soon or something
		AbstractSubWeaponEntity<T> sub = subWeaponItem.getEntityType(stack).create(ClientUtils.getClientPlayer().clientLevel);
		sub.setColor(ColorUtils.getInkColor(stack));
		sub.setItem(stack);
		
		sub.isItem = true;
		
		BakedModel itemModel = Minecraft.getInstance().getModelManager().getModel(new ModelResourceLocation(BuiltInRegistries.ITEM.getKey(subWeaponItem), "inventory"));
		itemModel.getTransforms().getTransform(ItemDisplayContext.GUI).apply(leftHanded, poseStack);
		
		Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(sub).render(sub, 0, partialTicks, poseStack, source, light);
		return true;
	}
	public static net.splatcraft.platform.event.CompoundEventResult<Component> onChatMessage(ChatType.Bound parameters, Component message, UUID sender)
	{
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null && Boolean.TRUE.equals(SplatcraftConfig.get("splatcraft.coloredPlayerNames")))
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
			
			if (!(message.getContents() instanceof TranslatableContents translatableContents))
			{
				return CompoundEventResult.pass();
			}
			
			for (Object arg : translatableContents.getArgs())
			{
				if (!(arg instanceof MutableComponent msgChildren))
					continue;
				String key = msgChildren.getString();
				
				if (players.containsKey(key))
					msgChildren.setStyle(msgChildren.getStyle().withColor(TextColor.fromRgb(ColorUtils.getPlayerColor(players.get(key), level).getColor())));
			}
		}
		return CompoundEventResult.interruptTrue(message);
	}
	public static Component modifyNameplate(Entity entity, Component label)
	{
		if (Boolean.TRUE.equals(SplatcraftConfig.get("splatcraft.coloredPlayerNames")) && entity instanceof LivingEntity)
		{
			InkColor color = ColorUtils.getColorLockedIfConfig(ColorUtils.getEntityColor(entity));
			if (SplatcraftConfig.get("splatcraft.colorLock"))
			{
				color = ColorUtils.getLockedColor(color);
			}
			if (color.isValid())
			{
				label = ((MutableComponent) label).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.getColor())));
			}
		}
		return label;
	}
	public static Component getDisplayName(PlayerInfo info)
	{
		return info.getTabListDisplayName() != null ? info.getTabListDisplayName().copy() : PlayerTeam.formatNameForTeam(info.getTeam(), Component.literal(info.getProfile().getName()));
	}
	public static EventResult renderGui(GuiGraphics context, DeltaTracker tickCounter)
	{
		int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		renderGuiInternal(context, tickCounter.getGameTimeDeltaPartialTick(true), width, height);
		return EventResult.pass();
	}
	public static void renderGuiInternal(GuiGraphics graphics, float tickDelta, int width, int height)
	{
		LocalPlayer player = ClientUtils.getClientPlayer();
		EntityInfo info = Components.ENTITY_INFO.getOrCreate(player);
		
		if (player.isSpectator() && !info.isMatchRespawning())
		{
			return;
		}
		
		PoseStack matrixStack = graphics.pose();
		if (player.getMainHandItem().getItem() instanceof IChargeableWeapon)
		{
			renderChargerGui(graphics, tickDelta, width, height, info, matrixStack, player.getMainHandItem());
		}
		else if (player.getOffhandItem().getItem() instanceof IChargeableWeapon)
		{
			renderChargerGui(graphics, tickDelta, width, height, info, matrixStack, player.getOffhandItem());
		}
		
		InkColor color = ColorUtils.getColorLockedIfConfig(info.getColor());
		float[] playerColor = color.getRGB();
		if (player.getMainHandItem().getItem() instanceof WeaponBaseItem<?> weaponBaseItem)
		{
			renderDeviationGui(graphics, tickDelta, width, height, weaponBaseItem, matrixStack, player, playerColor);
		}
		
		if (info.isPlaying())
		{
			renderMatchGui(graphics, tickDelta, width, height, info.getPlayingStageId(), info, matrixStack);
		}
		else
		{
			ArrayList<Stage> stagesInPos = Stage.getStagesForPosition(player.level(), player.position());
			stagesInPos.removeIf(v -> !SaveInfoCapability.get().playSessions().containsKey(v.id));
			if (!stagesInPos.isEmpty())
			{
				renderMatchGui(graphics, tickDelta, width, height, stagesInPos.getFirst().id, info, matrixStack);
			}
		}
		SplatcraftConfig.InkIndicator inkIndicator = SplatcraftConfig.get("splatcraft.inkIndicator");
		boolean showCrosshairInkIndicator = inkIndicator.equals(SplatcraftConfig.InkIndicator.BOTH) || inkIndicator.equals(SplatcraftConfig.InkIndicator.CROSSHAIR);
		boolean isHoldingMatchItem = player.getMainHandItem().is(SplatcraftTags.Items.MATCH_ITEMS) || player.getOffhandItem().is(SplatcraftTags.Items.MATCH_ITEMS);
		boolean showLowInkWarning = showCrosshairInkIndicator && Boolean.TRUE.equals(SplatcraftConfig.get("splatcraft.lowInkWarning")) && (isHoldingMatchItem || info.isSquid()) && !enoughInk(player, null, 10f, 0, false);
		
		boolean canUse = true;
		float inkPctg = 0;
		boolean isCoolingDown = false;
		if (player.getItemBySlot(EquipmentSlot.CHEST).has(SplatcraftComponents.TANK_DATA))
		{
			ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
			inkPctg = InkTankItem.getInkPercentage(stack);
			isCoolingDown = !InkTankItem.canRecharge(stack, false);
			if (isHoldingMatchItem)
				canUse = InkTankItem.canUse(player.getMainHandItem(), stack) || InkTankItem.canUse(player.getOffhandItem(), stack);
		}
		if (info.isSquid() || showLowInkWarning || !canUse)
		{
			squidTime += 0.15f * tickDelta;
			
			if (showCrosshairInkIndicator)
			{
				int heightAnim = Math.min(14, (int) squidTime);
				int glowAnim = Math.max(0, Math.min(18, (int) squidTime - 16));
				
				matrixStack.pushPose();
				RenderSystem.enableBlend();
				RenderSystem.setShaderTexture(0, WIDGETS);
				
				if (enoughInk(player, null, 220, 0, false))
				{ // checks if you have unlimited ink
					graphics.blit(WIDGETS, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 2, 0, 131, 18, 2, 256, 256);
					graphics.blit(WIDGETS, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 4 + heightAnim, 0, 131, 18, 4 + heightAnim, 256, 256);
					
					RenderSystem.setShaderColor(playerColor[0], playerColor[1], playerColor[2], 1);
					
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
					if (isCoolingDown)
					{
						inkFlash = -0.2f;
					}
					if (inkFlash > 0)
						inkFlash = CommonUtils.tickValue(0, inkFlash, 0.0004f, 0, 1).value();
					if (inkFlash < 0)
						inkFlash = CommonUtils.tickValueToMax(0, inkFlash, 0.004f, 0, 1).value();
					
					float inkPctgLerp = Mth.lerp(0.05f, prevInkPctg, inkPctg);
					float inkSize = (1 - inkPctg) * 18;
					
					RenderSystem.setShaderColor(playerColor[0] + inkFlash, playerColor[1] + inkFlash, playerColor[2] + inkFlash, 1);
					matrixStack.translate(0, inkSize - Math.floor(inkSize), 0);
					graphics.blit(WIDGETS, width / 2 + 9, (int) (height / 2 - 9 + (14 - heightAnim) + (1 - inkPctgLerp) * 18), 18, (int) ((4 + heightAnim) * inkPctgLerp), 18, 95 + inkSize, 18, (int) ((4 + heightAnim) * inkPctg), 256, 256);
					matrixStack.translate(0, -(inkSize - Math.floor(inkSize)), 0);
					
					if (SplatcraftConfig.get("splatcraft.vanillaInkDurability"))
					{
						float[] durRgb = ColorUtils.hexToRGB(Mth.hsvToRgb(Math.max(0.0F, inkPctgLerp) / 3.0F, 1.0F, 1.0F));
						RenderSystem.setShaderColor(durRgb[0], durRgb[1], durRgb[2], 1);
					}
					else
					{
						RenderSystem.setShaderColor(playerColor[0], playerColor[1], playerColor[2], 1);
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
		else
		{
			squidTime = 0;
		}
	}
	private static void renderMatchGui(GuiGraphics graphics, float tickDelta, int width, int height, String stageId, EntityInfo info, PoseStack matrixStack)
	{
		SaveInfo saveInfo = SaveInfoCapability.get();
		PlaySession session = saveInfo.playSessions().get(stageId);
		float nowSeconds = (ClientUtils.getClient().level.getGameTime() + tickDelta) / 20f;
		if (session != null && nowSeconds > (session.getMatchStartTime() - 3 * 20) / 20f)
		{
			Minecraft mc = Minecraft.getInstance();
			Font font = mc.font;
			matrixStack.pushPose();
			renderMatchTopLabels(graphics, width, nowSeconds, session, font);
			renderMatchPlayers(graphics, width, session, font);
			
			if (info.isMatchRespawning())
			{
				if (info.getMatchRespawnTimeLeft() < 60)
				{
					String label = "Uh oh moriste";
					graphics.drawString(font, label, width / 2 - font.width(label) / 2, height - 100, -1, true);
					label = "Respawn in " + (info.getMatchRespawnTimeLeft() / 20);
					graphics.drawString(font, label, width / 2 - font.width(label) / 2, height - 80, -1, true);
				}
			}
			
			matrixStack.popPose();
		}
	}
	private static void renderMatchPlayers(GuiGraphics graphics, int width, PlaySession session, Font font)
	{
		final int y = 10;
		final int iconWidth = 14, playerPadding = 4, teamPadding = 20;
		int totalWidth = 0;
		Stage stage = session.getStage();
		Map<InkColor, List<Player>> colorsWidth = new HashMap<>();
		ClientLevel level = ClientUtils.getClient().level;
		for (int i = 0; i < session.playerUuids.size(); i++)
		{
			Player player = level.getPlayerByUUID(session.playerUuids.get(i));
			if (player == null)
				continue;
			
			InkColor playerColor = ColorUtils.getEntityColor(player);
			colorsWidth.computeIfAbsent(playerColor, (col) -> new ArrayList<>()).add(player);
		}
		
		for (List<Player> list : colorsWidth.values())
		{
			totalWidth += list.size() * (iconWidth + playerPadding) - playerPadding + teamPadding;
		}
		if (totalWidth == 0)
			return;
		
		totalWidth -= teamPadding + iconWidth;
		int x = (width - totalWidth) / 2;
		for (Map.Entry<InkColor, List<Player>> entry : colorsWidth.entrySet())
		{
			int backgroundColor = FastColor.ARGB32.opaque(ColorUtils.makeBrighter(entry.getKey(), 0.3f, 0.5f));
			for (Player player : entry.getValue())
			{
				int playerColor = backgroundColor;
				if (Components.ENTITY_INFO.hasAnd(player, EntityInfo::isMatchRespawning))
					playerColor = ColorUtils.makeBrighter(playerColor, 0f, -8f);
				graphics.fill(x - iconWidth / 2, y, x + iconWidth / 2, y + iconWidth, playerColor);
				x += iconWidth + playerPadding;
			}
			x += teamPadding - playerPadding;
		}
	}
	private static void renderMatchTopLabels(GuiGraphics graphics, int width, float now, PlaySession session, Font font)
	{
		int y = 26;
		int seconds = (int) Math.max(0, session.getMatchEndTime() / 20f - now);
		int minutes = seconds / 60;
		
		String[] topLabels = new String[]
			{
				session.stageId,
				session.gameMode.name(),
				minutes + ":" + Strings.padStart(Integer.toString(seconds % 60), 2, '0')
			};
		
		float overtimeProgress = session.gameMode.overtimeChecker.test(session, ClientUtils.getClient().level);
		if (seconds == 0 && overtimeProgress != 0)
		{
			int overtimeColor = StageGameMode.getActiveColor(session.getMarkers(true)).map(v -> v.getColorWithAlpha(255))
				.orElse(0xFFACACAC);
			int splitX = width / 2 + 26 - (int) (52 * overtimeProgress);
			graphics.fill(width / 2 - 26, y + 19, splitX, y + 29, 0xFF555555);
			graphics.fill(splitX, y + 19, width / 2 + 26, y + 29, overtimeColor);
			
			topLabels[2] = "Overtime!";
		}
		for (int i = 0; i < 3; i++)
		{
			String currentLabel = topLabels[i];
			if (i == 2 && minutes <= 0)
			{
				graphics.drawCenteredString(font, Component.literal(currentLabel).withStyle(seconds < 10 ? ChatFormatting.RED : ChatFormatting.YELLOW), width / 2, y + 10 * i, -1);
				continue;
			}
			graphics.drawCenteredString(font, currentLabel, width / 2, y + 10 * i, -1);
		}
		
		switch (session.gameMode)
		{
			case SPLAT_ZONES:
				List<StageMarkerTileEntity> markers = session.getMarkers(true);
				if (markers.isEmpty())
					return;
				
				final int zonesWidth = 20 + 20 / markers.size();
				int padding = 10;
				int scoreY = y + 35;
				if (markers.size() == 1)
				{
					int color = markers.getFirst().getCurrentColor().map(v -> v.getColorWithAlpha(255)).orElse(0xFFDDDDDD);
					graphics.fill(width / 2 - zonesWidth / 2, scoreY, width / 2 + zonesWidth / 2, scoreY + padding, color);
				}
				else
				{
					float halfSize = (markers.size() - 1) / 2f;
					for (int i = 0; i < markers.size(); i++)
					{
						int color = markers.get(i).getCurrentColor().map(v -> v.getColorWithAlpha(255)).orElse(0xFFDDDDDD);
						int x = width / 2 + (int) ((i - halfSize) * (zonesWidth + padding));
						graphics.fill(x - zonesWidth / 2, scoreY, x + zonesWidth / 2, scoreY + padding, color);
					}
				}
				
				Stage stage = Stage.getStage(session.stageId);
				List<InkColor> teamColors = stage.getTeamIds().stream().map(stage::getTeamColor).toList();
				
				final int backgroundWidthHalf = 30 / 2;
				final int backgroundHeightHalf = 25 / 2;
				padding = 60;
				scoreY = 60 + y;
				if (teamColors.size() == 1) // what are you doing
				{
					drawTeamCounter(teamColors.getFirst(), session, graphics, width / 2, scoreY, backgroundWidthHalf, backgroundHeightHalf, font);
				}
				else
				{
					float halfSize = (teamColors.size() - 1) / 2f;
					for (int i = 0; i < teamColors.size(); i++)
					{
						int x = width / 2 + (int) ((i - halfSize) * (padding));
						drawTeamCounter(teamColors.get(i), session, graphics, x, scoreY, backgroundWidthHalf, backgroundHeightHalf, font);
					}
				}
				
				break;
		}
	}
	private static void drawTeamCounter(InkColor teamColor, PlaySession session, GuiGraphics graphics, int x, int y, int backgroundWidthHalf, int backgroundHeightHalf, Font font)
	{
		int color = FastColor.ARGB32.color(128, ColorUtils.makeBrighter(teamColor, 0.4f, 0.7f));
		
		PlaySession.TeamScore score = session.scores.get(teamColor);
		
		graphics.fill(x - backgroundWidthHalf, y - backgroundHeightHalf, x + backgroundWidthHalf, y + backgroundHeightHalf, color);
		
		graphics.drawCenteredString(font, String.valueOf(score.score() / 12), x, y - 8, -1);
		if (score.penalty() > 0)
		{
			graphics.pose().pushPose();
			graphics.pose().scale(0.8f, 0.8f, 0.8f);
			graphics.drawCenteredString(font, "+" + score.penalty() / 12, (int) (x / 0.8f), (int) ((y + 4) / 0.8f), 0xFFACACAC);
			graphics.pose().popPose();
		}
	}
	private static void renderDeviationGui(GuiGraphics graphics, float frameTime, int width, int height, WeaponBaseItem<?> weaponBaseItem, PoseStack matrixStack, LocalPlayer player, float[] playerColor)
	{
		float scale = width * height / 518400.0f;
		matrixStack.pushPose();
		RenderSystem.setShaderTexture(0, WIDGETS);
		RenderSystem.enableBlend();
		
		AbstractWeaponSettings<?, ?> settings = weaponBaseItem.getSettings(player.getMainHandItem());
		float speedForRender = settings.getSpeedForRender(player, player.getMainHandItem());
		if (speedForRender <= 0)
			return;
		
		CommonRecords.ShotDeviationDataRecord data = settings.getShotDeviationData(player.getMainHandItem(), player);
		SplatcraftComponents.WeaponPrecisionData deviationData = ShotDeviationHelper.getDeviationData(player.getMainHandItem());
		
		CommonUtils.Result actualChanceResult = CommonUtils.tickValue(deviationData.chanceDecreaseDelay(), deviationData.chance(), data.chanceDecreasePerTick(), data.minDeviateChance(), frameTime);
		CommonUtils.Result airInfluenceResult = CommonUtils.tickValue(deviationData.airborneDecreaseDelay(), deviationData.airborneInfluence(), data.airborneContractTimeToDecrease() == 0 ? Float.NaN : 1f / data.airborneContractTimeToDecrease(), 0, frameTime);
		
		Minecraft mc = Minecraft.getInstance();
		
		double fov = ((GameRendererFovAccessor) mc.gameRenderer).invokeGetFov(
			mc.gameRenderer.getMainCamera(),
			frameTime,
			true);
		Matrix4f projectionMatrix = mc.gameRenderer.getProjectionMatrix(
			fov);
		
		float currentAirInfluence = airInfluenceResult.value();
		float currentDeviationChance = actualChanceResult.value();
		
		float currentDeviation = Math.max(0.017453292f, Mth.lerp(ShotDeviationHelper.getModifiedAirInfluence(currentAirInfluence), data.airborneShotDeviation(), data.groundShotDeviation()) * Mth.DEG_TO_RAD / 2f);
		
		float aspectRatio = Mth.lerp(Math.min(1, (float) Math.pow(30f * currentDeviation, 2f)), 1, 0.5625f);
		
		float value = Math.min(0.71428573f, currentDeviationChance / data.maxDeviateChance() / 1.4f);
		float[] rgb = new float[]
			{
				Mth.lerp(value, 0.6f, playerColor[0]),
				Mth.lerp(value, 0.6f, playerColor[1]),
				Mth.lerp(value, 0.6f, playerColor[2])
			};
		RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], 0.4f);
		
		Vec3 deltaMovementLerped = player.getKnownMovement();
		
		deltaMovementLerped = EntityAccessor.invokeGetInputVector(deltaMovementLerped, (float) deltaMovementLerped.length(), -player.getViewYRot(frameTime));
		// TODO: do this correctly please this aproximation works half the time
		Vec3 relativePos = new Vec3(0, 0, speedForRender).add(deltaMovementLerped.x, deltaMovementLerped.y, deltaMovementLerped.z);
		double horizontalScale = Math.PI / relativePos.z;
		relativePos = relativePos.multiply(horizontalScale, horizontalScale, 1);
		float textureSize = 4 * (scale + 1);
		
		for (int x = -1; x <= 1; x += 2)
		{
			for (int y = -1; y <= 1; y += 2)
			{
				Vec3 rotatedPos = relativePos.yRot(currentDeviation * x).xRot(currentDeviation * y);
				
				Vector3f camSpace = rotatedPos.toVector3f();
				
				Vector4f projectiveCamSpace = new Vector4f(camSpace, 1f);
				projectionMatrix.transform(projectiveCamSpace);
				float w = projectiveCamSpace.w();
				
				Vector4f screenPos = new Vector4f(projectiveCamSpace.x() / w * width, projectiveCamSpace.y() / w * height, w, (float) Math.sqrt(relativePos.dot(relativePos)));
				
				// TODO: center this properly soon
				GraphicsUtils.drawTexture(graphics, WIDGETS,
					width / 2f - textureSize / 2 + screenPos.x,
					height / 2f - textureSize / 2 + screenPos.y * aspectRatio,
					textureSize, textureSize, 64 - 7 * x, 8 - 7 * y, 4, 4, 256, 256);
			}
		}
		
		RenderSystem.setShaderColor(1, 1, 1, 1);
		matrixStack.popPose();
	}
	private static void renderChargerGui(GuiGraphics graphics, float frameTime, int width, int height, EntityInfo info, PoseStack matrixStack, ItemStack itemStack)
	{
		matrixStack.pushPose();
		RenderSystem.enableBlend();
		RenderSystem.setShaderTexture(0, WIDGETS);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		
		graphics.blit(WIDGETS, width / 2 - 15, height / 2 + 14, 30, 9, 88, 0, 30, 9, 256, 256);
		float charge = info.getStoredCharge().map(v -> v.charge).orElse(itemStack.get(SplatcraftComponents.CHARGE_DATA).getCharge(frameTime));
		
		if (charge > 1)
		{
			RenderSystem.setShaderColor(1, 1, 1, 0.5f);
			graphics.blit(WIDGETS, width / 2 - 15, height / 2 + 14, 30, 9, 88, 9, 30, 9, 256, 256);
			
			if (Math.floor(charge) != charge)
				charge = charge % 1f;
		}
		
		RenderSystem.setShaderColor(1, 1, 1, 0.5f);
		graphics.blit(WIDGETS, width / 2 - 15, height / 2 + 14, (int) (30 * charge), 9, 88, 9, (int) (30 * charge), 9, 256, 256);
		
		RenderSystem.setShaderColor(1, 1, 1, 1);
		
		matrixStack.popPose();
	}
	public static void processHidingEntity(CallbackInfoReturnable<Boolean> cir, LivingEntity living)
	{
		LocalPlayer clientPlayer = ClientUtils.getClientPlayer();
		// if the client player, for some reason, renders their own name tag, or isnt playing, then do not cancel rendering
		if (living == clientPlayer || !Components.ENTITY_INFO.hasAnd(clientPlayer, EntityInfo::isPlaying))
			return;
		
		InkColor clientColor = ColorUtils.getEntityColor(clientPlayer);
		InkColor entityColor = ColorUtils.getEntityColor(living);
		
		// if the player isnt in a match or has the same color, do not cancel rendering
		if (!Components.ENTITY_INFO.hasAnd(living, EntityInfo::isPlaying) || clientColor.equals(entityColor))
			return;
		
		// if the player killed the client player, do not cancel rendering
		if (ClientUtils.killCamData != null && living.getUUID().equals(ClientUtils.killCamData.getFirst()))
			return;
		
		// if the player is dead (in a match), do not cancel rendering
		if (Components.ENTITY_INFO.hasAnd(living, EntityInfo::isMatchRespawning))
			return;
		
		cir.setReturnValue(false);
	}
}