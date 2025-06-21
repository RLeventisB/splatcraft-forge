package net.splatcraft.items.weapons;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.handlers.WeaponHandler;
import net.splatcraft.items.weapons.settings.CommonRecords.ProjectileDataRecord;
import net.splatcraft.items.weapons.settings.CommonRecords.ShotDataRecord;
import net.splatcraft.items.weapons.settings.DualieWeaponSettings;
import net.splatcraft.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.DodgeRollPacket;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkExplosion;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.EntityActionWithTime;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Optional;

@SuppressWarnings("UnusedReturnValue")
public class DualieItem extends WeaponBaseItem<DualieWeaponSettings>
{
	public static final ArrayList<DualieItem> dualies = Lists.newArrayList();
	public String settings;
	protected DualieItem(String settings)
	{
		super(settings, properties -> properties.component(SplatcraftComponents.SHOOTER_FIRING_DATA, SplatcraftComponents.ShooterFiringData.DEFAULT));

		this.settings = settings;

		dualies.add(this);
	}
	public static RegistrySupplier<DualieItem> create(DeferredRegister<Item> registry, String settings)
	{
		return registry.register(settings, () -> new DualieItem(settings));
	}
	public static RegistrySupplier<DualieItem> create(DeferredRegister<Item> registry, RegistrySupplier<DualieItem> parent, String name)
	{
		return registry.register(name, () -> new DualieItem(parent.value().components().get(SplatcraftComponents.WEAPON_SETTING_ID).toString()));
	}
	public static RegistrySupplier<DualieItem> create(DeferredRegister<Item> registry, String settings, String name)
	{
		return registry.register(name, () -> new DualieItem(settings));
	}
	private static float getInkForRoll(ItemStack stack)
	{
		return stack.getItem() instanceof DualieItem item ? item.getSettings(stack).rollData.inkConsumption() : 0;
	}
	public static int getRollTurretDuration(ItemStack stack)
	{
		if (stack.getItem() instanceof DualieItem dualie)
			return dualie.getSettings(stack).rollData.turretDuration();

		return 0;
	}
	public static int getRollCount(LivingEntity player)
	{
		return EntityInfoCapability.getOptional(player).map(EntityInfo::getDodgeCount).orElse(-1);
	}
	public static int getMaxRollCount(LivingEntity player)
	{
		float maxRolls = 0;
		if (player.getMainHandItem().getItem() instanceof DualieItem dualieItem)
		{
			maxRolls += dualieItem.getSettings(player.getMainHandItem()).rollData.count();
		}
		if (player.getOffhandItem().getItem() instanceof DualieItem dualieItem)
		{
			maxRolls += dualieItem.getSettings(player.getOffhandItem()).rollData.count();
		}
		return (int) maxRolls;
	}
	public static Vec2 getDodgeRollVector(LivingEntity entity, float rollSpeed)
	{
		Vec2 direction = new Vec2(entity.xxa, entity.zza);
		float yaw = entity.getYRot() * Mth.DEG_TO_RAD; // Entity::getInputVector
		Vec2 vec3 = direction.normalized().scale(rollSpeed);
		float f = Mth.sin(yaw);
		float f1 = Mth.cos(yaw);
		return new Vec2(vec3.x * f1 - vec3.y * f, vec3.y * f1 + vec3.x * f);
	}
	public static boolean canPerformRoll(LivingEntity entity)
	{
		return (!EntityAction.hasEntityAction(entity) || (EntityAction.getEntityAction(entity) instanceof DodgeRollAction dodgeRoll && dodgeRoll.canCancelRoll())) && entity.jumping && (entity.xxa != 0 || entity.zza != 0);
	}
	@Override
	public Class<DualieWeaponSettings> getSettingsClass()
	{
		return DualieWeaponSettings.class;
	}
	public void performRoll(LivingEntity entity, ItemStack activeDualie, EntitySlot dualieSlot, Vec2 rollPotency)
	{
		int rollCount = getRollCount(entity);

		DualieWeaponSettings activeSettings = getSettings(activeDualie);

		if (reduceInk(entity, this, getInkForRoll(activeDualie), activeSettings.rollData.inkRecoveryCooldown(), !entity.level().isClientSide()))
		{
			entity.getMainHandItem().update(SplatcraftComponents.SHOOTER_FIRING_DATA, SplatcraftComponents.ShooterFiringData.DEFAULT, v -> v.withCounter(Float.NaN));
			entity.getOffhandItem().update(SplatcraftComponents.SHOOTER_FIRING_DATA, SplatcraftComponents.ShooterFiringData.DEFAULT, v -> v.withCounter(Float.NaN));
			entity.stopUsingItem();

			int turretDuration = getRollTurretDuration(activeDualie);

			boolean allowFlying = entity instanceof Player player && player.getAbilities().mayfly;
			Optional<DodgeRollAction> previousDodgeRoll = EntityAction.getSpecificEntityActionOptional(entity, DodgeRollAction.class);
			if (previousDodgeRoll.isPresent())
			{
				allowFlying = previousDodgeRoll.get().didAllowFlying;
			}
			EntityAction.setEntityAction(entity, new DodgeRollAction(activeDualie, dualieSlot, rollPotency, activeSettings.rollData.rollStartup(), activeSettings.rollData.rollDuration(), activeSettings.rollData.rollEndlag(), (byte) turretDuration, activeSettings.rollData.canMove(), allowFlying));

			EntityInfoCapability.get(entity).setDodgeCount(rollCount + 1);
		}
	}
	public ClampedItemPropertyFunction getIsLeft()
	{
		return (stack, level, entity, seed) ->
		{
			if (entity == null)
			{
				return 0;
			}
			boolean mainLeft = entity.getMainArm().equals(HumanoidArm.LEFT);
			return
				mainLeft && ItemStack.isSameItem(entity.getMainHandItem(), stack) ||
					!mainLeft && ItemStack.isSameItem(entity.getOffhandItem(), stack) ? 1 : 0;
		};
	}
	@Override
	public @NotNull String getDescriptionId(ItemStack stack)
	{
		if (Boolean.TRUE.equals(stack.get(SplatcraftComponents.IS_PLURAL)))
		{
			return getDescriptionId() + ".plural";
		}
		return super.getDescriptionId(stack);
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);

