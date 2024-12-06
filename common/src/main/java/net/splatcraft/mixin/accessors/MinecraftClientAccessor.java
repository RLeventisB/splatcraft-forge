package net.splatcraft.mixin.accessors;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftClientAccessor
{
    @Accessor("rightClickDelay")
    void setRightClickDelay(int rightClickDelay);
}
