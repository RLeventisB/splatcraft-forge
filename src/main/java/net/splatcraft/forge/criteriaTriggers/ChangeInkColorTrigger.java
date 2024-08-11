package net.splatcraft.forge.criteriaTriggers;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.registries.SplatcraftInkColors;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.InkColor;
import org.jetbrains.annotations.NotNull;

public class ChangeInkColorTrigger extends SimpleCriterionTrigger<ChangeInkColorTrigger.TriggerInstance>
{
    static final ResourceLocation ID = new ResourceLocation(Splatcraft.MODID, "change_ink_color");

    public @NotNull ResourceLocation getId()
    {
        return ID;
    }

    public ChangeInkColorTrigger.@NotNull TriggerInstance createInstance(JsonObject json, @NotNull ContextAwarePredicate composite, @NotNull DeserializationContext context)
    {

        int color = -1;

        if (json.has("color"))
        {
            if (GsonHelper.isStringValue(json, "color"))
            {
                String str = GsonHelper.getAsString(json, "color");
                if (str.indexOf('#') == 0)
                    color = Integer.parseInt(str.substring(1), 16);
                else {
                    InkColor colorObj = SplatcraftInkColors.REGISTRY.get().getValue(new ResourceLocation(str));
                    if (colorObj != null)
                        color = colorObj.getColor();
                }
            } else color = Mth.clamp(GsonHelper.getAsInt(json, "color"), 0, 0xFFFFFF);
        }
        return new ChangeInkColorTrigger.TriggerInstance(composite, color);
    }

	public void trigger(ServerPlayer player)
    {
		this.trigger(player, (instance) -> instance.matches(player));
	}

    public static class TriggerInstance extends AbstractCriterionTriggerInstance
    {
        private final int color;

        public TriggerInstance(ContextAwarePredicate p_27688_, int color)
        {
            super(ChangeInkColorTrigger.ID, p_27688_);
            this.color = color;
        }

		public boolean matches(ServerPlayer player)
		{
			return color == -1 || ColorUtils.getPlayerColor(player) == color;
		}

        public @NotNull JsonObject serializeToJson(@NotNull SerializationContext context)
        {
            JsonObject jsonobject = super.serializeToJson(context);
            jsonobject.addProperty("color", color);
            return jsonobject;
        }
    }
}