		if (entity instanceof LivingEntity living)
		{
			InteractionHand hand = living.getItemInHand(InteractionHand.MAIN_HAND).equals(stack) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

			if (living.getItemInHand(hand).equals(stack) && living.getItemInHand(CommonUtils.otherHand(hand)).is(stack.getItem()))
			{
				stack.set(SplatcraftComponents.IS_PLURAL, true);
			}

			DualieWeaponSettings settings = getSettings(stack);

			stack.update(
				SplatcraftComponents.SHOOTER_FIRING_DATA,
				SplatcraftComponents.ShooterFiringData.DEFAULT,
				data ->
				{
					return data.tick(
						(accumulatedTime) ->
						{
							if (!EntityInfoCapability.isSquid(living))
								fire(settings, world, stack, living, accumulatedTime, hand);
							return v -> WeaponHandler.canContinueShooting(living) ? v : v.withRepeatingFlag(false);
						},
						(accumulatedTime) -> v -> v);
				}
			);
		}
	}
	@Override
	public void onUseTick(@NotNull Level world, @NotNull LivingEntity user, ItemStack stack, int remainingUseTicks)
	{
		doDodgeRollTick(user, stack);
		super.onUseTick(world, user, stack, remainingUseTicks);
	}
	private void doDodgeRollTick(LivingEntity user, ItemStack stack)
	{
		ItemStack offhandDualie = ItemStack.EMPTY;
		if (user.getOffhandItem().equals(stack) && user.getMainHandItem().getItem() instanceof DualieItem)
		{
			offhandDualie = user.getMainHandItem();
		}
		else if (user.getMainHandItem().equals(stack) && user.getOffhandItem().getItem() instanceof DualieItem)
		{
			offhandDualie = user.getOffhandItem();
		}

		int rollCount = getRollCount(user);
		int maxRolls = getMaxRollCount(user);
		if (rollCount > 0 && !EntityAction.hasEntityAction(user)) // fix just in case
		{
			rollCount = 0;
		}
		if (rollCount < maxRolls && canPerformRoll(user))
		{
			ItemStack activeDualie;
			boolean lastRoll = rollCount == maxRolls - 1;
			if (lastRoll)
			{
				activeDualie = getRollTurretDuration(stack) >= getRollTurretDuration(offhandDualie) ? stack : offhandDualie;
			}
			else
			{
				activeDualie = rollCount % 2 == 1 && offhandDualie.getItem() instanceof DualieItem ? offhandDualie : stack;
			}
			DualieWeaponSettings.RollDataRecord activeSettings = getSettings(activeDualie).rollData;
			// why does vec2 use floats but vec3 use doubles

			if (enoughInk(user, this, getInkForRoll(activeDualie), activeSettings.inkRecoveryCooldown(), false))
			{
				Vec2 rollPotency = getDodgeRollVector(user, activeSettings.getRollImpulse());
				EntitySlot usedDualie = EntitySlot.searchAndCreateWithStack(user, activeDualie, EntitySlot.StackComparator.ONLY_REFERENCE);

				performRoll(user, activeDualie, usedDualie, rollPotency);
				SplatcraftPacketHandler.sendToServer(new DodgeRollPacket(user.getUUID(), activeDualie, usedDualie, rollPotency));
			}
		}
	}
	@Override
	public void weaponUseTick(Level world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
	{
		Player player = (Player) entity;
		player.setYBodyRot(player.getVisualRotationYInDegrees()); // actually uncanny in third person but itll be useful when making dualies shoot actually from their muzzles

		stack.update(
			SplatcraftComponents.SHOOTER_FIRING_DATA,
			SplatcraftComponents.ShooterFiringData.DEFAULT,
			data -> data.notifyUsing(entity, getSettings(stack).getShotData(entity))
		);

		InteractionHand hand = entity.getItemInHand(InteractionHand.MAIN_HAND).equals(stack) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		ItemStack itemInOtherHand = entity.getItemInHand(CommonUtils.otherHand(hand));

		// returns if the other stack is empty, or isnt a dualie, or is already shooting
		if (
			itemInOtherHand.isEmpty() ||
				!(itemInOtherHand.getItem() instanceof DualieItem otherDualie) ||
				SplatcraftComponents.getOptional(itemInOtherHand, SplatcraftComponents.SHOOTER_FIRING_DATA)
					.map(SplatcraftComponents.ShooterFiringData::preventsChanging).orElse(true)) return;

		DualieWeaponSettings otherDualieSettings = otherDualie.getSettings(itemInOtherHand);
		float initialStartup = otherDualieSettings.getShotData(entity).repeatTicks() / 2f;
		float timeSinceStartup = entity.getTicksUsingItem() - initialStartup;
		if (timeSinceStartup < 0) return;

		itemInOtherHand.update(
			SplatcraftComponents.SHOOTER_FIRING_DATA,
			SplatcraftComponents.ShooterFiringData.DEFAULT,
			data ->
				data
					.notifyUsing(entity, otherDualieSettings.getShotData(entity))
					.tick((accumulatedTime) ->
						{
							fire(otherDualieSettings, world, itemInOtherHand, entity, accumulatedTime, hand);
							return v -> v;
						},
						(accumulatedTime) -> v -> v, timeSinceStartup)
		);
	}
	public void fire(DualieWeaponSettings settings, Level level, ItemStack stack, LivingEntity entity, float accumulatedTime, InteractionHand hand)
	{
		ShotDataRecord shotData = settings.getShotData(entity);
		ProjectileDataRecord projectileData = settings.getProjectileData(entity);

		if (reduceInk(entity, this, shotData.inkConsumption(), shotData.inkRecoveryCooldown(), true))
		{
			CommonUtils.setSquidDelay(entity, shotData.miscEndlagTicks());

			if (!level.isClientSide)
			{
				float inaccuracy = ShotDeviationHelper.updateShotDeviation(stack, level.getRandom(), shotData.accuracyData());
				ItemStack otherHand = entity.getItemInHand(CommonUtils.otherHand(hand));
				if (!otherHand.isEmpty() && otherHand.getItem() instanceof DualieItem)
				{
					stack.set(SplatcraftComponents.WEAPON_PRECISION_DATA, ShotDeviationHelper.getDeviationData(otherHand));
				}
				for (int i = 0; i < shotData.projectileCount(); i++)
				{
					InkProjectileEntity proj = new InkProjectileEntity(level, entity, stack, InkBlockUtils.getInkType(entity), projectileData.size(), settings);

					proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), shotData.pitchCompensation(), shotData.speed(), inaccuracy);
					proj.addExtraData(new ExtraSaveData.DualieExtraData(CommonUtils.isRolling(entity)));
					proj.setDualieStats(projectileData);
					level.addFreshEntity(proj);
					proj.tick(accumulatedTime);
				}

				level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.dualieShot, SoundSource.PLAYERS, 0.7F, CommonUtils.nextTriangular(level.getRandom(), 0.95F, 0.095F));
			}
		}
	}
	@Override
	public boolean preventsChanging(ItemStack stack, LivingEntity entity)
	{
		return SplatcraftComponents.getOptional(stack, SplatcraftComponents.SHOOTER_FIRING_DATA).map(SplatcraftComponents.ShooterFiringData::preventsChanging).orElse(false);
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(Player player, ItemStack stack)
	{
		Optional<DodgeRollAction> optional = EntityAction.getSpecificActionIf(player, DodgeRollAction::forceCrouch, DodgeRollAction.class);
		if (optional.isPresent())
			return PlayerPosingHandler.WeaponPose.TURRET_FIRE;
		return PlayerPosingHandler.WeaponPose.DUAL_FIRE;
	}
	public static class DodgeRollAction extends EntityActionWithTime
	{
		public static final Codec<DodgeRollAction> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			ItemStack.CODEC.fieldOf("stored_stack").forGetter(DodgeRollAction::getStoredStack),
			Codec.FLOAT.fieldOf("time").forGetter(DodgeRollAction::getTime),
			Codec.FLOAT.fieldOf("max_time").forGetter(DodgeRollAction::getMaxTime),
			EntitySlot.SERIALIZER_CODEC.fieldOf("dualie_slot").forGetter(DodgeRollAction::getItemSlot),
			Codec.BYTE.fieldOf("roll_frame").forGetter(v -> v.rollFrame),
			Codec.BYTE.fieldOf("roll_end_frame").forGetter(v -> v.rollEndFrame),
			Codec.BYTE.fieldOf("turret_mode_frame").forGetter(v -> v.turretModeFrame),
			CodecUtils.Codecs.VEC_2_CODEC.fieldOf("roll_direction").forGetter(v -> v.rollDirection),
			Codec.BOOL.fieldOf("can_slide").forGetter(v -> v.canSlide),
			RollState.CODEC.fieldOf("roll_state").forGetter(v -> v.rollState),
			Codec.BOOL.fieldOf("did_allow_flying").forGetter(v -> v.didAllowFlying)
		).apply(inst, DodgeRollAction::new));
		public final byte rollFrame, rollEndFrame, turretModeFrame;
		final ItemStack storedStack;
		final EntitySlot itemSlot;
		final Vec2 rollDirection;
		final boolean canSlide, didAllowFlying;
		RollState rollState = RollState.BEFORE_ROLL;
		public DodgeRollAction(ItemStack stack, EntitySlot itemSlot, Vec2 rollDirection, byte startupFrames, byte rollDuration, byte endlagFrames, byte turretModeFrames, boolean canSlide, boolean didAllowFlying)
		{
			super(startupFrames + rollDuration + endlagFrames + turretModeFrames);
			storedStack = stack;
			this.itemSlot = itemSlot;
			this.rollDirection = rollDirection;
			rollFrame = (byte) (rollDuration + turretModeFrames + endlagFrames);
			rollEndFrame = (byte) (turretModeFrames + endlagFrames);
			turretModeFrame = turretModeFrames;
			this.canSlide = canSlide;
			this.didAllowFlying = didAllowFlying;
		}
		public DodgeRollAction(ItemStack stack, float time, float maxTime, EntitySlot itemSlot, byte rollFrame, byte rollEndFrame, byte turretModeFrame, Vec2 rollDirection, boolean canSlide, RollState rollState, boolean didAllowFlying)
		{
			super(time, maxTime);
			storedStack = stack;
			this.itemSlot = itemSlot;
			this.rollDirection = rollDirection;
			this.rollFrame = rollFrame;
			this.rollEndFrame = rollEndFrame;
			this.turretModeFrame = turretModeFrame;
			this.canSlide = canSlide;
			this.rollState = rollState;
			this.didAllowFlying = didAllowFlying;
		}
		@Override
		public void onStart(LivingEntity entity)
		{
			if (entity instanceof Player player)
			{
				player.getAbilities().mayfly = false;
				player.getAbilities().flying = false;
			}
		}
		@Override
		public void tick(LivingEntity entity)
		{
			boolean local = entity.level().isClientSide;
			boolean doLogic = true;
			while (doLogic)
			{
				switch (rollState)
				{
					case BEFORE_ROLL:
						if (getTime() <= rollFrame)
						{
							if (!local)
							{
								entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.dualieDodge, SoundSource.PLAYERS, 0.7F, CommonUtils.nextTriangular(entity.level().random, 0.95f, 0.095f));
								InkExplosion.createInkExplosion(entity, entity.position(), 0.9f, InkBlockUtils.getInkType(entity), storedStack);
							}
							entity.setDiscardFriction(true);

							entity.setDeltaMovement(rollDirection.x, -0.5, rollDirection.y);
							rollState = RollState.ROLL;
							break;
						}
						doLogic = false;
						break;
					case ROLL:
						if (getTime() <= rollEndFrame)
						{
							entity.setDiscardFriction(false);

							rollState = RollState.AFTER_ROLL;
							break;
						}
						doLogic = false;
						break;
					case AFTER_ROLL:
						if (getTime() <= turretModeFrame)
						{
							rollState = RollState.TURRET;
							break;
						}
						doLogic = false;
						break;
					case TURRET:
						doLogic = false;
						break;
				}
			}
		}
		@Override
		public boolean canEnd(LivingEntity entity)
		{
			boolean endedTurretMode = entity.jumping ||
				entity.zza != 0 || entity.xxa != 0 || // these work in the client side
				WeaponHandler.getEntityPrevPos(entity).oldOldPosition.distanceToSqr(entity.position()) > 0.01 || // this works in the server side
				!entity.isUsingItem() || entity.getDeltaMovement().y > 0.1;
			if (endedTurretMode)
			{
				EntityInfoCapability.get(entity).setDodgeCount(0);
				if (entity instanceof Player player)
				{
					player.getAbilities().mayfly = didAllowFlying;
				}
				return true;
			}

			return false;
		}
		public boolean canCancelRoll()
		{
			return rollState == RollState.AFTER_ROLL || rollState == RollState.TURRET;
		}
		@Override
		public boolean canMove()
		{
			return canSlide && canCancelRoll();
		}
		@Override
		public boolean forceCrouch()
		{
			return rollState == RollState.TURRET;
		}
		@Override
		public boolean preventWeaponUse()
		{
			return rollState != RollState.TURRET;
		}
		@Override
		public ItemStack getStoredStack()
		{
			return storedStack;
		}
		@Override
		public EntitySlot getItemSlot()
		{
			return itemSlot;
		}
		public enum RollState implements StringRepresentable
		{
			BEFORE_ROLL(0),
			ROLL(1),
			AFTER_ROLL(2),
			TURRET(3);
			public static final Codec<RollState> CODEC = StringRepresentable.fromEnum(RollState::values);
			final byte value;
			RollState(int value)
			{
				this.value = (byte) value;
			}
			@Override
			public @NotNull String getSerializedName()
			{
				return name();
			}
		}
	}
}