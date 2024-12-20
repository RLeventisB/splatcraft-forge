package net.splatcraft.registries;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.splatcraft.Splatcraft;

public class SplatcraftAttributes
{
    protected static final DeferredRegister<EntityAttribute> REGISTRY = Splatcraft.deferredRegistryOf(Registries.ATTRIBUTE);
    public static final RegistrySupplier<EntityAttribute> inkSwimSpeed = REGISTRY.register("ink_swim_speed", () -> new ClampedEntityAttribute("attribute.splatcraft.ink_swim_speed", 0.075F, 0.0D, 1024.0D).setTracked(true));
    public static final RegistrySupplier<EntityAttribute> superJumpTravelTime = REGISTRY.register("super_jump_travel_time", () -> new ClampedEntityAttribute("attribute.splatcraft.super_jump_travel_time", 73, 0.0D, 1200.0D).setTracked(true));
    public static final RegistrySupplier<EntityAttribute> superJumpWindupTime = REGISTRY.register("super_jump_windup_time", () -> new ClampedEntityAttribute("attribute.splatcraft.super_jump_windup_time", 27, 0.0D, 1200.0D).setTracked(true));
    public static final RegistrySupplier<EntityAttribute> superJumpHeight = REGISTRY.register("super_jump_height", () -> new ClampedEntityAttribute("attribute.splatcraft.super_jump_height", 50, -256.0D, 256.0D).setTracked(true));
}
