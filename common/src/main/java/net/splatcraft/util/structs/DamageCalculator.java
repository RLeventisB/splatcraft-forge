package net.splatcraft.util.structs;

import com.mojang.serialization.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.Splatcraft;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.items.weapons.settings.ChargerWeaponSettings;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.items.weapons.settings.RollerWeaponSettings;
import net.splatcraft.util.CodecUtils;
import org.jetbrains.annotations.NotNull;

import static net.splatcraft.util.structs.DamageCalculators.*;

public interface DamageCalculator
{
	ResourceKey<Registry<Class<? extends DamageCalculator>>> REGISTRY_KEY = ResourceKey.createRegistryKey(Splatcraft.identifierOf("damage_calculator"));
	MappedRegistry<Class<? extends DamageCalculator>> REGISTRY = new MappedRegistry<>(REGISTRY_KEY, Lifecycle.stable());
	Object2ObjectOpenHashMap<ResourceLocation, MapCodec<? extends DamageCalculator>> ID_TO_CODEC_MAP = new Object2ObjectOpenHashMap<>();
	static void initialize()
	{
		register("empty", EmptyDamageCalculator.CODEC, EmptyDamageCalculator.class);
		register("basic", BasicDamageCalculator.CODEC, BasicDamageCalculator.class);
		register("slosher", SlosherDamageCalculator.CODEC, SlosherDamageCalculator.class);
	}
	private static <T extends DamageCalculator> void register(String name, MapCodec<T> mapCodec, Class<T> clazz)
	{
		ResourceLocation id = Splatcraft.identifierOf(name);
		Registry.register(REGISTRY, id, clazz);
		ID_TO_CODEC_MAP.put(id, mapCodec);
	}
	static DamageCalculator empty()
	{
		return new EmptyDamageCalculator();
	}
	static DamageCalculator basic(CommonRecords.ProjectileDataRecord data)
	{
		return new BasicDamageCalculator(data.baseDamage(), data.damageDecayStartTick(), data.damageDecayPerTick(), data.minDamage());
	}
	static DamageCalculator slosher(float spawnHeight, CommonRecords.ProjectileDataRecord data)
	{
		return new SlosherDamageCalculator(spawnHeight, data.baseDamage(), data.minDamage(), data.damageDecayStartTick(), data.damageDecayPerTick());
	}
	static DamageCalculator charger(ChargerWeaponSettings.ChargerProjectileDataRecord data, float charge)
	{
		return new StaticDamageCalculator(data.damage().getValue(charge));
	}
	static DamageCalculator fixed(float damage)
	{
		return new StaticDamageCalculator(damage);
	}
	static DamageCalculator roller(RollerWeaponSettings.RollerProjectileDataRecord data, Vec3 spawnPos, boolean weak)
	{
		return new RollerDamageCalculator(data.getDamageRanges(weak), spawnPos.toVector3f(), data.damageFalloffStartTick(), data.calculatePercentageFallofPerTick(), data.maxDamageFalloffPercent());
	}
	static <T> DataResult<DamageCalculator> parseDamageCalculator(@NotNull DynamicOps<T> ops, @NotNull T input)
	{
		return ops.get(input, "identifier")
			.flatMap(id -> ResourceLocation.CODEC.parse(ops, id))
			.map(ID_TO_CODEC_MAP::get)
			.flatMap(id -> id.codec().parse(ops, input))
			.map(result -> (DamageCalculator) result);
	}
	static <T, A extends DamageCalculator> DataResult<T> encodeDamageCalculator(@NotNull DynamicOps<T> ops, @NotNull A damageCalc)
	{
		DataResult<T> encodingDataResult = CodecUtils.dataResultOfOptional(
				(MapCodec<A>) ID_TO_CODEC_MAP.get(damageCalc.getClass()), () -> "DamageCalculator failed encoding, codec not registered.")
			.flatMap(codec -> codec.codec().encodeStart(ops, damageCalc));
		
		if (encodingDataResult.isError())
		{
			return encodingDataResult;
		}
		
		DataResult<T> idDataResult = CodecUtils.dataResultOfOptional(
				REGISTRY.getKey(damageCalc.getClass()), () -> "DamageCalculator failed encoding, id not registered.")
			.flatMap(id -> ResourceLocation.CODEC.encodeStart(ops, id));
		
		if (idDataResult.isError())
		{
			return encodingDataResult;
		}
		
		RecordBuilder<T> builder = ops.mapBuilder().add("identifier", idDataResult);
		return builder.build(encodingDataResult);
	}
	float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList extraDataList);
}
