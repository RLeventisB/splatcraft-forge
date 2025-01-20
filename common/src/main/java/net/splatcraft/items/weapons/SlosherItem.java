package net.splatcraft.items.weapons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.handlers.DataHandler;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.items.weapons.settings.BlasterWeaponSettings;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.items.weapons.settings.SlosherWeaponSettings;
import net.splatcraft.mixin.accessors.EntityAccessor;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.*;

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
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks)
	{
		if (remainingUseTicks != stack.getMaxUseTime(user)) // sloshers cannot be held to attack normally
			return;
		
		Optional<PlayerCooldown> cooldown = PlayerCooldown.getCooldownIf(user, PlayerCooldown::preventWeaponUse);
		boolean notPreventedByCooldown = cooldown.isEmpty();
		
		// if there is already an action, and the sloshing already shot a bullet, make it so you can slosh automatically after
		if (!notPreventedByCooldown && cooldown.get() instanceof SloshCooldown slosh && slosh.didSound)
		{
			slosh.doAction = true;
		}
		
		if (notPreventedByCooldown && ((!(user instanceof PlayerEntity player) || !CommonUtils.anyWeaponOnCooldown(player))))
		{
			weaponUseTick(world, user, stack, remainingUseTicks);
			user.setSprinting(false);
		}
	}
	@Override
	public void weaponUseTick(World world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
	{
		SlosherWeaponSettings settings = getSettings(stack);
		if (entity instanceof PlayerEntity player)
		{
			ItemCooldownManager cooldownTracker = player.getItemCooldownManager();
			if (cooldownTracker.isCoolingDown(this))
			{
				return;
			}
			PlayerCooldown.setPlayerCooldown(player, new SloshCooldown(player, stack, player.getInventory().selectedSlot, entity.getActiveHand(), settings, settings.shotData.endlagTicks(), settings.shotData.miscEndlagTicks()));
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
		public static final Codec<SloshCooldown> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			ItemStack.CODEC.fieldOf("stored_stack").forGetter(v -> v.storedStack),
			Codec.FLOAT.fieldOf("time").forGetter(PlayerCooldown::getTime),
			Codec.FLOAT.fieldOf("max_time").forGetter(PlayerCooldown::getMaxTime),
			Codec.INT.fieldOf("slot_index").forGetter(PlayerCooldown::getSlotIndex),
			Codec.BOOL.fieldOf("is_main_hand").forGetter(v -> v.getHand() == Hand.MAIN_HAND),
			Identifier.CODEC.fieldOf("slosh_setting_id").forGetter(v -> DataHandler.WeaponStatsListener.SETTINGS.inverse().get(v.sloshData)),
			Codec.BOOL.fieldOf("did_sound").forGetter(v -> v.didSound),
			Codec.BOOL.fieldOf("do_action").forGetter(v -> v.doAction),
			Codec.INT.fieldOf("endlag").forGetter(v -> v.endlag),
			Codec.FLOAT.fieldOf("pitch").forGetter(v -> v.pitch), // i am in the middle of codec-fying player cooldowns i WILL NOT make a codec for AttackId idc its such a niche bug too if it isn't serialized
			Codec.FLOAT.fieldOf("x_delta").forGetter(v -> v.xDelta),
			Codec.FLOAT.fieldOf("yaw").forGetter(v -> v.yaw),
			Codec.FLOAT.fieldOf("y_delta").forGetter(v -> v.yDelta),
			Codec.FLOAT.fieldOf("x_rot_old").forGetter(v -> v.xRotOld),
			Codec.FLOAT.fieldOf("y_rot_old").forGetter(v -> v.yRotOld)
		).apply(inst, SloshCooldown::new));
		private final int endlag;
		public SlosherWeaponSettings sloshData;
		public List<CalculatedSloshData> sloshes = new ArrayList<>();
		public boolean didSound, doAction = false;
		public AttackId attackId;
		public float pitch, xDelta, yaw, yDelta, xRotOld, yRotOld;
		public SloshCooldown(PlayerEntity player, ItemStack stack, int slotIndex, Hand hand, SlosherWeaponSettings sloshData, float duration, int endlag)
		{
			super(stack, duration, slotIndex, hand, true, false, true, false);
			pitch = xRotOld = player.getPitch();
			yaw = yRotOld = player.getYaw();
			this.sloshData = sloshData;
			this.endlag = endlag;
			
			calculateSloshes();
		}
		public SloshCooldown(ItemStack storedStack, float time, float maxTime, int slotIndex, boolean isMainHand, Identifier sloshDataId, boolean didSound, boolean doAction, int endlag, float pitch, Float xDelta, float yaw, Float yDelta, Float xRotOld, Float yRotOld)
		{
			super(storedStack, time, maxTime, slotIndex, isMainHand ? Hand.MAIN_HAND : Hand.OFF_HAND, true, false, true, false);
			sloshData = (SlosherWeaponSettings) DataHandler.WeaponStatsListener.SETTINGS.get(sloshDataId);
			calculateSloshes();
			
			this.didSound = didSound;
			this.doAction = doAction;
			this.endlag = endlag;
			this.pitch = pitch;
			this.xDelta = xDelta;
			this.yaw = yaw;
			this.yDelta = yDelta;
			this.xRotOld = xRotOld;
			this.yRotOld = yRotOld;
		}
		private void calculateSloshes()
		{
			sloshes.clear();
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
		@Override
		public void tick(LivingEntity entity)
		{
			World world = entity.getWorld();
			
			if (sloshData == null)
				return;
			
			float frame = getMaxTime() - getTime();
			SlosherWeaponSettings.SlosherShotDataRecord shotSetting = sloshData.shotData;
			SlosherItem slosherItem = (SlosherItem) storedStack.getItem();
			
			if (shotSetting.allowFlicking())
			{
				xDelta = xDelta * 0.7f + (MathHelper.subtractAngles(pitch, entity.getPitch())) * 0.12f;
				yDelta = yDelta * 0.7f + (MathHelper.subtractAngles(yaw, entity.getYaw())) * 0.12f;
				xRotOld = pitch;
				yRotOld = yaw;
				
				pitch += xDelta * (didSound ? 1 : 0.4f);
				yaw += yDelta * (didSound ? 1 : 0.4f);
			}
			else
			{
				xRotOld = pitch;
				yRotOld = yaw;
				pitch = entity.getPitch();
				yaw = entity.getYaw();
			}
			
			for (int i = 0; i < sloshes.size(); i++)
			{
				CalculatedSloshData calculatedSloshData = sloshes.get(i);
				if (calculatedSloshData.time <= frame)
				{
					float extraTime = frame - calculatedSloshData.time;
					float partialTick = 1 - extraTime;
					
					if ((didSound || reduceInk(entity, slosherItem, shotSetting.inkConsumption(), shotSetting.inkRecoveryCooldown(), true)))
					{
						SlosherWeaponSettings.SingularSloshShotData projectileSetting = shotSetting.sloshes().get(calculatedSloshData.sloshDataIndex);
						if (!world.isClient)
						{
							shootSlosh(entity, calculatedSloshData, world, partialTick, projectileSetting, shotSetting, slosherItem, extraTime);
						}
						
						if (!didSound)
						{
							if (entity.equals(ClientUtils.getClientPlayer()))
							{
								SplatcraftKeyHandler.autoSquidDelay = endlag;
							}
							world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.slosherShot, SoundCategory.PLAYERS, 0.7F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
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
		private void shootSlosh(LivingEntity entity, CalculatedSloshData calculatedSloshData, World world, float partialTick, SlosherWeaponSettings.SingularSloshShotData projectileSetting, SlosherWeaponSettings.SlosherShotDataRecord shotSetting, SlosherItem slosherItem, float extraTime)
		{
			CommonRecords.ProjectileDataRecord projectileData = sloshData.getProjectileDataAtIndex(calculatedSloshData.sloshDataIndex);
			
			InkProjectileEntity proj = new InkProjectileEntity(world, entity, storedStack, InkBlockUtils.getInkType(entity), projectileData.size(), sloshData);
			proj.setSlosherStats(projectileData);
			
			float xRotation = MathHelper.lerp(partialTick, yRotOld, yaw);
			proj.setVelocity(
				entity,
				MathHelper.lerp(partialTick, xRotOld, pitch),
				xRotation + projectileSetting.offsetAngle() - 3,
				shotSetting.pitchCompensation(),
				projectileData.speed() - projectileSetting.speedSubstract() * calculatedSloshData.indexInSlosh,
				0);
			proj.setAttackId(attackId);
			proj.refreshPositionAfterTeleport(proj.getPos().add(EntityAccessor.invokeMovementInputToVelocity(new Vec3d(-0.4, -1, 0), 1, xRotation)));
			
			switch (slosherItem.slosherType)
			{
				case EXPLODING:
					Optional<BlasterWeaponSettings.DetonationRecord> detonationData = projectileSetting.detonationData();
					if (detonationData.isPresent())
					{
						proj.explodes = true;
						BlasterWeaponSettings.DetonationRecord detonationRecord = detonationData.get();
						proj.addExtraData(new ExtraSaveData.ExplosionExtraData(detonationRecord));
					}
				case CYCLONE:
					proj.canPierce = true;
			}
			proj.addExtraData(new ExtraSaveData.SloshExtraData(calculatedSloshData.sloshDataIndex, proj.getY()));
			world.spawnEntity(proj);
			
			proj.tick(extraTime);
		}
		@Override
		public boolean canEnd(LivingEntity entity)
		{
			if (doAction)
			{
				setTime(getTime() + getMaxTime());
				calculateSloshes();
				
				xRotOld = pitch;
				yRotOld = yaw;
				pitch = entity.getPitch();
				yaw = entity.getYaw();
				
				doAction = false;
				didSound = false;
				return false;
			}
			return true;
		}
		@Override
		public boolean canMove()
		{
			return true;
		}
		@Override
		public boolean preventWeaponUse()
		{
			return true;
		}
		public record CalculatedSloshData(float time, byte indexInSlosh, int sloshDataIndex)
		{
			public static final Codec<CalculatedSloshData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.FLOAT.fieldOf("time").forGetter(CalculatedSloshData::time),
				Codec.BYTE.fieldOf("index_in_slosh").forGetter(CalculatedSloshData::indexInSlosh),
				Codec.INT.fieldOf("slosh_data_index").forGetter(CalculatedSloshData::sloshDataIndex)
			).apply(inst, CalculatedSloshData::new));
		}
	}
}