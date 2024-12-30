package net.splatcraft.items.weapons;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkBlockUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SubWeaponItem extends WeaponBaseItem<SubWeaponSettings>
{
	public static final ArrayList<SubWeaponItem> subs = new ArrayList<>();
	public final RegistrySupplier<? extends EntityType<? extends AbstractSubWeaponEntity>> entityType;
	public final SubWeaponAction useTick;
	public SubWeaponItem(RegistrySupplier<? extends EntityType<? extends AbstractSubWeaponEntity>> entityType, String settings, SubWeaponAction useTick)
	{
		super(settings);
		this.entityType = entityType;
		this.useTick = useTick;
		
		subs.add(this);
		DispenserBlock.registerBehavior(this, new SubWeaponItem.DispenseBehavior());
	}
	public SubWeaponItem(RegistrySupplier<? extends EntityType<? extends AbstractSubWeaponEntity>> entityType, String settings)
	{
		this(entityType, settings, (level, entity, stack, useTime) ->
		{
		});
	}
	public static boolean singleUse(ItemStack stack)
	{
		return Boolean.TRUE.equals(stack.get(SplatcraftComponents.SINGLE_USE));
	}
	@Override
	public Class<SubWeaponSettings> getSettingsClass()
	{
		return SubWeaponSettings.class;
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
		if (!(player.isSwimming() && !player.isSubmergedInWater()) && (singleUse(player.getStackInHand(hand)) || enoughInk(player, this, getSettings(player.getStackInHand(hand)).subDataRecord.inkConsumption(), 0, true, true)))
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
	@Override
	public void weaponUseTick(@NotNull World world, @NotNull LivingEntity entity, @NotNull ItemStack itemStack, int timeLeft)
	{
		SubWeaponSettings settings = getSettings(itemStack);
		
		int useTime = getMaxUseTime(itemStack, entity) - timeLeft;
		if (useTime == settings.subDataRecord.holdTime())
			throwSub(itemStack, world, entity);
		else if (useTime < settings.subDataRecord.holdTime())
			useTick.onUseTick(world, entity, itemStack, timeLeft);
	}
	@Override
	public void onStoppedUsing(@NotNull ItemStack stack, @NotNull World world, LivingEntity entity, int timeLeft)
	{
		super.onStoppedUsing(stack, world, entity, timeLeft);
		if (getMaxUseTime(stack, entity) - timeLeft < getSettings(stack).subDataRecord.holdTime())
			throwSub(stack, world, entity);
	}
	protected void throwSub(@NotNull ItemStack stack, @NotNull World world, LivingEntity entity)
	{
		entity.swingHand(entity.getOffHandStack().equals(stack) ? Hand.OFF_HAND : Hand.MAIN_HAND, false);
		
		SubWeaponSettings settings = getSettings(stack);
		if (!world.isClient())
		{
			AbstractSubWeaponEntity proj = AbstractSubWeaponEntity.create(entityType.get(), world, entity, stack.copy());
			
			stack.remove(SplatcraftComponents.SUB_WEAPON_DATA);
			
			proj.setItem(stack.copy());
			proj.shoot(entity, entity.getPitch(), entity.getYaw(), settings.subDataRecord.throwAngle(), settings.subDataRecord.throwVelocity(), 0);
			proj.setVelocity(proj.getVelocity().add(entity.getVelocity().multiply(1, 0, 1)));
			world.spawnEntity(proj);
		}
		world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.subThrow, SoundCategory.PLAYERS, 0.7F, 1);
		if (singleUse(stack))
		{
			if (entity instanceof PlayerEntity player && !player.isCreative())
				stack.decrement(1);
		}
		else
			reduceInk(entity, this, settings.subDataRecord.inkConsumption(), settings.subDataRecord.inkRecoveryCooldown(), false);
	}
	/*@Override
	public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer)
	{
		super.initializeClient(consumer);
		consumer.accept(new SubWeaponClientItemExtensions());
	}*/
	@Override
	public PlayerPosingHandler.WeaponPose getPose(PlayerEntity player, ItemStack stack)
	{
		return PlayerPosingHandler.WeaponPose.SUB_HOLD;
	}
	@Override
	public boolean phShouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
	{
		if (oldStack.contains(SplatcraftComponents.SUB_WEAPON_DATA) && newStack.contains(SplatcraftComponents.SUB_WEAPON_DATA))
		{
			oldStack = oldStack.copy();
			newStack = newStack.copy();
			
			oldStack.remove(SplatcraftComponents.SUB_WEAPON_DATA);
			newStack.remove(SplatcraftComponents.SUB_WEAPON_DATA);
			
			return !ItemStack.areItemsEqual(oldStack, newStack);
		}
		
		return super.phShouldCauseReequipAnimation(oldStack, newStack, slotChanged);
	}
	public interface SubWeaponAction
	{
		void onUseTick(World world, LivingEntity entity, ItemStack stack, int useTime);
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
				AbstractSubWeaponEntity projectileentity = getProjectile(world, iposition, thrownStack);
				projectileentity.shoot(direction.getOffsetX(), direction.getOffsetY() + 0.1F, direction.getOffsetZ(), getPower(), getUncertainty());
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
		protected AbstractSubWeaponEntity getProjectile(World levelIn, Position position, ItemStack stackIn)
		{
			if (!(stackIn.getItem() instanceof SubWeaponItem subWeaponItem))
				return null;
			
			return AbstractSubWeaponEntity.create(subWeaponItem.entityType.get(), levelIn, position.getX(), position.getY(), position.getZ(), ColorUtils.getInkColor(stackIn), InkBlockUtils.InkType.NORMAL, stackIn);
		}
		protected float getUncertainty()
		{
			return 0;
		}
	}
	
    /*private static class SubWeaponClientItemExtensions implements IClientItemExtensions
    {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer()
        {
            return SplatcraftItemRenderer.INSTANCE;
        }
    }*/
}
