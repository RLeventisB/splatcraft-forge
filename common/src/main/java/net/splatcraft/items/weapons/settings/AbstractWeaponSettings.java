package net.splatcraft.items.weapons.settings;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.splatcraft.Splatcraft;
import net.splatcraft.items.weapons.settings.CommonRecords.ProjectileDataRecord;
import net.splatcraft.items.weapons.settings.CommonRecords.ShotDataRecord;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.structs.WeaponTooltip;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWeaponSettings<SELF extends AbstractWeaponSettings<SELF, DATA>, DATA>
{
	public static final ResourceLocation DEFAULT_NAME = Splatcraft.identifierOf("default");
	public static final ResourceLocation WEAPON_MOBILITY_ATTIBUTE_ID = Splatcraft.identifierOf("weapon_mobility");
	private final ArrayList<WeaponTooltip<SELF>> statTooltips = new ArrayList<>();
	public ResourceLocation name;
	public float moveSpeed = 1;
	public boolean isSecret = false;
	private AttributeModifier SPEED_MODIFIER;
	public AbstractWeaponSettings(ResourceLocation name)
	{
		this.name = name;
	}
	public AbstractWeaponSettings(String name)
	{
		this.name = CodecUtils.tryParseResourceLocationWithCustomDefaultNamespace(name, Splatcraft.MODID);
	}
	public static float calculateAproximateRange(ProjectileDataRecord projSettings, ShotDataRecord shotSettings)
	{
		return calculateAproximateRange(projSettings.straightShotTicks(), projSettings.horizontalDrag(), shotSettings.speed(), projSettings.delaySpeedMult(), projSettings.lifeTicks());
	}
	public static float calculateAproximateRange(float straightShotTicks, float drag, float speed, float delaySpeedMult, float maxLifespan)
	{
		float dragOnEnd = (float) Math.pow(drag, maxLifespan - straightShotTicks);
		if (dragOnEnd < 0.01)
			return speed * (straightShotTicks + delaySpeedMult * drag / (1 - drag));
		return speed * (straightShotTicks + delaySpeedMult * dragOnEnd);
	}
	public void addStatsToTooltip(List<Component> tooltip, TooltipFlag flag)
	{
		for (WeaponTooltip<SELF> stat : statTooltips)
			tooltip.add(stat.getTextComponent((SELF) this, flag.isAdvanced()));
	}
	public AttributeModifier getSpeedModifier()
	{
		if (SPEED_MODIFIER == null)
		{
			SPEED_MODIFIER = new AttributeModifier(WEAPON_MOBILITY_ATTIBUTE_ID, moveSpeed - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		}

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
	public void serializeToBuffer(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeJsonWithCodec(getCodec(), getDataToSerialize());
	}
	public abstract float getSpeedForRender(Player player, ItemStack mainHandItem);
	public void deserialize(ResourceLocation key, JsonObject json)
	{
		getCodec().parse(JsonOps.INSTANCE, json).resultOrPartial(msg -> Splatcraft.LOGGER.error("Failed to load weapon settings for %s: %s".formatted(key, msg))).ifPresent(
			this::processResult
		);
	}
	@Override
	public String toString()
	{
		return name.toString();
	}
}
