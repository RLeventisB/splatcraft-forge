package net.splatcraft.util.action.specials;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.Products;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.handlers.SquidFormHandler;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.EntityActionWithTime;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

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
	public float getSpecialCharge(ItemStack providerStack, float currentCharge)
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
	public void tick(LivingEntity entity)
	{
		Optional<ItemStack> providerStackOptional = providerSlot.tryGetItemFrom(entity);
		providerStackOptional.ifPresent(providerStack ->
		{
			SplatcraftComponents.SpecialProviderData data = providerStack.get(SplatcraftComponents.SPECIAL_PROVIDER_DATA);
			providerStack.set(SplatcraftComponents.SPECIAL_PROVIDER_DATA, data.withStoredCharge(getSpecialCharge(providerStack, data.storedCharge())));
		});
		super.tick(entity);
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

	public void transformHeldWeaponRender(float[] dataArray, AtomicBoolean doRender, InteractionHand hand, float tickDelta, float time, PoseStack matrices)
	{
		if (time < 3)
			dataArray[0] = -0.5f * time / 3f;
		else
			doRender.set(false);
	}
}
