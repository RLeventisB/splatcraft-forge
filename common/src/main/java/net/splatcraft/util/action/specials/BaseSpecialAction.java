package net.splatcraft.util.action.specials;

import com.mojang.datafixers.Products;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.handlers.SquidFormHandler;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.EntityActionWithTime;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class BaseSpecialAction extends EntityActionWithTime
{
	protected final EntitySlot weaponSlot, providerSlot;
	public BaseSpecialAction(float time, float duration, EntitySlot weaponSlot, EntitySlot providerEntitySlot)
	{
		super(time, duration);
		this.weaponSlot = weaponSlot;
		this.providerSlot = providerEntitySlot;
	}
	public BaseSpecialAction(float duration, EntitySlot weaponSlot, EntitySlot providerSlot)
	{
		this(duration, duration, weaponSlot, providerSlot);
	}
	public static <T extends BaseSpecialAction> Products.P4<RecordCodecBuilder.Mu<T>, Float, Float, EntitySlot, EntitySlot> specialCodecStart(RecordCodecBuilder.Instance<T> instance)
	{
		return EntityActionWithTime.codecStart(instance)
			.and(instance.group(
				getWeaponSlotCodec(),
				getProviderSlotCodec()
			));
	}
	private static <T extends BaseSpecialAction> @NotNull RecordCodecBuilder<T, EntitySlot> getWeaponSlotCodec()
	{
		return EntitySlot.SERIALIZER_CODEC.fieldOf("weapon_slot").forGetter(EntityAction::getItemSlot);
	}
	private static <T extends BaseSpecialAction> @NotNull RecordCodecBuilder<T, EntitySlot> getProviderSlotCodec()
	{
		return EntitySlot.SERIALIZER_CODEC.fieldOf("provider_slot").forGetter(v -> v.providerSlot);
	}
	public boolean isProviderStack(LivingEntity entity, ItemStack stack)
	{
		return providerSlot.isItemForSlot(entity, stack);
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
				ClientUtils.setSquid(entity, info, false);
			else
				SquidFormHandler.setSquid(entity, info, false);
		});
	}
	@Override
	public boolean preventWeaponUse()
	{
		return true;
	}
	@Override
	public EntitySlot getItemSlot()
	{
		return weaponSlot;
	}
}
