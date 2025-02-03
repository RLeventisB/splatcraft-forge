package net.splatcraft.util.action.specials;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.entities.StingRayBeamEntity;
import net.splatcraft.items.weapons.settings.SpecialWeaponSettings;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;

import static net.splatcraft.items.weapons.settings.SpecialWeaponRecords.StingRayDataRecord;

public class StingRayAction extends BaseSpecialAction
{
	public static final Codec<StingRayAction> CODEC = RecordCodecBuilder.create(
		inst -> inst.group(
			StingRayDataRecord.CODEC.fieldOf("special_data").forGetter(v -> v.specialData),
			getTimeCodec(),
			getMaxTimeCodec(),
			getSlotIndexCodec(),
			getProviderEntitySlotCodec(),
			Codec.FLOAT.fieldOf("mobility").forGetter(v -> v.mobility)
		).apply(inst, StingRayAction::new)
	);
	private final StingRayDataRecord specialData;
	private final float mobility;
	protected int usageTick;
	public StingRayAction(StingRayDataRecord specialData, float time, float maxTime, int slotIndex, EntitySlot providerEntitySlot, float mobility)
	{
		super(time, maxTime, slotIndex, providerEntitySlot);
		this.specialData = specialData;
		this.mobility = mobility;
	}
	public StingRayAction(SpecialWeaponSettings<StingRayDataRecord> settings, int slotIndex, EntitySlot providerSlotIndex)
	{
		super(settings.dataRecord.specialDuration(), slotIndex, providerSlotIndex);
		specialData = settings.specialDataRecord;
		mobility = settings.dataRecord.mobility();
	}
	@Override
	public void tick(LivingEntity entity)
	{
		if (entity.isUsingItem() && !EntityInfoCapability.isSquid(entity))
		{
			World world = entity.getWorld();
			if (usageTick == 1 && world instanceof ServerWorld serverWorld)
			{
				StingRayBeamEntity beam = new StingRayBeamEntity(world,
					entity,
					ColorUtils.getEntityColor(entity),
					specialData.startupTicks(),
					specialData.shockwaveDelay(),
					specialData.turningValue(),
					specialData.turningValueWithShockwave(),
					specialData.radiusCenter(),
					specialData.radiusShockwave(),
					specialData.damageCenter(),
					specialData.damageShockwave()
				);
				serverWorld.spawnEntity(beam);
			}
			usageTick++;
		}
		else
		{
			usageTick = 0;
		}
	}
	@Override
	public boolean canEnd(LivingEntity entity)
	{
		World world = entity.getWorld();
		if (world.isClient && entity.equals(ClientUtils.getClientPlayer()))
		{
			SplatcraftKeyHandler.autoSquidDelay = 5;
		}
		return true;
	}
}
