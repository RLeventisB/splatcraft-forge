package net.splatcraft.items.weapons;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.handlers.ShootingHandler;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.items.weapons.settings.DualieWeaponSettings;
import net.splatcraft.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.DodgeRollEndPacket;
import net.splatcraft.network.c2s.DodgeRollPacket;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkExplosion;
import net.splatcraft.util.PlayerCooldown;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@SuppressWarnings("UnusedReturnValue")
public class DualieItem extends WeaponBaseItem<DualieWeaponSettings>
{
	public static final ArrayList<DualieItem> dualies = Lists.newArrayList();
	public String settings;
	protected DualieItem(String settings)
	{
		super(settings);
		
		this.settings = settings;
		
		dualies.add(this);
	}
	public static RegistrySupplier<DualieItem> create(DeferredRegister<Item> registry, String settings)
	{
		return registry.register(settings, () -> new DualieItem(settings));
	}
	public static RegistrySupplier<DualieItem> create(DeferredRegister<Item> registry, RegistrySupplier<DualieItem> parent, String name)
	{
		return registry.register(name, () -> new DualieItem(parent.get().settingsId.toString()));
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
		return EntityInfoCapability.hasCapability(player) ? EntityInfoCapability.get(player).getDodgeCount() : -1;
	}
	public static int getMaxRollCount(LivingEntity player)
	{
		float maxRolls = 0;
		if (player.getMainHandStack().getItem() instanceof DualieItem dualieItem)
		{
			maxRolls += dualieItem.getSettings(player.getMainHandStack()).rollData.count();
		}
		if (player.getOffHandStack().getItem() instanceof DualieItem dualieItem)
		{
			maxRolls += dualieItem.getSettings(player.getOffHandStack()).rollData.count();
		}
		return (int) maxRolls;
	}
	public static Vec2f getDodgeRollVector(LivingEntity entity, float rollSpeed)
	{
		Vec2f direction = new Vec2f(entity.sidewaysSpeed, entity.forwardSpeed);
		float p_20018_ = entity.getYaw(); // Entity::getInputVector
		Vec2f vec3 = direction.normalize().multiply(rollSpeed);
		float f = MathHelper.sin(p_20018_ * (0.017453292f));
		float f1 = MathHelper.cos(p_20018_ * (0.017453292f));
		return new Vec2f(vec3.x * f1 - vec3.y * f, vec3.y * f1 + vec3.x * f);
	}
	public static boolean canPerformRoll(LivingEntity entity)
	{
		return (!PlayerCooldown.hasPlayerCooldown(entity) || (PlayerCooldown.getPlayerCooldown(entity) instanceof DualieItem.DodgeRollCooldown dodgeRoll && dodgeRoll.canCancelRoll())) && entity.jumping && (entity.sidewaysSpeed != 0 || entity.forwardSpeed != 0);
	}
	@Override
	public Class<DualieWeaponSettings> getSettingsClass()
	{
		return DualieWeaponSettings.class;
	}
	public void performRoll(LivingEntity entity, ItemStack activeDualie, Hand hand, int maxRolls, Vec2f rollPotency, boolean local)
	{
		int rollCount = getRollCount(entity);
		
		DualieWeaponSettings activeSettings = getSettings(activeDualie);
		
		if (reduceInk(entity, this, getInkForRoll(activeDualie), activeSettings.rollData.inkRecoveryCooldown(), !entity.getWorld().isClient()))
		{
			ShootingHandler.notifyForceEndShooting(entity);
			int turretDuration = getRollTurretDuration(activeDualie);
			if (entity instanceof PlayerEntity player)
				PlayerCooldown.setPlayerCooldown(entity, new DodgeRollCooldown(activeDualie, player.getInventory().selectedSlot, hand, rollPotency, activeSettings.rollData.rollStartup(), activeSettings.rollData.rollDuration(), activeSettings.rollData.rollEndlag(), (byte) turretDuration, activeSettings.rollData.canMove()));
			
			EntityInfoCapability.get(entity).setDodgeCount(rollCount + 1);
		}
	}
	public ClampedModelPredicateProvider getIsLeft()
	{
		return (stack, level, entity, seed) ->
		{
			if (entity == null)
			{
				return 0;
			}
			else
			{
				entity.getMainArm();
			}
			boolean mainLeft = entity.getMainArm().equals(Arm.LEFT);
			return mainLeft && entity.getMainHandStack().equals(stack) || !mainLeft && entity.getOffHandStack().equals(stack) ? 1 : 0;
		};
	}
	@Override
	public @NotNull String getTranslationKey(ItemStack stack)
	{
		if (Boolean.TRUE.equals(stack.get(SplatcraftComponents.IS_PLURAL)))
		{
			return getTranslationKey() + ".plural";
		}
		return super.getTranslationKey(stack);
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull World world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);
		
