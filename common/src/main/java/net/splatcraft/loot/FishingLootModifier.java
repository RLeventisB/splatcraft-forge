package net.splatcraft.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.predicate.entity.FishingHookPredicate;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FishingLootModifier extends SplatcraftLootModifier
{
    public static final Codec<FishingLootModifier> CODEC = RecordCodecBuilder.create(inst ->
        inst.group(
            LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(lm -> lm.conditions),
            ItemStack.ITEM_CODEC.fieldOf("item").forGetter(v -> v.item),
            Codec.INT.fieldOf("countMin").forGetter(v -> v.countMin),
            Codec.INT.fieldOf("countMax").forGetter(v -> v.countMax),
            Codec.FLOAT.fieldOf("chance").forGetter(v -> v.chance),
            Codec.INT.fieldOf("quality").forGetter(v -> v.quality),
            Codec.BOOL.fieldOf("isTreasure").forGetter(v -> v.isTreasure)
        ).apply(inst, FishingLootModifier::new)
    );
    public final RegistryEntry<Item> item;
    public final int countMin;
    public final int countMax;
    public final float chance;
    public final int quality;
    public final boolean isTreasure;

    /**
     * Constructs a LootModifier.
     *
     * @param conditionsIn the ILootConditions that need to be matched before the loot is modified.
     */
    protected FishingLootModifier(LootCondition[] conditionsIn, RegistryEntry<Item> itemIn, int countMin, int countMax, float chance, int quality, boolean isTreasure)
    {
        super(conditionsIn, Objects::nonNull);
        item = itemIn;
        this.countMin = countMin;
        this.countMax = countMax;
        this.chance = chance;
        this.quality = quality;
        this.isTreasure = isTreasure;
    }

    @Override
    public Codec<? extends SplatcraftLootModifier> codec()
    {
        return CODEC;
    }

    @NotNull
    @Override
    public ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context)
    {
        if (!(context.get(LootContextParameters.THIS_ENTITY) instanceof FishingBobberEntity) || isTreasure && !FishingHookPredicate.of(true).test(context.get(LootContextParameters.THIS_ENTITY), null, null))
        {
            return generatedLoot;
        }

        float chanceMod = 0;
        if (context.get(LootContextParameters.ATTACKING_ENTITY) instanceof LivingEntity entity)
        {
            ItemStack stack = entity.getActiveItem();
            int fishingLuck = EnchantmentHelper.getFishingLuckBonus((ServerWorld) entity.getWorld(), stack, entity);
            float luck = entity instanceof PlayerEntity player ? player.getLuck() : 0;

            if (isTreasure)
            {
                chanceMod += fishingLuck;
            }
            chanceMod += luck;

            chanceMod *= quality * (chance / 2);
        }

        if (context.getRandom().nextInt(100) <= (chance + chanceMod) * 100)
        {
            if (generatedLoot.size() <= 1)
            {
                generatedLoot.clear();
            }
            generatedLoot.add(new ItemStack(item, (countMax - countMin <= 0 ? 0 : context.getRandom().nextInt(countMax - countMin)) + countMin));
        }
        return generatedLoot;
    }
}
