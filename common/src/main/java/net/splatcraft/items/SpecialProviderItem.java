package net.splatcraft.items;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.dummys.ISplatcraftForgeItemDummy;
import net.splatcraft.handlers.DataHandler;
import net.splatcraft.handlers.SpecialHandler;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.*;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendSpecialUsageDataPacket;
import net.splatcraft.registries.SplatcraftComponents.SpecialProviderData;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.specials.BaseSpecialAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static net.splatcraft.registries.SplatcraftComponents.SPECIAL_PROVIDER_DATA;

public class SpecialProviderItem extends Item implements ISplatcraftForgeItemDummy
{
	public SpecialProviderItem()
	{
		super(new Properties().durability(1).component(SPECIAL_PROVIDER_DATA, SpecialProviderData.DEFAULT));
	}
	@Override
	public @NotNull Component getName(ItemStack stack)
	{
		if (!stack.has(SPECIAL_PROVIDER_DATA))
			return Component.translatable(getDescriptionId());
		return Component.translatable(getDescriptionId() + ".active", getData(stack).getSpecialText());
	}
	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag type)
	{
		super.appendHoverText(stack, context, tooltip, type);

		SpecialProviderData data = getData(stack);
		if (data == null || (data.specialId().isEmpty() && data.weaponIdFilter().isEmpty()))
		{
			tooltip.add(Component.translatable(getDescriptionId() + ".tooltip_none").withStyle(ChatFormatting.GRAY));
			return;
		}

		if (data.weaponIdFilter().isPresent())
			tooltip.add(Component.translatable(getDescriptionId() + ".tooltip_linked_weapon", data.getWeaponFilterText()));
		else
			tooltip.add(Component.translatable(getDescriptionId() + ".tooltip_no_weapon").withStyle(ChatFormatting.GRAY));

		if (data.specialId().isEmpty())
			tooltip.add(Component.translatable(getDescriptionId() + ".tooltip_no_special").withStyle(ChatFormatting.GRAY));
		else
			tooltip.add(Component.translatable(getDescriptionId() + ".tooltip_linked_special", data.getSpecialText()));
	}
	@Override
	public boolean isBarVisible(ItemStack stack)
	{
		return stack.has(SPECIAL_PROVIDER_DATA);
	}
	@Override
	public int getBarColor(@NotNull ItemStack stack)
	{
		return SplatcraftConfig.get("splatcraft.vanillaInkDurability") ? super.getBarColor(stack) : getBarWidth(stack) == 1 ? 0xfab311 : 0xecf4c6;
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public int getBarWidth(@NotNull ItemStack stack)
	{
		SpecialProviderData data = getData(stack);
		float progress = 0;

		Optional<BaseSpecialAction> specialOptional = EntityAction.getSpecificEntityActionOptional(ClientUtils.getClientPlayer(), BaseSpecialAction.class);
		if (specialOptional.isPresent())
			progress = specialOptional.get().getSpecialCharge(stack, data != null ? data.storedCharge() : -1);
		else if (data != null)
		{
			if (data.delay() > 0 && data.maxDelay() > 0)
				progress = (float) data.delay() / data.maxDelay();
			else
				progress = data.storedCharge();
		}
		return (int) (progress * 13f);
	}
	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(Level world, Player user, @NotNull InteractionHand hand)
	{
		ItemStack stack = user.getItemInHand(hand);

		if (world.isClientSide)
			return InteractionResultHolder.pass(stack);

		// todo: some interactive menu for this lol
		ServerPlayer serverPlayer = user instanceof ServerPlayer ? (ServerPlayer) user : null;

		if (user.isShiftKeyDown())
		{
			SpecialProviderData data = getData(stack);
			if (data == null)
				data = SpecialProviderData.DEFAULT;

			List<ResourceLocation> weaponIds = new ObjectArrayList<>();
			weaponIds.addAll(DataHandler.WeaponStatsListener.getSettingsForClass(ShooterWeaponSettings.class));
			weaponIds.addAll(DataHandler.WeaponStatsListener.getSettingsForClass(RollerWeaponSettings.class));
			weaponIds.addAll(DataHandler.WeaponStatsListener.getSettingsForClass(ChargerWeaponSettings.class));
			weaponIds.addAll(DataHandler.WeaponStatsListener.getSettingsForClass(SlosherWeaponSettings.class));
			weaponIds.addAll(DataHandler.WeaponStatsListener.getSettingsForClass(BlasterWeaponSettings.class));
			weaponIds.addAll(DataHandler.WeaponStatsListener.getSettingsForClass(SplatlingWeaponSettings.CLASS));
			weaponIds.addAll(DataHandler.WeaponStatsListener.getSettingsForClass(DualieWeaponSettings.class));
			int index = data.weaponIdFilter().map(weaponIds::indexOf).orElse(-1);
			index++;
			if (index >= weaponIds.size())
				index = -1;
			data = data.withWeaponIdFilter(index == -1 ? null : weaponIds.get(index));
			setData(stack, data);

			if (serverPlayer != null)
				serverPlayer.sendSystemMessage(Component.literal("Set weapon filter to ").withStyle(ChatFormatting.RED).append(data.getWeaponFilterText()), true);
		}
		else
		{
			SpecialProviderData data = getData(stack);
			if (data == null)
				data = SpecialProviderData.DEFAULT;
			List<ResourceLocation> specialIds = SpecialHandler.getSpecialMap().keySet().stream().toList();
			int index = data.specialId().map(specialIds::indexOf).orElse(-1);
			index++;
			index %= specialIds.size();
			data = data.withSpecialId(specialIds.get(index));
			setData(stack, data);
			if (serverPlayer != null)
				serverPlayer.sendSystemMessage(Component.literal("Set special to ").withStyle(ChatFormatting.RED).append(data.getSpecialText()), true);
		}

		return InteractionResultHolder.pass(stack);
	}
	@Override
	public boolean overrideOtherStackedOnMe(@NotNull ItemStack stack, @NotNull ItemStack otherStack, @NotNull Slot slot, @NotNull ClickAction clickType, @NotNull Player player, @NotNull SlotAccess cursorStackReference)
	{
		return super.overrideOtherStackedOnMe(stack, otherStack, slot, clickType, player, cursorStackReference);
	}
	@Override
	public boolean overrideStackedOnOther(@NotNull ItemStack stack, @NotNull Slot slot, @NotNull ClickAction clickType, @NotNull Player player)
	{
		SpecialProviderData data = getData(stack);
		if (data == null || data.specialId().isEmpty() && data.weaponIdFilter().isEmpty())
			return false;

		setData(stack, data.withSpecialId(null).withWeaponIdFilter(null));
		return true;
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected)
	{
		stack.update(SPECIAL_PROVIDER_DATA, SpecialProviderData.DEFAULT, v ->
		{
			if (v.delay() > 0)
				return v.withDelay(v.delay() - 1);
			return v;
		});
		super.inventoryTick(stack, level, entity, slotId, isSelected);
	}
	public void tryUsingSpecial(Level world, LivingEntity entity, ItemStack providerStack, ItemStack weaponStack)
	{
		if (world.isClientSide())
			return;

		if (EntityAction.hasSpecificEntityAction(entity, BaseSpecialAction.class) || EntityAction.hasSpecificEntityAction(entity, SuperJumpCommand.SuperJump.class))
			return;

		SpecialProviderData data = getData(providerStack);
		ServerPlayer serverPlayer = entity instanceof ServerPlayer ? (ServerPlayer) entity : null;
		if (data == null)
		{
			if (serverPlayer != null)
				serverPlayer.sendSystemMessage(Component.translatable("status.provider_inactive").withStyle(ChatFormatting.RED), true);
			return;
		}

		if (data.specialId().isEmpty())
		{
			if (serverPlayer != null)
				serverPlayer.sendSystemMessage(Component.translatable("status.provider_unassigned_special").withStyle(ChatFormatting.RED), true);
			return;
		}

		if (!data.testWeapon(weaponStack))
		{
			if (serverPlayer != null)
				serverPlayer.sendSystemMessage(Component.translatable("status.provider_wrong_weapon").withStyle(ChatFormatting.RED), true);
			return;
		}

		if (!SpecialHandler.passesSpecialCost(providerStack))
		{
			if (serverPlayer != null)
				serverPlayer.sendSystemMessage(Component.translatable("status.not_enough_points_special").withStyle(ChatFormatting.RED), true);
			return;
		}

		if (!SpecialHandler.passesSpecialConditions(entity, providerStack) || data.delay() > 0)
		{
			if (serverPlayer != null)
				serverPlayer.sendSystemMessage(Component.translatable("status.cant_use").withStyle(ChatFormatting.RED), true);
			return;
		}

		Optional<SpecialHandler.ResetAction> mainResetAction = WeaponBaseItem.getResetAction(entity.getMainHandItem(), entity);
		Optional<SpecialHandler.ResetAction> offHandResetAction = WeaponBaseItem.getResetAction(entity.getOffhandItem(), entity);

		final Optional<SpecialHandler.ResetAction> resetFailed = Optional.of(SpecialHandler.ResetAction.RESET_FAILED);

		if (mainResetAction.equals(resetFailed) || offHandResetAction.equals(resetFailed))
			return;
		mainResetAction.ifPresent(SpecialHandler.ResetAction::run);
		offHandResetAction.ifPresent(SpecialHandler.ResetAction::run);

		providerStack.update(SPECIAL_PROVIDER_DATA, SpecialProviderData.DEFAULT, v -> v.withStoredCharge(0));

		Pair<EntitySlot, EntitySlot> providerAndWeaponSlot = SpecialHandler.startUsingSpecial(entity, data.specialId().get(), providerStack, weaponStack);
		SplatcraftPacketHandler.sendToPlayer(new SendSpecialUsageDataPacket(data.specialId().get(), entity.getUUID(), providerAndWeaponSlot.getFirst(), providerAndWeaponSlot.getSecond()), serverPlayer);
	}
	@Override
	public boolean phShouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
	{
		return !ItemStack.isSameItem(oldStack, newStack);
	}
	public SpecialProviderData getData(ItemStack stack)
	{
		return stack.getOrDefault(SPECIAL_PROVIDER_DATA, null);
	}
	public void setData(ItemStack stack, SpecialProviderData data)
	{
		stack.set(SPECIAL_PROVIDER_DATA, data);
	}
}