		if (entity instanceof LivingEntity livingEntity)
		{
			Hand hand = livingEntity.getStackInHand(Hand.MAIN_HAND).equals(stack) ? Hand.MAIN_HAND : Hand.OFF_HAND;
			
			if (livingEntity.getStackInHand(hand).equals(stack) && livingEntity.getStackInHand(Hand.values()[(hand.ordinal() + 1) % Hand.values().length]).getItem().equals(stack.getItem()))
			{
				stack.set(SplatcraftComponents.IS_PLURAL, true);
			}
		}
	}
	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks)
	{
		doDodgeRollTick(world, user, stack, remainingUseTicks);
		super.usageTick(world, user, stack, remainingUseTicks);
	}
	private void doDodgeRollTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks)
	{
		ItemStack offhandDualie = ItemStack.EMPTY;
		if (user.getActiveHand().equals(Hand.OFF_HAND) && user.getOffHandStack().equals(stack) && user.getOffHandStack().getItem() instanceof DualieItem)
		{
			offhandDualie = user.getOffHandStack();
		}
		
		int rollCount = getRollCount(user);
		int maxRolls = getMaxRollCount(user);
		if (rollCount > 0 && !PlayerCooldown.hasPlayerCooldown(user)) // fix just in case
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
				Vec2f rollPotency = getDodgeRollVector(user, activeSettings.getRollImpulse());
				
				performRoll(user, stack, user.getActiveHand(), maxRolls, rollPotency, true);
				SplatcraftPacketHandler.sendToServer(new DodgeRollPacket(user.getUuid(), activeDualie, user.getActiveHand(), maxRolls, rollPotency));
			}
		}
	}
	@Override
	public void weaponUseTick(World world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
	{
		PlayerEntity player = (PlayerEntity) entity;
		player.setBodyYaw(player.getBodyYaw()); // actually uncanny in third person but itll be useful when making dualies shoot actually from their muzzles
		
		ShootingHandler.notifyStartShooting(entity);
	}
	@Override
	public ShootingHandler.FiringStatData getWeaponFireData(ItemStack stack, LivingEntity entity)
	{
		DualieWeaponSettings settings = getSettings(stack);
		CommonRecords.ShotDataRecord shotData = settings.getShotData(entity);
		CommonRecords.ProjectileDataRecord projectileData = settings.getProjectileData(entity);
		World world = entity.getWorld();
		return ShootingHandler.FiringStatData.createFromShotData(shotData,
			null,
			(data, accumulatedTime, entity1) ->
			{
				if (!world.isClient())
				{
					if (reduceInk(entity1, this, shotData.inkConsumption(), shotData.inkRecoveryCooldown(), true))
					{
						float inaccuracy = ShotDeviationHelper.updateShotDeviation(stack, world.getRandom(), shotData.accuracyData());
						ItemStack otherHand = entity.getStackInHand(CommonUtils.otherHand(data.hand));
						if (!otherHand.isEmpty() && otherHand.getItem() instanceof DualieItem)
						{
							stack.set(SplatcraftComponents.WEAPON_PRECISION_DATA, ShotDeviationHelper.getDeviationData(otherHand));
						}
						for (int i = 0; i < shotData.projectileCount(); i++)
						{
							InkProjectileEntity proj = new InkProjectileEntity(world, entity, stack, InkBlockUtils.getInkType(entity), projectileData.size(), settings);
							
							proj.setVelocity(entity, entity.getPitch(), entity.getYaw(), shotData.pitchCompensation(), projectileData.speed(), inaccuracy);
							proj.addExtraData(new ExtraSaveData.DualieExtraData(CommonUtils.isRolling(entity1)));
							proj.setDualieStats(projectileData);
							world.spawnEntity(proj);
							proj.tick(accumulatedTime);
						}
						
						world.playSoundFromEntity(entity, SplatcraftSounds.dualieShot, SoundCategory.PLAYERS, 0.7F, (float) world.getRandom().nextTriangular(0.95f, 0.095f));
					}
				}
			}, null);
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(PlayerEntity player, ItemStack stack)
	{
		// loong if
		if (PlayerCooldown.hasPlayerCooldown(player) && ShootingHandler.isDoingShootingAction(player) && PlayerCooldown.getPlayerCooldown(player) instanceof DodgeRollCooldown dodgeRoll && dodgeRoll.rollState == DodgeRollCooldown.RollState.TURRET && ShootingHandler.shootingData.get(player).isDualFire())
			return PlayerPosingHandler.WeaponPose.TURRET_FIRE;
		return PlayerPosingHandler.WeaponPose.DUAL_FIRE;
	}
	public static class DodgeRollCooldown extends PlayerCooldown
	{
		public static final Codec<DodgeRollCooldown> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			ItemStack.CODEC.fieldOf("stored_stack").forGetter(v -> v.storedStack),
			Codec.FLOAT.fieldOf("time").forGetter(PlayerCooldown::getTime),
			Codec.FLOAT.fieldOf("max_time").forGetter(PlayerCooldown::getMaxTime),
			Codec.INT.fieldOf("slot_index").forGetter(PlayerCooldown::getSlotIndex),
			Codec.BOOL.fieldOf("is_main_hand").forGetter(v -> v.getHand() == Hand.MAIN_HAND),
			Codec.BYTE.fieldOf("roll_frame").forGetter(v -> v.rollFrame),
			Codec.BYTE.fieldOf("roll_end_frame").forGetter(v -> v.rollEndFrame),
			Codec.BYTE.fieldOf("turret_mode_frame").forGetter(v -> v.turretModeFrame),
			CommonUtils.VEC_2_CODEC.fieldOf("roll_direction").forGetter(v -> v.rollDirection),
			Codec.BOOL.fieldOf("can_slide").forGetter(v -> v.canSlide),
			RollState.CODEC.fieldOf("roll_state").forGetter(v -> v.rollState)
		).apply(inst, DodgeRollCooldown::new));
		final byte rollFrame, rollEndFrame, turretModeFrame;
		final Vec2f rollDirection;
		boolean canSlide;
		RollState rollState = RollState.BEFORE_ROLL;
		public DodgeRollCooldown(ItemStack stack, int slotIndex, Hand hand, Vec2f rollDirection, byte startupFrames, byte rollDuration, byte endlagFrames, byte turretModeFrames, boolean canSlide)
		{
			super(stack, startupFrames + rollDuration + endlagFrames + turretModeFrames, slotIndex, hand, false, false, true, false);
			this.rollDirection = rollDirection;
			rollFrame = (byte) (rollDuration + turretModeFrames + endlagFrames);
			rollEndFrame = (byte) (turretModeFrames + endlagFrames);
			turretModeFrame = turretModeFrames;
			this.canSlide = canSlide;
		}
		public DodgeRollCooldown(ItemStack storedStack, float time, Float maxTime, Integer slotIndex, Boolean isMainHand, Byte rollFrame, Byte rollEndFrame, Byte turretModeFrame, Vec2f rollDirection, Boolean canSlide, RollState rollState)
		{
			super(storedStack, time, maxTime, slotIndex, isMainHand ? Hand.MAIN_HAND : Hand.OFF_HAND, false, false, true, false);
			this.rollDirection = rollDirection;
			this.rollFrame = rollFrame;
			this.rollEndFrame = rollEndFrame;
			this.turretModeFrame = turretModeFrame;
			this.canSlide = canSlide;
			this.rollState = rollState;
		}
		@Override
		public void tick(LivingEntity entity)
		{
			boolean local = entity.getWorld().isClient;
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
								entity.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.dualieDodge, SoundCategory.PLAYERS, 0.7F, CommonUtils.nextTriangular(entity.getWorld().random, 0.95f, 0.095f));
								InkExplosion.createInkExplosion(entity, entity.getPos(), 0.9f, InkBlockUtils.getInkType(entity), storedStack);
							}
							entity.setNoDrag(true);
							
							entity.setVelocity(rollDirection.x, -0.5, rollDirection.y);
							rollState = RollState.ROLL;
							break;
						}
						doLogic = false;
						break;
					case ROLL:
						if (getTime() <= rollEndFrame)
						{
							entity.setNoDrag(false);
							
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
						if (getTime() <= 1)
						{
							if (local)
							{
								boolean endedTurretMode = entity.jumping || entity.forwardSpeed != 0 || entity.sidewaysSpeed != 0 || !entity.isUsingItem() || entity.getVelocity().y > 0.1;
								if (endedTurretMode)
								{
									setTime(0);
									EntityInfoCapability.get(entity).setDodgeCount(0);
									SplatcraftPacketHandler.sendToServer(new DodgeRollEndPacket(entity.getUuid()));
								}
								else
								{
									setTime(2);
								}
							}
							else if (getTime() > 0)
							{
								setTime(2);
							}
						}
						break;
				}
			}
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
		enum RollState implements StringIdentifiable
		{
			BEFORE_ROLL(0),
			ROLL(1),
			AFTER_ROLL(2),
			TURRET(3);
			public static final Codec<RollState> CODEC = StringIdentifiable.createCodec(RollState::values);
			final byte value;
			RollState(int value)
			{
				this.value = (byte) value;
			}
			public static RollState fromValue(byte value)
			{
				return switch (value)
				{
					case 0 -> BEFORE_ROLL;
					case 1 -> ROLL;
					case 2 -> TURRET;
					default -> throw new IllegalStateException("Unexpected value: " + value);
				};
			}
			@Override
			public String asString()
			{
				return name();
			}
		}
	}
}