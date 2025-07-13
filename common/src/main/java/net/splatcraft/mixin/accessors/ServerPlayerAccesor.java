package net.splatcraft.mixin.accessors;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayer.class)
public interface ServerPlayerAccesor
{
	@Accessor("lastSentHealth")
	void setLastSentHealth(float health);
}
