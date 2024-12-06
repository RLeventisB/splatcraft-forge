package net.splatcraft.criteriaTriggers;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.splatcraft.Splatcraft;
import org.jetbrains.annotations.NotNull;

public class FallIntoInkTrigger extends SimpleCriterionTrigger<FallIntoInkTrigger.TriggerInstance>
{
    static final ResourceLocation ID = new ResourceLocation(Splatcraft.MODID, "fall_into_ink");

    public @NotNull ResourceLocation getId()
    {
        return ID;
    }

    public FallIntoInkTrigger.@NotNull TriggerInstance createInstance(@NotNull JsonObject json, @NotNull ContextAwarePredicate composite, @NotNull DeserializationContext context)
    {
        return new FallIntoInkTrigger.TriggerInstance(composite, GsonHelper.getAsFloat(json, "distance", 0));
    }

    public void trigger(ServerPlayer player, float distance)
    {
        this.trigger(player, (instance) -> instance.matches(distance));
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance
    {
        private final float distance;

        public TriggerInstance(ContextAwarePredicate p_27688_, float distance)
        {
            super(FallIntoInkTrigger.ID, p_27688_);
            this.distance = distance;
        }

        public boolean matches(float distance)
        {
            return distance >= this.distance;
        }

        public @NotNull JsonObject serializeToJson(@NotNull SerializationContext context)
        {
            JsonObject jsonobject = super.serializeToJson(context);
            jsonobject.addProperty("distance", distance);
            return jsonobject;
        }
    }
}
