package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.text.Text;
import net.splatcraft.Splatcraft;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.WeaponTooltip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractWeaponSettings<SELF extends AbstractWeaponSettings<SELF, CODEC>, CODEC>
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
			SPEED_MODIFIER = new EntityAttributeModifier(Splatcraft.identifierOf(CommonUtils.makeStringIdentifierValid(name) + "_mobility"), moveSpeed - 1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		
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
	public abstract CommonRecords.ShotDeviationDataRecord getShotDeviationData(ItemStack stack, LivingEntity entity);
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
	public void serializeToBuffer(RegistryByteBuf buffer)
	{
		buffer.encodeAsJson(getCodec(), serialize());
	}
	public abstract float getSpeedForRender(ClientPlayerEntity player, ItemStack mainHandItem);
}
