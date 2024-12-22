package net.splatcraft.items;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface ISplatcraftForgeItemDummy
{
	default boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity)
	{
		return false;
	}
	@Environment(EnvType.CLIENT)
	default void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer)
	{
	
	}
	default boolean isCombineRepairable(ItemStack stack)
	{
		return stack.contains(DataComponentTypes.MAX_DAMAGE);
	}
	default int getMaxCount(ItemStack stack)
	{
		return stack.getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 1);
	}
	default boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
	{
		return !oldStack.equals(newStack);
	}
	interface IClientItemExtensions
	{
		IClientItemExtensions DEFAULT = new IClientItemExtensions()
		{
		};
		default BipedEntityModel<?> getHumanoidArmorModel(ItemStack itemStack, EquipmentSlot layerType, BipedEntityModel<?> original)
		{
			return original;
		}
	}
}
