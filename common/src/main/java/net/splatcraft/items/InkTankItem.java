package net.splatcraft.items;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.dummys.ISplatcraftForgeItemDummy;
import net.splatcraft.items.weapons.RollerItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.PlayerCharge;
import net.splatcraft.util.PlayerCooldown;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class InkTankItem extends ColoredArmorItem implements ISplatcraftForgeItemDummy
{
	public static final ArrayList<InkTankItem> inkTanks = new ArrayList<>();
	public final float capacity;
	public final Item.Settings settings;
	public InkTankItem(String tagId, float capacity, RegistryEntry<ArmorMaterial> material, Item.Settings settings)
	{
		super(material, Type.CHESTPLATE, settings.component(SplatcraftComponents.TANK_DATA, new SplatcraftComponents.TankData(false, false, 0, 0)));
		this.capacity = capacity;
		this.settings = settings;
		
		inkTanks.add(this);
		SplatcraftTags.Items.putInkTankTags(this, tagId);
	}
	public InkTankItem(String tagId, float capacity, RegistryEntry<ArmorMaterial> material)
	{
		this(tagId, capacity, material, new Item.Settings().maxCount(1));
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
		return MathHelper.clamp(data.inkLevel(), 0, capacity);
	}
	private static SplatcraftComponents.@Nullable TankData getTankData(ItemStack stack)
	{
		return stack.get(SplatcraftComponents.TANK_DATA);
	}
	public static void setInkAmount(ItemStack stack, float amount)
	{
		float capacity = ((InkTankItem) stack.getItem()).capacity;
		stack.apply(SplatcraftComponents.TANK_DATA, SplatcraftComponents.TankData.DEFAULT, v -> v.withInkLevel(Math.min(capacity, amount)));
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
				stack.apply(SplatcraftComponents.TANK_DATA, SplatcraftComponents.TankData.DEFAULT, v -> v.withInkRecoveryCooldown(0));
			return remainder;
		}
		if (updateCooldown)
			stack.apply(SplatcraftComponents.TANK_DATA, SplatcraftComponents.TankData.DEFAULT, v -> v.withInkRecoveryCooldown(Math.max(0, cooldown - 1)));
		return 0f;
	}
	public static void setRecoveryCooldown(ItemStack stack, float recoveryCooldown)
	{
		stack.apply(SplatcraftComponents.TANK_DATA, SplatcraftComponents.TankData.DEFAULT, v -> v.withInkRecoveryCooldown(Math.max(stack.get(SplatcraftComponents.TANK_DATA).inkRecoveryCooldown(), recoveryCooldown)));
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull World world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);
		
		if (entity instanceof PlayerEntity player && !world.isClient() && SplatcraftGameRules.getLocalizedRule(world, entity.getBlockPos(), SplatcraftGameRules.RECHARGEABLE_INK_TANK))
		{
			float ink = getInkAmount(stack);
			Item using = player.getActiveItem().getItem();
			float rechargeMult = rechargeMult(stack, true);
			
			if (rechargeMult > 0 && player.getEquippedStack(EquipmentSlot.CHEST).equals(stack) && ColorUtils.colorEquals(player, stack) && ink < capacity
				&& (!PlayerCooldown.hasPlayerCooldown(player))
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
	public void appendTooltip(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Text> tooltip, @NotNull TooltipType type)
	{
		if (ColorUtils.isColorLocked(stack))
			tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));
		
		super.appendTooltip(stack, context, tooltip, type);
		
		if (!stack.contains(DataComponentTypes.HIDE_TOOLTIP))
		{
			if (!canRecharge(stack, false))
			{
				tooltip.add(Text.translatable("item.splatcraft.ink_tank.cant_recharge"));
			}
			
			if (type.isAdvanced())
			{
				tooltip.add(Text.translatable("item.splatcraft.ink_tank.ink", String.format("%.1f", getInkAmount(stack)), capacity));
			}
		}
	}
	@Override
	public int getItemBarStep(@NotNull ItemStack stack)
	{
		return (int) (getInkAmount(stack) / capacity * 13);
	}
	@Override
	public int getItemBarColor(@NotNull ItemStack stack)
	{
		return SplatcraftConfig.get("splatcraft.vanillaInkDurability") ? super.getItemBarColor(stack) : ColorUtils.getInkColor(stack).getColorWithAlpha(255);
	}
	@Environment(EnvType.CLIENT)
	@Override
	public boolean isItemBarVisible(@NotNull ItemStack stack)
	{
		SplatcraftConfig.InkIndicator inkIndicator = SplatcraftConfig.get("splatcraft.inkIndicator");
		return (inkIndicator.equals(SplatcraftConfig.InkIndicator.BOTH) || inkIndicator.equals(SplatcraftConfig.InkIndicator.DURABILITY)) &&
			stack.contains(SplatcraftComponents.TANK_DATA) && getInkAmount(stack) < capacity;
	}
	@Override
	public boolean phIsRepairable(@Nullable ItemStack stack)
	{
		return false;
	}
	public boolean canUse(Item item)
	{
		boolean inWhitelist = item.arch$holder().isIn(SplatcraftTags.Items.INK_TANK_WHITELIST.get(this));
		boolean inBlacklist = item.arch$holder().isIn(SplatcraftTags.Items.INK_TANK_BLACKLIST.get(this));
		
		return !inBlacklist && inWhitelist;
	}
	public void refill(ItemStack stack)
	{
		setInkAmount(stack, capacity);
	}
}