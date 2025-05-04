package net.splatcraft.items;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
import net.splatcraft.data.EntitySlot;
import net.splatcraft.dummys.ISplatcraftForgeItemDummy;
import net.splatcraft.handlers.DataHandler;
import net.splatcraft.handlers.SpecialHandler;
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
	public boolean selectingItem;
	public SpecialProviderItem()
	{
		super(new Properties().durability(1).component(SPECIAL_PROVIDER_DATA, SpecialProviderData.DEFAULT));
	}
	@Override
	public @NotNull Component getName(ItemStack stack)
	{
		if (stack.has(SPECIAL_PROVIDER_DATA))
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
			tooltip.add(Component.translatable(getDescriptionId() + ".tooltip_linked_weapon", data.getWeaponText()));

		tooltip.add(data.specialId().isEmpty() ?
			Component.translatable(getDescriptionId() + ".tooltip_no_special").withStyle(ChatFormatting.GRAY) :
			Component.translatable(getDescriptionId() + ".tooltip_linked_special", data.getSpecialText())
		);
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
		Player player = ClientUtils.getClientPlayer();
		Optional<BaseSpecialAction> optional = EntityAction.getSpecificActionIf(player, v -> v.isProviderStack(player, stack), BaseSpecialAction.class);
		if (optional.isPresent())
		{
			progress = optional.get().getProgress();
		}
		else if (data != null)
		{
			progress = Math.min(1f, (float) data.storedPoints() / SpecialHandler.getRequiredSpecialPoints(player.getUseItem(), stack));
		}
		return (int) (progress * 13f);
	}
	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(Level world, Player user, @NotNull InteractionHand hand)
	{
		ItemStack stack = user.getItemInHand(hand);

		if (world.isClientSide)
			return InteractionResultHolder.pass(stack);

		ServerPlayer serverPlayer = user instanceof ServerPlayer ? (ServerPlayer) user : null;

		if (user.isShiftKeyDown())
		{
			SpecialProviderData data = getData(stack);
			if (data == null)
				data = SpecialProviderData.DEFAULT;
			List<ResourceLocation> specialIds = SpecialHandler.getSpecialMap().keySet().stream().toList();
			int index = data.specialId().map(specialIds::indexOf).orElse(-1);
			index++;
			index %= specialIds.size();
			setData(stack, data.withSpecialId(specialIds.get(index)));
			if (serverPlayer != null)
				serverPlayer.sendSystemMessage(Component.literal("Set special to" + specialIds.get(index)).withStyle(ChatFormatting.RED), true);
		}
		else
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
			weaponIds.addAll(DataHandler.WeaponStatsListener.getSettingsForClass(SplatlingWeaponSettings.class));
			weaponIds.addAll(DataHandler.WeaponStatsListener.getSettingsForClass(DualieWeaponSettings.class));
			int index = data.weaponIdFilter().map(weaponIds::indexOf).orElse(-1);
			index++;
			index %= weaponIds.size();
			data = data.withWeaponIdFilter(weaponIds.get(index));
			setData(stack, data);

			if (serverPlayer != null)
				serverPlayer.sendSystemMessage(Component.literal("Set weapon to " + data.getWeaponText()).withStyle(ChatFormatting.RED), true);
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
	public void tryUsingSpecial(Level world, LivingEntity entity, ItemStack providerStack, ItemStack weaponStack)
	{
		if (world.isClientSide())
			return;

		InteractionHand hand = entity.getUsedItemHand();
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

		if (!SpecialHandler.passesSpecialCost(weaponStack, providerStack, data.specialId().get()))
		{
			if (serverPlayer != null)
				serverPlayer.sendSystemMessage(Component.translatable("status.not_enough_points_special").withStyle(ChatFormatting.RED), true);
			return;
		}

		EntitySlot entitySlot = SpecialHandler.startUsingSpecial(entity, data.specialId().get(), providerStack);
		SplatcraftPacketHandler.sendToPlayer(new SendSpecialUsageDataPacket(data.specialId().get(), entity.getUUID(), entitySlot), serverPlayer);

		entity.startUsingItem(hand);
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
