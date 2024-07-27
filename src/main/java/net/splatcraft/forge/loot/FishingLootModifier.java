package net.splatcraft.forge.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancements.critereon.FishingHookPredicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class FishingLootModifier extends LootModifier
{
	public static final Supplier<Codec<FishingLootModifier>> CODEC = Suppliers.memoize(() -> RecordCodecBuilder.create(inst -> codecStart(inst).and(
		inst.group(
			ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(v -> v.item),
			Codec.INT.fieldOf("countMin").forGetter(v -> v.countMin),
			Codec.INT.fieldOf("countMax").forGetter(v -> v.countMax),
			Codec.FLOAT.fieldOf("chance").forGetter(v -> v.chance),
			Codec.INT.fieldOf("quality").forGetter(v -> v.quality),
			Codec.BOOL.fieldOf("isTreasure").forGetter(v -> v.isTreasure)
		)).apply(inst, FishingLootModifier::new)
	));
	public final Item item;
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
	protected FishingLootModifier(LootItemCondition[] conditionsIn, Item itemIn, int countMin, int countMax, float chance, int quality, boolean isTreasure)
	{
		super(conditionsIn);
		item = itemIn;
		this.countMin = countMin;
		this.countMax = countMax;
		this.chance = chance;
		this.quality = quality;
		this.isTreasure = isTreasure;
	}
	@Override
	public Codec<? extends IGlobalLootModifier> codec()
	{
		return CODEC.get();
	}
	@NotNull
	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context)
	{
		if (!(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof FishingHook) || isTreasure && !FishingHookPredicate.inOpenWater(true).matches(context.getParamOrNull(LootContextParams.THIS_ENTITY), null, null))
		{
			return generatedLoot;
		}
		
		float chanceMod = 0;
		if (context.getParamOrNull(LootContextParams.KILLER_ENTITY) instanceof LivingEntity entity)
		{
			ItemStack stack = entity.getUseItem();
			int fishingLuck = EnchantmentHelper.getFishingLuckBonus(stack);
			float luck = entity instanceof Player player ? player.getLuck() : 0;
			
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
