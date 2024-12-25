package net.splatcraft.items.weapons;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.items.weapons.settings.BlasterWeaponSettings;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.items.weapons.settings.SlosherWeaponSettings;
import net.splatcraft.mixin.accessors.EntityAccessor;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.AttackId;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.PlayerCooldown;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SlosherItem extends WeaponBaseItem<SlosherWeaponSettings>
{
	public Type slosherType = Type.DEFAULT;
	protected SlosherItem(String settings)
	{
		super(settings);
	}
	public static RegistrySupplier<SlosherItem> create(DeferredRegister<Item> register, String settings, String name, Type slosherType)
	{
		return register.register(name, () -> new SlosherItem(settings).setSlosherType(slosherType));
	}
	public static RegistrySupplier<SlosherItem> create(DeferredRegister<Item> register, RegistrySupplier<SlosherItem> parent, String name)
	{
		return register.register(name, () -> new SlosherItem(parent.get().settingsId.toString()).setSlosherType(parent.get().slosherType));
	}
	@Override
	public Class<SlosherWeaponSettings> getSettingsClass()
	{
		return SlosherWeaponSettings.class;
	}
	public SlosherItem setSlosherType(Type type)
	{
		slosherType = type;
		return this;
	}
	@Override
	public void weaponUseTick(World world, LivingEntity entity, ItemStack stack, int timeLeft)
	{
		if (world.isClient)
			return;
		
		SlosherWeaponSettings settings = getSettings(stack);
		if (entity instanceof PlayerEntity player)
		{
			ItemCooldownManager cooldownTracker = player.getItemCooldownManager();
			if (cooldownTracker.isCoolingDown(this))
			{
				return;
			}
			PlayerCooldown.setPlayerCooldown(player, new SloshCooldown(player, stack, player.getInventory().selectedSlot, entity.getActiveHand(), settings, settings.shotData.endlagTicks()));
			if (settings.shotData.endlagTicks() > 0)
			{
				cooldownTracker.set(this, settings.shotData.endlagTicks());
			}
		}
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(PlayerEntity player, ItemStack stack)
	{
		return PlayerPosingHandler.WeaponPose.BUCKET_SWING;
	}
	public enum Type
	{
		DEFAULT,
		EXPLODING,
		CYCLONE,
		BUBBLES
	}
	public static class SloshCooldown extends PlayerCooldown
	{
		public SlosherWeaponSettings sloshData = null;
		public List<CalculatedSloshData> sloshes = new ArrayList<>();
		public boolean didSound;
		public AttackId attackId;
		public float pitch, xDelta, yaw, yDelta, xRotOld, prevYawld;
		public SloshCooldown(PlayerEntity player, ItemStack stack, int slotIndex, Hand hand, SlosherWeaponSettings sloshData, int duration)
		{
			super(stack, duration, slotIndex, hand, true, false, true, false);
			pitch = xRotOld = player.getPitch();
			yaw = prevYawld = player.getYaw();
			this.sloshData = sloshData;
			for (int i = 0; i < sloshData.shotData.sloshes().size(); i++)
			{
				SlosherWeaponSettings.SingularSloshShotData slosh = sloshData.shotData.sloshes().get(i);
				for (byte j = 0; j < slosh.count(); j++)
				{
					sloshes.add(new CalculatedSloshData(slosh.startupTicks() + j * slosh.delayBetweenProjectiles(), j, i));
				}
			}
			attackId = AttackId.registerAttack().countProjectile(sloshes.size());
		}
		public SloshCooldown(RegistryWrapper.WrapperLookup wrapperLookup, NbtCompound nbt)
		{
			super(ItemStack.fromNbtOrEmpty(wrapperLookup, nbt.getCompound("StoredStack")), nbt.getFloat("MaxTime"), nbt.getInt("SlotIndex"), nbt.getBoolean("MainHand") ? Hand.MAIN_HAND : Hand.OFF_HAND, true, false, true, false);
			setTime(nbt.getFloat("Time"));
			didSound = nbt.getBoolean("DidSound");
			fromNbt(wrapperLookup, nbt);
		}
		@Override
		public void tick(LivingEntity player)
		{
			World world = player.getWorld();
			
			if (world.isClient || sloshData == null)
				return;
			
			float frame = getMaxTime() - getTime();
			SlosherWeaponSettings.SlosherShotDataRecord shotSetting = sloshData.shotData;
			SlosherItem slosherItem = (SlosherItem) storedStack.getItem();
			
			if (shotSetting.allowFlicking())
			{
				xDelta = xDelta * 0.7f + (MathHelper.subtractAngles(pitch, player.getPitch())) * 0.12f;
				yDelta = yDelta * 0.7f + (MathHelper.subtractAngles(yaw, player.getYaw())) * 0.12f;
				xRotOld = pitch;
				prevYawld = yaw;
				
				pitch += xDelta * (didSound ? 1 : 0.4f);
				yaw += yDelta * (didSound ? 1 : 0.4f);
			}
			else
			{
				xRotOld = pitch;
				prevYawld = yaw;
				pitch = player.getPitch();
				yaw = player.getYaw();
			}
			
			for (int i = 0; i < sloshes.size(); i++)
			{
				CalculatedSloshData calculatedSloshData = sloshes.get(i);
				if (calculatedSloshData.time <= frame)
				{
					float extraTime = frame - calculatedSloshData.time;
					float partialTick = 1 - extraTime;
					
					if (didSound || reduceInk(player, slosherItem, shotSetting.inkConsumption(), shotSetting.inkRecoveryCooldown(), true))
					{
						SlosherWeaponSettings.SingularSloshShotData projectileSetting = shotSetting.sloshes().get(calculatedSloshData.sloshDataIndex);
						CommonRecords.ProjectileDataRecord projectileData = sloshData.getProjectileDataAtIndex(calculatedSloshData.sloshDataIndex);
						
						InkProjectileEntity proj = new InkProjectileEntity(world, player, storedStack, InkBlockUtils.getInkType(player), projectileData.size(), sloshData);
						proj.setSlosherStats(projectileData);
						
						float xRotation = MathHelper.lerp(partialTick, prevYawld, yaw);
						proj.setVelocity(
							null,
							MathHelper.lerp(partialTick, xRotOld, pitch),
							xRotation + projectileSetting.offsetAngle() - 3,
							shotSetting.pitchCompensation(),
							projectileData.speed() - projectileSetting.speedSubstract() * calculatedSloshData.indexInSlosh,
							0,
							partialTick);
						proj.setAttackId(attackId);
						
						proj.refreshPositionAfterTeleport(proj.getPos().add(EntityAccessor.invokeMovementInputToVelocity(new Vec3d(-0.4, -1, 0), 1, xRotation)));
						
						switch (slosherItem.slosherType)
						{
							case EXPLODING:
								Optional<BlasterWeaponSettings.DetonationRecord> detonationData = projectileSetting.detonationData();
								if (detonationData.isPresent())
								{
									proj.explodes = true;
//                                    proj.setProjectileType(InkProjectileEntity.Types.BLASTER);
									BlasterWeaponSettings.DetonationRecord detonationRecord = detonationData.get();
									proj.addExtraData(new ExtraSaveData.ExplosionExtraData(detonationRecord));
								}
							case CYCLONE:
								proj.canPierce = true;
						}
						proj.addExtraData(new ExtraSaveData.SloshExtraData(calculatedSloshData.sloshDataIndex, proj.getY()));
						world.spawnEntity(proj);
						
						proj.tick(extraTime);
						
						if (!didSound)
						{
							world.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.slosherShot, SoundCategory.PLAYERS, 0.7F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
							didSound = true;
						}
					}
					else
					{
						setTime(0);
						break;
					}
					
					sloshes.remove(i);
					i--;
				}
			}
		}
		@Override
		public NbtCompound writeNBT(NbtCompound nbt, RegistryWrapper.WrapperLookup wrapperLookup)
		{
			nbt.putFloat("Time", getTime());
			nbt.putFloat("MaxTime", getMaxTime());
			nbt.putInt("SlotIndex", getSlotIndex());
			nbt.putBoolean("DidSound", didSound);
			nbt.putBoolean("MainHand", getHand().equals(Hand.MAIN_HAND));
			if (storedStack.getItem() != Items.AIR)
			{
				nbt.put("StoredStack", storedStack.encode(wrapperLookup));
			}
			
			nbt.putBoolean("SloshCooldown", true);
			
			return nbt;
		}
		public record CalculatedSloshData(float time, byte indexInSlosh, int sloshDataIndex)
		{
		}
	}
}