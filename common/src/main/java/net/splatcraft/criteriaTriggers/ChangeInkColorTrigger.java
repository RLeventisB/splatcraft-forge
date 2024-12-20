package net.splatcraft.criteriaTriggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;

import java.util.Optional;

public class ChangeInkColorTrigger extends AbstractCriterion<ChangeInkColorTrigger.Conditions>
{
    public void trigger(ServerPlayerEntity player)
    {
        trigger(player, (instance) -> instance.matches(player));
    }

    @Override
    public Codec<Conditions> getConditionsCodec()
    {
        return Conditions.CODEC;
    }

    public record Conditions(Optional<LootContextPredicate> player,
                             Optional<InkColor> color) implements AbstractCriterion.Conditions
    {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
            (instance) -> instance.group(
                EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                InkColor.CODEC.optionalFieldOf("color").forGetter(Conditions::color)
            ).apply(instance, Conditions::new));

        public Conditions(Optional<LootContextPredicate> player, Optional<InkColor> color)
        {
            this.player = player;
            this.color = color;
        }

        public boolean matches(ServerPlayerEntity player)
        {
            return color.isPresent() && ColorUtils.getEntityColor(player) == color.get();
        }
    }
}
