package net.splatcraft.registries;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.Splatcraft;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.items.weapons.subs.SubWeaponItem;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class SplatcraftComponents
{
	public static final DataComponentType<TankData> TANK_DATA = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("tank_data"),
		DataComponentType.<TankData>builder().persistent(TankData.CODEC).networkSynchronized(TankData.STREAM_CODEC).build()
	);
	public static final DataComponentType<ItemColorData> ITEM_COLOR_DATA = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("item_color_data"),
		DataComponentType.<ItemColorData>builder().persistent(ItemColorData.CODEC).networkSynchronized(ItemColorData.STREAM_CODEC).build()
	);
	public static final DataComponentType<WeaponPrecisionData> WEAPON_PRECISION_DATA = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("current_weapon_precision_data"),
		DataComponentType.<WeaponPrecisionData>builder().persistent(WeaponPrecisionData.CODEC).build()
	);
	public static final DataComponentType<ShooterFiringData> SHOOTER_FIRING_DATA = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("shooter_firing_data"),
		DataComponentType.<ShooterFiringData>builder().persistent(ShooterFiringData.CODEC).cacheEncoding().build()
	);
	public static final DataComponentType<ResourceLocation> WEAPON_SETTING_ID = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("settings_id"),
		DataComponentType.<ResourceLocation>builder().persistent(ResourceLocation.CODEC).build()
	);
	public static final DataComponentType<ResourceKey<EntityType<?>>> SUB_WEAPON_ENTITY_ID = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("sub_entity_id"),
		DataComponentType.<ResourceKey<EntityType<?>>>builder().persistent(ResourceKey.codec(Registries.ENTITY_TYPE)).build()
	);
	public static final DataComponentType<Boolean> SINGLE_USE = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("single_use"),
		DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build()
	);
	public static final DataComponentType<String> TEAM_ID = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("team_id"),
		DataComponentType.<String>builder().persistent(Codec.STRING).build()
	);
	public static final DataComponentType<RemoteInfo> REMOTE_INFO = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("remote_info"),
		DataComponentType.<RemoteInfo>builder().persistent(RemoteInfo.CODEC).build()
	);
	public static final DataComponentType<Float> CHARGE = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("charge"),
		DataComponentType.<Float>builder().persistent(Codec.FLOAT).build()
	);
	public static final DataComponentType<CompoundTag> SUB_WEAPON_DATA = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("sub_weapon_data"),
		DataComponentType.<CompoundTag>builder().persistent(CompoundTag.CODEC).build()
	);
	public static final DataComponentType<Boolean> IS_PLURAL = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("is_plural"),
		DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build()
	);
	public static final DataComponentType<List<String>> BLUEPRINT_WEAPONS = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("blueprint_weapons"),
		DataComponentType.<List<String>>builder().persistent(Codec.list(Codec.STRING)).build()
	);
	public static final DataComponentType<List<ResourceLocation>> BLUEPRINT_ADVANCEMENTS = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("blueprint_advancements"),
		DataComponentType.<List<ResourceLocation>>builder().persistent(Codec.list(ResourceLocation.CODEC)).build()
	);
	public static final DataComponentType<SpecialProviderData> SPECIAL_PROVIDER_DATA = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("special_provider_data"),
		DataComponentType.<SpecialProviderData>builder().persistent(SpecialProviderData.CODEC).build()
	);
	public static <T> Optional<T> getOptional(ItemStack stack, DataComponentType<T> type)
	{
		return Optional.ofNullable(stack.get(type));
	}
	public static <T> void applyToComponentIfContains(ItemStack stack, DataComponentType<T> type, Function<T, T> applier)
	{
		getOptional(stack, type).ifPresent(component -> stack.set(type, applier.apply(component)));
	}
	public interface FiringData<SELF extends FiringData<SELF>>
	{
		static <T extends FiringData> Products.P5<RecordCodecBuilder.Mu<T>, Float, Float, Float, Float, Boolean> codecStart(RecordCodecBuilder.Instance<T> instance)
		{
			return instance.group(
				Codec.FLOAT.fieldOf("counter").forGetter(FiringData::counter),
				Codec.FLOAT.fieldOf("startup_time").forGetter(FiringData::startupTime),
				Codec.FLOAT.fieldOf("repeat_time").forGetter(FiringData::repeatTime),
				Codec.FLOAT.fieldOf("endlag_time").forGetter(FiringData::endlagTime),
				Codec.BOOL.fieldOf("repeat_queued").forGetter(FiringData::isRepeating)
			);
		}

		default SELF tick(TimeAwareAction<SELF> onAction)
		{
			return tick(onAction, v -> (y -> y), 1f);
		}
		default SELF tick(TimeAwareAction<SELF> onAction, TimeAwareAction<SELF> onActionDone)
		{
			return tick(onAction, onActionDone, 1f);
		}
		default SELF tick(TimeAwareAction<SELF> onAction, TimeAwareAction<SELF> onActionDone, float timeDelta)
		{
			SELF self = (SELF) this;
			if (Float.isNaN(counter()))
				return self;

			// behavior: if time > 0, its the first iteration, after that its kept in the interval [0, -repeatTime[ if the is repeating flag is on
			// if the is repeating flag is off, when the counter reaches -repeatTime - endlagTime the action is done!!

			float nextTime = counter() - timeDelta;
			if (self.counter() > 0 && nextTime <= 0) // first iteration
			{
				self = onAction.run(-nextTime).apply(self);
			}
			while (self.isRepeating() && nextTime <= -self.repeatTime()) // repeating iteration
			{
				nextTime += self.repeatTime();
				self = onAction.run(-nextTime).apply(self);
			}
			if (nextTime <= -self.repeatTime() - self.endlagTime()) // last iteration
			{
				self = onActionDone.run(-(nextTime + self.repeatTime() + self.endlagTime())).apply(self);
				return self.withCounter(Float.NaN);
			}

			return self.withCounter(nextTime);
		}
		boolean isRepeating();
		float startupTime();
		float repeatTime();
		float endlagTime();
		float counter();
		SELF withRepeatingFlag(boolean repeating);
		SELF withStartupTime(float startupTime);
		SELF withRepeatTime(float repeatTime);
		SELF withEndlagTime(float endlagTime);
		SELF withCounter(float counter);
		boolean preventsChanging();
		default SELF initialize(float counter, float startupTime, float repeatTime, float endlagTime)
		{
			return initialize(counter, startupTime, repeatTime, endlagTime, true);
		}
		SELF initialize(float counter, float startupTime, float repeatTime, float endlagTime, boolean withRepeatingFlag);
		@FunctionalInterface
		interface TimeAwareAction<SELF>
		{
			UnaryOperator<SELF> run(float extraTime);
		}
	}
	public record ShooterFiringData(float counter, float startupTime, float repeatTime, float endlagTime,
	                                boolean isRepeating) implements FiringData<ShooterFiringData>
	{

		public static final Codec<ShooterFiringData> CODEC = RecordCodecBuilder.create(inst ->
			FiringData.codecStart(inst).
				apply(inst, ShooterFiringData::new)
		);
		public static final ShooterFiringData DEFAULT = new ShooterFiringData(0, 1, 1, 1, false);
		@Override
		public ShooterFiringData withRepeatingFlag(boolean repeating)
		{
			return new ShooterFiringData(counter, startupTime, repeatTime, endlagTime, repeating);
		}
		@Override
		public ShooterFiringData withStartupTime(float startupTime)
		{
			return new ShooterFiringData(counter, startupTime, repeatTime, endlagTime, isRepeating);
		}
		@Override
		public ShooterFiringData withRepeatTime(float repeatTime)
		{
			return new ShooterFiringData(counter, startupTime, repeatTime, endlagTime, isRepeating);
		}
		@Override
		public ShooterFiringData withEndlagTime(float endlagTime)
		{
			return new ShooterFiringData(counter, startupTime, repeatTime, endlagTime, isRepeating);
		}
		@Override
		public ShooterFiringData withCounter(float counter)
		{
			return new ShooterFiringData(counter, startupTime, repeatTime, endlagTime, isRepeating);
		}
		@Override
		public boolean preventsChanging()
		{
			return !Float.isNaN(counter);
		}
		@Override
		public ShooterFiringData initialize(float counter, float startupTime, float repeatTime, float endlagTime, boolean withRepeatingFlag)
		{
			return new ShooterFiringData(counter, startupTime, repeatTime, endlagTime, withRepeatingFlag);
		}
		public ShooterFiringData notifyUsing(LivingEntity entity, CommonRecords.ShotDataRecord settings)
		{
			if (!Float.isNaN(counter))
				return this;

			float startup = CommonUtils.startupSquidSwitch(entity, settings);
			return initialize(startup, startup, settings.repeatTicks(), settings.endlagTicks());
		}
	}
	public record RemoteInfo(Optional<String> stageId, Optional<ResourceKey<Level>> worldKey, Optional<String> targets,
	                         Optional<BlockPos> pointA, Optional<BlockPos> pointB, int modeIndex)
	{
		public static final Codec<RemoteInfo> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			Codec.STRING.optionalFieldOf("stage_id").forGetter(RemoteInfo::stageId),
			ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("world_key").forGetter(RemoteInfo::worldKey),
			Codec.STRING.optionalFieldOf("targets").forGetter(RemoteInfo::targets),
			BlockPos.CODEC.optionalFieldOf("point_a").forGetter(RemoteInfo::pointA),
			BlockPos.CODEC.optionalFieldOf("point_b").forGetter(RemoteInfo::pointB),
			Codec.INT.optionalFieldOf("mode_state", 0).forGetter(RemoteInfo::modeIndex)
		).apply(builder, RemoteInfo::new));
		public static final RemoteInfo DEFAULT = new RemoteInfo(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0);
		public RemoteInfo setStageId(String stageId)
		{
			return new RemoteInfo(Optional.ofNullable(stageId), worldKey, targets, pointA, pointB, modeIndex);
		}
		public RemoteInfo setWorldKey(ResourceKey<Level> worldKey)
		{
			return new RemoteInfo(stageId, Optional.ofNullable(worldKey), targets, pointA, pointB, modeIndex);
		}
		public RemoteInfo setTargets(String targets)
		{
			return new RemoteInfo(stageId, worldKey, Optional.ofNullable(targets), pointA, pointB, modeIndex);
		}
		public RemoteInfo setPointA(BlockPos pointA)
		{
			return new RemoteInfo(stageId, worldKey, targets, Optional.ofNullable(pointA), pointB, modeIndex);
		}
		public RemoteInfo setPointB(BlockPos pointB)
		{
			return new RemoteInfo(stageId, worldKey, targets, pointA, Optional.ofNullable(pointB), modeIndex);
		}
		public RemoteInfo setModeIndex(int modeIndex)
		{
			return new RemoteInfo(stageId, worldKey, targets, pointA, pointB, modeIndex);
		}
		public RemoteInfo setPoint(BlockPos pos)
		{
			if (pointA.isEmpty())
				return setPointA(pos);
			return setPointB(pos);
		}
	}
	public record TankData(boolean infiniteInk, boolean hideTooltip, float inkLevel, float maxCapacity,
	                       float inkRecoveryCooldown)
	{
		public static final Codec<TankData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			Codec.BOOL.optionalFieldOf("infinite_ink", false).forGetter(TankData::infiniteInk),
			Codec.BOOL.optionalFieldOf("hide_tooltip", false).forGetter(TankData::hideTooltip),
			Codec.FLOAT.optionalFieldOf("ink_level", 0f).forGetter(TankData::inkLevel),
			Codec.FLOAT.optionalFieldOf("max_capacity", 100f).forGetter(TankData::maxCapacity),
			Codec.FLOAT.optionalFieldOf("ink_recovery_cooldown", 0f).forGetter(TankData::inkRecoveryCooldown)
		).apply(builder, TankData::new));
		public static final TankData DEFAULT = new TankData(false, false, 0, 100, 0);
		public static final StreamCodec<? super RegistryFriendlyByteBuf, TankData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, TankData::infiniteInk,
			ByteBufCodecs.BOOL, TankData::hideTooltip,
			ByteBufCodecs.FLOAT, TankData::inkLevel,
			ByteBufCodecs.FLOAT, TankData::maxCapacity,
			ByteBufCodecs.FLOAT, TankData::inkRecoveryCooldown,
			TankData::new
		);
		public TankData withInkRecoveryCooldown(float inkRecoveryCooldown)
		{
			return new TankData(infiniteInk, hideTooltip, inkLevel, maxCapacity, inkRecoveryCooldown);
		}
		public TankData withInkLevel(float inkLevel)
		{
			return new TankData(infiniteInk, hideTooltip, inkLevel, maxCapacity, inkRecoveryCooldown);
		}
		public TankData withInkLevelClamped(float inkLevel)
		{
			return new TankData(infiniteInk, hideTooltip, Math.min(inkLevel, maxCapacity), maxCapacity, inkRecoveryCooldown);
		}
		public TankData withMaxCapacity(float maxCapacity)
		{
			return new TankData(infiniteInk, hideTooltip, inkLevel, maxCapacity, inkRecoveryCooldown);
		}
		public TankData withHideTooltip(boolean hideTooltip)
		{
			return new TankData(infiniteInk, hideTooltip, inkLevel, maxCapacity, inkRecoveryCooldown);
		}
		public TankData withInfiniteInk(boolean infiniteInk)
		{
			return new TankData(infiniteInk, hideTooltip, inkLevel, maxCapacity, inkRecoveryCooldown);
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
		public WeaponPrecisionData withChanceDecreaseDelay(float chanceDecreaseDelay)
		{
			return new WeaponPrecisionData(chanceDecreaseDelay, chance, airborneDecreaseDelay, airborneInfluence);
		}
		public WeaponPrecisionData withChance(float chance)
		{
			return new WeaponPrecisionData(chanceDecreaseDelay, chance, airborneDecreaseDelay, airborneInfluence);
		}
		public WeaponPrecisionData withAirborneDecreaseDelay(float airborneDecreaseDelay)
		{
			return new WeaponPrecisionData(chanceDecreaseDelay, chance, airborneDecreaseDelay, airborneInfluence);
		}
		public WeaponPrecisionData withAirborneInfluence(float airborneInfluence)
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
			InkColor.RAW_INT_CODEC.optionalFieldOf("color", InkColor.INVALID).forGetter(ItemColorData::color)
		).apply(builder, ItemColorData::new));
		public static final ItemColorData DEFAULT = new ItemColorData(false, false, InkColor.INVALID);
		public static final StreamCodec<RegistryFriendlyByteBuf, ItemColorData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, ItemColorData::colorLocked,
			ByteBufCodecs.BOOL, ItemColorData::hasInvertedColor,
			InkColor.PACKET_CODEC, ItemColorData::color,
			ItemColorData::new
		);
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
		public InkColor getEffectiveColor(Entity entity)
		{
			if (color == InkColor.INVALID)
			{
				return InkColor.getIfInversed(ColorUtils.getEntityColor(entity), hasInvertedColor);
			}
			return InkColor.getIfInversed(color, hasInvertedColor);
		}
	}
	public record SpecialProviderData(
		Optional<ResourceLocation> specialId,
		Optional<ResourceLocation> weaponIdFilter,
		Optional<Integer> pointsPerSpecialOverride,
		boolean allowSubs,
		int storedPoints
	)
	{
		public static final Codec<SpecialProviderData> CODEC = RecordCodecBuilder.create(
			inst -> inst.group(
				CodecUtils.Codecs.SPLATCRAFT_IDENTIFIER_CODEC.optionalFieldOf("special_id").forGetter(SpecialProviderData::specialId),
				CodecUtils.Codecs.SPLATCRAFT_IDENTIFIER_CODEC.optionalFieldOf("weapon_id_filter").forGetter(SpecialProviderData::weaponIdFilter),
				Codec.INT.optionalFieldOf("points_per_special_override").forGetter(SpecialProviderData::pointsPerSpecialOverride),
				Codec.BOOL.optionalFieldOf("allow_subs", true).forGetter(SpecialProviderData::allowSubs),
				Codec.INT.optionalFieldOf("stored_points", 0).forGetter(SpecialProviderData::storedPoints)
			).apply(inst, SpecialProviderData::new)
		);
		public static final SpecialProviderData DEFAULT = new SpecialProviderData(Optional.empty(), Optional.empty(), Optional.empty(), true, 0);
		public boolean testWeapon(ItemStack stack)
		{
			if (stack.isEmpty())
				return false;

			if (weaponIdFilter.isEmpty())
				return true;

			if (!(stack.getItem() instanceof WeaponBaseItem<?> weaponItem))
				return false;

			if (allowSubs && weaponItem instanceof SubWeaponItem<?>)
				return true;

			ResourceLocation weaponId = weaponItem.getSettingsAndValidId(stack).getFirst();
			if (weaponId == null)
				return false;

			return Objects.equals(weaponIdFilter.get(), weaponId);
		}
		public String getSpecialTranslationKey()
		{
			return specialId.map(identifier -> "special_weapon." + identifier.toLanguageKey()).orElse("special_weapon.none");
		}
		public Component getSpecialText()
		{
			return Component.translatable(getSpecialTranslationKey());
		}
		public Component getWeaponText()
		{
			return Component.translatable(weaponIdFilter.get().toLanguageKey("item"));
		}
		public SpecialProviderData withSpecialId(ResourceLocation id)
		{
			return new SpecialProviderData(Optional.ofNullable(id), weaponIdFilter, pointsPerSpecialOverride, allowSubs, storedPoints);
		}
		public SpecialProviderData withWeaponIdFilter(ResourceLocation id)
		{
			return new SpecialProviderData(specialId, Optional.ofNullable(id), pointsPerSpecialOverride, allowSubs, storedPoints);
		}
		public SpecialProviderData withOverridenSpecialCost(int cost)
		{
			return new SpecialProviderData(specialId, weaponIdFilter, Optional.of(cost), allowSubs, storedPoints);
		}
		public SpecialProviderData withStoredPoints(int points)
		{
			return new SpecialProviderData(specialId, weaponIdFilter, pointsPerSpecialOverride, allowSubs, points);
		}
		public SpecialProviderData withAllowedSubs(boolean allowedSubs)
		{
			return new SpecialProviderData(specialId, weaponIdFilter, pointsPerSpecialOverride, allowedSubs, storedPoints);
		}
		public SpecialProviderData incrementStoredPoints(int points)
		{
			return withStoredPoints(storedPoints + points);
		}
	}
}
