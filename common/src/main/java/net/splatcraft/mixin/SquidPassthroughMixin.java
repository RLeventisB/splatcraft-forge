package net.splatcraft.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public class SquidPassthroughMixin
{
    @Inject(at = @At("TAIL"), method = "getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;", cancellable = true)
    private void getCollisionShape(BlockView level, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> callback)
    {
        AbstractBlock.AbstractBlockState state = (AbstractBlock.AbstractBlockState) (Object) this;
        try
        {
            if (state.isIn(SplatcraftTags.Blocks.SQUID_PASSTHROUGH) && context instanceof EntityShapeContext eContext &&
                eContext.getEntity() instanceof LivingEntity entity && EntityInfoCapability.isSquid(entity))
                callback.setReturnValue(VoxelShapes.empty());
        }
        catch (IllegalStateException ignored)
        {
        }
    }
}
