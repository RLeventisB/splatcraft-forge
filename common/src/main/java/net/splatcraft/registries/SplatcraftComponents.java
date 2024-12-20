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
import net.splatcraft.util.InkColor;

import java.util.List;
import java.util.Objects;
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
    public static final ComponentType<ShotDeviationData> WEAPON_PRECISION_DATA = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Splatcraft.identifierOf("current_shot_deviation_data"),
        ComponentType.<ShotDeviationData>builder().codec(ShotDeviationData.CODEC).build()
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

    public static final class RemoteInfo
    {
        public static final Codec<RemoteInfo> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.STRING.optionalFieldOf("stage_id").forGetter(RemoteInfo::stageId),
            Codec.STRING.optionalFieldOf("dimension_id").forGetter(RemoteInfo::dimensionId),
            Codec.STRING.optionalFieldOf("targets").forGetter(RemoteInfo::dimensionId),
            BlockPos.CODEC.optionalFieldOf("point_a").forGetter(RemoteInfo::pointA),
            BlockPos.CODEC.optionalFieldOf("point_b").forGetter(RemoteInfo::pointB),
            Codec.INT.optionalFieldOf("mode_state", 0).forGetter(RemoteInfo::modeIndex)
        ).apply(builder, RemoteInfo::new));
        private Optional<String> stageId;
        private Optional<String> dimensionId;
        private Optional<String> targets;
        private Optional<BlockPos> pointA;
        private Optional<BlockPos> pointB;
        private int modeIndex;

        public RemoteInfo(
            Optional<String> stageId,
            Optional<String> dimensionId,
            Optional<String> targets,
            Optional<BlockPos> pointA,
            Optional<BlockPos> pointB,
            int modeIndex
        )
        {
            this.stageId = stageId;
            this.dimensionId = dimensionId;
            this.targets = targets;
            this.pointA = pointA;
            this.pointB = pointB;
            this.modeIndex = modeIndex;
        }

        public Optional<String> stageId() {return stageId;}

        public Optional<String> dimensionId() {return dimensionId;}

        public Optional<BlockPos> pointA() {return pointA;}

        public Optional<BlockPos> pointB() {return pointB;}

        public int modeIndex() {return modeIndex;}

        public Optional<String> targets() {return targets;}

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != getClass()) return false;
            var that = (RemoteInfo) obj;
            return
                Objects.equals(stageId, that.stageId) &&
                    Objects.equals(dimensionId, that.dimensionId) &&
                    Objects.equals(targets, that.targets) &&
                    Objects.equals(pointA, that.pointA) &&
                    Objects.equals(pointB, that.pointB) &&
                    modeIndex == that.modeIndex;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(stageId, dimensionId, targets, pointA, pointB, modeIndex);
        }

        @Override
        public String toString()
        {
            return "RemoteInfo[" +
                "stageId=" + stageId + ", " +
                "dimensionId=" + dimensionId + ", " +
                "targets=" + targets + ", " +
                "pointA=" + pointA + ", " +
                "pointB=" + pointB + ", " +
                "modeIndex=" + modeIndex + ']';
        }

        public void setStageId(String stageId)
        {
            this.stageId = Optional.ofNullable(stageId);
        }

        public void setDimensionId(String dimensionId)
        {
            this.dimensionId = Optional.ofNullable(dimensionId);
        }

        public void setTargets(String targets)
        {
            this.targets = Optional.ofNullable(targets);
        }

        public void setPointA(BlockPos pointA)
        {
            this.pointA = Optional.ofNullable(pointA);
        }

        public void setPointB(BlockPos pointB)
        {
            this.pointB = Optional.ofNullable(pointB);
        }

        public void setModeIndex(int modeIndex)
        {
            this.modeIndex = modeIndex;
        }
    }

    public static final class TankData
    {
        public static final Codec<TankData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.BOOL.optionalFieldOf("infinite_ink", false).forGetter(TankData::infiniteInk),
            Codec.BOOL.optionalFieldOf("hide_tooltip", false).forGetter(TankData::hideTooltip),
            Codec.FLOAT.optionalFieldOf("ink_level", 0f).forGetter(TankData::inkLevel),
            Codec.FLOAT.optionalFieldOf("ink_recovery_cooldown", 0f).forGetter(TankData::inkRecoveryCooldown)
        ).apply(builder, TankData::new));
        public static final TankData DEFAULT = new TankData(false, false, 0, 0);
        private boolean infiniteInk;
        private boolean hideTooltip;
        private float inkLevel;
        private float inkRecoveryCooldown;

        public TankData(
            boolean infiniteInk,
            boolean hideTooltip,
            float inkLevel,
            float inkRecoveryCooldown
        )
        {
            this.infiniteInk = infiniteInk;
            this.hideTooltip = hideTooltip;
            this.inkLevel = inkLevel;
            this.inkRecoveryCooldown = inkRecoveryCooldown;
        }

        public boolean infiniteInk() {return infiniteInk;}

        public boolean hideTooltip() {return hideTooltip;}

        public float inkLevel() {return inkLevel;}

        public float inkRecoveryCooldown() {return inkRecoveryCooldown;}

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != getClass()) return false;
            var that = (TankData) obj;
            return infiniteInk == that.infiniteInk &&
                hideTooltip == that.hideTooltip &&
                Float.floatToIntBits(inkLevel) == Float.floatToIntBits(that.inkLevel) &&
                Float.floatToIntBits(inkRecoveryCooldown) == Float.floatToIntBits(that.inkRecoveryCooldown);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(infiniteInk, hideTooltip, inkLevel, inkRecoveryCooldown);
        }

        @Override
        public String toString()
        {
            return "TankData[" +
                "infiniteInk=" + infiniteInk + ", " +
                "hideTooltip=" + hideTooltip + ", " +
                "inkLevel=" + inkLevel + ", " +
                "inkRecoveryCooldown=" + inkRecoveryCooldown + ']';
        }

        public TankData withInkRecoveryCooldown(float inkRecoveryCooldown)
        {
            this.inkRecoveryCooldown = inkRecoveryCooldown;
            return this;
        }

        public TankData withInkLevel(float inkLevel)
        {
            this.inkLevel = inkLevel;
            return this;
        }

        public TankData withHideTooltip(boolean hideTooltip)
        {
            this.hideTooltip = hideTooltip;
            return this;
        }

        public TankData withInfiniteInk(boolean infiniteInk)
        {
            this.infiniteInk = infiniteInk;
            return this;
        }
    }

    public static final class ShotDeviationData
    {
        public static final Codec<ShotDeviationData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.FLOAT.optionalFieldOf("chance_decrease_delay", 0f).forGetter(ShotDeviationData::chanceDecreaseDelay),
            Codec.FLOAT.optionalFieldOf("chance", 0f).forGetter(ShotDeviationData::chance),
            Codec.FLOAT.optionalFieldOf("airborne_decrease_delay", 0f).forGetter(ShotDeviationData::airborneDecreaseDelay),
            Codec.FLOAT.optionalFieldOf("airborne_influence", 0f).forGetter(ShotDeviationData::airborneInfluence)
        ).apply(builder, ShotDeviationData::new));
        private float chanceDecreaseDelay;
        private float chance;
        private float airborneDecreaseDelay;
        private float airborneInfluence;

        public ShotDeviationData(
            float chanceDecreaseDelay,
            float chance,
            float airborneDecreaseDelay,
            float airborneInfluence
        )
        {
            this.chanceDecreaseDelay = chanceDecreaseDelay;
            this.chance = chance;
            this.airborneDecreaseDelay = airborneDecreaseDelay;
            this.airborneInfluence = airborneInfluence;
        }

        public float chanceDecreaseDelay() {return chanceDecreaseDelay;}

        public float chance() {return chance;}

        public float airborneDecreaseDelay() {return airborneDecreaseDelay;}

        public float airborneInfluence() {return airborneInfluence;}

        @Override
        public String toString()
        {
            return "ShotDeviationData[" +
                "chanceDecreaseDelay=" + chanceDecreaseDelay + ", " +
                "chance=" + chance + ", " +
                "airborneDecreaseDelay=" + airborneDecreaseDelay + ", " +
                "airborneInfluence=" + airborneInfluence + ']';
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof ShotDeviationData that)) return false;
            return Float.compare(chanceDecreaseDelay, that.chanceDecreaseDelay) == 0 && Float.compare(chance, that.chance) == 0 && Float.compare(airborneDecreaseDelay, that.airborneDecreaseDelay) == 0 && Float.compare(airborneInfluence, that.airborneInfluence) == 0;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(chanceDecreaseDelay, chance, airborneDecreaseDelay, airborneInfluence);
        }

        public void setChanceDecreaseDelay(float chanceDecreaseDelay)
        {
            this.chanceDecreaseDelay = chanceDecreaseDelay;
        }

        public void setChance(float chance)
        {
            this.chance = chance;
        }

        public void setAirborneDecreaseDelay(float airborneDecreaseDelay)
        {
            this.airborneDecreaseDelay = airborneDecreaseDelay;
        }

        public void setAirborneInfluence(float airborneInfluence)
        {
            this.airborneInfluence = airborneInfluence;
        }

        public void cloneTo(ShotDeviationData other)
        {
            other.chanceDecreaseDelay = chanceDecreaseDelay;
            other.chance = chance;
            other.airborneDecreaseDelay = airborneDecreaseDelay;
            other.airborneInfluence = airborneInfluence;
        }

        public void registerJump(CommonRecords.ShotDeviationDataRecord shotDeviationData)
        {
            setAirborneDecreaseDelay(shotDeviationData.airborneContractDelay());
            setAirborneInfluence(1);
            setChance(shotDeviationData.deviationChanceWhenAirborne());
        }
    }

    public static final class ItemColorData
    {
        public static final Codec<ItemColorData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.BOOL.optionalFieldOf("color_immutable", false).forGetter(ItemColorData::isColorImmutable),
            Codec.BOOL.optionalFieldOf("inverted", false).forGetter(ItemColorData::inverted),
            InkColor.CODEC.optionalFieldOf("ink_color", InkColor.DEFAULT).forGetter(ItemColorData::inkColor)
        ).apply(builder, ItemColorData::new));
        public static final Supplier<ItemColorData> DEFAULT = () -> new ItemColorData(false, false, InkColor.DEFAULT);

        private boolean colorImmutable;

        private boolean inverted;

        private InkColor inkColor;

        public ItemColorData(boolean colorImmutable, boolean inverted, InkColor inkColor)
        {
            this.colorImmutable = colorImmutable;
            this.inverted = inverted;
            this.inkColor = inkColor;
        }

        public boolean isColorImmutable()
        {
            return colorImmutable;
        }

        public void setColorImmutable(boolean colorImmutable)
        {
            this.colorImmutable = colorImmutable;
        }

        public boolean inverted()
        {
            return inverted;
        }

        public void setInverted(boolean inverted)
        {
            this.inverted = inverted;
        }

        public InkColor inkColor()
        {
            return inkColor;
        }

        public void setInkColor(InkColor inkColor)
        {
            this.inkColor = inkColor;
        }

        @Override
        public String toString()
        {
            return "ItemColorData[" +
                "colorImmutable=" + colorImmutable + ", " +
                "inverted=" + inverted + ", " +
                "inkColor=" + inkColor + ']';
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof ItemColorData that)) return false;
            return colorImmutable == that.colorImmutable && inverted == that.inverted && Objects.equals(inkColor, that.inkColor);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(colorImmutable, inverted, inkColor);
        }

        public InkColor getEffectiveColor()
        {
            return InkColor.getIfInversed(inkColor, inverted);
        }
    }
}
