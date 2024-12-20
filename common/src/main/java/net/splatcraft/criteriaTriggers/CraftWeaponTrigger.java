package net.splatcraft.criteriaTriggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.splatcraft.Splatcraft;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class CraftWeaponTrigger extends AbstractCriterion<CraftWeaponTrigger.TriggerInstance>
{
    static final Identifier ID = Splatcraft.identifierOf("craft_weapon");

    public @NotNull Identifier getId()
    {
        return ID;
    }

    public void trigger(ServerPlayerEntity player, ItemStack stack)
    {
        trigger(player, (instance) -> instance.matches(stack));
    }

    @Override
    public Codec<TriggerInstance> getConditionsCodec()
    {
        return TriggerInstance.CODEC;
    }

    public record TriggerInstance(Optional<LootContextPredicate> player,
                                  Optional<ItemPredicate> item) implements AbstractCriterion.Conditions
    {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
            instance ->
                instance.group(
                        EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                        ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TriggerInstance::item))
                    .apply(instance, TriggerInstance::new));

        public TriggerInstance(Optional<LootContextPredicate> player, Optional<ItemPredicate> item)
        {
            this.player = player;
            this.item = item;
        }

        public boolean matches(ItemStack stack)
        {
            return item.isEmpty() || item.get().test(stack);
        }

        public Optional<LootContextPredicate> player()
        {
            return player;
        }

        public Optional<ItemPredicate> item()
        {
            return item;
        }
    }
}
