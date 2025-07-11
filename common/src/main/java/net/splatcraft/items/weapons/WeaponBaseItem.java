package net.splatcraft.items.weapons;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JavaOps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.blocks.InkedBlock;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.dummys.ISplatcraftForgeItemDummy;
import net.splatcraft.handlers.DataHandler;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.handlers.SpecialHandler;
import net.splatcraft.items.IColoredItem;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.weapons.settings.*;
import net.splatcraft.items.weapons.subs.SubWeaponItem;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.platform.Components;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public abstract class WeaponBaseItem<S extends AbstractWeaponSettings<S, ?>> extends Item implements IColoredItem, ISplatcraftForgeItemDummy
{
	public static final int USE_DURATION = 72000;
	private static final HashMap<Class<? extends AbstractWeaponSettings<?, ?>>, AbstractWeaponSettings<?, ?>> DEFAULTS = new HashMap<>() // sub weapons aren't here because they're handled differently (they literally just have generics)
	{{
		put(ShooterWeaponSettings.class, ShooterWeaponSettings.DEFAULT);
		put(BlasterWeaponSettings.class, BlasterWeaponSettings.DEFAULT);
		put(RollerWeaponSettings.class, RollerWeaponSettings.DEFAULT);
		put(ChargerWeaponSettings.class, ChargerWeaponSettings.DEFAULT);
		put(SlosherWeaponSettings.class, SlosherWeaponSettings.DEFAULT);
		put(DualieWeaponSettings.class, DualieWeaponSettings.DEFAULT);
		put(SplatlingWeaponSettings.CLASS, SplatlingWeaponSettings.DEFAULT);
	}};
	public boolean isSecret;
	public WeaponBaseItem(String settingsId)
	{
		this(settingsId, true);
	}
	public WeaponBaseItem(String settingsId, boolean withPrecisionComponent)
	{
		this(settingsId, v -> v, withPrecisionComponent);
	}
	public WeaponBaseItem(String settingsId, UnaryOperator<Properties> propertiesMutator)
	{
		this(settingsId, propertiesMutator, true);
	}
	public WeaponBaseItem(String settingsId, UnaryOperator<Properties> propertiesMutator, boolean withPrecisionComponent)
	{
		this(settingsId,
			propertiesMutator.apply(withPrecisionComponent ?
				new Properties().stacksTo(1).component(SplatcraftComponents.WEAPON_PRECISION_DATA, SplatcraftComponents.WeaponPrecisionData.DEFAULT) :
				new Properties().stacksTo(1)
			)
		);
	}
	public WeaponBaseItem(String settingsId, Properties settings)
	{
		super(settings.
			component(
				SplatcraftComponents.WEAPON_SETTING_ID,
				CodecUtils.Codecs.SPLATCRAFT_IDENTIFIER_CODEC.parse(JavaOps.INSTANCE, settingsId).getOrThrow()
			)
		);
		SplatcraftItems.inkColoredItems.add(this);
		SplatcraftItems.weapons.add(this);
		
		CauldronInteraction.WATER.map().put(this, (state, level, pos, player, hand, stack) ->
		{
			if (ColorUtils.isColorLocked(stack) && !player.isShiftKeyDown())
			{
				ColorUtils.withColorLocked(stack, false);
				
				player.awardStat(Stats.USE_CAULDRON);
				
				if (!player.isCreative())
					LayeredCauldronBlock.lowerFillLevel(state, level, pos);
				
				return ItemInteractionResult.sidedSuccess(level.isClientSide);
			}
			return ItemInteractionResult.FAIL;
		});
	}
	public static boolean reduceInk(LivingEntity entity, Item item, float amount, float recoveryCooldown, boolean sendMessage)
	{
		return reduceInk(entity, item, amount, recoveryCooldown, sendMessage, false);
	}
	public static boolean reduceInk(LivingEntity entity, Item item, float amount, float recoveryCooldown, boolean sendMessage, boolean force)
	{
		if (!enoughInk(entity, item, amount, recoveryCooldown, sendMessage, false) && !force) return false;
		ItemStack tank = entity.getItemBySlot(EquipmentSlot.CHEST);
		InkTankItem.setInkAmount(tank, InkTankItem.getInkAmount(tank) - amount);
		return true;
	}
	public static boolean refundInk(LivingEntity entity, float amount)
	{
		ItemStack tank = entity.getItemBySlot(EquipmentSlot.CHEST);
		InkTankItem.setInkAmount(tank, InkTankItem.getInkAmount(tank) + amount);
		return true;
	}
	public static boolean enoughInk(LivingEntity entity, Item item, float consumption, float recoveryCooldown, boolean sendMessage)
	{
		return enoughInk(entity, item, consumption, recoveryCooldown, sendMessage, false);
	}
	public static boolean enoughInk(LivingEntity entity, Item item, float consumption, float recoveryCooldown, boolean sendMessage, boolean sub)
	{
		ItemStack tank = entity.getItemBySlot(EquipmentSlot.CHEST);
		if (!SplatcraftGameRules.getLocalizedRule(entity.level(), entity.blockPosition(), SplatcraftGameRules.REQUIRE_INK_TANK)
			|| entity instanceof Player plr && plr.isCreative()
			&& SplatcraftGameRules.getBooleanRuleValue(entity.level(), SplatcraftGameRules.INFINITE_INK_IN_CREATIVE))
		{
			return true;
		}
		boolean enoughInk = InkTankItem.getInkAmount(tank) - consumption >= 0
			&& (item == null || InkTankItem.canUse(item, tank));
		if (!sub || enoughInk)
			InkTankItem.setRecoveryCooldown(tank, recoveryCooldown);
		if (!enoughInk && sendMessage)
			sendNoInkMessage(entity, sub ? SplatcraftSounds.noInkSub : SplatcraftSounds.noInkMain);
		return enoughInk;
	}
	public static boolean hasInkInTank(LivingEntity entity, Item item)
	{
		ItemStack tank = entity.getItemBySlot(EquipmentSlot.CHEST);
		if (!SplatcraftGameRules.getLocalizedRule(entity.level(), entity.blockPosition(), SplatcraftGameRules.REQUIRE_INK_TANK)
			|| entity instanceof Player player && player.isCreative()
			&& SplatcraftGameRules.getBooleanRuleValue(entity.level(), SplatcraftGameRules.INFINITE_INK_IN_CREATIVE))
		{
			return true;
		}
		
		return InkTankItem.getInkAmount(tank) > 0 && InkTankItem.canUse(item, tank);
	}
	public static void sendNoInkMessage(LivingEntity entity, SoundEvent sound)
	{
		if (entity instanceof Player player)
		{
			player.displayClientMessage(Component.translatable("status.no_ink").withStyle(ChatFormatting.RED), true);
			if (sound != null)
				playNoInkSound(entity, sound);
		}
	}
	public static void playNoInkSound(LivingEntity entity, SoundEvent sound)
	{
		entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, 0.8F,
			CommonUtils.nextTriangular(entity.level().getRandom(), 0.95f, 0.095f));
	}
	public static Optional<ResourceLocation> getWeaponId(ItemStack weaponStack)
	{
		return Optional.ofNullable(weaponStack.getOrDefault(SplatcraftComponents.WEAPON_SETTING_ID, null));
	}
	public static Optional<SpecialHandler.ResetAction> getResetAction(ItemStack stack, LivingEntity entity)
	{
		return stack.getItem() instanceof WeaponBaseItem<?> mainWeapon ? mainWeapon.getResetShootingAction(stack, entity) : Optional.empty();
	}
	public abstract Class<S> getSettingsClass();
	public S getSettings(ItemStack stack)
	{
		return getSettingsAndValidId(stack).getSecond();
	}
	public Pair<ResourceLocation, S> getSettingsAndValidId(ItemStack stack)
	{
		Optional<ResourceLocation> id = getWeaponId(stack);
		if (id.isPresent())
		{
			AbstractWeaponSettings<?, ?> settings = DataHandler.WeaponStatsListener.SETTINGS.get(id.get());
			if (settings != null && getSettingsClass().isInstance(settings))
			{
				return Pair.of(id.get(), getSettingsClass().cast(settings));
			}
		}
		return Pair.of(null, (S) DEFAULTS.get(getSettingsClass()));
	}
	public <T extends WeaponBaseItem<?>> T setSecret(boolean secret)
	{
		isSecret = secret;
		return (T) this;
	}
	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag type)
	{
		super.appendHoverText(stack, context, tooltip, type);
		
		if (ColorUtils.isColorLocked(stack))
		{
			tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));
		}
		else
		{
			tooltip.add(Component.literal(""));
		}
		
		if (!stack.has(DataComponents.HIDE_TOOLTIP))
			getSettings(stack).addStatsToTooltip(tooltip, type);
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);
		
		if (entity instanceof LivingEntity livingEntity)
		{
			CommonRecords.ShotDeviationDataRecord deviationData = getSettings(stack).getShotDeviationData(stack, livingEntity);
			if (deviationData != CommonRecords.ShotDeviationDataRecord.PERFECT_DEFAULT)
			{
				ShotDeviationHelper.tickDeviation(stack, deviationData, 1);
			}
		}
		if (entity instanceof Player player)
		{
			if (!ColorUtils.isColorLocked(stack) &&
				ColorUtils.getInkColor(stack) != ColorUtils.getEntityColor(player) &&
				Components.ENTITY_INFO.has(player))
				ColorUtils.withInkColor(stack, ColorUtils.getEntityColor(player));
			
			if (player.getCooldowns().isOnCooldown(stack.getItem()))
			{
				if (CommonUtils.isSquid(player))
				{
					Components.ENTITY_INFO.get(player).setIsSquid(false);
					if (!world.isClientSide())
					{
						SplatcraftPacketHandler.sendToTrackers(new PlayerSetSquidS2CPacket(player.getUUID(), false), player);
					}
				}
				
				player.setSprinting(false);
				if (Inventory.isHotbarSlot(itemSlot))
				{
					player.getInventory().selected = itemSlot;
				}
			}
		}
	}
	@Override
	public boolean phOnEntityItemUpdate(ItemStack stack, ItemEntity entity)
	{
		BlockPos pos = entity.blockPosition().below();
		
		if (entity.level().getBlockState(pos).getBlock() instanceof InkwellBlock)
		{
			if (ColorUtils.getInkColor(stack) != ColorUtils.getEffectiveColor(entity.level(), pos))
			{
				ColorUtils.withInkColor(entity.getItem(), ColorUtils.getEffectiveColor(entity.level(), pos));
				ColorUtils.withColorLocked(entity.getItem(), true);
			}
		}
		else if ((!(stack.getItem() instanceof SubWeaponItem) || !SubWeaponItem.singleUse(stack))
			&& InkedBlock.causesClear(entity.level(), pos, entity.level().getBlockState(pos)) && ColorUtils.getInkColor(stack) != InkColor.constructOrReuse(0xFFFFFF))
		{
			ColorUtils.withInkColor(stack, InkColor.constructOrReuse(0xFFFFFF));
			ColorUtils.withColorLocked(stack, false);
		}
		
		return false;
	}
	@Override
	public int getBarWidth(@NotNull ItemStack stack)
	{
		try
		{
			return (int) (ClientUtils.getDurabilityForDisplay() * 13);
		}
		catch (NoClassDefFoundError e)
		{
			return 13;
		}
	}
	@Override
	public int getBarColor(@NotNull ItemStack stack)
	{
		return SplatcraftConfig.get("splatcraft.vanillaInkDurability") ? super.getBarColor(stack) : ColorUtils.getInkColor(stack).getColor();
	}
	@Override
	public boolean isBarVisible(@NotNull ItemStack stack)
	{
		try
		{
			return ClientUtils.showDurabilityBar(stack);
		}
		catch (NoClassDefFoundError e)
		{
			return false;
		}
	}
	@Override
	public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity)
	{
		return USE_DURATION;
	}
	public final InteractionResultHolder<ItemStack> useSuper(Level world, Player player, InteractionHand hand)
	{
		return super.use(world, player, hand);
	}
	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, @NotNull InteractionHand hand)
	{
		if (!(player.isSwimming() && !player.isUnderWater()))
			player.startUsingItem(hand);
		return useSuper(world, player, hand);
	}
	@Override
	public void onUseTick(@NotNull Level world, @NotNull LivingEntity user, ItemStack stack, int remainingUseTicks)
	{
		if (remainingUseTicks == stack.getUseDuration(user))
		{
			user.swimAmount = 0.0F;
		}
		// this returns true if there is no cooldown, or the cooldown has preventWeaponUse set as false
		boolean notPreventedByAction = !EntityAction.hasEntityActionAnd(user, EntityAction::preventWeaponUse);
		
		if (notPreventedByAction && !(user instanceof Player player && CommonUtils.anyWeaponOnCooldown(player)))
		{
			weaponUseTick(world, user, stack, remainingUseTicks);
			user.setSprinting(false);
		}
	}
	@Override
	public void releaseUsing(@NotNull ItemStack stack, @NotNull Level world, @NotNull LivingEntity user, int remainingUseTicks)
	{
		super.releaseUsing(stack, world, user, remainingUseTicks);
	}
	public void weaponUseTick(Level world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
	{
	
	}
	public boolean hasSpeedModifier(LivingEntity entity, ItemStack stack)
	{
		return getSpeedModifier(entity, stack) != null;
	}
	public AttributeModifier getSpeedModifier(LivingEntity entity, ItemStack stack)
	{
		return getSettings(stack).getSpeedModifier();
	}
	public PlayerPosingHandler.WeaponPose getPose(Player player, ItemStack stack)
	{
		return PlayerPosingHandler.WeaponPose.NONE;
	}
	@Override
	public boolean phShouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
	{
		return !ItemStack.isSameItem(oldStack, newStack);
	}
	public abstract Optional<SpecialHandler.ResetAction> getResetShootingAction(ItemStack stack, LivingEntity entity);
	public boolean preventsChanging(ItemStack stack, LivingEntity entity)
	{
		return false;
	}
	public boolean preventsSquidForm(ItemStack stack, LivingEntity entity)
	{
		return preventsChanging(stack, entity);
	}
	public boolean preventsChargingInkTank(ItemStack stack, LivingEntity entity)
	{
		return preventsChanging(stack, entity);
	}
}