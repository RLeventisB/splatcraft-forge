package net.splatcraft.mixin.accessors;

import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//TODO use RenderLevelStageEvent to render ink over blocks instead of overriding block rendering with mixins,
// this may have been a bad idea for compatibility
@OnlyIn(Dist.CLIENT)
@Mixin(RenderChunkRegion.class)
public interface ChunkRegionAccessor
{
	@Accessor("level")
	Level getLevel();
}
