package net.splatcraft.forge.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ChestLootModifier extends LootModifier
{
	public static final Supplier<Codec<ChestLootModifier>> CODEC = Suppliers.memoize(() -> RecordCodecBuilder.create(inst -> codecStart(inst).and(
		inst.group(
			ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(v -> v.item),
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
	public final ResourceLocation parentTable;
	/**
	 * Constructs a LootModifier.
	 *
	 * @param conditionsIn the ILootConditions that need to be matched before the loot is modified.
	 */
	protected ChestLootModifier(LootItemCondition[] conditionsIn, Item itemIn, int countMin, int countMax, float chance, String parentTable)
	{
		super(conditionsIn);
		item = itemIn;
		this.countMin = countMin;
		this.countMax = countMax;
		this.chance = chance;
		this.parentTable = new ResourceLocation(parentTable);
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
