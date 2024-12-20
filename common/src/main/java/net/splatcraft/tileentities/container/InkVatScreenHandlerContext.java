package net.splatcraft.tileentities.container;

import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.function.BiFunction;

public record InkVatScreenHandlerContext(World world, BlockPos pos) implements ScreenHandlerContext
{
    @Override
    public <T> Optional<T> get(BiFunction<World, BlockPos, T> getter)
    {
        return Optional.of(getter.apply(world, pos));
    }
}
