package net.splatcraft.criteriaTriggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class FallIntoInkTrigger extends SimpleCriterionTrigger<FallIntoInkTrigger.TriggerInstance>
{
	public void trigger(ServerPlayer player, float distance)
	{
		trigger(player, (instance) -> instance.matches(distance));
	}
	@Override
	public @NotNull Codec<TriggerInstance> codec()
	{
		return TriggerInstance.CODEC;
	}
	public record TriggerInstance(Optional<ContextAwarePredicate> player,
	                              float distanceFallen) implements SimpleInstance
	{
		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
			inst -> inst.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
				Codec.FLOAT.optionalFieldOf("distance", 0f).forGetter(TriggerInstance::distanceFallen)
			).apply(inst, TriggerInstance::new)
		);
		public boolean matches(float distance)
		{
			return distance >= distanceFallen;
		}
	}
}
