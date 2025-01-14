package net.splatcraft.items.weapons.subs;

import com.mojang.serialization.DataResult;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.handlers.DataHandler;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.AbstractWeaponSettings;
import net.splatcraft.items.weapons.settings.SubWeaponRecords;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class SubWeaponItem<Data extends SubWeaponRecords.SubDataRecord<Data>> extends WeaponBaseItem<SubWeaponSettings<Data>>
{
	public static final ArrayList<SubWeaponItem<?>> subs = new ArrayList<>();
	public final RegistrySupplier<? extends EntityType<? extends AbstractSubWeaponEntity<Data>>> entityType;
	public SubWeaponItem(RegistrySupplier<? extends EntityType<? extends AbstractSubWeaponEntity<Data>>> entityType, String settings)
	{
		super(settings);
		this.entityType = entityType;
		
		subs.add(this);
		DispenserBlock.registerBehavior(this, new SubWeaponItem.DispenseBehavior());
	}
	public static boolean singleUse(ItemStack stack)
	{
		return Boolean.TRUE.equals(stack.get(SplatcraftComponents.SINGLE_USE));
	}
	@Override
	public Class<SubWeaponSettings<Data>> getSettingsClass()
	{
		return (Class<SubWeaponSettings<Data>>) (Object) SubWeaponSettings.class;
	}
	@Override
	public void appendTooltip(@NotNull ItemStack stack, @Nullable TooltipContext context, @NotNull List<Text> tooltip, @NotNull TooltipType flag)
	{
		if (singleUse(stack))
			tooltip.add(Text.translatable("item.splatcraft.tooltip.single_use"));
		super.appendTooltip(stack, context, tooltip, flag);
	}
	@Override
	public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, PlayerEntity player, @NotNull Hand hand)
	{
		// this !(bool && bool) confuses me
		if (!(player.isSwimming() && !player.isSubmergedInWater()) && (singleUse(player.getStackInHand(hand)) || enoughInk(player, this, getSettings(player.getStackInHand(hand)).dataRecord.inkUsage().consumption(), 0, true, true)))
			player.setCurrentHand(hand);
		return useSuper(world, player, hand);
	}
	@Override
	public int phGetMaxStackSize(ItemStack stack)
	{
		return singleUse(stack) ? 16 : 1;
	}
	@Override
	public boolean isItemBarVisible(@NotNull ItemStack stack)
	{
		return !singleUse(stack) && super.isItemBarVisible(stack);
	}
	public abstract void useSub(@NotNull ItemStack itemStack, @NotNull World world, @NotNull LivingEntity entity, int remainingUseTicks);
	@Override
	public boolean isUsedOnRelease(ItemStack stack)
	{
		return super.isUsedOnRelease(stack);
	}
	@Override // onStoppedUsing doesn't get called when the timeleft is 0??? but why :(
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity entity)
	{
		entity.stopUsingItem();
		return stack;
	}
	@Override
	public void onStoppedUsing(@NotNull ItemStack stack, @NotNull World world, LivingEntity entity, int remainingUseTicks)
	{
		useSub(stack, world, entity, remainingUseTicks);
		super.onStoppedUsing(stack, world, entity, remainingUseTicks);
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(PlayerEntity player, ItemStack stack)
	{
		return PlayerPosingHandler.WeaponPose.SUB_HOLD;
	}
	@Override
	public int getMaxUseTime(@NotNull ItemStack stack, LivingEntity entity)
	{
		SubWeaponSettings<Data> settings = getSettings(stack);
		if (settings != null && settings.dataRecord != null)
			return settings.dataRecord.holdTime();
		return super.getMaxUseTime(stack, entity);
	}
	@Override
	public SubWeaponSettings<Data> getSettings(ItemStack stack)
	{
		ComponentMap components = stack.getComponents();
		Identifier id = components.contains(SplatcraftComponents.WEAPON_SETTING_ID) ? components.get(SplatcraftComponents.WEAPON_SETTING_ID) : settingsId;
		
		DataResult<AbstractWeaponSettings<?, ?>> result = CommonUtils.getFromMap(DataHandler.WeaponStatsListener.SETTINGS, id);
		if (result.isSuccess() && result.getOrThrow() instanceof SubWeaponSettings<?> data)
		{
			return (SubWeaponSettings<Data>) data;
		}
		else
		{
			id = settingsId;
			result = CommonUtils.getFromMap(DataHandler.WeaponStatsListener.SETTINGS, id);
			if (result.isSuccess() && result.getOrThrow() instanceof SubWeaponSettings<?> data)
			{
				return (SubWeaponSettings<Data>) data;
			}
			return new SubWeaponSettings<>("default");
		}
	}
	public static class DispenseBehavior extends ItemDispenserBehavior
	{
		@Override
		public @NotNull ItemStack dispenseSilently(@NotNull BlockPointer source, @NotNull ItemStack stack)
		{
			if (singleUse(stack))
			{
				ItemStack thrownStack = stack.copy();
				thrownStack.remove(SplatcraftComponents.SUB_WEAPON_DATA);
				
				World world = source.world();
				Position iposition = DispenserBlock.getOutputLocation(source);
				Direction direction = source.state().get(DispenserBlock.FACING);
				AbstractSubWeaponEntity<?> projectileentity = getProjectile(world, iposition, thrownStack);
				projectileentity.setVelocity(direction.getOffsetX(), direction.getOffsetY() + 0.1F, direction.getOffsetZ(), getPower(), getUncertainty());
				world.spawnEntity(projectileentity);
				stack.decrement(1);
				
				source.world().playSound(null, source.pos(), SplatcraftSounds.subThrow, SoundCategory.PLAYERS, 0.7F, 1);
				
				return stack;
			}
			
			Direction direction = source.state().get(DispenserBlock.FACING);
			Position iposition = DispenserBlock.getOutputLocation(source);
			ItemStack itemstack = stack.split(1);
			spawnItem(source.world(), itemstack, 6, direction, iposition);
			return stack;
		}
		protected float getPower()
		{
			return 0.7f;
		}
		protected AbstractSubWeaponEntity<?> getProjectile(World levelIn, Position position, ItemStack stackIn)
		{
			if (!(stackIn.getItem() instanceof SubWeaponItem<?> subWeaponItem))
				return null;
			
			return AbstractSubWeaponEntity.create(subWeaponItem.entityType.get(), levelIn, position.getX(), position.getY(), position.getZ(), ColorUtils.getInkColor(stackIn), InkBlockUtils.InkType.NORMAL, stackIn);
		}
		protected float getUncertainty()
		{
			return 0;
		}
	}
}
