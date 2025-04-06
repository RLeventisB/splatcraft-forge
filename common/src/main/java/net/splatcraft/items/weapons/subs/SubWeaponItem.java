package net.splatcraft.items.weapons.subs;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.handlers.DataHandler;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.DynamicDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkBlockUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class SubWeaponItem<Data extends DynamicDataRecord<Data>> extends WeaponBaseItem<SubWeaponSettings<Data>>
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
	public void appendHoverText(@NotNull ItemStack stack, @Nullable TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
	{
		if (singleUse(stack))
			tooltip.add(Component.translatable("item.splatcraft.tooltip.single_use"));
		super.appendHoverText(stack, context, tooltip, flag);
	}
	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, @NotNull InteractionHand hand)
	{
		// this !(bool && bool) confuses me
		if (!(player.isSwimming() && !player.isUnderWater()) && (singleUse(player.getItemInHand(hand)) || enoughInk(player, this, getSettings(player.getItemInHand(hand)).dataRecord.inkUsage().consumption(), 0, true, true)))
			player.startUsingItem(hand);
		return useSuper(world, player, hand);
	}
	@Override
	public int phGetMaxStackSize(ItemStack stack)
	{
		return singleUse(stack) ? 16 : 1;
	}
	@Override
	public boolean isBarVisible(@NotNull ItemStack stack)
	{
		return !singleUse(stack) && super.isBarVisible(stack);
	}
	public abstract void useSub(@NotNull ItemStack itemStack, @NotNull Level world, @NotNull LivingEntity entity, int remainingUseTicks);
	@Override
	public boolean useOnRelease(ItemStack stack)
	{
		return super.useOnRelease(stack);
	}
	@Override // onStoppedUsing doesn't get called when the timeleft is 0??? but why :(
	public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity)
	{
		entity.releaseUsingItem();
		return stack;
	}
	@Override
	public void releaseUsing(@NotNull ItemStack stack, @NotNull Level world, LivingEntity entity, int remainingUseTicks)
	{
		useSub(stack, world, entity, remainingUseTicks);
		super.releaseUsing(stack, world, entity, remainingUseTicks);
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(Player player, ItemStack stack)
	{
		return PlayerPosingHandler.WeaponPose.SUB_HOLD;
	}
	@Override
	public int getUseDuration(@NotNull ItemStack stack, LivingEntity entity)
	{
		SubWeaponSettings<Data> settings = getSettings(stack);
		if (settings != null && settings.dataRecord != null)
			return settings.dataRecord.holdTime();
		return super.getUseDuration(stack, entity);
	}
	@Override
	public SubWeaponSettings<Data> getSettings(ItemStack stack)
	{
		DataComponentMap components = stack.getComponents();
		ResourceLocation id = components.has(SplatcraftComponents.WEAPON_SETTING_ID) ? components.get(SplatcraftComponents.WEAPON_SETTING_ID) : settingsId;

		if (DataHandler.WeaponStatsListener.SETTINGS.get(id) instanceof SubWeaponSettings<?> data)
		{
			return (SubWeaponSettings<Data>) data;
		}
		if (DataHandler.WeaponStatsListener.SETTINGS.get(settingsId) instanceof SubWeaponSettings<?> data)
		{
			return (SubWeaponSettings<Data>) data;
		}
		return new SubWeaponSettings<>("default");
	}
	public static class DispenseBehavior extends DefaultDispenseItemBehavior
	{
		@Override
		public @NotNull ItemStack execute(@NotNull BlockSource source, @NotNull ItemStack stack)
		{
			if (singleUse(stack))
			{
				ItemStack thrownStack = stack.copy();
				thrownStack.remove(SplatcraftComponents.SUB_WEAPON_DATA);

				Level world = source.level();
				Position iposition = DispenserBlock.getDispensePosition(source);
				Direction direction = source.state().getValue(DispenserBlock.FACING);
				AbstractSubWeaponEntity<?> projectileentity = getProjectile(world, iposition, thrownStack);
				projectileentity.shoot(direction.getStepX(), direction.getStepY() + 0.1F, direction.getStepZ(), getPower(), getUncertainty());
				world.addFreshEntity(projectileentity);
				stack.shrink(1);

				source.level().playSound(null, source.pos(), SplatcraftSounds.subThrow, SoundSource.PLAYERS, 0.7F, 1);

				return stack;
			}

			Direction direction = source.state().getValue(DispenserBlock.FACING);
			Position iposition = DispenserBlock.getDispensePosition(source);
			ItemStack itemstack = stack.split(1);
			spawnItem(source.level(), itemstack, 6, direction, iposition);
			return stack;
		}
		protected float getPower()
		{
			return 0.7f;
		}
		protected AbstractSubWeaponEntity<?> getProjectile(Level levelIn, Position position, ItemStack stackIn)
		{
			if (!(stackIn.getItem() instanceof SubWeaponItem<?> subWeaponItem))
				return null;

			return AbstractSubWeaponEntity.create(subWeaponItem.entityType.get(), levelIn, position.x(), position.y(), position.z(), ColorUtils.getInkColor(stackIn), InkBlockUtils.InkType.NORMAL, stackIn);
		}
		protected float getUncertainty()
		{
			return 0;
		}
	}
}
