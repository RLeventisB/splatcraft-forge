package net.splatcraft.items.weapons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.handlers.DataHandler;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.handlers.SpecialHandler;
import net.splatcraft.items.weapons.settings.BlasterWeaponSettings;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.items.weapons.settings.SlosherWeaponSettings;
import net.splatcraft.mixin.accessors.EntityAccessor;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.structs.AttackId;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.EntityActionWithTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
		return register.register(name, () -> new SlosherItem(parent.value().components().get(SplatcraftComponents.WEAPON_SETTING_ID).toString()).setSlosherType(parent.get().slosherType));
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
	public void weaponUseTick(Level world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
	{
		if (remainingUseTicks != stack.getUseDuration(entity))
			return;
		
		SlosherWeaponSettings settings = getSettings(stack);
		Optional<SloshAction> action = EntityAction.getSpecificEntityActionOptional(entity, SloshAction.class);
		if (action.isPresent())
		{
			if (action.get().didSound)
			{
				action.get().doAction = true;
				action.get().loadSetting(settings);
			}
			return;
		}
		EntityAction.setEntityAction(entity, new SloshAction(entity, stack, EntitySlot.searchAndCreateWithStack(entity, stack), settings));
	}
	@Override
	public Optional<SpecialHandler.ResetAction> getResetShootingAction(ItemStack stack, LivingEntity entity)
	{
		if (EntityAction.hasSpecificActionAnd(entity, roll -> !roll.didSound, SloshAction.class))
			return Optional.of(SpecialHandler.ResetAction.RESET_FAILED);
		
		return Optional.of(() ->
			EntityAction.setEntityAction(entity, null));
	}
	@Override
	public boolean preventsChanging(ItemStack stack, LivingEntity entity)
	{
		return EntityAction.hasSpecificEntityAction(entity, SloshAction.class);
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(Player player, ItemStack stack)
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
	public static class SloshAction extends EntityActionWithTime
	{
		public static final Codec<SloshAction> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			ItemStack.CODEC.fieldOf("stored_stack").forGetter(SloshAction::getStoredStack),
			Codec.FLOAT.fieldOf("time").forGetter(SloshAction::getTime),
			Codec.FLOAT.fieldOf("max_time").forGetter(SloshAction::getMaxTime),
			EntitySlot.SERIALIZER_CODEC.fieldOf("item_slot").forGetter(SloshAction::getItemSlot),
			ResourceLocation.CODEC.fieldOf("slosh_setting_id").forGetter(v -> DataHandler.WeaponStatsListener.SETTINGS.inverse().get(v.sloshData)),
			Codec.BOOL.fieldOf("did_sound").forGetter(v -> v.didSound),
			Codec.BOOL.fieldOf("do_action").forGetter(v -> v.doAction),
			Codec.INT.fieldOf("endlag").forGetter(v -> v.endlag),
			Codec.FLOAT.fieldOf("pitch").forGetter(v -> v.pitch), // i am in the middle of codec-fying player cooldowns i WILL NOT make a codec for AttackId idc its such a niche bug too if it isn't serialized
			Codec.FLOAT.fieldOf("x_delta").forGetter(v -> v.xDelta),
			Codec.FLOAT.fieldOf("yaw").forGetter(v -> v.yaw),
			Codec.FLOAT.fieldOf("y_delta").forGetter(v -> v.yDelta),
			Codec.FLOAT.fieldOf("x_rot_old").forGetter(v -> v.xRotOld),
			Codec.FLOAT.fieldOf("y_rot_old").forGetter(v -> v.yRotOld)
		).apply(inst, SloshAction::new));
		final ItemStack storedStack;
		final EntitySlot itemSlot;
		private int endlag;
		public SlosherWeaponSettings sloshData;
		public List<CalculatedSloshData> sloshes = new ArrayList<>();
		public boolean didSound, doAction = false;
		public AttackId attackId;
		public float pitch, xDelta, yaw, yDelta, xRotOld, yRotOld;
		public SloshAction(LivingEntity entity, ItemStack stack, EntitySlot itemSlot, SlosherWeaponSettings settings)
		{
			super(settings.shotData.endlagTicks());
			storedStack = stack;
			this.itemSlot = itemSlot;
			pitch = xRotOld = entity.getXRot();
			yaw = yRotOld = entity.getYRot();
			this.sloshData = settings;
			this.endlag = settings.shotData.miscEndlagTicks();
			
			calculateSloshes();
		}
		public SloshAction(ItemStack storedStack, float time, float maxTime, EntitySlot itemSlot, ResourceLocation sloshDataId, boolean didSound, boolean doAction, int endlag, float pitch, Float xDelta, float yaw, Float yDelta, Float xRotOld, Float yRotOld)
		{
			super(time, maxTime);
			sloshData = (SlosherWeaponSettings) DataHandler.WeaponStatsListener.SETTINGS.get(sloshDataId);
			calculateSloshes();
			
			this.storedStack = storedStack;
			this.itemSlot = itemSlot;
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
			float baseSpeed = sloshData.shotData.baseSpeed();
			for (int i = 0; i < sloshData.shotData.sloshes().size(); i++)
			{
				SlosherWeaponSettings.SingularSloshShotData slosh = sloshData.shotData.sloshes().get(i);
				float effectiveSpeed = slosh.modifiedSpeed().orElse(baseSpeed);
				for (byte j = 0; j < slosh.count(); j++)
				{
					sloshes.add(new CalculatedSloshData(slosh.startupTicks() + j * slosh.delayBetweenProjectiles(), j, i, effectiveSpeed - j * slosh.speedSubstract()));
				}
			}
			attackId = AttackId.registerAttack().countProjectile(sloshes.size());
		}
		@Override
		public void tick(LivingEntity entity)
		{
			Level world = entity.level();
			
			if (sloshData == null)
				return;
			
			float frame = getMaxTime() - getTime();
			SlosherWeaponSettings.SlosherShotDataRecord shotSetting = sloshData.shotData;
			SlosherItem slosherItem = (SlosherItem) storedStack.getItem();
			
			if (shotSetting.allowFlicking())
			{
				xDelta = xDelta * 0.7f + (Mth.degreesDifference(pitch, entity.getXRot())) * 0.12f;
				yDelta = yDelta * 0.7f + (Mth.degreesDifference(yaw, entity.getYRot())) * 0.12f;
				xRotOld = pitch;
				yRotOld = yaw;
				
				pitch += xDelta * (didSound ? 1 : 0.4f);
				yaw += yDelta * (didSound ? 1 : 0.4f);
			}
			else
			{
				xRotOld = pitch;
				yRotOld = yaw;
				pitch = entity.getXRot();
				yaw = entity.getYRot();
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
						if (!world.isClientSide)
						{
							shootSlosh(entity, calculatedSloshData, world, partialTick, projectileSetting, shotSetting, slosherItem, extraTime);
						}
						
						if (!didSound)
						{
							CommonUtils.setSquidDelay(entity, endlag);
							
							world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.slosherShot, SoundSource.PLAYERS, 0.7F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
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
		private void shootSlosh(LivingEntity entity, CalculatedSloshData calculatedSloshData, Level world, float partialTick, SlosherWeaponSettings.SingularSloshShotData projectileSetting, SlosherWeaponSettings.SlosherShotDataRecord shotSetting, SlosherItem slosherItem, float extraTime)
		{
			CommonRecords.ProjectileDataRecord projectileData = sloshData.getProjectileDataAtIndex(calculatedSloshData.sloshDataIndex);
			float speed = calculatedSloshData.sloshSpeed();
			
			InkProjectileEntity proj = new InkProjectileEntity(world, entity, storedStack, InkBlockUtils.getInkType(entity), projectileData.size(), sloshData);
			proj.setSlosherStats(projectileData);
			
			float xRotation = Mth.rotLerp(partialTick, yRotOld, yaw);
			proj.shootFromRotation(
				entity,
				Mth.rotLerp(partialTick, xRotOld, pitch),
				xRotation + projectileSetting.offsetAngle() - 3,
				shotSetting.pitchCompensation(),
				speed,
				0);
			proj.setAttackId(attackId);
			proj.moveTo(proj.position().add(EntityAccessor.invokeGetInputVector(new Vec3(-0.4, -1, 0), 1, xRotation)));
			
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
			world.addFreshEntity(proj);
			
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
				pitch = entity.getXRot();
				yaw = entity.getYRot();
				
				doAction = false;
				didSound = false;
				return false;
			}
			return true;
		}
		@Override
		public boolean preventWeaponUse()
		{
			return !didSound;
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
		public void loadSetting(SlosherWeaponSettings settings)
		{
			if (Objects.equals(sloshData.name, settings.name))
				return;
			
			maxTime = settings.shotData.endlagTicks();
			endlag = settings.shotData.miscEndlagTicks();
			sloshData = settings;
			
			calculateSloshes();
		}
		public record CalculatedSloshData(float time, byte subIndex, int sloshDataIndex, float sloshSpeed)
		{
			public static final Codec<CalculatedSloshData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.FLOAT.fieldOf("time").forGetter(CalculatedSloshData::time),
				Codec.BYTE.fieldOf("index_in_slosh").forGetter(CalculatedSloshData::subIndex),
				Codec.INT.fieldOf("slosh_data_index").forGetter(CalculatedSloshData::sloshDataIndex),
				Codec.FLOAT.fieldOf("time").forGetter(CalculatedSloshData::sloshSpeed)
			).apply(inst, CalculatedSloshData::new));
		}
	}
}