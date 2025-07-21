package net.splatcraft.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateInkOverlayPacket;
import net.splatcraft.platform.Components;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class SquidBumperEntity extends LivingEntity implements IColoredEntity
{
	public static final float maxInkHealth = 20.0F;
	public static final int maxRespawnTime = 60;
	private static final EntityDataAccessor<Boolean> IMMORTAL = SynchedEntityData.defineId(SquidBumperEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<InkColor> COLOR = SynchedEntityData.defineId(SquidBumperEntity.class, CommonUtils.INKCOLOR_DATA_HANDLER);
	private static final EntityDataAccessor<Integer> RESPAWN_TIME = SynchedEntityData.defineId(SquidBumperEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> SPLAT_HEALTH = SynchedEntityData.defineId(SquidBumperEntity.class, EntityDataSerializers.FLOAT);
	public boolean inkproof = false;
	/**
	 * After punching the stand, the cooldown before you can punch it again without breaking it.
	 */
	public long punchCooldown;
	public long hurtCooldown;
	public SquidBumperEntity(EntityType<? extends LivingEntity> type, Level world)
	{
		super(type, world);
	}
	public static AttributeSupplier.Builder setCustomAttributes()
	{
		return createLivingAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0D);
	}
	@Override
	protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(COLOR, ColorUtils.getDefaultColor());
		builder.define(SPLAT_HEALTH, maxInkHealth);
		builder.define(RESPAWN_TIME, maxRespawnTime);
		builder.define(IMMORTAL, false);
	}
	@Override
	public void tick()
	{
		super.tick();
		
		hurtCooldown = Math.max(hurtCooldown - 1, 0);
		
		if (getRespawnTime() > 1)
			setRespawnTime(getRespawnTime() - 1);
		
		if (getRespawnTime() == 20 && getInkHealth() <= 0)
		{
			level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.squidBumperRespawning, getSoundSource(), 1, 1);
		}
		else if (getRespawnTime() == 1)
			respawn();
		
		BlockPos pos = getBlockPosBelowThatAffectsMyMovement();
		
		if (level().getBlockState(pos).getBlock() == SplatcraftBlocks.inkwell.get() && level().getBlockEntity(pos) instanceof InkColorTileEntity te)
		{
			if (te.getInkColor() != getColor())
				setColor(te.getInkColor());
		}
	}
	@Override
	public boolean isPickable()
	{
		return getInkHealth() > 0 || isImmortal();
	}
	@Override
	public boolean onEntityInked(DamageSource source, float damage, InkColor color)
	{
		if (hurtCooldown <= 0 && isPickable() && !inkproof)
		{
			ink(damage, color);
			if (!isPickable())
			{
				level().broadcastEntityEvent(this, (byte) 34);
			}
			return true;
		}
		return false;
	}
	/**
	 * Called when the entity is attacked.
	 */
	@Override
	public boolean hurt(@NotNull DamageSource source, float amount)
	{
		if (!level().isClientSide() && isAlive())
		{
			if (source.is(DamageTypes.FELL_OUT_OF_WORLD))
			{
				discard();
				return false;
			}
			else if (!isInvulnerableTo(source))
			{
				if (source.is(DamageTypes.EXPLOSION))
				{
					dropBumper();
					discard();
					return false;
				}
				else if (source.is(DamageTypes.IN_FIRE))
				{
					if (isOnFire())
					{
						damageBumper(source, 0.15F);
					}
					else
					{
						igniteForSeconds(5);
					}
					
					return false;
				}
				else if (source.is(DamageTypes.ON_FIRE) && getHealth() > 0.5F)
				{
					damageBumper(source, 4.0F);
					return false;
				}
				else
				{
					if (
						(source.getDirectEntity() instanceof AbstractArrow projectileEntity &&
							projectileEntity.getPierceLevel() > 0 &&
							source.getMsgId().equals("player")) ||
							(
								source.getEntity() instanceof Player player && player.getAbilities().mayBuild
							))
					{
					
					}
					boolean flag1 = source.getDirectEntity() instanceof AbstractArrow projectileEntity && projectileEntity.getPierceLevel() > 0;
					if (!"player".equals(source.getMsgId()) && !(source.getDirectEntity() instanceof AbstractArrow))
					{
						return false;
					}
					else if (source.getEntity() instanceof Player player && !player.getAbilities().mayBuild)
					{
						return false;
					}
					else if (source.isCreativePlayer())
					{
						playBrokenSound();
						playParticles();
						discard();
						return flag1;
					}
					else
					{
						long i = level().getGameTime();
						if (i - punchCooldown > 5L && !(source.getDirectEntity() instanceof AbstractArrow projectileEntity))
						{
							level().broadcastEntityEvent(this, (byte) 32);
							punchCooldown = i;
						}
						else
						{
							dropBumper();
							playParticles();
							discard();
						}
						
						return true;
					}
				}
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}
	private void playParticles()
	{
		if (level() instanceof ServerLevel serverLevel)
		{
			serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WHITE_WOOL.defaultBlockState()), getX(), getEyePosition().y, getZ(), 10, getBbWidth() / 4.0F, getBbHeight() / 4.0F, getBbWidth() / 4.0F, 0.05D);
		}
	}
	private void playPopParticles()
	{
		for (int i = 0; i < 10; i++)
		{
			level().addParticle(new InkSplashParticleData(getColor(), 2), getX(), getY() + getBbHeight() * 0.5, getZ(), random.nextDouble() * 0.5 - 0.25, random.nextDouble() * 0.5 - 0.25, random.nextDouble() * 0.5 - 0.25);
		}
		level().addParticle(new InkExplosionParticleData(getColor(), 2), getX(), getY() + getBbHeight() * 0.5, getZ(), 0, 0, 0);
	}
	private void playHealParticles()
	{
		level().addParticle(new InkSplashParticleData(Components.INK_OVERLAY.get(this).getColor().get(), 2), getX(), getY() + getBbHeight() * 0.5, getZ(), 0, 0, 0);
	}
	private void playBrokenSound()
	{
		level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.squidBumperBreak, getSoundSource(), 1.0F, 1.0F);
	}
	private void damageBumper(DamageSource source, float dmg)
	{
		float f = getHealth();
		f -= dmg;
		if (f <= 0.5F)
		{
			dropBumper();
			discard();
		}
		else
		{
			setHealth(f);
		}
	}
	@Override
	@OnlyIn(Dist.CLIENT)
	public void handleEntityEvent(byte id)
	{
		switch (id)
		{
			case 31:
				if (level().isClientSide())
				{
					hurtCooldown = level().getGameTime();
					level().playLocalSound(getX(), getY(), getZ(), SplatcraftSounds.squidBumperInk, getSoundSource(), 0.3F, 1.0F, false);
				}
				break;
			case 32:
				if (level().isClientSide())
				{
					level().playLocalSound(getX(), getY(), getZ(), SplatcraftSounds.squidBumperHit, getSoundSource(), 0.3F, 1.0F, false);
					punchCooldown = level().getGameTime();
				}
				break;
			case 34:
				if (level().isClientSide())
				{
					level().playLocalSound(getX(), getY(), getZ(), SplatcraftSounds.squidBumperPop, getSoundSource(), 0.5F, 20.0F, false);
					Components.INK_OVERLAY.getOrCreate(this).setAmount(0);
					playPopParticles();
				}
				break;
			
			default:
				super.handleEntityEvent(id);
		}
	}
	@Override
	protected boolean isImmobile()
	{
		return true;
	}
	@Override
	public boolean isPushable()
	{
		return false;
	}
	@Override
	public void doPush(@NotNull Entity entity)
	{
		if (!isPickable())
			return;
		
		if (!isPassengerOfSameVehicle(entity))
		{
			if (!entity.noPhysics && !noPhysics)
			{
				double d0 = entity.getX() - getX();
				double d1 = entity.getZ() - getZ();
				double d2 = Mth.absMax(d0, d1);
				
				if (d2 >= 0.009999999776482582D)
				{
					d2 = Math.sqrt(d2);
					d0 = d0 / d2;
					d1 = d1 / d2;
					double d3 = 1.0D / d2;
					
					if (d3 > 1.0D)
					{
						d3 = 1.0D;
					}
					
					d0 = d0 * d3;
					d1 = d1 * d3;
					d0 = d0 * 0.05000000074505806D;
					d1 = d1 * 0.05000000074505806D;
					d0 *= 3;
					d1 *= 3;
					
					if (!entity.isVehicle())
					{
						entity.push(d0, 0.0D, d1);
					}
				}
			}
		}
	}
	@Override
	public void push(double p_233627_1_, double p_233627_2_, double p_233627_4_)
	{
	}
	public void dropBumper()
	{
		CommonUtils.blockDrop(level(), blockPosition(), ColorUtils.withColorLocked(ColorUtils.withInkColor(new ItemStack(SplatcraftItems.squidBumper.get()), getColor()), true));
	}
	@Override
	protected void dropAllDeathLoot(@NotNull ServerLevel world, @NotNull DamageSource damageSource)
	{
		super.dropAllDeathLoot(world, damageSource);
	}
	@Override
	public @NotNull Iterable<ItemStack> getArmorSlots()
	{
		return Collections.EMPTY_LIST;
	}
	@Override
	public @NotNull ItemStack getItemBySlot(@NotNull EquipmentSlot slotIn)
	{
		return ItemStack.EMPTY;
	}
	@Override
	public void setItemSlot(@NotNull EquipmentSlot slotIn, @NotNull ItemStack stack)
	{
	
	}
	@Override
	public @NotNull HumanoidArm getMainArm()
	{
		return HumanoidArm.RIGHT;
	}
	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag nbt)
	{
		super.readAdditionalSaveData(nbt);
		if (nbt.contains("Color"))
			setColor(InkColor.getFromNbt(nbt.get("Color")));
		else setColor(ColorUtils.getRandomStarterColor());
		
		if (nbt.contains("Inkproof"))
			inkproof = nbt.getBoolean("Inkproof");
		
		if (nbt.contains("InkHealth"))
			setInkHealth(nbt.getFloat("InkHealth"));
		if (nbt.contains("RegenTicks"))
			setRespawnTime(nbt.getInt("RegenTicks"));
		if (nbt.contains("Immortal"))
			setImmortal(nbt.getBoolean("Immortal"));
	}
	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag nbt)
	{
		super.addAdditionalSaveData(nbt);
		nbt.put("Color", getColor().getNbt());
		nbt.putBoolean("Inkproof", inkproof);
		
		nbt.putFloat("InkHealth", getInkHealth());
		nbt.putInt("RegenTicks", getRespawnTime());
		nbt.putBoolean("Immortal", isImmortal());
	}
	@Override
	public InkColor getColor()
	{
		return entityData.get(COLOR);
	}
	@Override
	public void setColor(InkColor color)
	{
		entityData.set(COLOR, color);
	}
	public float getInkHealth()
	{
		return entityData.get(SPLAT_HEALTH);
	}
	public void setInkHealth(float value)
	{
		entityData.set(SPLAT_HEALTH, value);
	}
	public int getRespawnTime()
	{
		return entityData.get(RESPAWN_TIME);
	}
	public void setRespawnTime(int value)
	{
		entityData.set(RESPAWN_TIME, value);
	}
	public boolean isImmortal()
	{
		return entityData.get(IMMORTAL);
	}
	public void setImmortal(boolean immortal)
	{
		entityData.set(IMMORTAL, immortal);
	}
	public float getBumperScale(float partialTicks)
	{
		return getInkHealth() <= 0 && !isImmortal() ? (10 - Math.min(getRespawnTime() - 1 + partialTicks, 10)) / 10f : 1;
	}
	public void ink(float damage, InkColor color)
	{
		level().broadcastEntityEvent(this, (byte) 31);
		if (!isImmortal())
			setRespawnTime(maxRespawnTime);
		hurtCooldown = invulnerableTime;
		
		if (entityData.get(IMMORTAL))
		{
			setInkHealth(maxInkHealth - damage);
		}
		else
		{
			setInkHealth(getInkHealth() - damage);
			if (!level().isClientSide())
			{
				if (!isUnderWater())
				{
					InkOverlayInfo info = Components.INK_OVERLAY.getOrCreate(this);
					
					if (getInkHealth() > 0)
					{
						if (info.getAmount() < maxInkHealth * 1.5)
							info.addAmount(damage);
					}
					else info.setAmount(0);
					
					info.setColor(color);
					SplatcraftPacketHandler.sendToTrackers(new UpdateInkOverlayPacket(this, info), this);
				}
			}
		}
	}
	public void respawn()
	{
		if (getInkHealth() <= 0)
			level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.squidBumperReady, getSoundSource(), 1, 1);
		setInkHealth(maxInkHealth);
		setRespawnTime(0);
		
		Components.INK_OVERLAY.getOrCreate(this).setAmount(0);
	}
}