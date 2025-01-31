package net.splatcraft.items.weapons;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.splatcraft.blocks.ColoredBarrierBlock;
import net.splatcraft.client.audio.RollerRollTickableSound;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.entities.SquidBumperEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.handlers.WeaponHandler;
import net.splatcraft.items.weapons.settings.RollerWeaponSettings;
import net.splatcraft.mixin.accessors.EntityAccessor;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateEntityActionOnlyPacket;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.*;
import net.splatcraft.util.NumberRange.FloatRange;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.EntityActionWithTime;
import org.joml.Vector3f;
import org.joml.sampling.PoissonSampling;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RollerItem extends WeaponBaseItem<RollerWeaponSettings>
{
	public static final ArrayList<RollerItem> rollers = Lists.newArrayList();
	public boolean isMoving;
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
		return registry.register(name, () -> new RollerItem(parent.get().settingsId.toString()));
	}
	public static void applyRecoilKnockback(LivingEntity entity, double pow)
	{
		entity.setVelocity(new Vec3d(Math.cos(Math.toRadians(entity.getYaw() + 90)) * -pow, entity.getVelocity().y, Math.sin(Math.toRadians(entity.getYaw() + 90)) * -pow));
		entity.velocityModified = true;
	}
	@Environment(EnvType.CLIENT)
	protected static void playRollSound(boolean isBrush)
	{
		MinecraftClient.getInstance().getSoundManager().playNextTick(new RollerRollTickableSound(ClientUtils.getClientPlayer(), isBrush));
	}
	@Override
	public Class<RollerWeaponSettings> getSettingsClass()
	{
		return RollerWeaponSettings.class;
	}
	public ClampedModelPredicateProvider getUnfolded()
	{
		return (stack, level, entity, seed) ->
		{
			if (entity instanceof PlayerEntity player)
			{
				InitialSwingAction action = EntityAction.getSpecificEntityAction(player, InitialSwingAction.class);
				if (action != null)
				{
					if (action.getSlotIndex() > -1)
					{
						ItemStack weaponStack = action.getHand() == Hand.MAIN_HAND ? player.getInventory().main.get(action.getSlotIndex())
							: entity.getOffHandStack();
						return stack.equals(weaponStack) && (getSettings(stack).isBrush || action.isGrounded() || action.getTime() < action.attackFrame - 2) ? 1 : 0;
					}
				}
			}
			return entity != null && entity.isUsingItem() && entity.getActiveItem() == stack && entity.getItemUseTime() > 10 ? 1 : 0;
		};
	}
	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks)
	{
		if (remainingUseTicks == stack.getMaxUseTime(user))
		{
			usedOnGround = user.isOnGround();
			Optional<InitialSwingAction> optional = EntityAction.getSpecificEntityActionOptional(user, InitialSwingAction.class);
			optional.ifPresentOrElse(action ->
			{
				if (action.canQueueSwing())
				{
					action.isAttackQueued = true;
				}
			}, () ->
			{
				RollerWeaponSettings settings = getSettings(stack);
				RollerWeaponSettings.RollerAttackDataRecord attackData = settings.getAttackData(usedOnGround).attackData();
				if (!world.isClient && enoughInk(user, stack.getItem(), attackData.inkConsumption(), attackData.inkRecoveryCooldown(), false))
				{
					EntityAction.setEntityAction(user, new InitialSwingAction(stack, attackData.startupTicks(), attackData.endlagTicks(), user));
					SplatcraftPacketHandler.sendToTrackersAndSelf(new UpdateEntityActionOnlyPacket(user), user);
				}
			});
		}
		
		super.usageTick(world, user, stack, remainingUseTicks);
	}
	@Override
	public void weaponUseTick(World world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
	{
		RollerWeaponSettings settings = getSettings(stack);
		
		int rollTime = entity.getItemUseTime() - Math.round(settings.getAttackData(usedOnGround).attackData().attackTime());
		if (rollTime <= 0)
			return;
		
		float toConsume = MathHelper.lerp(Math.min(1, rollTime / settings.rollData.dashTime()), settings.rollData.inkConsumption(), settings.rollData.dashConsumption());
		if (world.isClient)
		{
			isMoving = Math.abs(entity.prevHeadYaw - entity.headYaw) > 0 || (entity.forwardSpeed != 0 || entity.sidewaysSpeed != 0);
		}
		else
		{
			WeaponHandler.OldEntityTransformData oldData = WeaponHandler.getEntityPrevPos(entity);
			isMoving = oldData.oldOldPosition.squaredDistanceTo(entity.getPos()) > 0 || Math.abs(oldData.oldOldRot.y - entity.getYaw()) > 0;
		}
		
		boolean doPush = false;
		if (isMoving)
		{
			doPush = doRoll(world, entity, stack, remainingUseTicks, settings, toConsume);
		}
		if (doPush)
			applyRecoilKnockback(entity, 0.8);
	}
	private boolean doRoll(World world, LivingEntity entity, ItemStack stack, int remainingUseTicks, RollerWeaponSettings settings, float inkConsumption)
	{
		double dxOff = 0;
		double dzOff = 0;
		for (int i = 1; i <= 2; i++)
		{
			dxOff = Math.cos(Math.toRadians(entity.getYaw() + 90)) * i;
			dzOff = Math.sin(Math.toRadians(entity.getYaw() + 90)) * i;
			
			BlockPos pos = CommonUtils.createBlockPos(entity.getX() + dxOff, entity.getY(), entity.getZ() + dzOff);
			if (!InkBlockUtils.canInkPassthrough(world, pos))
				break;
		}
		
		boolean doPush = false;
		BlockInkedResult result = BlockInkedResult.FAIL;
		float step = CommonUtils.calculateStep(settings.rollData.inkSize(), 1);
		for (float i = 0; i < settings.rollData.inkSize(); i += step)
		{
			float off = i - settings.rollData.inkSize() / 2f;
			boolean insideDamage = Math.abs(off) < settings.rollData.hitboxSize() / 2f;
			double xOff = Math.cos(Math.toRadians(entity.getYaw())) * off;
			double zOff = Math.sin(Math.toRadians(entity.getYaw())) * off;
			
			for (float yOff = 0; yOff >= -3; yOff--)
			{
				if (!enoughInk(entity, this, inkConsumption, 0, remainingUseTicks % 4 == 0))
				{
					break;
				}
				
				if (yOff == -3)
				{
					dxOff = Math.cos(Math.toRadians(entity.getYaw() + 90));
					dzOff = Math.sin(Math.toRadians(entity.getYaw() + 90));
				}
				
				BlockPos pos = CommonUtils.createBlockPos(entity.getX() + xOff + dxOff, entity.getY() + yOff, entity.getZ() + zOff + dzOff);
				
				if (world.getBlockState(pos).getBlock() instanceof ColoredBarrierBlock block && block.canAllowThrough(pos, entity))
					continue;
				
				if (!InkBlockUtils.canInkPassthrough(world, pos))
				{
					VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos);
					
					result = InkBlockUtils.inkBlock(entity, world, pos, ColorUtils.getInkColor(stack), Direction.UP, InkBlockUtils.getInkType(entity), settings.rollData.damage());
					double blockHeight = shape.isEmpty() ? 0 : shape.getBoundingBox().maxY;
					
					if (yOff != -3 && !(shape.getBoundingBox().minX <= 0 && shape.getBoundingBox().minZ <= 0 && shape.getBoundingBox().maxX >= 1 && shape.getBoundingBox().maxZ >= 1))
					{
						BlockInkedResult secondResult = InkBlockUtils.inkBlock(entity, world, pos.down(), ColorUtils.getInkColor(stack), Direction.UP, InkBlockUtils.getInkType(entity), settings.rollData.damage());
						if (result == BlockInkedResult.FAIL)
						{
							result = secondResult;
						}
					}
					
					if (result != BlockInkedResult.FAIL && insideDamage)
					{
						world.addParticle(new InkSplashParticleData(ColorUtils.getInkColor(stack), 1), entity.getX() + xOff + dxOff, pos.getY() + blockHeight + 0.1, entity.getZ() + zOff + dzOff, 0, 0, 0);
						if (i > 0)
						{
							double xhOff = dxOff + Math.cos(Math.toRadians(entity.getYaw())) * (off - 0.5);
							double zhOff = dzOff + Math.sin(Math.toRadians(entity.getYaw())) * (off - 0.5);
							world.addParticle(new InkSplashParticleData(ColorUtils.getInkColor(stack), 1), entity.getX() + xhOff, pos.getY() + blockHeight + 0.1, entity.getZ() + zhOff, 0, 0, 0);
						}
					}
					break;
				}
			}
			
			if (world.isClient())
			{
				// Damage and knockback are dealt server-side
				continue;
			}
			
			if (!insideDamage)
			{
				continue;
			}
			
			BlockPos attackPos = CommonUtils.createBlockPos(entity.getX() + xOff + dxOff, entity.getY() - 1, entity.getZ() + zOff + dzOff);
			for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, Box.enclosing(attackPos, attackPos.add(1, 2, 1)), EntityPredicates.EXCEPT_SPECTATOR.and(e ->
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
		if (EntityAction.hasSpecificEntityAction(entity, RollerItem.InitialSwingAction.class) || !entity.getActiveItem().equals(stack))
			return false;
		return super.hasSpeedModifier(entity, stack);
	}
	@Override
	public EntityAttributeModifier getSpeedModifier(LivingEntity entity, ItemStack stack)
	{
		RollerWeaponSettings settings = getSettings(stack);
		double appliedMobility;
		float useTime = entity.getItemUseTime() - settings.getAttackData(usedOnGround).attackData().attackTime();
		float dashProgress = Math.min(1, useTime / settings.rollData.dashTime());
		
		if (enoughInk(entity, this, Math.min(settings.rollData.dashConsumption(), settings.rollData.inkConsumption()), 0, false))
		{
			if (entity instanceof PlayerEntity && EntityAction.hasEntityAction(entity))
				appliedMobility = settings.swingData.mobility();
			else
			{
				appliedMobility = dashProgress * (settings.rollData.dashMobility() - settings.rollData.mobility()) + settings.rollData.mobility();
			}
		}
		else
		{
			appliedMobility = 0.7;
		}
		
		return new EntityAttributeModifier(SplatcraftItems.SPEED_MOD_IDENTIFIER, appliedMobility - 1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(PlayerEntity player, ItemStack stack)
	{
		return getSettings(stack).isBrush ? PlayerPosingHandler.WeaponPose.BRUSH : PlayerPosingHandler.WeaponPose.ROLL;
	}
	public static class InitialSwingAction extends EntityActionWithTime
	{
		public static final Codec<InitialSwingAction> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			ItemStack.OPTIONAL_CODEC.fieldOf("stored_stack").forGetter(EntityAction::getStoredStack),
			Codec.FLOAT.fieldOf("total_time").forGetter(InitialSwingAction::getMaxTime),
			Codec.FLOAT.fieldOf("attack_frame").forGetter(v -> v.attackFrame),
			Codec.INT.fieldOf("slot_index").forGetter(InitialSwingAction::getSlotIndex),
			CodecUtils.HAND_NULL_IS_MAIN_CODEC.fieldOf("used_hand").forGetter(InitialSwingAction::getHand),
			Codec.BOOL.fieldOf("is_grounded").forGetter(InitialSwingAction::isGrounded),
			Codec.BOOL.fieldOf("is_action_queued").forGetter(v -> v.isAttackQueued),
			Codec.BOOL.fieldOf("has_attacked").forGetter(v -> v.hasAttacked),
			Codec.FLOAT.fieldOf("time").forGetter(InitialSwingAction::getTime)
		).apply(inst, InitialSwingAction::new));
		public final float attackFrame;
		final ItemStack storedStack;
		final int slotIndex;
		final Hand hand;
		protected boolean isGrounded, isAttackQueued, hasAttacked;
		public InitialSwingAction(ItemStack stack, float windupTime, float endlagTime, LivingEntity entity)
		{
			super(windupTime + endlagTime);
			isGrounded = entity.isOnGround();
			storedStack = stack;
			attackFrame = endlagTime;
			slotIndex = entity instanceof PlayerEntity player ? player.getInventory().selectedSlot : -1;
			hand = entity.getActiveHand();
		}
		public InitialSwingAction(ItemStack stack, float totalTime, float attackFrame, int slotIndex, Hand hand, boolean isGrounded, boolean isAttackQueued, boolean hasAttacked, float time)
		{
			super(time, totalTime);
			this.attackFrame = attackFrame;
			this.isAttackQueued = isAttackQueued;
			this.hasAttacked = hasAttacked;
			this.isGrounded = isGrounded;
			storedStack = stack;
			this.slotIndex = slotIndex;
			this.hand = hand;
		}
		static List<Vector3f> poissonDiskSampling(Random random, float maxAngle, float radiusApart, float minSpeedSquared, float maxSpeed, int maxTries, float yaw)
		{
			List<Vector3f> points = new ObjectArrayList<>();
			// todo: optimize this implementation so it doesn't process points that are outside the angle
			new PoissonSampling.Disk(random.nextLong(), maxSpeed, radiusApart, maxTries, (x, y) ->
			{
				float dist = x * x + y * y;
				if (dist < minSpeedSquared)
					return;
				
				float thisAngle = (float) Math.atan2(y, x) * MathHelper.DEGREES_PER_RADIAN;
				float distanceFromYaw = MathHelper.abs(MathHelper.wrapDegrees(yaw - thisAngle));
				if (distanceFromYaw > maxAngle)
					return;
				
				points.add(new Vector3f(thisAngle, distanceFromYaw, MathHelper.sqrt(dist)));
			});
			// wtf org.joml has everything
			return points;
		}
		private static List<Vector3f> calculateAngleAndSpeeds(Random random, FloatRange speedRange, float swingAngle, float projectileSize, float straightShotFrames, float yaw)
		{
			return poissonDiskSampling(random, swingAngle, projectileSize / straightShotFrames / 1.35f, speedRange.min() * speedRange.min() / (straightShotFrames * straightShotFrames), speedRange.max(), 30, yaw);
		}
		public boolean isGrounded()
		{
			return isGrounded;
		}
		@Override
		public void tick(LivingEntity entity)
		{
			if (!hasAttacked)
			{
				if (getTime() <= attackFrame)
				{
					hasAttacked = true;
					float extraTime = attackFrame - getTime();
					if (!(getStoredStack().getItem() instanceof RollerItem rollerItem))
					{
						return;
					}
					RollerWeaponSettings settings = rollerItem.getSettings(getStoredStack());
					World world = entity.getWorld();
					
					if (world.isClient())
						playRollSound(settings.isBrush);
					
					RollerWeaponSettings.SwingDataRecord swingData = settings.swingData;
					RollerWeaponSettings.RollerAttackDataRecord attackData = settings.getAttackData(isGrounded()).attackData();
					if (world.isClient() || !reduceInk(entity, rollerItem, attackData.inkConsumption(), attackData.inkRecoveryCooldown(), !settings.isBrush || entity.getItemUseTimeLeft() % 4 == 0))
						return;
					
					if (settings.isBrush)
					{
						world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.brushFling, SoundCategory.PLAYERS, 0.8F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
						int total = swingData.blobCount().getRandom(entity.getRandom());
						int countSmall = Math.round(total * 0.4f);
						int countNormal = total - countSmall;
						AttackId attackId = AttackId.registerSelectiveAttack(total);
						attackId.countProjectile(total);
						
						Random random = entity.getRandom();
						
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
						world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.rollerFling, SoundCategory.PLAYERS, 0.8F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
						if (isGrounded())
						{
							List<Vector3f> anglesAndVelocities = calculateAngleAndSpeeds(
								entity.getRandom(),
								swingData.attackData().speedRange(),
								swingData.attackAngle(),
								swingData.projectileData().size(),
								swingData.projectileData().straightShotTicks(),
								entity.getYaw(extraTime)
							);
							
							AttackId attackId = AttackId.registerSelectiveAttack(anglesAndVelocities.size());
							attackId.countProjectile(anglesAndVelocities);
							for (Vector3f data : anglesAndVelocities)
							{
								// x is the projectile's yaw
								// y is the distance between the player's yaw and the projectile's yaw
								// z is the magnitude of the projectile's speed
								InkProjectileEntity proj = new InkProjectileEntity(world, entity, storedStack, InkBlockUtils.getInkType(entity), swingData.projectileData().size(), settings);
								
								proj.setVelocity(entity, entity.getPitch(extraTime), data.x, 0, data.z, 0f);
//								Vec3d offset = new Vec3d((entity.getRandom().nextFloat() * 2f - 1f) * 0.7f, 0.5f, 0.6f);
								Vec3d offset = new Vec3d(0f, 0.5f, 0.6f);
								offset = offset.rotateY(-entity.getYaw(extraTime) * MathHelper.RADIANS_PER_DEGREE);
								proj.refreshPositionAfterTeleport(proj.getX() + offset.x, proj.getY() + offset.y, proj.getZ() + offset.z);
								
								proj.setRollerSwingStats(settings, false, data.y > swingData.letalAngle());
								proj.setAttackId(attackId);
								world.spawnEntity(proj);
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
								InkProjectileEntity proj = new InkProjectileEntity(world, entity, storedStack, InkBlockUtils.getInkType(entity), flingData.projectileData().size(), settings);
								
								float progress = (float) i / Math.max(1, count - 1);
								proj.setVelocity(
									entity,
									entity.getPitch() - MathHelper.lerp(progress, flingData.startPitchCompensation(), flingData.endPitchCompensation()),
									entity.getYaw(), 0,
									attackData.speedRange().getValue(progress),
									0.05f);
								
								proj.setRollerSwingStats(settings, true, false);
								proj.accumulatedDrops = progress;
								proj.refreshPositionAfterTeleport(proj.getPos().add(EntityAccessor.invokeMovementInputToVelocity(new Vec3d(0, 1, 0), 1.4f, proj.getYaw())));
								proj.setAttackId(attackId);
								world.spawnEntity(proj);
								proj.tick(extraTime);
							}
						}
					}
				}
			}
		}
		private void createBrushBlobs(LivingEntity entity, List<Float> preparedAngles, int count, World world, RollerWeaponSettings settings, AttackId attackId, float extraTime, boolean weak)
		{
			RollerWeaponSettings.SwingDataRecord swingData = settings.swingData;
			Random random = entity.getRandom();
			for (int i = 0; i < count; i++)
			{
				InkProjectileEntity proj = new InkProjectileEntity(world, entity, storedStack, InkBlockUtils.getInkType(entity), swingData.projectileData().size(), settings);
				
				Float angle = preparedAngles.remove(random.nextInt(preparedAngles.size()));
				if (angle == null)
					angle = 0f;
				
				proj.setVelocity(entity, entity.getPitch(), entity.getYaw() + angle, 0, swingData.attackData().speedRange().getRandom(random) * (weak ? 0.6f : 1f), 0f);
				proj.refreshPositionAfterTeleport(proj.getX(), proj.getY() - entity.getStandingEyeHeight() / 2f, proj.getZ());
				proj.setAttackId(attackId);
				proj.setBrushSwingStats(settings, weak);
				world.spawnEntity(proj);
				proj.tick(extraTime);
			}
		}
		public boolean canQueueSwing()
		{
			return getTime() < attackFrame + 1;
		}
		@Override
		public boolean canEnd(LivingEntity entity)
		{
			if (isAttackQueued)
			{
				isGrounded = entity.isOnGround();
				hasAttacked = false;
				isAttackQueued = false;
				setTime(getTime() + getMaxTime());
				SplatcraftPacketHandler.sendToTrackersAndSelf(new UpdateEntityActionOnlyPacket(entity), entity);
				return false;
			}
			return true;
		}
		@Override
		public boolean preventWeaponUse()
		{
			return true;
		}
		@Override
		public ItemStack getStoredStack()
		{
			return storedStack;
		}
		@Override
		public int getSlotIndex()
		{
			return slotIndex;
		}
		@Override
		public Hand getHand()
		{
			return hand;
		}
	}
}