package net.splatcraft.items;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.dummys.ISplatcraftForgeItemDummy;
import net.splatcraft.handlers.WeaponHandler;
import net.splatcraft.items.weapons.RollerItem;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.EntityStoredCharge;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.action.EntityAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InkTankItem extends ColoredArmorItem implements ISplatcraftForgeItemDummy
{
	public static final ArrayList<InkTankItem> inkTanks = new ArrayList<>();
	public InkTankItem(String tagId, float capacity, Holder<ArmorMaterial> material, Item.Properties settings)
	{
		super(material, Type.CHESTPLATE, settings.component(SplatcraftComponents.TANK_DATA, new SplatcraftComponents.TankData(false, false, 0, capacity, 0)));

		inkTanks.add(this);
		SplatcraftTags.Items.putInkTankTags(this, tagId);
	}
	public InkTankItem(String tagId, float capacity, Holder<ArmorMaterial> material)
	{
		this(tagId, capacity, material, new Item.Properties().stacksTo(1));
	}
	public InkTankItem(String name, float capacity)
	{
		this(name, capacity, SplatcraftItems.DEFAULT_INK_TANK_MATERIAL);
	}
	public static float getInkAmount(ItemStack stack)
	{
		Optional<SplatcraftComponents.@Nullable TankData> data = getTankDataOptional(stack);
		return data.map(v -> v.infiniteInk() ?
			v.maxCapacity() :
			Mth.clamp(v.inkLevel(), 0, v.maxCapacity())).orElse(0f);
	}
	public static float getInkCapacity(ItemStack stack)
	{
		Optional<SplatcraftComponents.@Nullable TankData> data = getTankDataOptional(stack);
		return data.map(SplatcraftComponents.TankData::maxCapacity).orElse(0f);
	}
	public static float getInkPercentage(ItemStack stack)
	{
		Optional<SplatcraftComponents.@Nullable TankData> data = getTankDataOptional(stack);
		return data.map(v -> v.infiniteInk() ?
			1f :
			Mth.clamp(v.inkLevel() / v.maxCapacity(), 0, 1f)).orElse(0f);
	}
	private static Optional<SplatcraftComponents.@Nullable TankData> getTankDataOptional(ItemStack stack)
	{
		return Optional.ofNullable(stack.get(SplatcraftComponents.TANK_DATA));
	}
	private static SplatcraftComponents.@Nullable TankData getTankData(ItemStack stack)
	{
		return stack.get(SplatcraftComponents.TANK_DATA);
	}
	public static void setInkAmount(ItemStack stack, float amount)
	{
		stack.update(SplatcraftComponents.TANK_DATA, SplatcraftComponents.TankData.DEFAULT, v -> v.withInkLevelClamped(amount));
	}
	public static void setInkAmountUnclamped(ItemStack stack, float amount)
	{
		stack.update(SplatcraftComponents.TANK_DATA, SplatcraftComponents.TankData.DEFAULT, v -> v.withInkLevel(amount));
	}
	public static boolean canRecharge(ItemStack stack, boolean updateCooldown)
	{
		return getInkRecoveryCooldownMultiplier(stack, updateCooldown) >= 0;
	}
	public static float getInkRecoveryCooldownMultiplier(ItemStack stack, boolean updateCooldown)
	{
		// since ink recovery frames arent discrete amounts anymore
		// this calculates how much ink should be recovered in the next frame

		SplatcraftComponents.TankData data = getTankData(stack);

		if (data == null)
			return 0f;

		float cooldown = data.inkRecoveryCooldown();
		if (cooldown < 1)
		{
			float remainder = 1f - cooldown;
			if (updateCooldown)
				stack.update(SplatcraftComponents.TANK_DATA, SplatcraftComponents.TankData.DEFAULT, v -> v.withInkRecoveryCooldown(0));
			return remainder;
		}

		if (updateCooldown)
			stack.update(SplatcraftComponents.TANK_DATA, SplatcraftComponents.TankData.DEFAULT, v -> v.withInkRecoveryCooldown(Math.max(0, cooldown - 1)));
		return 0f;
	}
	public static void setRecoveryCooldown(ItemStack stack, float recoveryCooldown)
	{
		stack.update(SplatcraftComponents.TANK_DATA, SplatcraftComponents.TankData.DEFAULT, v -> v.withInkRecoveryCooldown(
			stack.has(SplatcraftComponents.TANK_DATA) ?
				Math.max(stack.get(SplatcraftComponents.TANK_DATA).inkRecoveryCooldown(), recoveryCooldown) :
				recoveryCooldown
		));
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);

		if (world.isClientSide())
		{
			return;
		}

		if (!(entity instanceof LivingEntity living))
		{
			return;
		}

		if (!living.getItemBySlot(EquipmentSlot.CHEST).equals(stack))
		{
			return;
		}

		if (!SplatcraftGameRules.getLocalizedRule(world, entity.blockPosition(), SplatcraftGameRules.RECHARGEABLE_INK_TANK))
		{
			return;
		}

		if (!ColorUtils.colorEquals(living, stack))
		{
			return;
		}

		float ink = getInkAmount(stack);
		if (ink >= getInkCapacity(stack))
		{
			return;
		}

		float rechargeMult = getInkRecoveryCooldownMultiplier(stack, true);
		if (rechargeMult <= 0)
		{
			return;
		}

		if (EntityAction.hasEntityAction(living) || EntityStoredCharge.hasCharge(living))
		{
			return;
		}

		Item using = living.getUseItem().getItem();

		// rollers dont have components yet so they have an specific condition
		if (WeaponHandler.getWeaponHand(living, (x, y) -> y.preventsChargingInkTank(x, living)).isPresent()
		    && (!(using instanceof RollerItem r) || r.isMoving))
		{
			return;
		}
		float inkToRecover = 0.5f;
		if (CommonUtils.isSquid(living) && InkBlockUtils.canSquidHide(living))
			inkToRecover *= 10f / 3f;

		// if a weapon is being used but doesnt prevent charging the ink tank (like chargers without enough ink), charge the ink tank slower
		if (WeaponHandler.getWeaponHand(living, (x, y) -> y.preventsChanging(x, living) && !y.preventsChargingInkTank(x, living)).isPresent())
			inkToRecover /= 2;

		inkToRecover *= rechargeMult;

		setInkAmount(stack, ink + inkToRecover);
	}
	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag type)
	{
		if (ColorUtils.isColorLocked(stack))
			tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));

		super.appendHoverText(stack, context, tooltip, type);

		if (!stack.has(DataComponents.HIDE_TOOLTIP))
		{
			if (!canRecharge(stack, false))
			{
				tooltip.add(Component.translatable("item.splatcraft.ink_tank.cant_recharge"));
			}

			if (type.isAdvanced())
			{
				tooltip.add(Component.translatable("item.splatcraft.ink_tank.ink", String.format("%.1f", getInkAmount(stack)), getInkCapacity(stack)));
			}
		}
	}
	@Override
	public int getBarWidth(@NotNull ItemStack stack)
	{
		return (int) (getInkPercentage(stack) * 13);
	}
	@Override
	public int getBarColor(@NotNull ItemStack stack)
	{
		return SplatcraftConfig.get("splatcraft.vanillaInkDurability") ? super.getBarColor(stack) : ColorUtils.getInkColor(stack).getColorWithAlpha(255);
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public boolean isBarVisible(@NotNull ItemStack stack)
	{
		SplatcraftConfig.InkIndicator inkIndicator = SplatcraftConfig.get("splatcraft.inkIndicator");
		return (inkIndicator.equals(SplatcraftConfig.InkIndicator.BOTH) || inkIndicator.equals(SplatcraftConfig.InkIndicator.DURABILITY)) &&
		       stack.has(SplatcraftComponents.TANK_DATA) && getInkPercentage(stack) < 1;
	}
	@Override
	public boolean phIsRepairable(@Nullable ItemStack stack)
	{
		return false;
	}
	public static boolean canUse(Item itemToTest, ItemStack tank)
	{
		boolean inWhitelist = itemToTest.builtInRegistryHolder().is(SplatcraftTags.Items.TANK_WHITELIST_TAG_MAP.get(tank.getItem()));
		boolean inBlacklist = itemToTest.builtInRegistryHolder().is(SplatcraftTags.Items.TANK_BLACKLIST_TAG_MAP.get(tank.getItem()));

		return !inBlacklist && inWhitelist;
	}
	public static boolean canUse(ItemStack itemToTest, ItemStack tank)
	{
		if (!(tank.getItem() instanceof InkTankItem tankItem))
			return false;

		boolean inWhitelist = itemToTest.is(SplatcraftTags.Items.TANK_WHITELIST_TAG_MAP.get(tankItem));
		boolean inBlacklist = itemToTest.is(SplatcraftTags.Items.TANK_BLACKLIST_TAG_MAP.get(tankItem));

		return !inBlacklist && inWhitelist;
	}
	public static void refill(ItemStack stack)
	{
		setInkAmount(stack, Float.POSITIVE_INFINITY);
	}
}