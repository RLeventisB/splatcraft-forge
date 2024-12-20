package net.splatcraft.network.c2s;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.splatcraft.crafting.SplatcraftRecipeTypes;
import net.splatcraft.crafting.StackedIngredient;
import net.splatcraft.crafting.WeaponWorkbenchRecipe;
import net.splatcraft.crafting.WeaponWorkbenchSubtypeRecipe;
import net.splatcraft.registries.SplatcraftStats;
import net.splatcraft.util.CommonUtils;

import java.util.Optional;

public class CraftWeaponPacket extends PlayC2SPacket
{
    private static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(CraftWeaponPacket.class);
    Identifier recipeID;
    int subtype;

    public CraftWeaponPacket(Identifier recipeID, int subtype)
    {
        this.recipeID = recipeID;
        this.subtype = subtype;
    }

    public static CraftWeaponPacket decode(RegistryByteBuf buffer)
    {
        return new CraftWeaponPacket(buffer.readIdentifier(), buffer.readInt());
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeIdentifier(recipeID);
        buffer.writeInt(subtype);
    }

    @Override
    public void execute(PlayerEntity player)
    {
        Optional<? extends RecipeEntry<?>> recipeOptional = player.getWorld().getRecipeManager().get(recipeID);

        if (recipeOptional.isPresent() && recipeOptional.get().value() instanceof WeaponWorkbenchRecipe workbenchRecipe)
        {
            WeaponWorkbenchSubtypeRecipe recipe = workbenchRecipe.getRecipeFromIndex(player, subtype);
            for (StackedIngredient ing : recipe.getInput())
            {
                if (!SplatcraftRecipeTypes.getItem(player, ing.getIngredient(), ing.getCount(), false))
                {
                    return;
                }
            }

            for (StackedIngredient ing : recipe.getInput())
            {
                SplatcraftRecipeTypes.getItem(player, ing.getIngredient(), ing.getCount(), true);
            }
            ItemStack output = recipe.getOutput().copy();

            if (!output.isEmpty())
            {
                SplatcraftStats.CRAFT_WEAPON_TRIGGER.get().trigger((net.minecraft.server.network.ServerPlayerEntity) player, output.copy());
                player.incrementStat(Stats.CRAFTED.getOrCreateStat(output.copy().getItem()));
                player.incrementStat(SplatcraftStats.WEAPONS_CRAFTED);

                if (!player.giveItemStack(output))
                {
                    ItemEntity item = player.dropItem(output, false);
                    if (item != null)
                    {
                        item.resetPickupDelay();
                    }
                }
                else
                {
                    player.playerScreenHandler.sendContentUpdates();
                }
            }
        }
    }
}
