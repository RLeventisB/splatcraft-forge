package net.splatcraft.forge.items.weapons.settings;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.TooltipFlag;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.util.WeaponTooltip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractWeaponSettings<SELF extends AbstractWeaponSettings<SELF, CODEC>, CODEC>
{
	public String name;
	public float moveSpeed = 1;
	public boolean isSecret = false;
	private final ArrayList<WeaponTooltip<SELF>> statTooltips = new ArrayList<>();
	public AbstractWeaponSettings(String name)
	{
		this.name = name;
	}
	public abstract float calculateDamage(float tickCount, boolean airborne, Object[] data);
	public abstract float getMinDamage();
	public void addStatsToTooltip(List<Component> tooltip, TooltipFlag flag)
	{
		for (WeaponTooltip<SELF> stat : statTooltips)
			tooltip.add(stat.getTextComponent((SELF) this, flag.isAdvanced()));
	}
	private AttributeModifier SPEED_MODIFIER;
	public AttributeModifier getSpeedModifier()
	{
		if (SPEED_MODIFIER == null)
			SPEED_MODIFIER = new AttributeModifier(SplatcraftItems.SPEED_MOD_UUID, name + " mobility", moveSpeed - 1, AttributeModifier.Operation.MULTIPLY_TOTAL);
		
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
		Collections.addAll(statTooltips, tooltipsToRegister());
	}
	public abstract WeaponTooltip<SELF>[] tooltipsToRegister();
	public abstract Codec<CODEC> getCodec();
	public void castAndDeserialize(Object o)
	{
		try
		{
			deserialize((CODEC) o);
		}
		catch (ClassCastException ignored)
		{
		}
	}
	public abstract void deserialize(CODEC o);
	public abstract CODEC serialize();
	public void serializeToBuffer(FriendlyByteBuf buffer)
	{
		buffer.writeWithCodec(getCodec(), serialize());
	}
	public static float calculateAproximateRange(CommonRecords.ProjectileDataRecord settings)
	{
		final float minSpeedToCalculate = 0.01f;
		float drag = settings.horizontalDrag();
		float speed = settings.speed();
		int exponent = (int) Math.round(Math.log10(minSpeedToCalculate / speed) / Math.log10(drag));
		float dragDistance = 0, draggedSpeed = speed * drag;
		for (int i = 0; i < exponent; i++)
		{
			dragDistance += draggedSpeed;
			draggedSpeed *= drag;
		}
		return ((settings.straightShotTicks() + 1) * speed + dragDistance);
	}
}
