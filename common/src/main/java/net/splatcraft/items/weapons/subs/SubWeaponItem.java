package net.splatcraft.items.weapons.subs;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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
import net.splatcraft.handlers.SpecialHandler;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.DynamicDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public abstract class SubWeaponItem<Data extends DynamicDataRecord<Data>> extends WeaponBaseItem<SubWeaponSettings<Data>>
{
	private static final int SUB_WEAPON_ENDLAG = 7;
	private static final ResourceKey<EntityType<?>> defaultSubEntityTypeId = ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.tryBuild("splatcraft", "splat_bomb"));
	public SubWeaponItem(RegistrySupplier<? extends EntityType<?>> entityType, String settings)
	{
		super(settings, v ->
			v.component(SplatcraftComponents.SUB_WEAPON_ENTITY_ID, (ResourceKey<EntityType<?>>) entityType.unwrapKey().get()), false);
		
		DispenserBlock.registerBehavior(this, new DispenseBehavior());
	}
	public static boolean singleUse(ItemStack stack)
	{
		return stack.getOrDefault(SplatcraftComponents.SINGLE_USE, false);
	}
	public static EntityType<?> getSubEntityTypeUnrestricted(ItemStack stack)
	{
		return BuiltInRegistries.ENTITY_TYPE.get(stack.getOrDefault(SplatcraftComponents.SUB_WEAPON_ENTITY_ID, defaultSubEntityTypeId));
	}
	public static <Data extends DynamicDataRecord<Data>> EntityType<AbstractSubWeaponEntity<Data>> getSubEntityTypeStatic(ItemStack stack)
	{
		EntityType<?> type = getSubEntityTypeUnrestricted(stack);
		
		try
		{
			return (EntityType<AbstractSubWeaponEntity<Data>>) type;
		}
		catch (Exception ignored)
		{
			return null;
		}
	}
	public <Entity extends AbstractSubWeaponEntity<Data>> EntityType<Entity> getEntityType(ItemStack stack)
	{
		return (EntityType<Entity>) SubWeaponItem.<Data>getSubEntityTypeStatic(stack);
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
		// nvm morgans law
		if (!(player.isSwimming() && !player.isUnderWater()))
			if (singleUse(player.getItemInHand(hand)) || enoughInk(player, this, getSettings(player.getItemInHand(hand)).dataRecord.inkUsage().consumption(), 0, true, true))
			{
				player.startUsingItem(hand);
			}
			else
			{
				CommonUtils.setSquidDelay(player, SUB_WEAPON_ENDLAG);
			}
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
	public void releaseUsing(@NotNull ItemStack stack, @NotNull Level world, @NotNull LivingEntity entity, int remainingUseTicks)
	{
		useSub(stack, world, entity, remainingUseTicks);
		super.releaseUsing(stack, world, entity, remainingUseTicks);
	}
	public void applyCooldown(@NotNull LivingEntity entity)
	{
		if (entity instanceof Player player)
			player.getCooldowns().addCooldown(this, SUB_WEAPON_ENDLAG);
		CommonUtils.setSquidDelay(entity, SUB_WEAPON_ENDLAG);
	}
	@Override
	public void weaponUseTick(Level world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
	{
		CommonUtils.setSquidDelay(entity, 2);
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(Player player, ItemStack stack)
	{
		return PlayerPosingHandler.WeaponPose.SUB_HOLD;
	}
	@Override
	public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity)
	{
		SubWeaponSettings<Data> settings = getSettings(stack);
		if (settings != null && settings.dataRecord != null)
			return settings.dataRecord.holdTime();
		return super.getUseDuration(stack, entity);
	}
	@Override
	public SubWeaponSettings<Data> getSettings(ItemStack stack)
	{
		ResourceLocation id = stack.get(SplatcraftComponents.WEAPON_SETTING_ID);
		
		if (DataHandler.WeaponStatsListener.SETTINGS.get(id) instanceof SubWeaponSettings<?> data)
		{
			return (SubWeaponSettings<Data>) data;
		}
		return new SubWeaponSettings<>("default");
	}
	@Override
	public Optional<SpecialHandler.ResetAction> getResetShootingAction(ItemStack stack, LivingEntity entity)
	{
		return Optional.empty();
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
				Position position = DispenserBlock.getDispensePosition(source);
				Direction direction = source.state().getValue(DispenserBlock.FACING);
				AbstractSubWeaponEntity<?> projectileentity = getProjectile(world, position, thrownStack);
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
		protected AbstractSubWeaponEntity<?> getProjectile(Level level, Position position, ItemStack stack)
		{
			if (!(stack.getItem() instanceof SubWeaponItem<?> subWeaponItem))
				return null;
			
			return AbstractSubWeaponEntity.create(subWeaponItem.getEntityType(stack), level, position.x(), position.y(), position.z(), ColorUtils.getInkColor(stack), InkBlockUtils.InkType.NORMAL, stack);
		}
		protected float getUncertainty()
		{
			return 0;
		}
	}
}
