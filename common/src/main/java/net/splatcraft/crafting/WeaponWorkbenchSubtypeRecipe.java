package net.splatcraft.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Optional;

public class WeaponWorkbenchSubtypeRecipe extends AbstractWeaponWorkbenchRecipe
{
	public static final Codec<WeaponWorkbenchSubtypeRecipe> CODEC = RecordCodecBuilder.create(inst ->
		inst.group(
			ComponentSerialization.CODEC.optionalFieldOf("name", Component.literal("null")).forGetter(v -> v.name),
			ItemStack.CODEC.fieldOf("result").forGetter(v -> v.recipeOutput),
			StackedIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(v -> v.recipeItems),
			ResourceLocation.CODEC.optionalFieldOf("advancement").forGetter(v -> v.advancement),
			Codec.BOOL.optionalFieldOf("require_other", false).forGetter(v -> v.requireOther)
		).apply(inst, WeaponWorkbenchSubtypeRecipe::new)
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, WeaponWorkbenchSubtypeRecipe> STREAM_CODEC = StreamCodec.of(
		(buffer, recipe) ->
		{
			ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buffer, recipe.name);
			ItemStack.STREAM_CODEC.encode(buffer, recipe.recipeOutput);
			StackedIngredient.LIST_PACKET_CODEC.encode(buffer, recipe.recipeItems);
			ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buffer, recipe.advancement);
			ByteBufCodecs.BOOL.encode(buffer, recipe.requireOther);
		},
		(buffer) ->
			new WeaponWorkbenchSubtypeRecipe(
				ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buffer),
				ItemStack.STREAM_CODEC.decode(buffer),
				StackedIngredient.LIST_PACKET_CODEC.decode(buffer),
				ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buffer),
				ByteBufCodecs.BOOL.decode(buffer)
			)
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, List<WeaponWorkbenchSubtypeRecipe>> LIST_PACKET_CODEC = STREAM_CODEC.apply(ByteBufCodecs.list());
	public final NonNullList<WeaponWorkbenchSubtypeRecipe> siblings = NonNullList.create();
	private final Optional<ResourceLocation> advancement;
	private final boolean requireOther;
	public WeaponWorkbenchSubtypeRecipe(Component name, ItemStack recipeOutput, List<StackedIngredient> recipeItems, Optional<ResourceLocation> advancement, boolean requireOther)
	{
		super(name, recipeOutput, recipeItems);
		this.advancement = advancement;
		this.requireOther = requireOther;
	}
	public boolean isAvailable(Player player)
	{
		if (requireOther)
			for (WeaponWorkbenchSubtypeRecipe sibling : siblings)
				if (!sibling.isAvailable(player))
					return false;
		
		if (advancement.isEmpty())
			return true;
		if (player.level().isClientSide())
			return isAvailableOnClient(player);
		if (player instanceof ServerPlayer serverPlayer)
		{
			AdvancementHolder advancementEntry = serverPlayer.getServer().getAdvancements().get(advancement.get());
			if (advancementEntry != null)
				return serverPlayer.getAdvancements().getOrStartProgress(advancementEntry).isDone();
		}
		
		return true;
	}
	@Environment(EnvType.CLIENT)
	private boolean isAvailableOnClient(Player player)
	{
		if (!(player instanceof LocalPlayer clientPlayer) || advancement.isEmpty())
			return true;
		
		AdvancementHolder advancement = clientPlayer.connection.getAdvancements().get(this.advancement.get());
		return clientPlayer.connection.getAdvancements().progress.containsKey(advancement) && clientPlayer.connection.getAdvancements().progress.get(advancement).isDone();
	}
}
