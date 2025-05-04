package net.splatcraft.tileentities.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.BiFunction;

public record InkVatScreenHandlerContext(Level world, BlockPos pos) implements ContainerLevelAccess
{
	@Override
	public <T> @NotNull Optional<T> evaluate(BiFunction<Level, BlockPos, T> getter)
	{
		return Optional.of(getter.apply(world, pos));
	}
}
