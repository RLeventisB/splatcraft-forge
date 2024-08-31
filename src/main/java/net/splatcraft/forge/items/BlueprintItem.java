package net.splatcraft.forge.items;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.splatcraft.forge.data.SplatcraftTags;
import net.splatcraft.forge.items.weapons.*;
import net.splatcraft.forge.registries.SplatcraftItemGroups;
import net.splatcraft.forge.registries.SplatcraftItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class BlueprintItem extends Item
{
    public static final HashMap<String, Predicate<Item>> weaponPools = new HashMap<>()
    {{
        put("shooters", instanceOf(ShooterItem.class));
        put("blasters", instanceOf(BlasterItem.class));
        put("rollers", instanceOf(RollerItem.class));
        put("chargers", instanceOf(ChargerItem.class));
        put("sloshers", instanceOf(SlosherItem.class));
        put("splatlings", instanceOf(SplatlingItem.class));
        put("dualies", instanceOf(DualieItem.class));
        put("sub_weapons", instanceOf(SubWeaponItem.class));
        put("ink_tanks", instanceOf(InkTankItem.class));
        put("wildcard", item -> true);
    }};

    public static Predicate<Item> instanceOf(Class<? extends Item> clazz)
    {
        return clazz::isInstance;
    }

    public BlueprintItem()
    {
        super(new Properties().stacksTo(16));
        SplatcraftItemGroups.addGeneralItem(this);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> components, @NotNull TooltipFlag flag)
    {
        super.appendHoverText(stack, level, components, flag);

        if (stack.hasTag())
        {
            CompoundTag nbt = stack.getTag();

            if (nbt.getBoolean("HideTooltip"))
                return;

            if (nbt.contains("Advancements"))
            {
                components.add(Component.translatable("item.splatcraft.blueprint.tooltip"));
                return;
            }

            if (nbt.contains("Pools"))
            {
                components.add(Component.translatable("item.splatcraft.blueprint.tooltip"));
                nbt.getList("Pools", Tag.TAG_STRING).forEach((weaponType) ->
                        components.add(Component.translatable("item.splatcraft.blueprint.tooltip." + weaponType.getAsString())
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.BLUE).withItalic(false)))
                );
                return;
            }
        }

        components.add(Component.translatable("item.splatcraft.blueprint.tooltip.empty"));
    }

    //	@Override
//	public void fillItemCategory(@NotNull CreativeModeTab tab, @NotNull NonNullList<ItemStack> list)
//	{
//		if (tab == CreativeModeTab.TAB_SEARCH)
//			weaponPools.forEach((key, value) -> list.add(setPoolFromWeaponType(new ItemStack(this), key)));
//		else if (allowdedIn(tab))
//		{
//			list.add(setPoolFromWeaponType(new ItemStack(this), "wildcard"));
//		}
//	}
    public static ItemStack addToAdvancementPool(ItemStack blueprint, String... advancementIds)
    {
        return addToAdvancementPool(blueprint, Arrays.stream(advancementIds));
    }

    public static ItemStack setPoolFromWeaponType(ItemStack blueprint, String weaponType)
    {
        if (!weaponPools.containsKey(weaponType))
            return blueprint;

        CompoundTag nbt = blueprint.getOrCreateTag();
        ListTag pool = nbt.contains("Pools", Tag.TAG_LIST) ? nbt.getList("Pools", Tag.TAG_STRING) : new ListTag();
        pool.add(StringTag.valueOf(weaponType));

        nbt.put("Pools", pool);

        return blueprint;
    }

    public static ItemStack addToAdvancementPool(ItemStack blueprint, Stream<String> advancementIds)
    {
        CompoundTag nbt = blueprint.getOrCreateTag();
        ListTag pool = nbt.contains("Advancements", Tag.TAG_LIST) ? nbt.getList("Advancements", Tag.TAG_STRING) : new ListTag();

        advancementIds.map(StringTag::valueOf).forEach(pool::add);

        nbt.put("Advancements", pool);
        return blueprint;
    }

    public static List<Advancement> getAdvancementPool(Level level, ItemStack blueprint)
    {
        List<Advancement> output = new ArrayList<>();

        if (blueprint.hasTag())
        {
            blueprint.getTag().getList("Advancements", Tag.TAG_STRING).forEach(
                    tag ->
                    {
                        Advancement advancement;
                        advancement = level.getServer().getAdvancements().getAdvancement(new ResourceLocation(tag.getAsString()));

                        if (advancement != null)
                            output.add(advancement);
                    }
            );
            blueprint.getTag().getList("Pools", Tag.TAG_STRING).forEach(
                    tag ->
                            SplatcraftItems.weapons.stream().
                                    filter(weaponPools.get(tag.getAsString()).and(item ->
                                            !Objects.equals(ForgeRegistries.ITEMS.getKey(item), SplatcraftTags.Items.BLUEPRINT_EXCLUDED.location())))
                                    .map(ForgeRegistries.ITEMS::getKey).filter(Objects::nonNull).map(holder ->
                                            new ResourceLocation(holder.getNamespace(), "unlocks/" + holder.getPath())).map(level.getServer().getAdvancements()::getAdvancement)
                                    .filter(Objects::nonNull).forEach(output::add)
            );
        }

        return output;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand)
    {
        if (!(player instanceof ServerPlayer serverPlayer))
            return super.use(level, player, hand);

        ItemStack stack = player.getItemInHand(hand);

        if (stack.hasTag())
        {
            List<Advancement> pool = getAdvancementPool(level, stack);
            int count = pool.size();

            if (count > 0)
            {
                pool.removeIf(advancement -> serverPlayer.getAdvancements().getOrStartProgress(advancement).isDone());

                if (!pool.isEmpty())
                {
                    Advancement advancement = pool.get(level.random.nextInt(pool.size()));

                    for (String key : serverPlayer.getAdvancements().getOrStartProgress(advancement).getRemainingCriteria())
                        serverPlayer.getAdvancements().award(advancement, key);

                    if (advancement.getDisplay() != null && !advancement.getDisplay().shouldShowToast())
                        player.displayClientMessage(Component.translatable("status.blueprint.unlock", advancement.getDisplay().getTitle()), true);

                    stack.shrink(1);
                    return InteractionResultHolder.consume(stack);
                }

                player.displayClientMessage(Component.translatable("status.blueprint.already_unlocked" + (count > 1 ? "" : ".single")), true);
                return super.use(level, player, hand);
            }
        }

        player.displayClientMessage(Component.translatable("status.blueprint.invalid"), true);
        return super.use(level, player, hand);
    }
}
