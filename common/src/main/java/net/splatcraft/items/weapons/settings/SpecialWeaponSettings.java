package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.WeaponTooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.splatcraft.items.weapons.settings.CommonRecords.ShotDeviationDataRecord;

public class SpecialWeaponSettings<T extends DynamicDataRecord<T>> extends DynamicWeaponSettings<SpecialWeaponSettings<T>, SpecialWeaponSettings.DataRecord, T>
{
	public static final SpecialWeaponSettings<?> DEFAULT = new SpecialWeaponSettings<>("default");
	public T specialDataRecord;
	public DataRecord dataRecord = DataRecord.DEFAULT;
	public SpecialWeaponSettings(String name)
	{
		super(name);
	}
	@Override
	public Map.Entry<Identifier, MapCodec<? extends T>>[] getDynamicCodecs()
	{
		return new Map.Entry[] {
			Map.entry(SpecialWeaponRecords.StingRayDataRecord.ID, SpecialWeaponRecords.StingRayDataRecord.CODEC)
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
	public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
	{
		return 0;
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
	public ShotDeviationDataRecord getShotDeviationData(ItemStack stack, LivingEntity entity)
	{
		return ShotDeviationDataRecord.PERFECT_DEFAULT;
	}
	@Override
	public float getSpeedForRender(ClientPlayerEntity player, ItemStack mainHandItem)
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
		Object2ObjectOpenHashMap<Identifier, Integer> pointOverride
	)
	{
		public static final Codec<SpecialCostData> CODEC = RecordCodecBuilder.create(
			inst -> inst.group(
				Codec.INT.optionalFieldOf("default_points", 200).forGetter(SpecialCostData::defaultPoints),
				CodecUtils.hashMapCodec(Identifier.CODEC, Codec.INT).optionalFieldOf("weapon_overrides", new Object2ObjectOpenHashMap<>(0)).forGetter(SpecialCostData::pointOverride)
			).apply(inst, SpecialCostData::new)
		);
		public static final SpecialCostData DEFAULT = new SpecialCostData(200, new Object2ObjectOpenHashMap<>(0));
		public int getCost(ItemStack stack)
		{
			if (!pointOverride.isEmpty() && stack.getItem() instanceof WeaponBaseItem<?> weaponItem)
			{
				Identifier settingId = weaponItem.getSettingsAndValidId(stack).getFirst();
				Integer overridenPoints = pointOverride.get(settingId);
				if (overridenPoints != null)
					return overridenPoints;
			}
			return defaultPoints;
		}
	}
}
