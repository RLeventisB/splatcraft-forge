package net.splatcraft.util.action.specials;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.action.EntityActionWithTime;

import java.util.Optional;

public abstract class BaseSpecialAction extends EntityActionWithTime
{
	protected final EntitySlot providerEntitySlot;
	protected final int slotIndex;
	public BaseSpecialAction(float time, float duration, int slotIndex, EntitySlot providerEntitySlot)
	{
		super(time, duration);
		this.slotIndex = slotIndex;
		this.providerEntitySlot = providerEntitySlot;
	}
	public BaseSpecialAction(float duration, int slotIndex, EntitySlot providerEntitySlot)
	{
		this(duration, duration, slotIndex, providerEntitySlot);
	}
	public static <T extends BaseSpecialAction> RecordCodecBuilder<T, Integer> getSlotIndexCodec()
	{
		return Codec.INT.fieldOf("slot_index").forGetter(v -> v.slotIndex);
	}
	public static <T extends BaseSpecialAction> RecordCodecBuilder<T, EntitySlot> getProviderEntitySlotCodec()
	{
		return EntitySlot.SERIALIZER_CODEC.fieldOf("provider_slot_index").forGetter(v -> v.providerEntitySlot);
	}
	public boolean isProviderStack(LivingEntity entity, ItemStack stack)
	{
		return providerEntitySlot.isSlotFor(entity, stack);
	}
	public float getProgress()
	{
		return getTime() / getMaxTime();
	}
	@Override
	public void onStart(LivingEntity entity)
	{
		Level world = entity.level();
		if (world.isClientSide)
		{
			boolean sameTeam = ClientUtils.getClientPlayer() != null && ColorUtils.getEntityColor(entity).equals(ColorUtils.getEntityColor(ClientUtils.getClientPlayer()));
			world.playLocalSound(entity, SplatcraftSounds.specialUsage, SoundSource.PLAYERS, sameTeam ? 0.5f : 1f, 1f);
			
			return;
		}
		
		Optional<EntityInfo> optional = EntityInfoCapability.getOptional(entity);
		optional.ifPresent(info ->
		{
			if (entity.level().isClientSide)
				ClientUtils.setSquid(info, false);
			else
				info.setIsSquid(false);
		});
	}
	@Override
	public boolean preventWeaponUse()
	{
		return true;
	}
	@Override
	public int getSlotIndex()
	{
		return slotIndex;
	}
}
