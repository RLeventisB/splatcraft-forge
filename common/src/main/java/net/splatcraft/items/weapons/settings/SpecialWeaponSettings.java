package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.structs.WeaponTooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.splatcraft.items.weapons.settings.CommonRecords.ShotDeviationDataRecord;

public class SpecialWeaponSettings<T extends DynamicDataRecord<T>> extends DynamicWeaponSettings<SpecialWeaponSettings<T>, SpecialWeaponSettings.DataRecord, T, ResourceLocation>
{
	public static final SpecialWeaponSettings<?> DEFAULT = new SpecialWeaponSettings<>(DEFAULT_NAME);
	public T specialDataRecord;
	public DataRecord dataRecord = DataRecord.DEFAULT;
	public SpecialWeaponSettings(ResourceLocation name)
	{
		super(name);
	}
	@Override
	public Map.Entry<ResourceLocation, MapCodec<? extends T>>[] getDynamicCodecs()
	{
		return new Map.Entry[] {
			Map.entry(SpecialWeaponRecords.StingRayDataRecord.ID, SpecialWeaponRecords.StingRayDataRecord.CODEC),
			Map.entry(SpecialWeaponRecords.InkJetDataRecord.ID, SpecialWeaponRecords.InkJetDataRecord.CODEC),
			Map.entry(SpecialWeaponRecords.InkStormDataRecord.ID, SpecialWeaponRecords.InkStormDataRecord.CODEC)
		};
	}
	@Override
	public T getDynamicDataToSerialize()
	{
		return specialDataRecord;
	}
	@Override
	public DataRecord getDataToSerialize()
	{
		return dataRecord;
	}
	@Override
	protected void processResult(DataRecord dataRecord, T subData)
	{
		this.dataRecord = dataRecord;
		specialDataRecord = SplatcraftConvertors.convert(subData);
	}
	@Override
	public List<WeaponTooltip<SpecialWeaponSettings<T>>> tooltipsToRegister()
	{
		List<WeaponTooltip<SpecialWeaponSettings<T>>> weaponTooltips = new ArrayList<>();

		specialDataRecord.addTooltips(weaponTooltips);
		return weaponTooltips;
	}
	@Override
	public MapCodec<DataRecord> getMapCodec()
	{
		return DataRecord.CODEC;
	}
	@Override
	public Codec<ResourceLocation> getDynamicCodecKeyCodec()
	{
		return CodecUtils.Codecs.SPLATCRAFT_IDENTIFIER_CODEC;
	}
	@Override
	public String getDynamicCodecKeyName()
	{
		return "special_type";
	}
	@Override
	public ShotDeviationDataRecord getShotDeviationData(ItemStack stack, LivingEntity entity)
	{
		return ShotDeviationDataRecord.PERFECT_DEFAULT;
	}
	@Override
	public float getSpeedForRender(Player player, ItemStack mainHandItem)
	{
		return 0;
	}
	public record DataRecord(
		boolean refillTank,
		float specialDuration,
		float mobility,
		SpecialCostData costData
	)
	{
		public static final MapCodec<DataRecord> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				Codec.BOOL.optionalFieldOf("refill_tank", true).forGetter(DataRecord::refillTank),
				Codec.FLOAT.fieldOf("special_duration").xmap(v -> v * 20, v -> v / 20).forGetter(DataRecord::specialDuration),
				Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::mobility),
				SpecialCostData.CODEC.optionalFieldOf("cost_data", SpecialCostData.DEFAULT).forGetter(DataRecord::costData)
			).apply(inst, DataRecord::new)
		);
		public static final DataRecord DEFAULT = new DataRecord(true, 140, 1f, SpecialCostData.DEFAULT);
	}
	public record SpecialCostData(
		int defaultPoints,
		Object2ObjectMap<ResourceLocation, Integer> pointOverride
	)
	{
		public static final Codec<SpecialCostData> CODEC = RecordCodecBuilder.create(
			inst -> inst.group(
				Codec.INT.optionalFieldOf("default_points", 200).forGetter(SpecialCostData::defaultPoints),
				CodecUtils.hashMapCodec(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("weapon_overrides", new Object2ObjectOpenHashMap<>(0)).forGetter(SpecialCostData::pointOverride)
			).apply(inst, SpecialCostData::new)
		);
		public static final SpecialCostData DEFAULT = new SpecialCostData(200, new Object2ObjectOpenHashMap<>(0));
		public int getCost(ItemStack stack)
		{
			Optional<ResourceLocation> settingId = WeaponBaseItem.getWeaponId(stack);
			return settingId.map(this::getCost).orElse(defaultPoints);
		}
		public int getCost(ResourceLocation weaponId)
		{
			if (weaponId != null && !pointOverride.isEmpty())
			{
				Integer overridenPoints = pointOverride.get(weaponId);
				if (overridenPoints != null)
					return overridenPoints;
			}
			return defaultPoints;
		}
	}
}
