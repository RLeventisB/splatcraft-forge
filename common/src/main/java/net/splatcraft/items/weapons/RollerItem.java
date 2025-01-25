package net.splatcraft.items.weapons;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
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
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.*;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.EntityActionWithTime;

import java.util.ArrayList;
import java.util.Optional;

public class RollerItem extends WeaponBaseItem<RollerWeaponSettings>
{
	public static final ArrayList<RollerItem> rollers = Lists.newArrayList();
	public boolean isMoving;
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
						return stack.equals(weaponStack) && (getSettings(stack).isBrush || action.isGrounded()) ? 1 : 0;
					}
				}
			}
			return entity != null && entity.isUsingItem() && entity.getActiveItem() == stack ? 1 : 0;
		};
	}
	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks)
	{
		if (remainingUseTicks == stack.getMaxUseTime(user))
		{
			Optional<InitialSwingAction> optional = EntityAction.getSpecificActionIf(user, EntityAction::preventWeaponUse, InitialSwingAction.class);
			optional.ifPresent(action ->
			{
				if (action.canQueueSwing())
				{
					action.isAttackQueued = true;
				}
			});
		}
		
		super.usageTick(world, user, stack, remainingUseTicks);
	}
	@Override
	public void weaponUseTick(World world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
	{
		RollerWeaponSettings settings = getSettings(stack);
		RollerWeaponSettings.RollerAttackDataRecord attackData = entity.isOnGround() ? settings.swingData.attackData() : settings.flingData.attackData();
		float startupTicks = attackData.startupTime();
		int rollTime = getMaxUseTime(stack, entity) - remainingUseTicks;
		if (rollTime < startupTicks)
		{
			if (enoughInk(entity, stack.getItem(), attackData.inkConsumption(), attackData.inkRecoveryCooldown(), false))
			{
				EntityAction.setEntityAction(entity, new InitialSwingAction(stack, startupTicks, attackData.endlagTicks(), entity));
			}
			return;
		}
		
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
		for (int i = 0; i < settings.rollData.inkSize(); i++)
		{
			double off = (double) i - (settings.rollData.inkSize() - 1) / 2d;
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
					
					if (result != BlockInkedResult.FAIL && i < settings.rollData.hitboxSize())
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
			
			if (i >= settings.rollData.hitboxSize())
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
		if (entity instanceof PlayerEntity && EntityAction.hasEntityAction(entity) || !entity.getActiveItem().equals(stack))
			return false;
		return super.hasSpeedModifier(entity, stack);
	}
	@Override
	public EntityAttributeModifier getSpeedModifier(LivingEntity entity, ItemStack stack)
	{
		RollerWeaponSettings settings = getSettings(stack);
		double appliedMobility;
		int useTime = entity.getItemUseTime() - entity.getItemUseTimeLeft();
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
		final ItemStack storedStack;
		final int slotIndex;
		final Hand hand;
		final float attackFrame;
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
					
					RollerWeaponSettings.RollerAttackDataRecord attackData = isGrounded() ? settings.swingData.attackData() : settings.flingData.attackData();
					if (world.isClient() || !reduceInk(entity, rollerItem, attackData.inkConsumption(), attackData.inkRecoveryCooldown(), !settings.isBrush || entity.getItemUseTimeLeft() % 4 == 0))
						return;
					
					AttackId attackId = AttackId.registerAttack();
					
					if (settings.isBrush)
					{
						world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.brushFling, SoundCategory.PLAYERS, 0.8F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
						int total = settings.rollData.inkSize() * 2 + 1;
						for (int i = 0; i < total; i++)
						{
							InkProjectileEntity proj = new InkProjectileEntity(world, entity, storedStack, InkBlockUtils.getInkType(entity), 1.6f, settings);
							proj.setProjectileType(InkProjectileEntity.Types.ROLLER);
							proj.dropImpactSize = proj.getProjectileSize() * 0.5f;
							proj.setVelocity(entity, entity.getPitch(), entity.getYaw() + (i - total / 2f) * 20, 0, settings.swingData.attackData().maxSpeed(), 0.05f);
							proj.refreshPositionAfterTeleport(proj.getX(), proj.getY() - entity.getStandingEyeHeight() / 2f, proj.getZ());
							world.spawnEntity(proj);
						}
					}
					else
					{
						world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.rollerFling, SoundCategory.PLAYERS, 0.8F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
						if (isGrounded())
						{
							// TODO: use https://sighack.com/post/poisson-disk-sampling-bridsons-algorithm for better distribution

                            /*
                            for (float i = 0; i < settings.swingProjectileCount; i++)
                            {
                                float progress = 1 - (float) Math.pow(1 - i / (float) (settings.swingProjectileCount - 1), 2);
                                InkProjectileEntity proj = new InkProjectileEntity(level, player, stack, InkBlockUtils.getInkType(player), 1.6f, settings);
                                proj.throwerAirborne = false;
                                float extraAngle = settings.swingAttackAngle * (float) (proj.getRandom().nextGaussian());
                                proj.shootFromRotation(player, player.getPitch(), player.getYaw() + extraAngle * side, 0, settings.swingProjectileSpeed * progress, 0.05f);
                                proj.refreshPositionAfterTeleport(proj.getX(), proj.getY() + 0.5, proj.getZ());
			
                                if (extraAngle > settings.swingLetalAngle)
                                {
                                    proj.damageMultiplier = settings.swingOffAnglePenalty;
                                }
                                proj.setRollerSwingStats(settings);
                                level.spawnEntity(proj);
			
                                side = -side;
                            }
                            */
						}
						else
						{
							int count = settings.flingData.calculateProjectileCount();
							attackId.countProjectile(count);
							
							for (int i = 0; i < count; i++)
							{
								InkProjectileEntity proj = new InkProjectileEntity(world, entity, storedStack, InkBlockUtils.getInkType(entity), 1.6f, settings);
								proj.throwerAirborne = true;
								
								float progress = (float) i / Math.max(1, count - 1);
								proj.setVelocity(
									entity,
									entity.getPitch() - MathHelper.lerp(progress, settings.flingData.startPitchCompensation(), settings.flingData.endPitchCompensation()),
									entity.getYaw(), 0,
									MathHelper.lerp(progress, attackData.minSpeed(), attackData.maxSpeed()),
									0.05f);
								
								proj.refreshPositionAfterTeleport(proj.getPos().add(EntityAccessor.invokeMovementInputToVelocity(new Vec3d(0, 1, 0), 1.4f, proj.getYaw())));
								proj.setRollerSwingStats(settings);
								proj.setAttackId(attackId);
								world.spawnEntity(proj);
								proj.tick(extraTime);
							}
						}
					}
				}
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