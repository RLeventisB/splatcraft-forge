package net.splatcraft.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

public class WeaponWorkbenchSubtypeRecipe extends AbstractWeaponWorkbenchRecipe
{
    public static final Codec<WeaponWorkbenchSubtypeRecipe> CODEC = RecordCodecBuilder.create(inst ->
        inst.group(
            TextCodecs.CODEC.optionalFieldOf("name", Text.literal("null")).forGetter(v -> v.name),
            ItemStack.CODEC.fieldOf("result").forGetter(v -> v.recipeOutput),
            StackedIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(v -> v.recipeItems),
            Identifier.CODEC.optionalFieldOf("advancement", null).forGetter(v -> v.advancement),
            Codec.BOOL.optionalFieldOf("require_other", false).forGetter(v -> v.requireOther)
        ).apply(inst, WeaponWorkbenchSubtypeRecipe::new)
    );
    public static final PacketCodec<RegistryByteBuf, WeaponWorkbenchSubtypeRecipe> PACKET_CODEC = PacketCodec.ofStatic(
        (buffer, recipe) ->
        {
            TextCodecs.PACKET_CODEC.encode(buffer, recipe.name);
            ItemStack.PACKET_CODEC.encode(buffer, recipe.recipeOutput);
            StackedIngredient.LIST_PACKET_CODEC.encode(buffer, recipe.recipeItems);
            Identifier.PACKET_CODEC.encode(buffer, recipe.advancement);
            PacketCodecs.BOOL.encode(buffer, recipe.requireOther);
        },
        (buffer) ->
            new WeaponWorkbenchSubtypeRecipe(
                TextCodecs.PACKET_CODEC.decode(buffer),
                ItemStack.PACKET_CODEC.decode(buffer),
                StackedIngredient.LIST_PACKET_CODEC.decode(buffer),
                Identifier.PACKET_CODEC.decode(buffer),
                PacketCodecs.BOOL.decode(buffer)
            )
    );
    public static final PacketCodec<RegistryByteBuf, List<WeaponWorkbenchSubtypeRecipe>> LIST_PACKET_CODEC = PACKET_CODEC.collect(PacketCodecs.toList());

    public final DefaultedList<WeaponWorkbenchSubtypeRecipe> siblings = DefaultedList.of();
    private final Identifier advancement;
    private final boolean requireOther;

    public WeaponWorkbenchSubtypeRecipe(Text name, ItemStack recipeOutput, List<StackedIngredient> recipeItems, Identifier advancement, boolean requireOther)
    {
        super(name, recipeOutput, recipeItems);
        this.advancement = advancement;
        this.requireOther = requireOther;
    }

    public boolean isAvailable(PlayerEntity player)
    {
        if (requireOther)
            for (WeaponWorkbenchSubtypeRecipe sibling : siblings)
                if (!sibling.isAvailable(player))
                    return false;

        if (advancement == null)
            return true;
        if (player.getWorld().isClient())
            return isAvailableOnClient(player);
        if (player instanceof ServerPlayerEntity serverPlayer)
        {
            AdvancementEntry advancementEntry = serverPlayer.getServer().getAdvancementLoader().get(advancement);
            if (advancementEntry != null)
                return serverPlayer.getAdvancementTracker().getProgress(advancementEntry).isDone();
        }

        return true;
    }

    @Environment(EnvType.CLIENT)
    private boolean isAvailableOnClient(PlayerEntity player)
    {
        if (!(player instanceof ClientPlayerEntity clientPlayer))
            return true;

        AdvancementEntry advancement = clientPlayer.networkHandler.getAdvancementHandler().get(this.advancement);
        return clientPlayer.networkHandler.getAdvancementHandler().advancementProgresses.containsKey(advancement) && clientPlayer.networkHandler.getAdvancementHandler().advancementProgresses.get(advancement).isDone();
    }
}
