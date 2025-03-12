package net.splatcraft.items;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.dummys.ISplatcraftForgeItemDummy;
import net.splatcraft.items.weapons.RollerItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.PlayerCharge;
import net.splatcraft.util.action.EntityAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class InkTankItem extends ColoredArmorItem implements ISplatcraftForgeItemDummy
{
	public static final ArrayList<InkTankItem> inkTanks = new ArrayList<>();
	public final float capacity;
	public final Item.Properties settings;
	public InkTankItem(String tagId, float capacity, Holder<ArmorMaterial> material, Item.Properties settings)
	{
		super(material, Type.CHESTPLATE, settings.component(SplatcraftComponents.TANK_DATA, new SplatcraftComponents.TankData(false, false, 0, 0)));
		this.capacity = capacity;
		this.settings = settings;
		
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
		if (!(stack.getItem() instanceof InkTankItem inkTankItem))
			return 0;
		
		float capacity = inkTankItem.capacity;
		SplatcraftComponents.TankData data = getTankData(stack);
		if (data.infiniteInk()) return capacity;
		return Mth.clamp(data.inkLevel(), 0, capacity);
	}
	private static SplatcraftComponents.@Nullable TankData getTankData(ItemStack stack)
	{
		return stack.get(SplatcraftComponents.TANK_DATA);
	}
	public static void setInkAmount(ItemStack stack, float amount)
	{
		float capacity = ((InkTankItem) stack.getItem()).capacity;
		stack.update(SplatcraftComponents.TANK_DATA, SplatcraftComponents.TankData.DEFAULT, v -> v.withInkLevel(Math.min(capacity, amount)));
	}
	public static boolean canRecharge(ItemStack stack, boolean updateCooldown)
	{
		return rechargeMult(stack, updateCooldown) != 0;
	}
	public static float rechargeMult(ItemStack stack, boolean updateCooldown)
	{
		SplatcraftComponents.TankData data = getTankData(stack);
		
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
		stack.update(SplatcraftComponents.TANK_DATA, SplatcraftComponents.TankData.DEFAULT, v -> v.withInkRecoveryCooldown(Math.max(stack.get(SplatcraftComponents.TANK_DATA).inkRecoveryCooldown(), recoveryCooldown)));
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);
		
		if (entity instanceof Player player && !world.isClientSide() && SplatcraftGameRules.getLocalizedRule(world, entity.blockPosition(), SplatcraftGameRules.RECHARGEABLE_INK_TANK))
		{
			float ink = getInkAmount(stack);
			Item using = player.getUseItem().getItem();
			float rechargeMult = rechargeMult(stack, true);
			
			if (rechargeMult > 0 && player.getItemBySlot(EquipmentSlot.CHEST).equals(stack) && ColorUtils.colorEquals(player, stack) && ink < capacity
				&& (!EntityAction.hasEntityAction(player))
				&& !PlayerCharge.hasCharge(player)
				&& (!(using instanceof WeaponBaseItem)
				|| (using instanceof RollerItem r && !r.isMoving))
			)
			{
				setInkAmount(stack, ink + (5.0f / ((InkBlockUtils.canSquidHide(player) && EntityInfoCapability.isSquid(player)) ? 3f : 10f)) * rechargeMult);
			}
		}
	}
	@Override
	public void appendHoverText(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag type)
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
				tooltip.add(Component.translatable("item.splatcraft.ink_tank.ink", String.format("%.1f", getInkAmount(stack)), capacity));
			}
		}
	}
	@Override
	public int getBarWidth(@NotNull ItemStack stack)
	{
		return (int) (getInkAmount(stack) / capacity * 13);
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
			stack.has(SplatcraftComponents.TANK_DATA) && getInkAmount(stack) < capacity;
	}
	@Override
	public boolean phIsRepairable(@Nullable ItemStack stack)
	{
		return false;
	}
	public boolean canUse(Item item)
	{
		boolean inWhitelist = item.builtInRegistryHolder().is(SplatcraftTags.Items.INK_TANK_WHITELIST.get(this));
		boolean inBlacklist = item.builtInRegistryHolder().is(SplatcraftTags.Items.INK_TANK_BLACKLIST.get(this));
		
		return !inBlacklist && inWhitelist;
	}
	public void refill(ItemStack stack)
	{
		setInkAmount(stack, capacity);
	}
}