package net.splatcraft.forge.criteriaTriggers;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.forge.Splatcraft;
import org.jetbrains.annotations.NotNull;

public class CraftWeaponTrigger  extends SimpleCriterionTrigger<CraftWeaponTrigger.TriggerInstance>
{
	static final ResourceLocation ID = new ResourceLocation(Splatcraft.MODID, "craft_weapon");

	public @NotNull ResourceLocation getId() {
		return ID;
	}

	public CraftWeaponTrigger.@NotNull TriggerInstance createInstance(JsonObject json, EntityPredicate.@NotNull Composite composite, @NotNull DeserializationContext context) {
		ItemPredicate itempredicate = ItemPredicate.fromJson(json.get("item"));
		return new CraftWeaponTrigger.TriggerInstance(composite, itempredicate);
	}

	public void trigger(ServerPlayer player, ItemStack stack) {
		this.trigger(player, (instance) -> instance.matches(stack));
	}

	public static class TriggerInstance extends AbstractCriterionTriggerInstance {
		private final ItemPredicate item;

		public TriggerInstance(EntityPredicate.Composite p_27688_, ItemPredicate item) {
			super(CraftWeaponTrigger.ID, p_27688_);
			this.item = item;
		}

		public boolean matches(ItemStack otherItem)
		{
			return this.item.matches(otherItem);
		}

		public @NotNull JsonObject serializeToJson(@NotNull SerializationContext context)
		{
			JsonObject jsonobject = super.serializeToJson(context);
			jsonobject.add("item", this.item.serializeToJson());
			return jsonobject;
		}
	}
}
