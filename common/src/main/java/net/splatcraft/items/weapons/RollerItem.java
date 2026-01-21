package net.splatcraft.items.weapons;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.blocks.ColoredBarrierBlock;
import net.splatcraft.client.audio.RollerRollTickableSound;
import net.splatcraft.client.handlers.PlayerMovementHandler;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.entities.SquidBumperEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.handlers.SpecialHandler;
import net.splatcraft.handlers.WeaponHandler;
import net.splatcraft.items.weapons.settings.RollerWeaponSettings;
import net.splatcraft.mixin.accessors.EntityAccessor;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.OverwriteJumpingStatePacket;
import net.splatcraft.network.s2c.UpdateEntityActionOnlyPacket;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.*;
import net.splatcraft.util.action.ActionEndResult;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.EntityActionWithTime;
import net.splatcraft.util.structs.AttackId;
import net.splatcraft.util.structs.BlockInkedResult;
import net.splatcraft.util.structs.DamageCalculator;
import net.splatcraft.util.structs.NumberRange.FloatRange;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.sampling.PoissonSampling;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RollerItem extends WeaponBaseItem<RollerWeaponSettings>
{
	public static final ArrayList<RollerItem> rollers = Lists.newArrayList();
	public boolean isMoving; // oh no, item instances are shared so these are basically static fields
	public boolean usedOnGround;
	protected RollerItem(String settings)
	{
		super(settings);
		rollers.add(this);
	}
	public static RegistrySupplier<RollerItem> create(DeferredRegister<Item> registry, String settings, String name)
	{
		return registry.register(name, () -> new RollerItem(settings));
	}
	public static RegistrySupplier<RollerItem> create(DeferredRegister<Item> registry, RegistrySupplier<RollerItem> parent, String name)
	{
		return registry.register(name, () -> new RollerItem(parent.value().components().get(SplatcraftComponents.WEAPON_SETTING_ID).toString()));
	}
	public static void applyRecoilKnockback(LivingEntity entity, double pow)
	{
		entity.setDeltaMovement(new Vec3(Math.cos(Math.toRadians(entity.getYRot() + 90)) * -pow, entity.getDeltaMovement().y, Math.sin(Math.toRadians(entity.getYRot() + 90)) * -pow));
		entity.hurtMarked = true;
	}
	@OnlyIn(Dist.CLIENT)
	protected static void playRollSound(boolean isBrush)
	{
		Minecraft.getInstance().getSoundManager().queueTickingSound(new RollerRollTickableSound(ClientUtils.getClientPlayer(), isBrush));
	}
	@Override
	public Class<RollerWeaponSettings> getSettingsClass()
	{
		return RollerWeaponSettings.class;
	}
	public ClampedItemPropertyFunction getUnfolded()
	{
		return (stack, level, entity, seed) ->
		{
			if (entity instanceof Player player)
			{
				InitialSwingAction action = EntityAction.getSpecificEntityAction(player, InitialSwingAction.class);
				if (action != null)
				{
					Optional<ItemStack> weaponStackOptional = action.getItemSlot().tryGetItemFrom(entity);
					if (weaponStackOptional.isPresent())
					{
						return stack.equals(weaponStackOptional.get()) && (getSettings(stack).isBrush || action.isGrounded() || action.getTime() >= action.attackFrame + 1) ? 1 : 0;
					}
				}
			}
			return entity != null && entity.isUsingItem() && entity.getUseItem() == stack && entity.getTicksUsingItem() > 10 ? 1 : 0;
		};
	}
	@Override
	public void onUseTick(@NotNull Level world, @NotNull LivingEntity user, ItemStack stack, int remainingUseTicks)
	{
		if (remainingUseTicks == stack.getUseDuration(user))
		{
			usedOnGround = user.onGround();
			Optional<InitialSwingAction> optional = EntityAction.getSpecificEntityActionOptional(user, InitialSwingAction.class);
			optional.ifPresentOrElse(action ->
			{
				if (action.canQueueSwing())
				{
					action.isAttackQueued = true;
				}
			}, () ->
			{
				boolean notPreventedByAction = !EntityAction.hasEntityActionAnd(user, EntityAction::preventWeaponUse);

				if (notPreventedByAction && ((!(user instanceof Player player) || !CommonUtils.anyWeaponOnCooldown(player))))
				{
					RollerWeaponSettings settings = getSettings(stack);
					RollerWeaponSettings.RollerAttackDataRecord attackData = settings.getAttackData(usedOnGround).attackData();
					EntityAction.setEntityAction(user, new InitialSwingAction(stack, attackData.startupTicks(), attackData.endlagTicks(), user));
					if (world.isClientSide() && user instanceof Player player)
						sendGroundedSynchronizationPacket(player);
					user.setSprinting(false);
				}
			});
		}

		super.onUseTick(world, user, stack, remainingUseTicks);
	}
	@OnlyIn(Dist.CLIENT)
	private static void sendGroundedSynchronizationPacket(Player player)
	{
		if (player == ClientUtils.getClientPlayer())
			SplatcraftPacketHandler.sendToServer(new OverwriteJumpingStatePacket(player.onGround()));
	}
	@Override
	public void weaponUseTick(Level world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
	{
		RollerWeaponSettings settings = getSettings(stack);

		int rollTime = entity.getTicksUsingItem() - Math.round(settings.getAttackData(usedOnGround).attackData().getRollDelay());
		if (rollTime <= 0)
			return;

		float toConsume = Mth.lerp(Math.min(1, rollTime / settings.rollData.dashTime()), settings.rollData.inkConsumption(), settings.rollData.dashConsumption());
		if (world.isClientSide)
		{
			isMoving = Math.abs(entity.yHeadRotO - entity.yHeadRot) > 0 || (entity.zza != 0 || entity.xxa != 0);
		}
		else
		{
			WeaponHandler.OldEntityTransformData oldData = WeaponHandler.getEntityPrevPos(entity);
			isMoving = oldData.oldOldPosition.distanceToSqr(entity.position()) > 0 || Math.abs(oldData.oldOldRot.y - entity.getYRot()) > 0;
		}

		boolean doPush = false;
		if (isMoving)
		{
			doPush = doRoll(world, entity, stack, remainingUseTicks, settings, toConsume);
		}
		if (doPush)
			applyRecoilKnockback(entity, 0.8);
	}
	private boolean doRoll(Level world, LivingEntity entity, ItemStack stack, int remainingUseTicks, RollerWeaponSettings settings, float inkConsumption)
	{
		double dxOff = 0;
		double dzOff = 0;
		for (int i = 1; i <= 2; i++)
		{
			dxOff = Math.cos(Math.toRadians(entity.getYRot() + 90)) * i;
			dzOff = Math.sin(Math.toRadians(entity.getYRot() + 90)) * i;

			BlockPos pos = BlockPos.containing(entity.getX() + dxOff, entity.getY(), entity.getZ() + dzOff);
			if (!InkBlockUtils.canInkPassthrough(world, pos))
				break;
		}

		boolean doPush = false;
		BlockInkedResult result = BlockInkedResult.FAIL;
		float step = CommonUtils.calculateStep(settings.rollData.inkSize(), 1);
		for (float i = 0; i < settings.rollData.inkSize(); i += step)
		{
			float off = i - settings.rollData.inkSize() / 2f;
			boolean insideDamage = i == 0 || Math.abs(off) < settings.rollData.hitboxSize() / 2f;
			double xOff = Math.cos(Math.toRadians(entity.getYRot())) * off;
			double zOff = Math.sin(Math.toRadians(entity.getYRot())) * off;

			for (float yOff = 0; yOff >= -3; yOff--)
			{
				if (!enoughInk(entity, this, inkConsumption, 0, remainingUseTicks % 4 == 0))
				{
					break;
				}

				if (yOff == -3)
				{
					dxOff = Math.cos(Math.toRadians(entity.getYRot() + 90));
					dzOff = Math.sin(Math.toRadians(entity.getYRot() + 90));
				}

				BlockPos pos = BlockPos.containing(entity.getX() + xOff + dxOff, entity.getY() + yOff, entity.getZ() + zOff + dzOff);

				if (world.getBlockState(pos).getBlock() instanceof ColoredBarrierBlock block && block.canAllowThrough(pos, entity))
					continue;

				if (!InkBlockUtils.canInkPassthrough(world, pos))
				{
					VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos);

					result = InkBlockUtils.inkBlock(entity, world, pos, ColorUtils.getInkColor(stack), Direction.UP, InkBlockUtils.getInkType(entity), settings.rollData.damage());
					double blockHeight = shape.isEmpty() ? 0 : shape.bounds().maxY;

					if (yOff != -3 && !(shape.bounds().minX <= 0 && shape.bounds().minZ <= 0 && shape.bounds().maxX >= 1 && shape.bounds().maxZ >= 1))
					{
						BlockInkedResult secondResult = InkBlockUtils.inkBlock(entity, world, pos.below(), ColorUtils.getInkColor(stack), Direction.UP, InkBlockUtils.getInkType(entity), settings.rollData.damage());
						if (result == BlockInkedResult.FAIL)
						{
							result = secondResult;
						}
					}

					if (result == BlockInkedResult.SUCCESS)
						InkBlockUtils.awardTurfPoints(entity, stack, 1);

					if (result != BlockInkedResult.FAIL)
					{
						if (insideDamage)
						{
							world.addParticle(new InkSplashParticleData(ColorUtils.getInkColor(stack), 1), entity.getX() + xOff + dxOff, pos.getY() + blockHeight + 0.1, entity.getZ() + zOff + dzOff, 0, 0, 0);
							if (i > 0)
							{
								double xhOff = dxOff + Math.cos(Math.toRadians(entity.getYRot())) * (off - 0.5);
								double zhOff = dzOff + Math.sin(Math.toRadians(entity.getYRot())) * (off - 0.5);
								world.addParticle(new InkSplashParticleData(ColorUtils.getInkColor(stack), 1), entity.getX() + xhOff, pos.getY() + blockHeight + 0.1, entity.getZ() + zhOff, 0, 0, 0);
							}
						}
					}
					break;
				}
			}

			if (world.isClientSide())
			{
				// Damage and knockback are dealt server-side
				continue;
			}

			if (!insideDamage)
			{
				continue;
			}

			Vec3 hitCenter = new Vec3(entity.getX() + xOff + dxOff, entity.getY(), entity.getZ() + zOff + dzOff);
			for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(hitCenter, 0.6, 0.8, 0.6), EntitySelector.NO_SPECTATORS.and(e ->
			{
				if (e instanceof LivingEntity target)
				{
					if (InkDamageUtils.isSplatted(target)) return false;
					return InkDamageUtils.canDamage(e, entity) || e instanceof SquidBumperEntity;
				}
				return false;
			})))
			{
				if (!target.equals(entity) && (!enoughInk(entity, this, inkConsumption, 0, false) || !InkDamageUtils.doRollDamage(target, settings.rollData.damage(), entity, entity, stack) || !InkDamageUtils.isSplatted(target)))
				{
					doPush = true;
				}
			}
		}
		if (result != BlockInkedResult.FAIL)
			reduceInk(entity, this, inkConsumption, settings.rollData.inkRecoveryCooldown(), false);
		return doPush;
	}
	@Override
	public boolean hasSpeedModifier(LivingEntity entity, ItemStack stack)
	{
		RollerWeaponSettings settings = getSettings(stack);

		float rollTime = settings.getAttackData(usedOnGround).attackData().getRollDelay();

		if (EntityAction.hasSpecificEntityActionAnd(entity, v -> v.getTime() < rollTime, RollerItem.InitialSwingAction.class) || !entity.getUseItem().equals(stack))
			return false;
		return super.hasSpeedModifier(entity, stack);
	}
	@Override
	public AttributeModifier getSpeedModifier(LivingEntity entity, ItemStack stack)
	{
		RollerWeaponSettings settings = getSettings(stack);
		RollerWeaponSettings.RollerAttackDataRecord data = settings.getAttackData(usedOnGround).attackData();
		double appliedMobility;
		float rollTime = data.getRollDelay();
		float useTime = entity.getTicksUsingItem() - data.getTotalAttackTime();
		float dashProgress = Math.min(1, useTime / settings.rollData.dashTime());

		if (enoughInk(entity, this, Math.min(settings.rollData.dashConsumption(), settings.rollData.inkConsumption()), 0, false))
		{
			if (EntityAction.hasSpecificEntityActionAnd(entity, v -> v.getTime() < rollTime, RollerItem.InitialSwingAction.class))
				appliedMobility = settings.swingData.mobility();
			else
			{
				appliedMobility = Mth.lerp(dashProgress, settings.rollData.mobility(), settings.rollData.dashMobility());
			}
		}
		else
		{
			appliedMobility = 0.7;
		}

		return new AttributeModifier(PlayerMovementHandler.SPEED_MOD_IDENTIFIER, appliedMobility - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(Player player, ItemStack stack)
	{
		return getSettings(stack).isBrush ? PlayerPosingHandler.WeaponPose.BRUSH : PlayerPosingHandler.WeaponPose.ROLLER_SWING;
	}
	@Override
	public Optional<SpecialHandler.ResetAction> getResetShootingAction(ItemStack stack, LivingEntity entity)
	{
		if (EntityAction.hasSpecificEntityActionAnd(entity, roll -> !roll.hasAttacked, InitialSwingAction.class))
			return Optional.of(SpecialHandler.ResetAction.RESET_FAILED);

		return Optional.of(() ->
			EntityAction.setEntityAction(entity, null));
	}
	@Override
	public boolean preventsChanging(ItemStack stack, LivingEntity entity)
	{
		return EntityAction.hasSpecificEntityAction(entity, RollerItem.InitialSwingAction.class) || entity.getUseItem().equals(stack);
	}
	@Override
	public boolean preventsChargingInkTank(ItemStack stack, LivingEntity entity)
	{
		return isMoving;
	}
	public static class InitialSwingAction extends EntityActionWithTime
	{
		public static final Codec<InitialSwingAction> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			ItemStack.OPTIONAL_CODEC.fieldOf("stored_stack").forGetter(EntityAction::getStoredStack),
			Codec.FLOAT.fieldOf("total_time").forGetter(InitialSwingAction::getMaxTime),
			Codec.FLOAT.fieldOf("attack_frame").forGetter(v -> v.attackFrame),
			EntitySlot.SERIALIZER_CODEC.fieldOf("item_slot").forGetter(InitialSwingAction::getItemSlot),
			Codec.BOOL.fieldOf("is_grounded").forGetter(InitialSwingAction::isGrounded),
			Codec.BOOL.fieldOf("is_action_queued").forGetter(v -> v.isAttackQueued),
			Codec.BOOL.fieldOf("has_attacked").forGetter(v -> v.hasAttacked),
			Codec.FLOAT.fieldOf("time").forGetter(InitialSwingAction::getTime)
		).apply(inst, InitialSwingAction::new));
		public float attackFrame;
		final ItemStack storedStack;
		final EntitySlot itemSlot;
		protected boolean isGrounded;
		protected boolean isAttackQueued;
		protected boolean hasAttacked;
		public InitialSwingAction(ItemStack stack, float windupTime, float endlagTime, LivingEntity entity)
		{
			super(windupTime + endlagTime);
			isGrounded = entity.onGround();
			storedStack = stack;
			attackFrame = windupTime;
			itemSlot = EntitySlot.searchAndCreateWithStack(entity, stack);
		}
		public InitialSwingAction(ItemStack stack, float totalTime, float attackFrame, EntitySlot itemSlot, boolean isGrounded, boolean isAttackQueued, boolean hasAttacked, float time)
		{
			super(time, totalTime);
			this.attackFrame = attackFrame;
			this.isAttackQueued = isAttackQueued;
			this.hasAttacked = hasAttacked;
			this.isGrounded = isGrounded;
			storedStack = stack;
			this.itemSlot = itemSlot;
		}
		static List<Vector3f> poissonDiskSampling(RandomSource random, float maxAngle, float radiusApart, float minSpeedSquared, float maxSpeed, int maxTries, float yaw)
		{
			List<Vector3f> points = new ObjectArrayList<>();
			// todo: optimize this implementation so it doesn't process points that are outside the angle
			new PoissonSampling.Disk(random.nextLong(), maxSpeed, radiusApart, maxTries, (x, y) ->
			{
				float dist = x * x + y * y;
				if (dist < minSpeedSquared)
					return;

				float thisAngle = (float) Math.atan2(y, x) * Mth.RAD_TO_DEG;
				float distanceFromYaw = Mth.abs(Mth.wrapDegrees(yaw - thisAngle));
				if (distanceFromYaw > maxAngle)
					return;

				points.add(new Vector3f(thisAngle, distanceFromYaw, Mth.sqrt(dist)));
			});
			// wtf org.joml has everything
			return points;
		}
		private static List<Vector3f> calculateAngleAndSpeeds(RandomSource random, FloatRange speedRange, float swingAngle, float projectileSize, float straightShotFrames, float yaw)
		{
			return poissonDiskSampling(random, swingAngle, projectileSize / straightShotFrames * 1.7f, speedRange.min() * speedRange.min() / (straightShotFrames * straightShotFrames), speedRange.max(), 30, yaw);
		}
		public boolean isGrounded()
		{
			return isGrounded;
		}
		@Override
		public ActionEndResult tick(LivingEntity entity)
		{
			if (hasAttacked)
				return ActionEndResult.dontEnd(this);

			if (getTime() < attackFrame)
			{
				if (!entity.level().isClientSide() && entity instanceof Player player)
				{
					OverwriteJumpingStatePacket.popForcedGroundedState(player).ifPresent(grounded ->
						{
							isGrounded = grounded;
							if (!(getStoredStack().getItem() instanceof RollerItem rollerItem))
							{
								setTime(getMaxTime());
								return;
							}
							RollerWeaponSettings settings = rollerItem.getSettings(getStoredStack());
							RollerWeaponSettings.RollerAttackDataRecord data = settings.getAttackData(isGrounded).attackData();
							attackFrame = data.startupTicks();
							setMaxTime(data.getTotalAttackTime());

							SplatcraftPacketHandler.sendToTrackersAndSelf(UpdateEntityActionOnlyPacket.create(entity), entity);
						}
					);
				}
				return ActionEndResult.dontEnd(this);
			}

			hasAttacked = true;
			float extraTime = getTime() - attackFrame;
			if (!(getStoredStack().getItem() instanceof RollerItem rollerItem))
				return ActionEndResult.END_ACTION;

			RollerWeaponSettings settings = rollerItem.getSettings(getStoredStack());
			Level world = entity.level();

			if (world.isClientSide())
				playRollSound(settings.isBrush);

			RollerWeaponSettings.SwingDataRecord swingData = settings.swingData;
			RollerWeaponSettings.RollerAttackDataRecord attackData = settings.getAttackData(isGrounded()).attackData();
			CommonUtils.setSquidDelay(entity, attackData.miscEndlagTicks());
			if (world.isClientSide() || !reduceInk(entity, rollerItem, attackData.inkConsumption(), attackData.inkRecoveryCooldown(), !settings.isBrush || entity.getUseItemRemainingTicks() % 4 == 0))
				return ActionEndResult.dontEnd(this);

			if (settings.isBrush)
			{
				world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.brushFling, SoundSource.PLAYERS, 0.8F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
				int total = swingData.blobCount().getRandom(entity.getRandom());
				int countSmall = Math.round(total * 0.4f);
				int countNormal = total - countSmall;
				AttackId attackId = AttackId.registerSelectiveAttack(total);
				attackId.countProjectile(total);

				RandomSource random = entity.getRandom();

				List<Float> blobAngles = new ObjectArrayList<>(countNormal);
				List<Float> weakBlobAngles = new ObjectArrayList<>(countSmall);

				// how to cope with randomness 101: make a complicated method that is less random
				// yes this is to populate sectors that are divided into equal parts and then select a random part in that sector
				if (total == 1)
				{
					blobAngles.add(0f);
				}
				else
				{
					for (int i = 0; i < countNormal; i++)
					{
						blobAngles.add(((i + random.nextFloat()) / countNormal - 0.5f) * swingData.attackAngle());
					}
					for (int i = 0; i < countSmall; i++)
					{
						weakBlobAngles.add(((i + random.nextFloat()) / countNormal - 0.5f) * swingData.attackAngle());
					}
				}
				createBrushBlobs(entity, blobAngles, countNormal, world, settings, attackId, extraTime, false);
				createBrushBlobs(entity, weakBlobAngles, countSmall, world, settings, attackId, extraTime, true);
			}
			else
			{
				RollerWeaponSettings.FlingDataRecord flingData = settings.flingData;
				world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.rollerFling, SoundSource.PLAYERS, 0.8F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
				if (isGrounded())
				{
					List<Vector3f> anglesAndVelocities = calculateAngleAndSpeeds(
						entity.getRandom(),
						swingData.attackData().speedRange(),
						swingData.attackAngle(),
						swingData.projectileData().size().hitboxRadius(),
						swingData.projectileData().straightShotTicks(),
						entity.getViewYRot(extraTime)
					);

					AttackId attackId = AttackId.registerSelectiveAttack(anglesAndVelocities.size());
					attackId.countProjectile(anglesAndVelocities);
					for (Vector3f data : anglesAndVelocities)
					{
						// x is the projectile's yaw
						// y is the distance between the player's yaw and the projectile's yaw
						// z is the magnitude of the projectile's speed
						InkProjectileEntity proj = new InkProjectileEntity(world, entity, storedStack, InkBlockUtils.getInkType(entity), swingData.projectileData().size(), DamageCalculator.empty());

						proj.shootFromRotation(entity, entity.getViewXRot(extraTime), data.x, 0, data.z, 0f);
//								Vec3d offset = new Vec3d((entity.getRandom().nextFloat() * 2f - 1f) * 0.7f, 0.5f, 0.6f);
						Vec3 offset = new Vec3(0f, 0.5f, 0.6f);
						offset = offset.yRot(-entity.getViewYRot(extraTime) * Mth.DEG_TO_RAD);
						proj.moveTo(proj.getX() + offset.x, proj.getY() + offset.y, proj.getZ() + offset.z);

						proj.damage = DamageCalculator.roller(swingData.projectileData(), proj.position(), data.y > swingData.letalAngle());
						proj.setRollerSwingStats(settings, false, data.y > swingData.letalAngle());
						proj.setAttackId(attackId);
						world.addFreshEntity(proj);
						proj.tick(extraTime);
					}
				}
				else
				{
					int count = flingData.calculateProjectileCount();
					AttackId attackId = AttackId.registerSelectiveAttack(count);
					attackId.countProjectile(count);

					for (int i = 0; i < count; i++)
					{
						InkProjectileEntity proj = new InkProjectileEntity(world, entity, storedStack, InkBlockUtils.getInkType(entity), flingData.projectileData().size(), DamageCalculator.empty());

						float progress = (float) i / Math.max(1, count - 1);
						proj.shootFromRotation(
							entity,
							entity.getXRot() - Mth.lerp(progress, flingData.startPitchCompensation(), flingData.endPitchCompensation()),
							entity.getYRot(), 0,
							attackData.speedRange().getValue(progress),
							0.05f);

						proj.setRollerSwingStats(settings, true, false);
						proj.accumulatedDrops = progress;
						proj.moveTo(proj.position().add(EntityAccessor.invokeGetInputVector(new Vec3(0, 1, 0), 1.4f, proj.getYRot())));
						proj.damage = DamageCalculator.roller(swingData.projectileData(), proj.position(), false);

						proj.setAttackId(attackId);
						world.addFreshEntity(proj);
						proj.tick(extraTime);
					}
				}
			}
			return ActionEndResult.dontEnd(this);
		}
		private void createBrushBlobs(LivingEntity entity, List<Float> preparedAngles, int count, Level world, RollerWeaponSettings settings, AttackId attackId, float extraTime, boolean weak)
		{
			RollerWeaponSettings.SwingDataRecord swingData = settings.swingData;
			RandomSource random = entity.getRandom();
			for (int i = 0; i < count; i++)
			{
				InkProjectileEntity proj = new InkProjectileEntity(world, entity, storedStack, InkBlockUtils.getInkType(entity), swingData.projectileData().size(), DamageCalculator.empty());

				Float angle = preparedAngles.remove(random.nextInt(preparedAngles.size()));
				if (angle == null)
					angle = 0f;

				proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot() + angle, 0, swingData.attackData().speedRange().getRandom(random) * (weak ? 0.6f : 1f), 0f);
				proj.moveTo(proj.getX(), proj.getY() - entity.getEyeHeight() / 2f, proj.getZ());

				proj.damage = DamageCalculator.roller(swingData.projectileData(), proj.position(), weak);
				proj.setAttackId(attackId);
				proj.setBrushSwingStats(settings, weak);
				world.addFreshEntity(proj);
				proj.tick(extraTime);
			}
		}
		public boolean canQueueSwing()
		{
			return getTime() >= attackFrame - 2;
		}
		@Override
		public ActionEndResult canEnd(LivingEntity entity, EndType endType)
		{
			if (isAttackQueued)
			{
				isGrounded = entity.onGround();
				hasAttacked = false;
				isAttackQueued = false;

				if (!(getStoredStack().getItem() instanceof RollerItem rollerItem))
				{
					return null;
				}
				RollerWeaponSettings settings = rollerItem.getSettings(getStoredStack());
				RollerWeaponSettings.RollerAttackDataRecord data = settings.getAttackData(isGrounded).attackData();
				attackFrame = data.startupTicks();
				setTime(getTime() - getMaxTime());
				setMaxTime(data.getTotalAttackTime());

				if (entity.level().isClientSide())
				{
					if (entity instanceof Player player)
						sendGroundedSynchronizationPacket(player);
				}
				else
					SplatcraftPacketHandler.sendToTrackersAndSelf(UpdateEntityActionOnlyPacket.create(entity), entity);
				return ActionEndResult.dontEnd(this);
			}
			return ActionEndResult.END_ACTION;
		}
		@Override
		public boolean reversedTime()
		{
			return true;
		}
		@Override
		public boolean preventWeaponUse()
		{
			return !canQueueSwing();
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
	}
}