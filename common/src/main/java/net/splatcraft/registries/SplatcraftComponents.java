package net.splatcraft.registries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.splatcraft.Splatcraft;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class SplatcraftComponents
{
	public static final ComponentType<TankData> TANK_DATA = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("tank_data"),
		ComponentType.<TankData>builder().codec(TankData.CODEC).build()
	);
	public static final ComponentType<ItemColorData> ITEM_COLOR_DATA = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("item_color_data"),
		ComponentType.<ItemColorData>builder().codec(ItemColorData.CODEC).build()
	);
	public static final ComponentType<WeaponPrecisionData> WEAPON_PRECISION_DATA = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("current_weapon_precision_data"),
		ComponentType.<WeaponPrecisionData>builder().codec(WeaponPrecisionData.CODEC).build()
	);
	public static final ComponentType<Identifier> WEAPON_SETTING_ID = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("weapon_settings"),
		ComponentType.<Identifier>builder().codec(Identifier.CODEC).build()
	);
	public static final ComponentType<Boolean> SINGLE_USE = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("single_use"),
		ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
	);
	public static final ComponentType<String> TEAM_ID = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("team_id"),
		ComponentType.<String>builder().codec(Codec.STRING).build()
	);
	public static final ComponentType<RemoteInfo> REMOTE_INFO = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("remote_info"),
		ComponentType.<RemoteInfo>builder().codec(RemoteInfo.CODEC).build()
	);
	public static final ComponentType<Float> CHARGE = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("charge"),
		ComponentType.<Float>builder().codec(Codec.FLOAT).build()
	);
	public static final ComponentType<NbtCompound> SUB_WEAPON_DATA = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("sub_weapon_data"),
		ComponentType.<NbtCompound>builder().codec(NbtCompound.CODEC).build()
	);
	public static final ComponentType<Boolean> IS_PLURAL = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("is_plural"),
		ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
	);
	public static final ComponentType<List<String>> BLUEPRINT_WEAPONS = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("blueprint_weapons"),
		ComponentType.<List<String>>builder().codec(Codec.list(Codec.STRING)).build()
	);
	public static final ComponentType<List<Identifier>> BLUEPRINT_ADVANCEMENTS = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("blueprint_advancements"),
		ComponentType.<List<Identifier>>builder().codec(Codec.list(Identifier.CODEC)).build()
	);
	public static void initialize()
	{
		// oh, components get registered by the field
	}
	public static <T> DataResult<T> getComponent(ItemStack stack, ComponentType<T> type)
	{
		if (stack.getComponents().contains(type))
		{
			return DataResult.success(stack.getComponents().get(type));
		}
		return DataResult.error(() -> "This ItemStack doesn't have the specified component type");
	}
	public record RemoteInfo(Optional<String> stageId, Optional<String> dimensionId, Optional<String> targets,
	                         Optional<BlockPos> pointA, Optional<BlockPos> pointB, int modeIndex)
	{
		public static final Codec<RemoteInfo> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			Codec.STRING.optionalFieldOf("stage_id").forGetter(RemoteInfo::stageId),
			Codec.STRING.optionalFieldOf("dimension_id").forGetter(RemoteInfo::dimensionId),
			Codec.STRING.optionalFieldOf("targets").forGetter(RemoteInfo::dimensionId),
			BlockPos.CODEC.optionalFieldOf("point_a").forGetter(RemoteInfo::pointA),
			BlockPos.CODEC.optionalFieldOf("point_b").forGetter(RemoteInfo::pointB),
			Codec.INT.optionalFieldOf("mode_state", 0).forGetter(RemoteInfo::modeIndex)
		).apply(builder, RemoteInfo::new));
		public RemoteInfo setStageId(String stageId)
		{
			return new RemoteInfo(Optional.ofNullable(stageId), dimensionId, targets, pointA, pointB, modeIndex);
		}
		public RemoteInfo setDimensionId(String dimensionId)
		{
			return new RemoteInfo(stageId, Optional.ofNullable(dimensionId), targets, pointA, pointB, modeIndex);
		}
		public RemoteInfo setTargets(String targets)
		{
			return new RemoteInfo(stageId, dimensionId, Optional.ofNullable(targets), pointA, pointB, modeIndex);
		}
		public RemoteInfo setPointA(BlockPos pointA)
		{
			return new RemoteInfo(stageId, dimensionId, targets, Optional.ofNullable(pointA), pointB, modeIndex);
		}
		public RemoteInfo setPointB(BlockPos pointB)
		{
			return new RemoteInfo(stageId, dimensionId, targets, pointA, Optional.ofNullable(pointB), modeIndex);
		}
		public RemoteInfo setModeIndex(int modeIndex)
		{
			return new RemoteInfo(stageId, dimensionId, targets, pointA, pointB, modeIndex);
		}
	}
	public record TankData(boolean infiniteInk, boolean hideTooltip, float inkLevel, float inkRecoveryCooldown)
	{
		public static final Codec<TankData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			Codec.BOOL.optionalFieldOf("infinite_ink", false).forGetter(TankData::infiniteInk),
			Codec.BOOL.optionalFieldOf("hide_tooltip", false).forGetter(TankData::hideTooltip),
			Codec.FLOAT.optionalFieldOf("ink_level", 0f).forGetter(TankData::inkLevel),
			Codec.FLOAT.optionalFieldOf("ink_recovery_cooldown", 0f).forGetter(TankData::inkRecoveryCooldown)
		).apply(builder, TankData::new));
		public static final TankData DEFAULT = new TankData(false, false, 0, 0);
		public TankData withInkRecoveryCooldown(float inkRecoveryCooldown)
		{
			return new TankData(infiniteInk, hideTooltip, inkLevel, inkRecoveryCooldown);
		}
		public TankData withInkLevel(float inkLevel)
		{
			return new TankData(infiniteInk, hideTooltip, inkLevel, inkRecoveryCooldown);
		}
		public TankData withHideTooltip(boolean hideTooltip)
		{
			return new TankData(infiniteInk, hideTooltip, inkLevel, inkRecoveryCooldown);
		}
		public TankData withInfiniteInk(boolean infiniteInk)
		{
			return new TankData(infiniteInk, hideTooltip, inkLevel, inkRecoveryCooldown);
		}
	}
	public record WeaponPrecisionData(float chanceDecreaseDelay, float chance, float airborneDecreaseDelay,
	                                  float airborneInfluence)
	{
		public static final Codec<WeaponPrecisionData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			Codec.FLOAT.optionalFieldOf("chance_decrease_delay", 0f).forGetter(WeaponPrecisionData::chanceDecreaseDelay),
			Codec.FLOAT.optionalFieldOf("chance", 0f).forGetter(WeaponPrecisionData::chance),
			Codec.FLOAT.optionalFieldOf("airborne_decrease_delay", 0f).forGetter(WeaponPrecisionData::airborneDecreaseDelay),
			Codec.FLOAT.optionalFieldOf("airborne_influence", 0f).forGetter(WeaponPrecisionData::airborneInfluence)
		).apply(builder, WeaponPrecisionData::new));
		public static final WeaponPrecisionData DEFAULT = new WeaponPrecisionData(0, 0, 0, 0);
		public WeaponPrecisionData setChanceDecreaseDelay(float chanceDecreaseDelay)
		{
			return new WeaponPrecisionData(chanceDecreaseDelay, chance, airborneDecreaseDelay, airborneInfluence);
		}
		public WeaponPrecisionData setChance(float chance)
		{
			return new WeaponPrecisionData(chanceDecreaseDelay, chance, airborneDecreaseDelay, airborneInfluence);
		}
		public WeaponPrecisionData setAirborneDecreaseDelay(float airborneDecreaseDelay)
		{
			return new WeaponPrecisionData(chanceDecreaseDelay, chance, airborneDecreaseDelay, airborneInfluence);
		}
		public WeaponPrecisionData setAirborneInfluence(float airborneInfluence)
		{
			return new WeaponPrecisionData(chanceDecreaseDelay, chance, airborneDecreaseDelay, airborneInfluence);
		}
		public WeaponPrecisionData registerJump(CommonRecords.ShotDeviationDataRecord data)
		{
			return new WeaponPrecisionData(chanceDecreaseDelay, data.deviationChanceWhenAirborne(), data.airborneContractDelay(), 1);
		}
	}
	public record ItemColorData(boolean colorLocked, boolean hasInvertedColor, InkColor color)
	{
		public static final Codec<ItemColorData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			Codec.BOOL.optionalFieldOf("color_locked", false).forGetter(ItemColorData::colorLocked),
			Codec.BOOL.optionalFieldOf("inverted", false).forGetter(ItemColorData::hasInvertedColor),
			InkColor.CODEC.optionalFieldOf("color", ColorUtils.getDefaultColor()).forGetter(ItemColorData::color)
		).apply(builder, ItemColorData::new));
		public static final Supplier<ItemColorData> DEFAULT = () -> new ItemColorData(false, false, InkColor.INVALID);
		public ItemColorData withColorLocked(boolean colorLocked)
		{
			return new ItemColorData(colorLocked, hasInvertedColor, color);
		}
		public ItemColorData withInvertedColor(boolean inverted)
		{
			return new ItemColorData(colorLocked, inverted, color);
		}
		public ItemColorData withInkColor(InkColor inkColor)
		{
			return new ItemColorData(colorLocked, hasInvertedColor, inkColor);
		}
		public InkColor getEffectiveColor()
		{
			return InkColor.getIfInversed(color, hasInvertedColor);
		}
	}
}
