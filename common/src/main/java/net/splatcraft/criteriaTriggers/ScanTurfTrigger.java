package net.splatcraft.criteriaTriggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class ScanTurfTrigger extends SimpleCriterionTrigger<ScanTurfTrigger.Conditions>
{
    public void trigger(ServerPlayer player, int blocksInked, boolean winner)
    {
        trigger(player, (instance) -> instance.matches(blocksInked, winner));
    }

    @Override
    public Codec<Conditions> codec()
    {
        return Conditions.CODEC;
    }

    public record Conditions(Optional<ContextAwarePredicate> player,
                             int blocksInked,
                             boolean winner) implements SimpleCriterionTrigger.SimpleInstance
    {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
            (instance) -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                Codec.INT.optionalFieldOf("blocks_inked", 0).forGetter(Conditions::blocksInked),
                Codec.BOOL.optionalFieldOf("winner", false).forGetter(Conditions::winner)
            ).apply(instance, Conditions::new));

        public Conditions(Optional<ContextAwarePredicate> player, int blocksInked, boolean winner)
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
