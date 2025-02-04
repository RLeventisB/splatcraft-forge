package net.splatcraft.items;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
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
		super(new Settings().maxDamage(1).component(SPECIAL_PROVIDER_DATA, SpecialProviderData.DEFAULT));
	}
	@Override
	public Text getName(ItemStack stack)
	{
		SpecialProviderData data = getData(stack);
		if (data == null)
			return Text.translatable(getTranslationKey());
		return Text.translatable(getTranslationKey() + ".active", data.getSpecialText());
	}
	@Override
	public void appendTooltip(@NotNull ItemStack stack, @Nullable TooltipContext context, @NotNull List<Text> tooltip, @NotNull TooltipType type)
	{
		super.appendTooltip(stack, context, tooltip, type);
		
		SpecialProviderData data = getData(stack);
		if (data == null || (data.specialId().isEmpty() && data.weaponId().isEmpty()))
		{
			tooltip.add(Text.translatable(getTranslationKey() + ".tooltip_none").formatted(Formatting.GRAY));
			return;
		}
		
		tooltip.add(data.weaponId().isEmpty() ?
			Text.translatable(getTranslationKey() + ".tooltip_no_weapon").formatted(Formatting.GRAY) :
			Text.translatable(getTranslationKey() + ".tooltip_linked_weapon", data.getWeaponText())
		);
		tooltip.add(data.specialId().isEmpty() ?
			Text.translatable(getTranslationKey() + ".tooltip_no_special").formatted(Formatting.GRAY) :
			Text.translatable(getTranslationKey() + ".tooltip_linked_special", data.getSpecialText())
		);
	}
	@Override
	public boolean isItemBarVisible(ItemStack stack)
	{
		return getData(stack) != null;
	}
	@Override
	public int getItemBarColor(ItemStack stack)
	{
		return SplatcraftConfig.get("splatcraft.vanillaInkDurability") ? super.getItemBarColor(stack) : getItemBarStep(stack) == 1 ? 0xfab311 : 0xecf4c6;
	}
	@Override
	public int getItemBarStep(ItemStack stack)
	{
		SpecialProviderData data = getData(stack);
		float progress = 0;
		ClientPlayerEntity player = ClientUtils.getClientPlayer();
		Optional<BaseSpecialAction> optional = EntityAction.getSpecificActionIf(player, v -> v.isProviderStack(player, stack), BaseSpecialAction.class);
		if (optional.isPresent())
		{
			progress = optional.get().getProgress();
		}
		else if (data != null)
		{
			progress = Math.min(1f, (float) data.storedPoints() / SpecialHandler.getRequiredSpecialPoints(player.getActiveItem(), stack));
		}
		return (int) (progress * 13f);
	}
	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand)
	{
		ItemStack stack = user.getStackInHand(hand);
		
		if (world.isClient)
			return TypedActionResult.pass(stack);
		
		ServerPlayerEntity serverPlayer = user instanceof ServerPlayerEntity ? (ServerPlayerEntity) user : null;
		
		if (user.isSneaking())
		{
			SpecialProviderData data = getData(stack);
			if (data == null)
				data = SpecialProviderData.DEFAULT;
			List<Identifier> specialIds = SpecialHandler.getSpecialMap().keySet().stream().toList();
			int index = data.specialId().map(specialIds::indexOf).orElse(-1);
			index++;
			index %= specialIds.size();
			setData(stack, data.withSpecialId(specialIds.get(index)));
			if (serverPlayer != null)
				serverPlayer.sendMessageToClient(Text.literal("Set special to" + specialIds.get(index)).formatted(Formatting.RED), true);
		}
		else
		{
			SpecialProviderData data = getData(stack);
			if (data == null)
				data = SpecialProviderData.DEFAULT;
			
			List<Identifier> weaponIds = new ObjectArrayList<>();
			weaponIds.addAll(DataHandler.WeaponStatsListener.getSettingsForClass(ShooterWeaponSettings.class));
			weaponIds.addAll(DataHandler.WeaponStatsListener.getSettingsForClass(RollerWeaponSettings.class));
			weaponIds.addAll(DataHandler.WeaponStatsListener.getSettingsForClass(ChargerWeaponSettings.class));
			weaponIds.addAll(DataHandler.WeaponStatsListener.getSettingsForClass(SlosherWeaponSettings.class));
			weaponIds.addAll(DataHandler.WeaponStatsListener.getSettingsForClass(BlasterWeaponSettings.class));
			weaponIds.addAll(DataHandler.WeaponStatsListener.getSettingsForClass(SplatlingWeaponSettings.class));
			weaponIds.addAll(DataHandler.WeaponStatsListener.getSettingsForClass(DualieWeaponSettings.class));
			int index = data.weaponId().map(weaponIds::indexOf).orElse(-1);
			index++;
			index %= weaponIds.size();
			data = data.withWeaponId(weaponIds.get(index));
			setData(stack, data);
			
			if (serverPlayer != null)
				serverPlayer.sendMessageToClient(Text.literal("Set weapon to " + data.getWeaponText()).formatted(Formatting.RED), true);
		}
		
		return TypedActionResult.pass(stack);
	}
	@Override
	public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference)
	{
		return super.onClicked(stack, otherStack, slot, clickType, player, cursorStackReference);
	}
	@Override
	public boolean onStackClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player)
	{
		return super.onStackClicked(stack, slot, clickType, player);
	}
	public void tryUsingSpecial(World world, LivingEntity entity, ItemStack providerStack, ItemStack weaponStack)
	{
		if (world.isClient())
			return;
		
		Hand hand = entity.getActiveHand();
		SpecialProviderData data = getData(providerStack);
		ServerPlayerEntity serverPlayer = entity instanceof ServerPlayerEntity ? (ServerPlayerEntity) entity : null;
		if (data == null)
		{
			if (serverPlayer != null)
				serverPlayer.sendMessageToClient(Text.translatable("status.provider_inactive").formatted(Formatting.RED), true);
			return;
		}
		
		if (data.specialId().isEmpty())
		{
			if (serverPlayer != null)
				serverPlayer.sendMessageToClient(Text.translatable("status.provider_unassigned_special").formatted(Formatting.RED), true);
			return;
		}
		
		if (data.weaponId().isEmpty())
		{
			if (serverPlayer != null)
				serverPlayer.sendMessageToClient(Text.translatable("status.provider_unassigned_weapon").formatted(Formatting.RED), true);
			return;
		}
		
		if (!data.testWeapon(weaponStack))
		{
			if (serverPlayer != null)
				serverPlayer.sendMessageToClient(Text.translatable("status.provider_wrong_weapon").formatted(Formatting.RED), true);
			return;
		}
		
		if (!SpecialHandler.passesSpecialCost(weaponStack, providerStack, data.specialId().get()))
		{
			if (serverPlayer != null)
				serverPlayer.sendMessageToClient(Text.translatable("status.not_enough_points_special").formatted(Formatting.RED), true);
			return;
		}
		
		EntitySlot entitySlot = SpecialHandler.startUsingSpecial(entity, data.specialId().get(), providerStack);
		SplatcraftPacketHandler.sendToPlayer(new SendSpecialUsageDataPacket(data.specialId().get(), entity.getUuid(), entitySlot), serverPlayer);
		
		entity.setCurrentHand(hand);
	}
	@Override
	public boolean phShouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
	{
		return !ItemStack.areItemsEqual(oldStack, newStack);
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
