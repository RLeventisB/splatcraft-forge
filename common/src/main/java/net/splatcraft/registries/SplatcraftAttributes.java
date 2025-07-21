package net.splatcraft.registries;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.splatcraft.Splatcraft;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.RegistrySupplier;

public class SplatcraftAttributes
{
	public static final DeferredRegister<Attribute> REGISTRY = Splatcraft.deferredRegistryOf(BuiltInRegistries.ATTRIBUTE);
	public static final RegistrySupplier<Attribute> inkSwimSpeed = REGISTRY.register("ink_swim_speed", () -> new RangedAttribute("attribute.splatcraft.ink_swim_speed", 0.075F, 0.0D, 1024.0D).setSyncable(true));
	public static final RegistrySupplier<Attribute> superJumpTravelTime = REGISTRY.register("super_jump_travel_time", () -> new RangedAttribute("attribute.splatcraft.super_jump_travel_time", 73, 0.0D, 1200.0D).setSyncable(true));
	public static final RegistrySupplier<Attribute> superJumpWindupTime = REGISTRY.register("super_jump_windup_time", () -> new RangedAttribute("attribute.splatcraft.super_jump_windup_time", 27, 0.0D, 1200.0D).setSyncable(true));
	public static final RegistrySupplier<Attribute> superJumpHeight = REGISTRY.register("super_jump_height", () -> new RangedAttribute("attribute.splatcraft.super_jump_height", 50, -256.0D, 256.0D).setSyncable(true));
	public static final RegistrySupplier<Attribute> enemyInkResistanceTime = REGISTRY.register("ink_resistance_time", () -> new RangedAttribute("attribute.splatcraft.ink_resistance_time", 0, 0, 256.0D).setSyncable(true));
	public static final RegistrySupplier<Attribute> maxEnemyInkDamage = REGISTRY.register("max_enemy_ink_damage", () -> new RangedAttribute("attribute.splatcraft.max_enemy_ink_damage", 8, 0, 256.0D).setSyncable(true));
	public static final RegistrySupplier<Attribute> enemyInkJumpMultiplier = REGISTRY.register("enemy_ink_jump_multiplier", () -> new RangedAttribute("attribute.splatcraft.enemy_ink_jump_multiplier", 0.6, 0, 1f).setSyncable(true));
	public static final RegistrySupplier<Attribute> specialLoss = REGISTRY.register("special_loss", () -> new RangedAttribute("attribute.splatcraft.special_loss", 0.6, 0, 1f).setSyncable(true));
}
