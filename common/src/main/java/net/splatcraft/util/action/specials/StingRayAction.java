package net.splatcraft.util.action.specials;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
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
		inst ->
			specialCodecStart(inst).and(
				inst.group(
					StingRayDataRecord.CODEC.fieldOf("special_data").forGetter(v -> v.specialData),
					Codec.FLOAT.fieldOf("mobility").forGetter(v -> v.mobility)
				)
			).apply(inst, StingRayAction::new)
	);
	private final StingRayDataRecord specialData;
	private final float mobility;
	protected int usageTick;
	public StingRayAction(float time, float maxTime, EntitySlot weaponSlot, EntitySlot providerSlot, StingRayDataRecord specialData, float mobility)
	{
		super(time, maxTime, weaponSlot, providerSlot);
		this.specialData = specialData;
		this.mobility = mobility;
	}
	public StingRayAction(SpecialWeaponSettings<StingRayDataRecord> settings, EntitySlot weaponSlot, EntitySlot providerSlot)
	{
		super(settings.dataRecord.specialDuration(), weaponSlot, providerSlot);
		specialData = settings.specialDataRecord;
		mobility = settings.dataRecord.mobility();
	}
	@Override
	public void tick(LivingEntity entity)
	{
		if (entity.isUsingItem() && !EntityInfoCapability.isSquid(entity))
		{
			Level world = entity.level();
			if (usageTick == 1 && world instanceof ServerLevel serverWorld)
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
					specialData.damageShockwave(),
					specialData.paintingRadius(),
					specialData.paintSearchRadius()
				);
				serverWorld.addFreshEntity(beam);
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
		Level world = entity.level();
		if (world.isClientSide && entity.equals(ClientUtils.getClientPlayer()))
		{
			SplatcraftKeyHandler.squidAndSubDelay = 5;
		}
		return true;
	}
}
