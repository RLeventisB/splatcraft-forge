package net.splatcraft.criteriaTriggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

public class FallIntoInkTrigger extends AbstractCriterion<FallIntoInkTrigger.TriggerInstance>
{
    public void trigger(ServerPlayerEntity player, float distance)
    {
        trigger(player, (instance) -> instance.matches(distance));
    }

    @Override
    public Codec<TriggerInstance> getConditionsCodec()
    {
        return TriggerInstance.CODEC;
    }

    public record TriggerInstance(Optional<LootContextPredicate> player,
                                  float distanceFallen) implements Conditions
    {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
            inst -> inst.group(
                EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.FLOAT.optionalFieldOf("distance", 0f).forGetter(TriggerInstance::distanceFallen)
            ).apply(inst, TriggerInstance::new)
        );

        public boolean matches(float distance)
        {
            return distance >= distanceFallen;
        }
    }
}
