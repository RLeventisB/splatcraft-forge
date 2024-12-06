package net.splatcraft.mixin.compat;

import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.util.InkBlockUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Mixin(value = Contraption.class, remap = false)
public class CreateMixin
{
    @Unique
    private static short splatcraft$nextId = 0;
    @Unique
    private static Map<Contraption, Short> splatcraft$mapId = new HashMap<>();

    @Inject(method = "removeBlocksFromWorld", at = @At(value = "HEAD"))
    public void splatcraft$registerBlocks(Level world, BlockPos offset, CallbackInfo ci)
    {
        Contraption contraption = (Contraption) (Object) this;
        if (splatcraft$mapId.containsKey(contraption))
            return;

        ChunkInkHandler.addBlocksToIgnoreRemoveInk(world,
            contraption.getBlocks().keySet().stream().map(v -> v.offset(contraption.anchor).offset(offset)).filter(v -> InkBlockUtils.isInkedAny(world, v)).collect(Collectors.toList()));

        splatcraft$mapId.put(contraption, splatcraft$nextId++);
    }
}
