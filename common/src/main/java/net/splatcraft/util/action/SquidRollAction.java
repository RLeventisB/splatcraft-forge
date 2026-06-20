package net.splatcraft.util.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class SquidRollAction extends EntityActionWithTime
{
	public static final Codec<SquidRollAction> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		Codec.FLOAT.fieldOf("time").forGetter(SquidRollAction::getTime),
		Codec.FLOAT.fieldOf("horizontal_angle").forGetter(SquidRollAction::getHorizontalAngle),
		Codec.FLOAT.fieldOf("old_horizontal_angle").forGetter(SquidRollAction::getOldHorizontalAngle)
	).apply(inst, SquidRollAction::new));
	private float horizontalAngle = Float.NaN, oldHorizontalAngle = Float.NaN;
	public SquidRollAction()
	{
		super(0, 0);
	}
	public SquidRollAction(float time, float horizontalAngle, float oldHorizontalAngle)
	{
		super(time, 0);
		this.horizontalAngle = horizontalAngle;
		this.oldHorizontalAngle = oldHorizontalAngle;
	}
	@Override
	public ActionEndResult tick(LivingEntity entity)
	{
		if (entity.level().isClientSide())
		{
			oldHorizontalAngle = horizontalAngle;

			Vec3 deltaMovement = entity.getDeltaMovement();
			if (deltaMovement.horizontalDistanceSqr() > 10e-6)
				horizontalAngle = (float) (Mth.atan2(-deltaMovement.x, deltaMovement.z) * Mth.RAD_TO_DEG);

			if (Float.isNaN(oldHorizontalAngle))
				oldHorizontalAngle = horizontalAngle;
		}
		return super.tick(entity);
	}
	@Override
	public boolean reversedTime()
	{
		return true;
	}
	@Override
	public ActionEndResult canEnd(LivingEntity entity, EndType endType)
	{
		return entity.onGround() ? ActionEndResult.END_ACTION : ActionEndResult.dontEnd(this);
	}
	public float getHorizontalAngle(float partialTick)
	{
		return Mth.lerp(partialTick, oldHorizontalAngle, horizontalAngle);
	}
	public float getHorizontalAngle()
	{
		return horizontalAngle;
	}
	public float getOldHorizontalAngle()
	{
		return oldHorizontalAngle;
	}
}
