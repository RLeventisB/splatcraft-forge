package net.splatcraft.util.action.specials;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.entities.InkStormDeployerEntity;
import net.splatcraft.items.weapons.settings.SpecialWeaponRecords.InkStormDataRecord;
import net.splatcraft.items.weapons.settings.SpecialWeaponSettings;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.action.ActionEndResult;
import net.splatcraft.util.structs.trajectory.TrajectoryProcessor;
import org.joml.Vector3f;

import java.util.Optional;

public class InkStormAction extends BaseSpecialAction implements ActionWithThrowable
{
	public static final Codec<InkStormAction> CODEC = RecordCodecBuilder.create(
		inst ->
			CodecUtils.MissingProducts.and(specialCodecStart(inst),
				inst.group(
					InkStormDataRecord.CODEC.fieldOf("special_data").forGetter(v -> v.specialData),
					Codec.FLOAT.fieldOf("mobility").forGetter(v -> v.mobility),
					ResourceLocation.CODEC.fieldOf("special_data_id").forGetter(v -> v.specialDataId),
					Codec.BOOL.fieldOf("thrown").forGetter(v -> v.thrown),
					Codec.INT.fieldOf("usage_tick").forGetter(v -> v.usageTick),
					Codec.BYTE.fieldOf("time_since_throw").forGetter(v -> v.timeSinceThrow)
				)
			).apply(inst, InkStormAction::new)
	);
	private final InkStormDataRecord specialData;
	private final float mobility;
	private final ResourceLocation specialDataId;
	private boolean thrown;
	private byte timeSinceThrow;
	protected int usageTick;
	public InkStormAction(float time, float maxTime, EntitySlot weaponSlot, EntitySlot providerSlot, InkStormDataRecord specialData, float mobility, ResourceLocation specialDataId, boolean thrown, int usageTick, byte timeSinceThrow)
	{
		super(time, maxTime, weaponSlot, providerSlot);
		this.specialData = specialData;
		this.mobility = mobility;
		this.specialDataId = specialDataId;
		this.thrown = thrown;
		this.usageTick = usageTick;
		this.timeSinceThrow = timeSinceThrow;
	}
	public InkStormAction(SpecialWeaponSettings<InkStormDataRecord> settings, EntitySlot weaponSlot, EntitySlot providerSlot)
	{
		super(10, weaponSlot, providerSlot);
		specialDataId = settings.name;
		specialData = settings.specialDataRecord;
		mobility = settings.dataRecord.mobility();
	}
	@Override
	public ActionEndResult tick(LivingEntity entity)
	{
		if (entity.isUsingItem() && !CommonUtils.isSquid(entity))
		{
			usageTick++;
		}
		else
		{
			if (!thrown && usageTick > 0)
			{
				Level level = entity.level();
				if (!level.isClientSide())
				{
					InkStormDeployerEntity deployer = new InkStormDeployerEntity(level,
						entity,
						ColorUtils.getEntityColor(entity),
						specialDataId,
						specialData.riseTime(),
						specialData.cloudRiseHeight() / specialData.riseTime(),
						providerSlot
					);
					deployer.setPos(entity.getEyePosition());
					deployer.setDeltaMovement(entity, entity.getXRot(), entity.getYRot(), 0, specialData.throwVelocity(), 0, 0.4);
					level.addFreshEntity(deployer);
				}
				thrown = true;
				Optional<ItemStack> providerStack = providerSlot.tryGetItemFrom(entity);
				providerStack.ifPresent(stack ->
				{
					stack.update(SplatcraftComponents.SPECIAL_PROVIDER_DATA, SplatcraftComponents.SpecialProviderData.DEFAULT,
						v -> v.withDelay(-1, 1).withStoredCharge(1f)
					);
				});
			}
			usageTick = 0;
		}
		if (!thrown)
			setTime(getMaxTime());
		else if (timeSinceThrow < 127)
			timeSinceThrow++;
		return super.tick(entity);
	}
	@Override
	public float getSpecialCharge(ItemStack providerStack, float currentCharge)
	{
		return 1f;
	}
	@Override
	public boolean isCancellable(LivingEntity entity)
	{
		return true;
	}
	@Override
	public Optional<Float> mobility(LivingEntity entity)
	{
		if (usageTick > 1)
			return Optional.of(mobility);
		return super.mobility(entity);
	}
	@Override
	public ActionEndResult canEnd(LivingEntity entity, EndType endType)
	{
		Level world = entity.level();
		if (endType == EndType.CANCELLED)
		{
			if (world.isClientSide && entity.equals(ClientUtils.getClientPlayer()))
			{
				SplatcraftKeyHandler.squidAndSubDelay = 5;
			}
			return ActionEndResult.dontEnd(this);
		}
		return ActionEndResult.END_ACTION;
	}
	@Override
	public TrajectoryProcessor getTrajectory(LivingEntity entity, float partialTicks)
	{
		if (thrown)
			return null;

		return TrajectoryProcessor.ofFragileIgnoreEntities(entity.level(), specialData.throwVelocity(), 0, 0.1f, 0.4, new Vector3f(0.8f));
	}
}
