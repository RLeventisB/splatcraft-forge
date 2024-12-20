package net.splatcraft.loot;
// todo: architectury doesnt have loot modifier eye think
/*
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ChestLootModifier extends LootModifier
{
    public static final Supplier<Codec<ChestLootModifier>> CODEC = Suppliers.memoize(() -> RecordCodecBuilder.create(inst -> codecStart(inst).and(
        inst.group(
            Registries.ITEM.getCodec().fieldOf("item").forGetter(v -> v.item),
            Codec.INT.fieldOf("countMin").forGetter(v -> v.countMin),
            Codec.INT.fieldOf("countMax").forGetter(v -> v.countMax),
            Codec.FLOAT.fieldOf("chance").forGetter(v -> v.chance),
            Codec.STRING.fieldOf("parent").forGetter(v -> v.parentTable.toString())
        )).apply(inst, ChestLootModifier::new)
    ));
    public final Item item;
    public final int countMin;
    public final int countMax;
    public final float chance;
    public final Identifier parentTable;

    *
     * Constructs a LootModifier.
     *
     * @param conditionsIn the ILootConditions that need to be matched before the loot is modified.

    protected ChestLootModifier(LootCondition[] conditionsIn, Item itemIn, int countMin, int countMax, float chance, String parentTable)
    {
        super(conditionsIn);
        item = itemIn;
        this.countMin = countMin;
        this.countMax = countMax;
        this.chance = chance;
        this.parentTable = Identifier.of(parentTable);
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context)
    {
        if (!context.getQueriedLootTableId().equals(parentTable))
        {
            return generatedLoot;
        }

        float c = context.getRandom().nextFloat();

        if (c <= chance)
        {
            generatedLoot.add(new ItemStack(item, (countMax - countMin <= 0 ? 0 : context.getRandom().nextInt(countMax - countMin)) + countMin));
        }

        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec()
    {
        return CODEC.get();
    }
}
*/