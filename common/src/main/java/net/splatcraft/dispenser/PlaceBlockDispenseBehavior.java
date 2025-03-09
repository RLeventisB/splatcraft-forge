package net.splatcraft.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.DispenserBlock;
import org.jetbrains.annotations.NotNull;

public class PlaceBlockDispenseBehavior extends OptionalDispenseItemBehavior
{
    @Override
    protected @NotNull ItemStack execute(@NotNull BlockSource source, ItemStack stack)
    {
        setSuccess(false);
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem)
        {
            Direction direction = source.state().getValue(DispenserBlock.FACING);
            BlockPos blockpos = source.pos().relative(direction);
            Direction direction1 = source.level().isEmptyBlock(blockpos.below()) ? direction : Direction.UP;
            setSuccess(blockItem.place(new DirectionalPlaceContext(source.level(), blockpos, direction, stack, direction1)).consumesAction());
        }

        return stack;
    }
}
