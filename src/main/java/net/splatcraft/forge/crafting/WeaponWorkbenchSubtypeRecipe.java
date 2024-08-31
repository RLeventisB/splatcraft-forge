package net.splatcraft.forge.crafting;

import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.JsonUtils;

import java.util.ArrayList;
import java.util.List;

public class WeaponWorkbenchSubtypeRecipe extends AbstractWeaponWorkbenchRecipe
{
    private final ResourceLocation advancement;
    private final boolean requireOther;
    public final List<WeaponWorkbenchSubtypeRecipe> siblings = new ArrayList<>();

    public WeaponWorkbenchSubtypeRecipe(ResourceLocation id, Component name, ItemStack recipeOutput, NonNullList<StackedIngredient> recipeItems, ResourceLocation advancement, boolean requireOther)
    {
        super(id, name, recipeOutput, recipeItems);
        this.advancement = advancement;
        this.requireOther = requireOther;
    }

    public boolean isAvailable(Player player)
    {
        if (requireOther)
            for (WeaponWorkbenchSubtypeRecipe sibling : siblings)
                if (!sibling.isAvailable(player))
                    return false;

        if (advancement == null)
            return true;
        if (player.level().isClientSide())
            return isAvailableOnClient(player);
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.getServer().getAdvancements().getAdvancement(advancement) != null)
            return serverPlayer.getAdvancements().getOrStartProgress(serverPlayer.getServer().getAdvancements().getAdvancement(advancement)).isDone();

        return true;
    }

    @OnlyIn(Dist.CLIENT)
    private boolean isAvailableOnClient(Player player)
    {
        if (!(player instanceof LocalPlayer clientPlayer))
            return true;

        Advancement advancement = clientPlayer.connection.getAdvancements().getAdvancements().get(this.advancement);
        return clientPlayer.connection.getAdvancements().progress.containsKey(advancement) && clientPlayer.connection.getAdvancements().progress.get(advancement).isDone();
    }

    public static WeaponWorkbenchSubtypeRecipe fromJson(ResourceLocation recipeId, JsonObject json)
    {
        JsonObject resultJson = GsonHelper.getAsJsonObject(json, "result");
        ItemStack output = ShapedRecipe.itemStackFromJson(resultJson);

        if (resultJson.has("nbt"))
            output.setTag(JsonUtils.readNBT(resultJson, "nbt"));

        NonNullList<StackedIngredient> input = readIngredients(json.getAsJsonArray("ingredients"));

        Component displayComponent;

        if (GsonHelper.isStringValue(json, "name"))
            displayComponent = Component.translatable(GsonHelper.getAsString(json, "name"));
        else
            displayComponent = json.has("name") ? Component.Serializer.fromJson(json.getAsJsonObject("name")) : Component.literal("null");

        ResourceLocation advancement = json.has("advancement") && !GsonHelper.getAsString(json, "advancement").isEmpty()
                ? new ResourceLocation(GsonHelper.getAsString(json, "advancement")) : null;

        return new WeaponWorkbenchSubtypeRecipe(recipeId, displayComponent, output, input, advancement, GsonHelper.getAsBoolean(json, "requires_other", false));
    }

    public static WeaponWorkbenchSubtypeRecipe fromBuffer(ResourceLocation recipeId, FriendlyByteBuf buffer)
    {
        int i = buffer.readVarInt();
        NonNullList<StackedIngredient> input = NonNullList.withSize(i, StackedIngredient.EMPTY);

        input.replaceAll(ignored -> new StackedIngredient(Ingredient.fromNetwork(buffer), buffer.readInt()));

        return new WeaponWorkbenchSubtypeRecipe(recipeId, buffer.readComponent(), buffer.readItem(), input, buffer.readBoolean() ? new ResourceLocation(buffer.readUtf()) : null, buffer.readBoolean());
    }

    public void toBuffer(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(this.recipeItems.size());
        for (StackedIngredient ingredient : this.recipeItems)
        {
            ingredient.getIngredient().toNetwork(buffer);
            buffer.writeInt(ingredient.getCount());
        }
        buffer.writeComponent(this.name);
        buffer.writeItem(this.recipeOutput);

        buffer.writeBoolean(advancement != null);
        if (advancement != null)
            buffer.writeUtf(advancement.toString());

        buffer.writeBoolean(requireOther);
    }
}
