package net.splatcraft.criteriaTriggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.Splatcraft;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class CraftWeaponTrigger extends SimpleCriterionTrigger<CraftWeaponTrigger.TriggerInstance>
{
	static final ResourceLocation ID = Splatcraft.identifierOf("craft_weapon");

	public @NotNull ResourceLocation getId()
	{
		return ID;
	}

	public void trigger(ServerPlayer player, ItemStack stack)
	{
		trigger(player, (instance) -> instance.matches(stack));
	}

	@Override
	public @NotNull Codec<TriggerInstance> codec()
	{
		return TriggerInstance.CODEC;
	}

	public record TriggerInstance(Optional<ContextAwarePredicate> player,
	                              Optional<ItemPredicate> item) implements SimpleCriterionTrigger.SimpleInstance
	{
		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
			instance ->
				instance.group(
						EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
						ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TriggerInstance::item))
					.apply(instance, TriggerInstance::new));

		public TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item)
		{
			this.player = player;
			this.item = item;
		}

		public boolean matches(ItemStack stack)
		{
			return item.isEmpty() || item.get().test(stack);
		}

		public @NotNull Optional<ContextAwarePredicate> player()
		{
			return player;
		}

		public Optional<ItemPredicate> item()
		{
			return item;
		}
	}
}
