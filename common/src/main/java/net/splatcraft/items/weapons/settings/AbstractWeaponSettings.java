package net.splatcraft.items.weapons.settings;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.splatcraft.Splatcraft;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.util.WeaponTooltip;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWeaponSettings<SELF extends AbstractWeaponSettings<SELF, DATA>, DATA>
{
	private final ArrayList<WeaponTooltip<SELF>> statTooltips = new ArrayList<>();
	public String name;
	public float moveSpeed = 1;
	public boolean isSecret = false;
	private EntityAttributeModifier SPEED_MODIFIER;
	public AbstractWeaponSettings(String name)
	{
		this.name = name;
	}
	public static float calculateAproximateRange(CommonRecords.ProjectileDataRecord settings)
	{
		return calculateAproximateRange(settings.straightShotTicks(), settings.horizontalDrag(), settings.speed(), settings.delaySpeedMult(), settings.lifeTicks());
	}
	public static float calculateAproximateRange(float straightShotTicks, float drag, float speed, float delaySpeedMult, float maxLifespan)
	{
		float dragOnEnd = (float) Math.pow(drag, maxLifespan - straightShotTicks);
		if (dragOnEnd < 0.01)
			return speed * (straightShotTicks + delaySpeedMult * drag / (1 - drag));
		return speed * (straightShotTicks + delaySpeedMult * dragOnEnd);
	}
	public abstract float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list);
	public void addStatsToTooltip(List<Text> tooltip, TooltipType flag)
	{
		for (WeaponTooltip<SELF> stat : statTooltips)
			tooltip.add(stat.getTextComponent((SELF) this, flag.isAdvanced()));
	}
	public EntityAttributeModifier getSpeedModifier()
	{
		if (SPEED_MODIFIER == null)
			SPEED_MODIFIER = new EntityAttributeModifier(Splatcraft.identifierOf("weapon_mobility"), moveSpeed - 1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		
		return SPEED_MODIFIER;
	}
	public SELF setMoveSpeed(float value)
	{
		moveSpeed = value;
		return (SELF) this;
	}
	public SELF setSecret(boolean value)
	{
		isSecret = value;
		return (SELF) this;
	}
	public void registerStatTooltips()
	{
		statTooltips.addAll(tooltipsToRegister());
	}
	public abstract List<WeaponTooltip<SELF>> tooltipsToRegister();
	public abstract Codec<DATA> getCodec();
	public abstract CommonRecords.ShotDeviationDataRecord getShotDeviationData(ItemStack stack, LivingEntity entity);
	public void processResult(Object o)
	{
		try
		{
			processData((DATA) o);
		}
		catch (ClassCastException ignored)
		{
		}
	}
	public abstract void processData(DATA o);
	public abstract DATA getDataToSerialize();
	public void serializeToBuffer(RegistryByteBuf buffer)
	{
		buffer.encodeAsJson(getCodec(), getDataToSerialize());
	}
	public abstract float getSpeedForRender(ClientPlayerEntity player, ItemStack mainHandItem);
	public void onStartReading(JsonObject json)
	{
	
	}
	public void deserialize(Identifier key, JsonObject json)
	{
		onStartReading(json);
		getCodec().parse(JsonOps.INSTANCE, json).resultOrPartial(msg -> Splatcraft.LOGGER.error("Failed to load weapon settings for %s: %s".formatted(key, msg))).ifPresent(
			this::processResult
		);
	}
}
