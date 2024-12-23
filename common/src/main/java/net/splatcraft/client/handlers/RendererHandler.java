package net.splatcraft.client.handlers;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientChatEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.message.MessageType;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.*;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.splatcraft.Splatcraft;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.client.layer.PlayerInkColoredSkinLayer;
import net.splatcraft.client.renderer.InkSquidRenderer;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.playerinfo.EntityInfo;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.handlers.ShootingHandler;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.weapons.IChargeableWeapon;
import net.splatcraft.items.weapons.SubWeaponItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.AbstractWeaponSettings;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.mixin.accessors.EntityAccessor;
import net.splatcraft.mixin.accessors.GameRendererFovAccessor;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.UUID;

import static net.splatcraft.items.weapons.WeaponBaseItem.enoughInk;

public class RendererHandler
{
	private static final Identifier WIDGETS = Splatcraft.identifierOf("textures/gui/widgets.png");
	private static float oldCooldown = 0;
	private static float tickTime = 0;
	private static InkSquidRenderer squidRenderer;
	//Render PlayerEntity HUD elements
	private static float squidTime = 0;
	private static float prevInkPctg = 0;
	private static float inkFlash = 0;
	public static void registerEvents()
	{
		ClientChatEvent.RECEIVED.register(RendererHandler::onChatMessage);
		ClientTickEvent.CLIENT_POST.register(RendererHandler::onRenderTick);
	}
	public static boolean playerRender(PlayerEntityRenderer instance, AbstractClientPlayerEntity player, float f, float g, MatrixStack matrixStack, VertexConsumerProvider consumerProvider, int i)
	{
		if (player.isSpectator()) return false;
		
		if (EntityInfoCapability.isSquid(player))
		{
			if (squidRenderer == null)
				squidRenderer = new InkSquidRenderer(InkSquidRenderer.getContext());
			if (!InkBlockUtils.canSquidHide(player))
			{
				squidRenderer.render(player, f, g, matrixStack, consumerProvider, i);
				CommonUtils.doPlayerSquidForgeEvent(player, squidRenderer, g, matrixStack, consumerProvider, i);
			}
			return true;
		}
		return false;
	}
	public static void onRenderTick(MinecraftClient client)
	{
		PlayerEntity player = ClientUtils.getClientPlayer();
		if (player != null && !player.isSpectator())
		{
			if (ShootingHandler.isDoingShootingAction(player))
			{
				player.getInventory().selectedSlot = ShootingHandler.shootingData.get(player).selected;
			}
			if (PlayerCooldown.hasPlayerCooldown(player) && PlayerCooldown.getPlayerCooldown(player).getSlotIndex() >= 0)
			{
				player.getInventory().selectedSlot = PlayerCooldown.getPlayerCooldown(player).getSlotIndex();
			}
		}
	}
	public static void renderArm(PlayerEntityRenderer instance, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve)
	{
		PlayerEntityModel<AbstractClientPlayerEntity> playerModel = instance.getModel();
		PlayerInkColoredSkinLayer.renderHand(playerModel, matrixStack, vertexConsumerProvider, light, player, arm, sleeve);
	}
	public static boolean renderHand(float tickDelta, Hand hand, MatrixStack matrices)
	{
		PlayerEntity player = ClientUtils.getClientPlayer();
		if (EntityInfoCapability.isSquid(player))
		{
			return false;
		}
		
		if (PlayerCooldown.hasPlayerCooldown(player) && PlayerCooldown.getPlayerCooldown(player).getHand().equals(hand))
		{
			PlayerCooldown cooldown = PlayerCooldown.getPlayerCooldown(player);
			float time = cooldown.getTime();
			float maxTime = cooldown.getMaxTime();
			if (time != oldCooldown)
			{
				oldCooldown = time;
				tickTime = 0;
			}
			tickTime = (tickTime + 1) % 10;
			float yOff = -0.5f * ((time - tickDelta) / maxTime);// - (tickTime/20f));
			
			if (player.getStackInHand(hand).getItem() instanceof WeaponBaseItem<?> weaponBaseItem)
			{
				switch (weaponBaseItem.getPose(player, player.getStackInHand(hand)))
				{
					case ROLL:
						yOff = -((time - tickDelta) / maxTime) + 0.5f;
						break;
					case BRUSH:
						matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(yOff * ((player.getMainArm() == Arm.RIGHT ? hand.equals(Hand.MAIN_HAND) : hand.equals(Hand.OFF_HAND)) ? 1 : -1)));
						yOff = 0;
						break;
				}
			}
			
			matrices.translate(0, yOff, 0);
		}
		else
		{
			tickTime = 0;
		}
		return true;
	}
	public static boolean renderSubWeapon(ItemStack stack, MatrixStack poseStack, VertexConsumerProvider source, int light, float partialTicks)
	{
		if (stack.getItem() instanceof SubWeaponItem subWeaponItem)
		{
			AbstractSubWeaponEntity sub = subWeaponItem.entityType.get().create(ClientUtils.getClientPlayer().clientWorld);
			sub.setColor(ColorUtils.getInkColor(stack));
			sub.setItem(stack);
			sub.readItemData(stack.get(DataComponentTypes.ENTITY_DATA).copyNbt());
			
			sub.isItem = true;
			
			poseStack.translate(.5f, .55f, .5f);
			
			MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(sub).render(sub, 0, partialTicks, poseStack, source, light);
			return true;
		}
		return false;
	}

    /*public static void renderItem(ItemRenderer itemRenderer, ItemStack stack, ModelTransformationMode renderMode, boolean leftHand, MatrixStack matrices, VertexConsumerProvider consumer, int light, int overlay, BakedModel model)
    {
        if (!stack.isEmpty())
        {
            matrices.push();
            boolean bl = renderMode == ModelTransformationMode.GUI || renderMode == ModelTransformationMode.GROUND || renderMode == ModelTransformationMode.FIXED;
            if (bl) {
                if (stack.isOf(Items.TRIDENT)) {
                    model = itemRenderer.getModels().getModelManager().getModel(ModelIdentifier.ofInventoryVariant(Identifier.ofVanilla("trident")));
                } else if (stack.isOf(Items.SPYGLASS)) {
                    model = itemRenderer.getModels().getModelManager().getModel(ModelIdentifier.ofInventoryVariant(Identifier.ofVanilla("spyglass")));
                }
            }

            model.getTransformation().getTransformation(renderMode).apply(leftHand, matrices);
            matrices.translate(-0.5F, -0.5F, -0.5F);
            if (!model.isBuiltin() && (!stack.isOf(Items.TRIDENT) || bl)) {
            {
                boolean flag1;
                if (renderMode != ModelTransformationMode.GUI && !renderMode.isFirstPerson() && stack.getItem() instanceof BlockItem blockItem)
                {
                    Block block = blockItem.getBlock();
                    flag1 = !(block instanceof TranslucentBlock) && !(block instanceof StainedGlassPaneBlock);
                }
                else
                {
                    flag1 = true;
                }

                RenderLayer renderLayer = RenderLayers.getItemLayer(stack, flag1);
                net.minecraft.client.render.VertexConsumer ivertexbuilder;
                if (stack.getItem() == Items.COMPASS || stack.getItem() == Items.CLOCK && stack.hasGlint())
                {
                    matrices.push();
                    MatrixStack.Entry matrixstack$entry = matrices.peek().copy();
                    if (renderMode == ModelTransformationMode.GUI)
                    {
                        matrixstack$entry.getMatrices().scale(0.5F);
                    }
                    else if (renderMode.firstPerson())
                    {
                        matrixstack$entry.getMatrices().scale(0.75F);
                    }

                    if (flag1)
                    {
                        ivertexbuilder = ItemRenderer.getCompassFoilBufferDirect(consumer, rendertype, matrixstack$entry);
                    }
                    else
                    {
                        ivertexbuilder = ItemRenderer.getCompassFoilBuffer(consumer, rendertype, matrixstack$entry);
                    }

                    matrices.pop();
                }
                else if (flag1)
                {
                    ivertexbuilder = ItemRenderer.getFoilBufferDirect(consumer, rendertype, true, stack.hasGlint());
                }
                else
                {
                    ivertexbuilder = ItemRenderer.getFoilBuffer(consumer, rendertype, true, stack.hasGlint());
                }

                itemRenderer.renderModelLists(model, stack, light, overlay, matrices, ivertexbuilder);
            }
            else
            {
                itemRenderer. IClientItemExtensions.of(stack).getCustomRenderer().renderByItem(stack, renderMode, matrices, consumer, light, overlay);
            }

            matrices.pop();
        }
    }*/
	public static CompoundEventResult<Text> onChatMessage(MessageType.Parameters parameretes, Text message)
	{
		ClientWorld level = MinecraftClient.getInstance().world;
		if (level != null && Boolean.TRUE.equals(SplatcraftConfig.get("splatcraft.coloredPlayerNames")))
		{
			HashMap<String, UUID> players = new HashMap<>();
			ClientPlayNetworkHandler connection = MinecraftClient.getInstance().getNetworkHandler();
			if (connection != null)
			{
				for (PlayerListEntry info : connection.getPlayerList())
				{
					players.put(getDisplayName(info).getString(), info.getProfile().getId());
				}
			}
			
			if (!(message.getContent() instanceof TranslatableTextContent translatableContents))
			{
				return CompoundEventResult.pass();
			}
			
			for (Object arg : translatableContents.getArgs())
			{
				if (!(arg instanceof MutableText msgChildren))
					continue;
				String key = msgChildren.getString();
				
				if (players.containsKey(key))
					msgChildren.setStyle(msgChildren.getStyle().withColor(TextColor.fromRgb(ClientUtils.getClientPlayerColor(players.get(key)).getColor())));
			}
		}
		return CompoundEventResult.interruptTrue(message);
	}
	public static Text modifyNameplate(Entity entity, Text label)
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
				label = ((MutableText) label).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.getColor())));
			}
		}
		return label;
	}
	public static Text getDisplayName(PlayerListEntry info)
	{
		return info.getDisplayName() != null ? info.getDisplayName().copy() : Team.decorateName(info.getScoreboardTeam(), Text.literal(info.getProfile().getName()));
	}
	public static EventResult renderGui(DrawContext context, RenderTickCounter tickCounter)
	{
		int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
		int height = MinecraftClient.getInstance().getWindow().getScaledHeight();
		renderGuiInternal(context, tickCounter.getTickDelta(true), width, height);
		return EventResult.pass();
	}
	public static void renderGuiInternal(DrawContext graphics, float frameTime, int width, int height)
	{
		ClientPlayerEntity player = ClientUtils.getClientPlayer();
		boolean hasCapability = EntityInfoCapability.hasCapability(player);
		if (player.isSpectator() || !hasCapability)
		{
			return;
		}
		EntityInfo info = EntityInfoCapability.get(player);
		//if (event.getType().equals(RenderGameOverlayEvent.ElementType.LAYER))
		{
			if (player.getMainHandStack().getItem() instanceof IChargeableWeapon || player.getOffHandStack().getItem() instanceof IChargeableWeapon)
			{
				MatrixStack matrixStack = graphics.getMatrices();
				matrixStack.push();
				RenderSystem.enableBlend();
				RenderSystem.setShaderTexture(0, WIDGETS);
				RenderSystem.setShaderColor(1, 1, 1, 1);
				
				graphics.drawTexture(WIDGETS, width / 2 - 15, height / 2 + 14, 30, 9, 88, 0, 30, 9, 256, 256);
				if (info.getPlayerCharge() != null)
				{
					PlayerCharge playerCharge = info.getPlayerCharge();
					float charge = lerp(playerCharge.prevCharge, playerCharge.charge, frameTime);
					
					if (charge > 1)
					{
						RenderSystem.setShaderColor(1, 1, 1, playerCharge.getDischargeValue(frameTime) * 0.05f);
						graphics.drawTexture(WIDGETS, width / 2 - 15, height / 2 + 14, 30, 9, 88, 9, 30, 9, 256, 256);
						
						if (Math.floor(charge) != charge)
							charge = charge % 1f;
					}
					
					RenderSystem.setShaderColor(1, 1, 1, playerCharge.getDischargeValue(frameTime));
					graphics.drawTexture(WIDGETS, width / 2 - 15, height / 2 + 14, (int) (30 * charge), 9, 88, 9, (int) (30 * charge), 9, 256, 256);
				}
				RenderSystem.setShaderColor(1, 1, 1, 1);
				
				matrixStack.pop();
			}
		}
		
		InkColor color = ColorUtils.getColorLockedIfConfig(info.getColor());
		float[] playerColor = color.getRGB();
		if (player.getMainHandStack().getItem() instanceof WeaponBaseItem<?> weaponBaseItem)
		{
			float scale = width * height / 518400.0f;
			MatrixStack matrixStack = graphics.getMatrices();
			matrixStack.push();
			RenderSystem.setShaderTexture(0, WIDGETS);
			RenderSystem.enableBlend();
			
			AbstractWeaponSettings<?, ?> settings = weaponBaseItem.getSettings(player.getMainHandStack());
			CommonRecords.ShotDeviationDataRecord data = settings.getShotDeviationData(player.getMainHandStack(), player);
			SplatcraftComponents.WeaponPrecisionData deviationData = ShotDeviationHelper.getDeviationData(player.getMainHandStack());
			
			CommonUtils.Result actualChanceResult = CommonUtils.tickValue(deviationData.chanceDecreaseDelay(), deviationData.chance(), data.chanceDecreasePerTick(), data.minDeviateChance(), frameTime);
			CommonUtils.Result airInfluenceResult = CommonUtils.tickValue(deviationData.airborneDecreaseDelay(), deviationData.airborneInfluence(), data.airborneContractTimeToDecrease() == 0 ? Float.NaN : 1f / data.airborneContractTimeToDecrease(), 0, frameTime);
			
			MinecraftClient mc = MinecraftClient.getInstance();
			
			double fov = ((GameRendererFovAccessor) mc.gameRenderer).invokeGetFov(
				mc.gameRenderer.getCamera(),
				frameTime,
				true);
			Matrix4f projectionMatrix = mc.gameRenderer.getBasicProjectionMatrix(
				fov);
			
			float currentAirInfluence = airInfluenceResult.value();
			float currentDeviationChance = actualChanceResult.value();
			
			float currentDeviation = Math.max(0.017453292f, MathHelper.lerp(ShotDeviationHelper.getModifiedAirInfluence(currentAirInfluence), data.airborneShotDeviation(), data.groundShotDeviation()) * MathHelper.RADIANS_PER_DEGREE / 2f * 1.34f);
			
			float aspectRatio = MathHelper.lerp(Math.min(1, (float) Math.pow(30f * currentDeviation, 2f)), 1, 0.5625f);
			
			float value = Math.min(0.71428573f, currentDeviationChance / data.maxDeviateChance() / 1.4f);
			float[] rgb = new float[]
				{
					MathHelper.lerp(value, 0.6f, playerColor[0]),
					MathHelper.lerp(value, 0.6f, playerColor[1]),
					MathHelper.lerp(value, 0.6f, playerColor[2])
				};
			RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], 0.4f);
			
			Vec3d deltaMovementLerped = player.getMovement();
			
			deltaMovementLerped = EntityAccessor.invokeMovementInputToVelocity(deltaMovementLerped, (float) deltaMovementLerped.length(), -player.getYaw(frameTime));
			// TODO: do this correctly please this aproximation works half the time
			Vec3d relativePos = new Vec3d(0, 0, weaponBaseItem.getSettings(player.getMainHandStack()).getSpeedForRender(player, player.getMainHandStack())).add(deltaMovementLerped.x, deltaMovementLerped.y, deltaMovementLerped.z);
			double horizontalScale = Math.PI / relativePos.z;
			relativePos = relativePos.multiply(horizontalScale, horizontalScale, 1);
			float textureSize = 4 * (scale + 1);
			
			for (int x = -1; x <= 1; x += 2)
			{
				for (int y = -1; y <= 1; y += 2)
				{
					Vec3d rotatedPos = relativePos.rotateY(currentDeviation * x).rotateX(currentDeviation * y);
					
					Vector3f camSpace = rotatedPos.toVector3f();
					
					Vector4f projectiveCamSpace = new Vector4f(camSpace, 1f);
					projectionMatrix.transform(projectiveCamSpace);
					float w = projectiveCamSpace.w();
					
					Vector4f screenPos = new Vector4f(projectiveCamSpace.x() / w * width, projectiveCamSpace.y() / w * height, w, (float) Math.sqrt(relativePos.dotProduct(relativePos)));
					
					// TODO: center this properly soon
					GraphicsUtils.drawTexture(graphics, WIDGETS,
						width / 2f - textureSize / 2 + screenPos.x,
						height / 2f - textureSize / 2 + (screenPos.y * aspectRatio),
						textureSize, textureSize, 64 - 7 * x, 8 - 7 * y, 4, 4, 256, 256);
				}
			}
			
			RenderSystem.setShaderColor(1, 1, 1, 1);
			matrixStack.pop();
		}
		
		Object inkIndicator = SplatcraftConfig.get("splatcraft.inkIndicator");
		boolean showCrosshairInkIndicator = inkIndicator.equals(SplatcraftConfig.InkIndicator.BOTH) || inkIndicator.equals(SplatcraftConfig.InkIndicator.CROSSHAIR);
		boolean isHoldingMatchItem = player.getMainHandStack().isIn(SplatcraftTags.Items.MATCH_ITEMS) || player.getOffHandStack().isIn(SplatcraftTags.Items.MATCH_ITEMS);
		boolean showLowInkWarning = showCrosshairInkIndicator && Boolean.TRUE.equals(SplatcraftConfig.get("splatcraft.lowInkWarning")) && (isHoldingMatchItem || info.isSquid()) && !enoughInk(player, null, 10f, 0, false);
		
		boolean canUse = true;
		float inkPctg = 0;
		boolean isCoolingDown = false;
		if (player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof InkTankItem tankItem)
		{
			ItemStack stack = player.getEquippedStack(EquipmentSlot.CHEST);
			inkPctg = InkTankItem.getInkAmount(stack) / tankItem.capacity;
			isCoolingDown = !InkTankItem.canRecharge(stack, false);
			if (isHoldingMatchItem)
				canUse = tankItem.canUse(player.getMainHandStack().getItem()) || tankItem.canUse(player.getOffHandStack().getItem());
		}
		if (info.isSquid() || showLowInkWarning || !canUse)
		{
			squidTime += 0.15f * frameTime;
			
			if (showCrosshairInkIndicator)
			{
				int heightAnim = Math.min(14, (int) (squidTime));
				int glowAnim = Math.max(0, Math.min(18, (int) (squidTime) - 16));
				
				MatrixStack matrixStack = graphics.getMatrices();
				matrixStack.push();
				RenderSystem.enableBlend();
				RenderSystem.setShaderTexture(0, WIDGETS);
				
				if (enoughInk(player, null, 220, 0, false))
				{ // checks if you have unlimited ink
					graphics.drawTexture(WIDGETS, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 2, 0, 131, 18, 2, 256, 256);
					graphics.drawTexture(WIDGETS, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 4 + heightAnim, 0, 131, 18, 4 + heightAnim, 256, 256);
					
					RenderSystem.setShaderColor(playerColor[0], playerColor[1], playerColor[2], 1);
					
					graphics.drawTexture(WIDGETS, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 4 + heightAnim, 18, 131, 18, 4 + heightAnim, 256, 256);
					graphics.drawTexture(WIDGETS, width / 2 + 9 + 18 - glowAnim, height / 2 - 9, glowAnim, 18, 18 - glowAnim, 149, glowAnim, 18, 256, 256);
				}
				else
				{
					graphics.drawTexture(WIDGETS, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 2, 0, 95, 18, 2, 256, 256);
					graphics.drawTexture(WIDGETS, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 4 + heightAnim, 0, 95, 18, 4 + heightAnim, 256, 256);
					
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
					
					float inkPctgLerp = lerp(prevInkPctg, inkPctg, 0.05f);
					float inkSize = (1 - inkPctg) * 18;
					
					RenderSystem.setShaderColor(playerColor[0] + inkFlash, playerColor[1] + inkFlash, playerColor[2] + inkFlash, 1);
					matrixStack.translate(0, inkSize - Math.floor(inkSize), 0);
					graphics.drawTexture(WIDGETS, width / 2 + 9, (int) (height / 2 - 9 + (14 - heightAnim) + (1 - inkPctgLerp) * 18), 18, (int) ((4 + heightAnim) * inkPctgLerp), 18, 95 + inkSize, 18, (int) ((4 + heightAnim) * inkPctg), 256, 256);
					matrixStack.translate(0, -(inkSize - Math.floor(inkSize)), 0);
					
					if (SplatcraftConfig.get("splatcraft.vanillaInkDurability"))
					{
						float[] durRgb = ColorUtils.hexToRGB(MathHelper.hsvToRgb(Math.max(0.0F, inkPctgLerp) / 3.0F, 1.0F, 1.0F));
						RenderSystem.setShaderColor(durRgb[0], durRgb[1], durRgb[2], 1);
					}
					else
					{
						RenderSystem.setShaderColor(playerColor[0], playerColor[1], playerColor[2], 1);
					}
					
					graphics.drawTexture(WIDGETS, width / 2 + 9 + 18 - glowAnim, height / 2 - 9, glowAnim, 18, 18 - glowAnim, 113, glowAnim, 18, 256, 256);
					
					RenderSystem.setShaderColor(1, 1, 1, 1);
					if (glowAnim == 18)
					{
						if (!canUse)
						{
							graphics.drawTexture(WIDGETS, width / 2 + 9, height / 2 - 9, 36, 112, 18, 18, 256, 256);
						}
						else if (showLowInkWarning)
						{
							graphics.drawTexture(WIDGETS, width / 2 + 9, height / 2 - 9, 18, 112, 18, 18, 256, 256);
						}
					}
				}
				RenderSystem.setShaderColor(1, 1, 1, 1);
				matrixStack.pop();
			}
			prevInkPctg = inkPctg;
		}
		else
		{
			squidTime = 0;
		}
	}
	public static void blitCentered(DrawContext graphics, Identifier atlasLocation, int pX, int pY, int pWidth, int pHeight, float pUOffset, float pVOffset, int pUWidth, int pVHeight, int pTextureWidth, int pTextureHeight)
	{
		graphics.drawTexture(atlasLocation,
			pX + pWidth / 2,
			pY - pHeight / 2,
			pWidth,
			pHeight,
			pUOffset,
			pVOffset,
			pUWidth,
			pVHeight,
			pTextureWidth,
			pTextureHeight);
	}
	private static float lerp(float a, float b, float f)
	{
		return a + f * (b - a);
	}
}