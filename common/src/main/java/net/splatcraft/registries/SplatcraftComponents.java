package net.splatcraft.registries;

import com.google.common.base.Predicates;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.handlers.SpecialHandler;
import net.splatcraft.handlers.WeaponHandler;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.ChargerWeaponSettings;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.items.weapons.settings.SplatlingWeaponSettings;
import net.splatcraft.items.weapons.subs.SubWeaponItem;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class SplatcraftComponents
{
	public static final DataComponentType<TankData> TANK_DATA = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("tank_data"),
		DataComponentType.<TankData>builder().networkSynchronized(TankData.STREAM_CODEC).persistent(TankData.CODEC).build()
	);
	public static final DataComponentType<ItemColorData> ITEM_COLOR_DATA = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("item_color_data"),
		DataComponentType.<ItemColorData>builder().networkSynchronized(ItemColorData.STREAM_CODEC).persistent(ItemColorData.CODEC).build()
	);
	public static final DataComponentType<WeaponPrecisionData> WEAPON_PRECISION_DATA = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("current_weapon_precision_data"),
		DataComponentType.<WeaponPrecisionData>builder().networkSynchronized(WeaponPrecisionData.STREAM_CODEC).persistent(WeaponPrecisionData.CODEC).build()
	);
	public static final DataComponentType<ShooterFiringData> SHOOTER_FIRING_DATA = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("shooter_firing_data"),
		DataComponentType.<ShooterFiringData>builder().networkSynchronized(ShooterFiringData.STREAM_CODEC).persistent(ShooterFiringData.CODEC).cacheEncoding().build()
	);
	public static final DataComponentType<ChargerFiringData> CHARGER_FIRING_DATA = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("charger_firing_data"),
		DataComponentType.<ChargerFiringData>builder().networkSynchronized(ChargerFiringData.STREAM_CODEC).persistent(ChargerFiringData.CODEC).cacheEncoding().build()
	);
	public static final DataComponentType<SplatlingFiringData> SPLATLING_FIRING_DATA = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("splatling_firing_data"),
		DataComponentType.<SplatlingFiringData>builder().networkSynchronized(SplatlingFiringData.STREAM_CODEC).persistent(SplatlingFiringData.CODEC).cacheEncoding().build()
	);
	public static final DataComponentType<ResourceLocation> WEAPON_SETTING_ID = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("settings_id"),
		DataComponentType.<ResourceLocation>builder().networkSynchronized(ResourceLocation.STREAM_CODEC).persistent(ResourceLocation.CODEC).build()
	);
	public static final DataComponentType<ResourceKey<EntityType<?>>> SUB_WEAPON_ENTITY_ID = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("sub_entity_id"),
		DataComponentType.<ResourceKey<EntityType<?>>>builder().networkSynchronized(ResourceKey.streamCodec(Registries.ENTITY_TYPE)).persistent(ResourceKey.codec(Registries.ENTITY_TYPE)).build()
	);
	public static final DataComponentType<Boolean> SINGLE_USE = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("single_use"),
		DataComponentType.<Boolean>builder().networkSynchronized(ByteBufCodecs.BOOL).persistent(Codec.BOOL).build()
	);
	public static final DataComponentType<String> TEAM_ID = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("team_id"),
		DataComponentType.<String>builder().networkSynchronized(ByteBufCodecs.STRING_UTF8).persistent(Codec.STRING).build()
	);
	public static final DataComponentType<RemoteInfo> REMOTE_INFO = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("remote_info"),
		DataComponentType.<RemoteInfo>builder().networkSynchronized(RemoteInfo.STREAM_CODEC).persistent(RemoteInfo.CODEC).build()
	);
	public static final DataComponentType<ChargeData> CHARGE_DATA = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("charge"),
		DataComponentType.<ChargeData>builder().networkSynchronized(ChargeData.STREAM_CODEC).persistent(ChargeData.CODEC).build()
	);
	public static final DataComponentType<CompoundTag> SUB_WEAPON_DATA = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("sub_weapon_data"),
		DataComponentType.<CompoundTag>builder().networkSynchronized(ByteBufCodecs.COMPOUND_TAG).persistent(CompoundTag.CODEC).build()
	);
	public static final DataComponentType<Boolean> IS_PLURAL = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("is_plural"),
		DataComponentType.<Boolean>builder().networkSynchronized(ByteBufCodecs.BOOL).persistent(Codec.BOOL).build()
	);
	public static final DataComponentType<List<String>> BLUEPRINT_WEAPONS = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("blueprint_weapons"),
		DataComponentType.<List<String>>builder().networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).persistent(Codec.list(Codec.STRING)).build()
	);
	public static final DataComponentType<List<ResourceLocation>> BLUEPRINT_ADVANCEMENTS = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("blueprint_advancements"),
		DataComponentType.<List<ResourceLocation>>builder().networkSynchronized(ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list())).persistent(Codec.list(ResourceLocation.CODEC)).build()
	);
	public static final DataComponentType<SpecialProviderData> SPECIAL_PROVIDER_DATA = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Splatcraft.identifierOf("special_provider_data"),
		DataComponentType.<SpecialProviderData>builder().networkSynchronized(SpecialProviderData.STREAM_CODEC).persistent(SpecialProviderData.CODEC).build()
	);
	public static <T> Optional<T> getOptional(ItemStack stack, DataComponentType<T> type)
	{
		return Optional.ofNullable(stack.get(type));
	}
	public static <T> void applyToComponentIfContains(ItemStack stack, DataComponentType<T> type, Function<T, T> applier)
	{
		getOptional(stack, type).ifPresent(component -> stack.set(type, applier.apply(component)));
	}
	public record ChargerFiringData(float counter, boolean charging, boolean queuedShot)
	{
		public static final ChargerFiringData DEFAULT = new ChargerFiringData(Float.NaN, false, false);
		public static final Codec<ChargerFiringData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.FLOAT.fieldOf("counter").forGetter(ChargerFiringData::counter),
			Codec.BOOL.fieldOf("charging").forGetter(ChargerFiringData::charging),
			Codec.BOOL.fieldOf("queued_shot").forGetter(ChargerFiringData::queuedShot)
		).apply(inst, ChargerFiringData::new));
		public static final StreamCodec<ByteBuf, ChargerFiringData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, ChargerFiringData::counter,
			ByteBufCodecs.BOOL, ChargerFiringData::charging,
			ByteBufCodecs.BOOL, ChargerFiringData::queuedShot,
			ChargerFiringData::new
		);
		public ChargerFiringData tick(LivingEntity entity, ItemStack stack, ChargerWeaponSettings settings, float chargeMult, BiConsumer<Float, Float> onCharge, TimeAwareAction onRelease)
		{
			return tick(entity, stack, settings, chargeMult, onCharge, onRelease, 1);
		}
		public ChargerFiringData tick(LivingEntity entity, ItemStack stack, ChargerWeaponSettings settings, float chargeMult, BiConsumer<Float, Float> onCharge, TimeAwareAction onRelease, float timeDelta)
		{
			if (Float.isNaN(counter()))
				return this;
			
			// if the counter is higher than 0, it acts as a "delay" to charging, otherwise if the counter is less than 0 its because the weapon is on endlag, otherwise, the weapon is charging
			if (counter < 0)
			{
				stack.update(CHARGE_DATA, ChargeData.DEFAULT, v -> v.updateCharge(0).registerChargeDeltaTime(1));
				float nextCounter = counter + timeDelta;
				if (nextCounter >= 0)
				{
					if (queuedShot)
						return new ChargerFiringData(settings.chargeData.chargeStartup() - nextCounter, true, false);
					return new ChargerFiringData(Float.NaN, false, false);
				}
				return new ChargerFiringData(nextCounter, true, false);
			}
			
			boolean newCharging = charging && WeaponHandler.canContinueShooting(entity);
			
			if (counter > 0)
			{
				float nextCounter = counter - timeDelta;
				if (nextCounter < 0)
				{
					timeDelta += nextCounter;
				}
				else if (nextCounter > 0)
				{
					return new ChargerFiringData(nextCounter, newCharging, queuedShot);
				}
			}
			
			float charge = stack.get(CHARGE_DATA).charge;
			if (charge < 1)
			{
				if (charge == 0 && !newCharging) // tap shot
				{
					onCharge.accept(0f, 0f);
					
					return fireShotAndGoToEndlag(settings, onRelease, stack, 0, -(counter - timeDelta), 0);
				}
				float chargeTime = settings.chargeData.chargeTime();
				float chargeStep = (chargeMult * timeDelta) / chargeTime;
				float nextCharge = charge + chargeStep;
				float cutoffTime;
				if (nextCharge > 1)
				{
					cutoffTime = Mth.inverseLerp(1, charge, nextCharge);
					nextCharge = 1;
				}
				else
				{
					cutoffTime = 1f;
				}
				
				float finalNextCharge = nextCharge;
				stack.update(CHARGE_DATA, ChargeData.DEFAULT, v -> v.updateCharge(finalNextCharge).registerChargeDeltaTime(cutoffTime));
				
				onCharge.accept(charge, nextCharge);
				
				if (!newCharging)
				{
					return fireShotAndGoToEndlag(settings, onRelease, stack, nextCharge, timeDelta, cutoffTime);
				}
			}
			else
			{
				onCharge.accept(1f, 1f);
				
				stack.update(CHARGE_DATA, ChargeData.DEFAULT, v -> v.updateCharge(1).registerChargeDeltaTime(1));
				
				if (!newCharging)
				{
					return fireShotAndGoToEndlag(settings, onRelease, stack, 1, 0, 1f);
				}
			}
			
			return new ChargerFiringData(0, true, queuedShot);
		}
		public ChargerFiringData retrieveCharge(ChargerWeaponSettings settings)
		{
			return new ChargerFiringData(settings.chargeData.chargeStorageShootLag(), true, false);
		}
		public @NotNull ChargerFiringData fireShotAndGoToEndlag(ChargerWeaponSettings settings, TimeAwareAction onRelease, ItemStack stack, float charge, float extraTime, float chargeDeltaTime)
		{
			onRelease.run(charge, extraTime);
			stack.update(CHARGE_DATA, ChargeData.DEFAULT, v -> v.updateCharge(0).registerChargeDeltaTime(chargeDeltaTime));
			return new ChargerFiringData(-settings.shotData.endlagTicks() + extraTime, false, queuedShot);
		}
		public ChargerFiringData notifyUsage(LivingEntity entity, ChargerWeaponSettings settings)
		{
			if (!WeaponHandler.canContinueShooting(entity))
				return this;
			
			if (Float.isNaN(counter))
				return new ChargerFiringData(settings.chargeData.chargeStartup(), true, true);
			
			if (counter < 0)
				return new ChargerFiringData(counter, false, true);
			
			if (!charging)
				return new ChargerFiringData(counter, false, true);
			return new ChargerFiringData(counter, true, false);
		}
		public boolean preventsChanging()
		{
			return !Float.isNaN(counter);
		}
		@FunctionalInterface
		public interface TimeAwareAction
		{
			void run(float charge, float extraTime);
		}
	}
	public record SplatlingFiringData(float counter, Optional<Boolean> charging, float delay, short shotTypeData,
	                                  float chargeStart)
	{
		public static final SplatlingFiringData DEFAULT = new SplatlingFiringData(Float.NaN, Optional.empty(), 0f, (short) 0, 0f);
		public static final Codec<SplatlingFiringData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.FLOAT.fieldOf("counter").forGetter(SplatlingFiringData::counter),
			Codec.BOOL.optionalFieldOf("charging").forGetter(SplatlingFiringData::charging),
			Codec.FLOAT.fieldOf("delay").forGetter(SplatlingFiringData::delay),
			Codec.SHORT.fieldOf("shot_type_data").forGetter(SplatlingFiringData::shotTypeData),
			Codec.FLOAT.fieldOf("charge_start").forGetter(SplatlingFiringData::chargeStart)
		).apply(inst, SplatlingFiringData::new));
		public static final StreamCodec<ByteBuf, SplatlingFiringData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, SplatlingFiringData::counter,
			ByteBufCodecs.optional(ByteBufCodecs.BOOL), SplatlingFiringData::charging,
			ByteBufCodecs.FLOAT, SplatlingFiringData::delay,
			ByteBufCodecs.SHORT, SplatlingFiringData::shotTypeData,
			ByteBufCodecs.FLOAT, SplatlingFiringData::chargeStart,
			SplatlingFiringData::new
		);
		public SplatlingFiringData tick(LivingEntity entity, ItemStack stack, SplatlingWeaponSettings settings, float chargeMult, BiConsumer<Float, Float> onCharge, TriConsumer<Float, Float, Short> onChargeRelease, Consumer<Float> onFiringEnd, SplatlingShootAction onShoot)
		{
			return tick(entity, stack, settings, chargeMult, onCharge, onChargeRelease, onFiringEnd, onShoot, 1);
		}
		public SplatlingFiringData tick(LivingEntity entity, ItemStack stack, SplatlingWeaponSettings<?> settings, float chargeMult, BiConsumer<Float, Float> onCharge, TriConsumer<Float, Float, Short> onChargeRelease, Consumer<Float> onFiringEnd, SplatlingShootAction onShoot, float timeDelta)
		{
			if (charging.isEmpty())
			{
				if (stack.get(CHARGE_DATA).charge > 0)
					stack.update(CHARGE_DATA, ChargeData.DEFAULT, v -> v.updateCharge(0).registerChargeDeltaTime(1));
				if (settings.getDynamicDataKey() == SplatlingWeaponSettings.ShotDataSelectorType.TIME_USED)
				{
					if (EntityInfoCapability.isSquid(entity))
					{
						return new SplatlingFiringData(counter, Optional.empty(), delay, (short) 0, chargeStart);
					}
					if (shotTypeData > 0)
					{
						return new SplatlingFiringData(counter, Optional.empty(), delay, (short) (shotTypeData - 1), chargeStart);
					}
				}
				return this;
			}
			short nextShotTypeData = shotTypeData;
			boolean previousCharging = charging.get();
			boolean using = WeaponHandler.canContinueShooting(entity) || counter < 0f;
			boolean nextCharging = previousCharging && using;
			
			if (nextCharging)
			{
				return chargeSplatling(stack, settings, chargeMult, onCharge, timeDelta, entity.getUseItemRemainingTicks() % 2 == 0);
			}
			else
			{
				if (previousCharging) // just started shooting
				{
					float charge = stack.get(CHARGE_DATA).charge;
					if (settings.getDynamicDataKey() == SplatlingWeaponSettings.ShotDataSelectorType.STATIC)
					{
						nextShotTypeData = SplatlingWeaponSettings.getShotIndexFromCharge(charge);
					}
					onChargeRelease.accept(charge, chargeStart, nextShotTypeData);
				}
				
				return fireSplatling(stack, settings, onShoot, timeDelta, nextShotTypeData, onFiringEnd);
			}
		}
		private @NotNull SplatlingFiringData chargeSplatling(ItemStack stack, SplatlingWeaponSettings<?> settings, float chargeMult, BiConsumer<Float, Float> onCharge, float timeDelta, boolean evenTick)
		{
			short nextShotTypeData = shotTypeData;
			if (settings.getDynamicDataKey() == SplatlingWeaponSettings.ShotDataSelectorType.TIME_USED && nextShotTypeData > 0)
			{
//				if (evenTick)
				{
					nextShotTypeData--;
				}
			}
			else
			{
				nextShotTypeData = 0;
			}
			
			AtomicReference<Float> nextDelay = new AtomicReference<>(0f);
			stack.update(CHARGE_DATA, ChargeData.DEFAULT, v ->
			{
				float charge = v.charge;
				if (charge >= 2)
				{
					onCharge.accept(2f, 2f);
					return v.updateCharge(2).registerChargeDeltaTime(1);
				}
				
				float nextCharge = charge + settings.chargeData.getChargeStep(charge) * chargeMult;
				float cutoffTime;
				if (nextCharge > 2)
				{
					cutoffTime = Mth.inverseLerp(2, charge, nextCharge);
					nextDelay.set(-cutoffTime);
					nextCharge = 2;
				}
				else
				{
					cutoffTime = 1f;
				}
				
				onCharge.accept(charge, nextCharge);
				return v.updateCharge(nextCharge).registerChargeDeltaTime(cutoffTime);
			});
			
			float nextCounter = counter;
			if (nextCounter < 0)
				nextCounter = Math.min(0, nextCounter + timeDelta);
			
			return new SplatlingFiringData(nextCounter, Optional.of(true), nextDelay.get(), nextShotTypeData, chargeStart);
		}
		private @NotNull SplatlingFiringData fireSplatling(ItemStack stack, SplatlingWeaponSettings settings, SplatlingShootAction onShoot, float timeDelta, short nextShotTypeData, Consumer<Float> onFiringEnd)
		{
			ChargeData chargeData = stack.get(CHARGE_DATA);
			
			float nextCounter = counter;
			float nextDelay = delay;
			if (nextCounter > 0)
			{
				nextCounter -= timeDelta;
				if (nextCounter > 0)
				{
					return new SplatlingFiringData(nextCounter, Optional.of(false), nextDelay, nextShotTypeData, chargeStart);
				}
				if (chargeData.charge <= 0)
				{
					onFiringEnd.accept(nextCounter);
					return new SplatlingFiringData(Float.NaN, Optional.empty(), 0f, (short) 0, 0f);
				}
				else
				{
					nextDelay += nextCounter;
				}
			}
			
			CommonRecords.ProjectileDataRecord projectileData;
			SplatlingWeaponSettings.SplatlingShotDataRecord shotData;
			chargeData = chargeData.withPreviousCharge(chargeData.charge);
			while (nextDelay <= 0 && chargeData.charge > 0)
			{
				float index = settings.getShotTypeIndex(chargeData.charge, nextShotTypeData);
				Pair<CommonRecords.ProjectileDataRecord, SplatlingWeaponSettings.SplatlingShotDataRecord> dataPair = settings.interpolateData(index);
				projectileData = dataPair.getFirst();
				shotData = dataPair.getSecond();
				
				float nextCharge = chargeData.charge - shotData.chargeUsePerShot();
				float cutoffTime;
				if (nextCharge < 0)
				{
					cutoffTime = Mth.inverseLerp(0, chargeData.charge, nextCharge);
					nextCharge = 0;
					nextCounter = shotData.endlagTicks() - timeDelta * cutoffTime;
				}
				else
				{
					cutoffTime = 1f;
				}
				
				chargeData = chargeData.withCharge(nextCharge).registerChargeDeltaTime(cutoffTime);
				onShoot.run(projectileData, shotData, -nextDelay, index);
				
				nextDelay += shotData.repeatTicks();
			}
			nextDelay -= timeDelta;
			stack.set(CHARGE_DATA, chargeData);
			if (settings.getDynamicDataKey() == SplatlingWeaponSettings.ShotDataSelectorType.TIME_USED)
			{
				SplatlingWeaponSettings.TimeUsedSelectionData timeUsedData = (SplatlingWeaponSettings.TimeUsedSelectionData) settings.shotSelectionData;
				if (nextShotTypeData < timeUsedData.shotTimeForSecondLevel() * 2)
				{
					nextShotTypeData++;
				}
			}
			
			return new SplatlingFiringData(nextCounter, Optional.of(false), nextDelay, nextShotTypeData, chargeStart);
		}
		public SplatlingFiringData retrieveCharge(SplatlingWeaponSettings<?> settings)
		{
			return new SplatlingFiringData(-settings.chargeData.chargeStorageShootLag(), Optional.of(true), delay, shotTypeData, chargeStart);
		}
		public SplatlingFiringData notifyUsage(LivingEntity entity, SplatlingWeaponSettings<?> settings, ItemStack stack)
		{
			if (!WeaponHandler.canContinueShooting(entity))
				return this;
			
			if (charging.isEmpty()) // first use
				return new SplatlingFiringData(-settings.chargeData.minChargeTime(), Optional.of(true), 0f, (short) 0, 0);
			
			if (!charging.get() && settings.chargeData.canRechargeWhileFiring()) // firing, if the weapon is allowed to recharge, then do that
				return new SplatlingFiringData(0f, Optional.of(true), 0f, shotTypeData, stack.get(CHARGE_DATA).charge);
			
			return this;
		}
		public boolean preventsChanging()
		{
			return !Float.isNaN(counter);
		}
		@FunctionalInterface
		public interface SplatlingShootAction
		{
			void run(CommonRecords.ProjectileDataRecord projectileData, SplatlingWeaponSettings.SplatlingShotDataRecord shotData, float extraTime, float dataIndex);
		}
	}
	public record ShooterFiringData(float counter, float startupTime, float repeatTime, float endlagTime,
	                                float repeatPunishTime, boolean isRepeating)
	{
		public static final Codec<ShooterFiringData> CODEC = RecordCodecBuilder.create(inst ->
			inst.group(
				Codec.FLOAT.fieldOf("counter").forGetter(ShooterFiringData::counter),
				Codec.FLOAT.fieldOf("startup_time").forGetter(ShooterFiringData::startupTime),
				Codec.FLOAT.fieldOf("repeat_time").forGetter(ShooterFiringData::repeatTime),
				Codec.FLOAT.fieldOf("endlag_time").forGetter(ShooterFiringData::endlagTime),
				Codec.FLOAT.fieldOf("repeat_punish_time").forGetter(ShooterFiringData::repeatPunishTime),
				Codec.BOOL.fieldOf("repeat_queued").forGetter(ShooterFiringData::isRepeating)
			).apply(inst, ShooterFiringData::new)
		);
		public static final ShooterFiringData DEFAULT = new ShooterFiringData(0, 1, 1, 1, 0, false);
		public static final StreamCodec<ByteBuf, ShooterFiringData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, ShooterFiringData::counter,
			ByteBufCodecs.FLOAT, ShooterFiringData::startupTime,
			ByteBufCodecs.FLOAT, ShooterFiringData::repeatTime,
			ByteBufCodecs.FLOAT, ShooterFiringData::endlagTime,
			ByteBufCodecs.FLOAT, ShooterFiringData::repeatPunishTime,
			ByteBufCodecs.BOOL, ShooterFiringData::isRepeating,
			ShooterFiringData::new
		);
		public ShooterFiringData withRepeatingFlag(boolean repeating)
		{
			return new ShooterFiringData(counter, startupTime, repeatTime, endlagTime, repeatPunishTime, repeating);
		}
		public ShooterFiringData withStartupTime(float startupTime)
		{
			return new ShooterFiringData(counter, startupTime, repeatTime, endlagTime, repeatPunishTime, isRepeating);
		}
		public ShooterFiringData withRepeatTime(float repeatTime)
		{
			return new ShooterFiringData(counter, startupTime, repeatTime, endlagTime, repeatPunishTime, isRepeating);
		}
		public ShooterFiringData withEndlagTime(float endlagTime)
		{
			return new ShooterFiringData(counter, startupTime, repeatTime, endlagTime, repeatPunishTime, isRepeating);
		}
		public ShooterFiringData withRepeatPunishTime(float repeatPunishTime)
		{
			return new ShooterFiringData(counter, startupTime, repeatTime, endlagTime, repeatPunishTime, isRepeating);
		}
		public ShooterFiringData withCounter(float counter)
		{
			return new ShooterFiringData(counter, startupTime, repeatTime, endlagTime, repeatPunishTime, isRepeating);
		}
		public boolean preventsChanging()
		{
			return !Float.isNaN(counter);
		}
		public ShooterFiringData notifyUsing(LivingEntity entity, CommonRecords.ShotDataRecord settings)
		{
			float initialStartup = CommonUtils.startupSquidSwitch(entity, settings);
			float startup = Math.max(initialStartup, repeatPunishTime);
			return notifyUsing(entity, settings, startup, initialStartup);
		}
		public ShooterFiringData notifyUsing(LivingEntity entity, CommonRecords.ShotDataRecord settings, float startup, float initialStartup)
		{
			if (!WeaponHandler.canContinueShooting(entity) || !Float.isNaN(counter)) return this;
			
			return new ShooterFiringData(startup, initialStartup, settings.repeatTicks(), settings.endlagTicks(), 0f, true);
		}
		public ShooterFiringData tick(TimeAwareAction onAction)
		{
			return tick(onAction, (v, y) -> v, Predicates.alwaysTrue(), 1f);
		}
		public ShooterFiringData tick(TimeAwareAction onAction, Predicate<ShooterFiringData> isStillRepeating)
		{
			return tick(onAction, (v, y) -> v, isStillRepeating, 1f);
		}
		public ShooterFiringData tick(TimeAwareAction onAction, TimeAwareAction onActionDone, Predicate<ShooterFiringData> isStillRepeating)
		{
			return tick(onAction, onActionDone, isStillRepeating, 1f);
		}
		public ShooterFiringData tick(TimeAwareAction onAction, TimeAwareAction onActionDone, Predicate<ShooterFiringData> isStillRepeating, float timeDelta)
		{
			if (Float.isNaN(counter()))
				return repeatPunishTime > 0 ? withRepeatPunishTime(Math.max(0, repeatPunishTime - timeDelta)) : this;
			
			ShooterFiringData self = this;
			
			// behavior: if time > 0, its the first iteration, after that its kept in the interval [0, -repeatTime[ if the is repeating flag is on
			// if the is repeating flag is off, when the counter reaches -repeatTime - endlagTime the action is done!!
			
			float nextTime = counter() - timeDelta;
			float repeatCheckInstant = -self.endlagTime();
			if (repeatPunishTime > 0)
				self = self.withRepeatPunishTime(Math.max(0, repeatPunishTime - timeDelta));
			
			// fix for slow weapons (like blasters) that cancels the repeating flag if they're not shooting after the endlag is done
			// or if the counter is before the startup and the entity isn't shooting (like for dualies)
			if (self.isRepeating() && counter() > repeatCheckInstant && nextTime <= repeatCheckInstant)
			{
				if (!isStillRepeating.test(self))
				{
					self = self.withRepeatingFlag(false);
				}
			}
			if (counter() > startupTime() && nextTime <= startupTime())
			{
				if (!isStillRepeating.test(self))
				{
					return self.withCounter(Float.NaN);
				}
			}
			if (self.counter() > 0 && nextTime <= 0) // first iteration
			{
				self = onAction.run(self, -nextTime).withRepeatPunishTime(repeatTime + nextTime);
			}
			while (self.isRepeating() && nextTime <= -self.repeatTime()) // repeating iteration
			{
				nextTime += self.repeatTime();
				self = onAction.run(self, -nextTime).withRepeatPunishTime(repeatTime + nextTime);
			}
			if (!self.isRepeating() && nextTime <= -self.endlagTime()) // last iteration
			{
				self = onActionDone.run(self, -(nextTime + self.repeatTime() + self.endlagTime()));
				if (!self.isRepeating())
					return self.withCounter(Float.NaN);
			}
			
			return self.withCounter(nextTime);
		}
		@FunctionalInterface
		public interface TimeAwareAction
		{
			ShooterFiringData run(ShooterFiringData data, float extraTime);
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
		public static final StreamCodec<ByteBuf, RemoteInfo> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), RemoteInfo::stageId,
			ByteBufCodecs.optional(ResourceKey.streamCodec(Registries.DIMENSION)), RemoteInfo::worldKey,
			ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), RemoteInfo::targets,
			ByteBufCodecs.optional(BlockPos.STREAM_CODEC), RemoteInfo::pointA,
			ByteBufCodecs.optional(BlockPos.STREAM_CODEC), RemoteInfo::pointB,
			ByteBufCodecs.INT, RemoteInfo::modeIndex,
			RemoteInfo::new
		);
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
		public static final StreamCodec<ByteBuf, WeaponPrecisionData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, WeaponPrecisionData::chanceDecreaseDelay,
			ByteBufCodecs.FLOAT, WeaponPrecisionData::chance,
			ByteBufCodecs.FLOAT, WeaponPrecisionData::airborneDecreaseDelay,
			ByteBufCodecs.FLOAT, WeaponPrecisionData::airborneInfluence,
			WeaponPrecisionData::new);
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
		float storedCharge
	)
	{
		public static final Codec<SpecialProviderData> CODEC = RecordCodecBuilder.create(
			inst -> inst.group(
				CodecUtils.Codecs.SPLATCRAFT_IDENTIFIER_CODEC.optionalFieldOf("special_id").forGetter(SpecialProviderData::specialId),
				CodecUtils.Codecs.SPLATCRAFT_IDENTIFIER_CODEC.optionalFieldOf("weapon_id_filter").forGetter(SpecialProviderData::weaponIdFilter),
				Codec.INT.optionalFieldOf("points_per_special_override").forGetter(SpecialProviderData::pointsPerSpecialOverride),
				Codec.BOOL.optionalFieldOf("allow_subs", true).forGetter(SpecialProviderData::allowSubs),
				Codec.FLOAT.optionalFieldOf("stored_charge", 0f).forGetter(SpecialProviderData::storedCharge)
			).apply(inst, SpecialProviderData::new)
		);
		public static final StreamCodec<ByteBuf, SpecialProviderData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.optional(CodecUtils.Codecs.SPLATCRAFT_IDENTIFIER_STREAM_CODEC), SpecialProviderData::specialId,
			ByteBufCodecs.optional(CodecUtils.Codecs.SPLATCRAFT_IDENTIFIER_STREAM_CODEC), SpecialProviderData::weaponIdFilter,
			ByteBufCodecs.optional(ByteBufCodecs.INT), SpecialProviderData::pointsPerSpecialOverride,
			ByteBufCodecs.BOOL, SpecialProviderData::allowSubs,
			ByteBufCodecs.FLOAT, SpecialProviderData::storedCharge,
			SpecialProviderData::new
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
		public Component getSpecialText()
		{
			return specialId.map(identifier -> Component.translatable("special_weapon." + identifier.toLanguageKey())).orElse(Component.literal("none"));
		}
		public Component getWeaponFilterText()
		{
			return weaponIdFilter.map(WeaponHandler::getWeaponNameComponent).orElseGet(() -> Component.literal("none"));
		}
		public SpecialProviderData withSpecialId(ResourceLocation id)
		{
			return new SpecialProviderData(Optional.ofNullable(id), weaponIdFilter, pointsPerSpecialOverride, allowSubs, storedCharge);
		}
		public SpecialProviderData withWeaponIdFilter(ResourceLocation id)
		{
			return new SpecialProviderData(specialId, Optional.ofNullable(id), pointsPerSpecialOverride, allowSubs, storedCharge);
		}
		public SpecialProviderData withOverridenSpecialCost(int cost)
		{
			return new SpecialProviderData(specialId, weaponIdFilter, Optional.of(cost), allowSubs, storedCharge);
		}
		public SpecialProviderData withOverridenSpecialCost(Optional<Integer> cost)
		{
			return new SpecialProviderData(specialId, weaponIdFilter, cost, allowSubs, storedCharge);
		}
		public SpecialProviderData withAllowedSubs(boolean allowedSubs)
		{
			return new SpecialProviderData(specialId, weaponIdFilter, pointsPerSpecialOverride, allowedSubs, storedCharge);
		}
		public SpecialProviderData withStoredCharge(float charge)
		{
			return new SpecialProviderData(specialId, weaponIdFilter, pointsPerSpecialOverride, allowSubs, charge);
		}
		public SpecialProviderData withStoredPoints(int points, Optional<ResourceLocation> weaponId)
		{
			return withStoredCharge((float) points / getPointsPerSpecial(weaponId));
		}
		public SpecialProviderData incrementStoredCharge(float charge)
		{
			return withStoredCharge(Math.min(storedCharge + charge, 1f));
		}
		public SpecialProviderData incrementStoredPoints(int points, Optional<ResourceLocation> weaponId)
		{
			return incrementStoredCharge((float) points / getPointsPerSpecial(weaponId));
		}
		public int getPointsPerSpecial(Optional<ResourceLocation> weaponId)
		{
			return pointsPerSpecialOverride.orElseGet(() ->
			{
				if (specialId.isPresent() && weaponId.isPresent())
					return SpecialHandler.getSpecialCost(weaponId.get(), specialId.get());
				return SpecialHandler.DEFAULT_SPECIAL_COST;
			});
		}
	}
	public record ChargeData(float charge, float previousCharge, float chargeDeltaTime)
	{
		public static final ChargeData DEFAULT = new ChargeData(0, 0, 0);
		public static final StreamCodec<ByteBuf, ChargeData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, ChargeData::charge,
			ByteBufCodecs.FLOAT, ChargeData::previousCharge,
			ByteBufCodecs.FLOAT, ChargeData::chargeDeltaTime,
			ChargeData::new
		);
		public static final Codec<ChargeData> CODEC = RecordCodecBuilder.create(
			inst -> inst.group(
				Codec.FLOAT.fieldOf("charge").forGetter(ChargeData::charge),
				Codec.FLOAT.fieldOf("previous_charge").forGetter(ChargeData::previousCharge),
				Codec.FLOAT.fieldOf("charge_delta_time").forGetter(ChargeData::chargeDeltaTime)
			).apply(inst, ChargeData::new)
		);
		public ChargeData updateCharge(float charge)
		{
			return new ChargeData(charge, this.charge, chargeDeltaTime);
		}
		public ChargeData withCharge(float charge)
		{
			return new ChargeData(charge, previousCharge, chargeDeltaTime);
		}
		public ChargeData withPreviousCharge(float charge)
		{
			return new ChargeData(this.charge, charge, chargeDeltaTime);
		}
		public ChargeData withCharge(float charge, float previousCharge)
		{
			return new ChargeData(charge, previousCharge, chargeDeltaTime);
		}
		public ChargeData registerChargeDeltaTime(float chargeCompleteExtraTime)
		{
			return new ChargeData(charge, previousCharge, chargeCompleteExtraTime);
		}
		public float getCharge(float frameTime)
		{
			return Mth.clamp(Mth.lerp(frameTime / (chargeDeltaTime), previousCharge, charge), previousCharge, charge);
		}
	}
}
