package net.splatcraft.mixin.accessors;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccesor
{
	@Accessor("lastDamageStamp")
	long getLastDamageStamp();
	@Accessor("lastDamageStamp")
	void setLastDamageStamp(long value);
	@Accessor("lastDamageSource")
	void setLastDamageSource(DamageSource value);
}
