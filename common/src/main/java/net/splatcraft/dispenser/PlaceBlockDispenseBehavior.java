package net.splatcraft.dispenser;

import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.FallibleItemDispenserBehavior;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

public class PlaceBlockDispenseBehavior extends FallibleItemDispenserBehavior
{
    @Override
    protected @NotNull ItemStack dispenseSilently(@NotNull BlockPointer source, ItemStack stack)
    {
        setSuccess(false);
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem)
        {
            Direction direction = source.state().get(DispenserBlock.FACING);
            BlockPos blockpos = source.pos().offset(direction);
            Direction direction1 = source.world().isAir(blockpos.down()) ? direction : Direction.UP;
            setSuccess(blockItem.place(new AutomaticItemPlacementContext(source.world(), blockpos, direction, stack, direction1)).isAccepted());
        }

        return stack;
    }
}
