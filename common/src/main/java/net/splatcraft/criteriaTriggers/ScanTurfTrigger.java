package net.splatcraft.criteriaTriggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

public class ScanTurfTrigger extends AbstractCriterion<ScanTurfTrigger.Conditions>
{
    public void trigger(ServerPlayerEntity player, int blocksInked, boolean winner)
    {
        trigger(player, (instance) -> instance.matches(blocksInked, winner));
    }

    @Override
    public Codec<Conditions> getConditionsCodec()
    {
        return Conditions.CODEC;
    }

    public record Conditions(Optional<LootContextPredicate> player,
                             int blocksInked,
                             boolean winner) implements AbstractCriterion.Conditions
    {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
            (instance) -> instance.group(
                EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                Codec.INT.optionalFieldOf("blocks_inked", 0).forGetter(Conditions::blocksInked),
                Codec.BOOL.optionalFieldOf("winner", false).forGetter(Conditions::winner)
            ).apply(instance, Conditions::new));

        public Conditions(Optional<LootContextPredicate> player, int blocksInked, boolean winner)
        {
            this.player = player;
            this.blocksInked = blocksInked;
            this.winner = winner;
        }

        public boolean matches(int blocksInked, boolean winner)
        {
            return blocksInked >= this.blocksInked && (winner || !this.winner);
        }
    }
}
