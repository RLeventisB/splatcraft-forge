package net.splatcraft.forge.items.weapons;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.splatcraft.forge.entities.ExtraSaveData;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.handlers.PlayerPosingHandler;
import net.splatcraft.forge.items.weapons.settings.BlasterWeaponSettings;
import net.splatcraft.forge.items.weapons.settings.SlosherWeaponSettings;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.PlayerCooldown;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SlosherItem extends WeaponBaseItem<SlosherWeaponSettings>
{
	public List<SlosherWeaponSettings.SingularSloshShotData> pendingSloshes = new ArrayList<>();
	public Type slosherType = Type.DEFAULT;
	public static RegistryObject<SlosherItem> create(DeferredRegister<Item> register, String settings, String name, Type slosherType)
	{
		return register.register(name, () -> new SlosherItem(settings).setSlosherType(slosherType));
	}
	public static RegistryObject<SlosherItem> create(DeferredRegister<Item> register, RegistryObject<SlosherItem> parent, String name)
	{
		return register.register(name, () -> new SlosherItem(parent.get().settingsId.toString()).setSlosherType(parent.get().slosherType));
	}
	protected SlosherItem(String settings)
	{
		super(settings);
	}
	@Override
	public Class<SlosherWeaponSettings> getSettingsClass()
	{
		return SlosherWeaponSettings.class;
	}
	public SlosherItem setSlosherType(Type type)
	{
		this.slosherType = type;
		return this;
	}
	@Override
	public void weaponUseTick(Level level, LivingEntity entity, ItemStack stack, int timeLeft)
	{
		SlosherWeaponSettings settings = getSettings(stack);
		if (entity instanceof Player player && getUseDuration(stack) - timeLeft < settings.shotData.endlagTicks())
		{
			ItemCooldowns cooldownTracker = player.getCooldowns();
			if (!cooldownTracker.isOnCooldown(this))
			{
				pendingSloshes.addAll(settings.shotData.sloshes());
				PlayerCooldown.setPlayerCooldown(player, new SloshCooldown(stack, player.getInventory().selected, entity.getUsedItemHand(), settings, settings.shotData.endlagTicks()));
				if (!level.isClientSide && settings.shotData.endlagTicks() > 0)
				{
					cooldownTracker.addCooldown(this, settings.shotData.endlagTicks());
				}
			}
		}
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(ItemStack stack)
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
		public record CalculatedSloshData(float time, byte indexInSlosh, int sloshDataIndex)
		{
		}
		public SlosherWeaponSettings sloshData = null;
		public List<CalculatedSloshData> sloshes = new ArrayList<>();
		public boolean didSound;
		public SloshCooldown(ItemStack stack, int slotIndex, InteractionHand hand, SlosherWeaponSettings sloshData, int duration)
		{
			super(stack, duration, slotIndex, hand, true, false, true, false);
			this.sloshData = sloshData;
			for (int i = 0; i < sloshData.shotData.sloshes().size(); i++)
			{
				SlosherWeaponSettings.SingularSloshShotData slosh = sloshData.shotData.sloshes().get(i);
				for (byte j = 0; j < slosh.count(); j++)
				{
					sloshes.add(new CalculatedSloshData(slosh.startupTicks() + j * slosh.delayBetweenProjectiles(), j, i));
				}
			}
		}
		public SloshCooldown(CompoundTag nbt)
		{
			super(ItemStack.of(nbt.getCompound("StoredStack")), nbt.getInt("MaxTime"), nbt.getInt("SlotIndex"), nbt.getBoolean("MainHand") ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true, false, true, false);
			setTime(nbt.getInt("Time"));
			didSound = nbt.getBoolean("DidSound");
			fromNbt(nbt);
		}
		@Override
		public void tick(Player player)
		{
			Level level = player.level;
			
			if (level.isClientSide || sloshData == null)
				return;
			
			float frame = getMaxTime() - getTime();
			float lastFrame = Math.max(0, frame - 1);
			SlosherWeaponSettings.SlosherShotDataRecord shotSetting = sloshData.shotData;
			SlosherItem slosherItem = (SlosherItem) storedStack.getItem();
			
			for (int i = 0; i < sloshes.size(); i++)
			{
				CalculatedSloshData sloshTime = sloshes.get(i);
				if (sloshTime.time <= frame)
				{
					float extraTime = frame - sloshTime.time;
					float partialTick = 1 - extraTime;
					
					if (didSound || reduceInk(player, slosherItem, shotSetting.inkConsumption(), shotSetting.inkRecoveryCooldown(), true))
					{
						SlosherWeaponSettings.SingularSloshShotData projectileSetting = shotSetting.sloshes().get(sloshTime.sloshDataIndex);
						
						InkProjectileEntity proj = new InkProjectileEntity(level, player, storedStack, InkBlockUtils.getInkType(player), projectileSetting.projectile().size(), sloshData);
						proj.shootFromRotation(
							player.getViewXRot(partialTick),
							player.getViewYRot(partialTick) + projectileSetting.offsetAngle(),
							shotSetting.pitchCompensation(),
							projectileSetting.projectile().speed() - projectileSetting.speedSubstract() * sloshTime.indexInSlosh,
							0,
							partialTick);
						List<Object> dataList = new ArrayList<>();
						dataList.add(sloshTime.sloshDataIndex);
						switch (slosherItem.slosherType)
						{
							case EXPLODING:
								Optional<BlasterWeaponSettings.DetonationRecord> detonationData = projectileSetting.projectile().detonationData();
								if (detonationData.isPresent())
								{
									proj.explodes = true;
									proj.setProjectileType(InkProjectileEntity.Types.BLASTER);
									dataList.add(detonationData.get().explosionRadius());
									dataList.add(detonationData.get().maxIndirectDamage());
									dataList.add(detonationData.get().sparkDamagePenalty());
									dataList.add(detonationData.get().explosionPaint());
								}
							case CYCLONE:
								proj.canPierce = true;
						}
						proj.setExtraData(new ExtraSaveData.SloshExtraData(sloshTime.sloshDataIndex));
						proj.setSlosherStats(projectileSetting.projectile());
						level.addFreshEntity(proj);
						
						proj.tick(extraTime);
						
						if (!didSound)
						{
							level.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.slosherShot, SoundSource.PLAYERS, 0.7F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
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
		public CompoundTag writeNBT(CompoundTag nbt)
		{
			nbt.putInt("Time", getTime());
			nbt.putInt("MaxTime", getMaxTime());
			nbt.putInt("SlotIndex", getSlotIndex());
			nbt.putBoolean("DidSound", didSound);
			nbt.putBoolean("MainHand", getHand().equals(InteractionHand.MAIN_HAND));
			
			if (storedStack.getItem() != Items.AIR)
			{
				nbt.put("StoredStack", storedStack.serializeNBT());
			}
			
			nbt.putBoolean("SloshCooldown", true);
			
			return nbt;
		}
	}
}
