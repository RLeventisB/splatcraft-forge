package net.splatcraft.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.splatcraft.tileentities.container.WeaponWorkbenchContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WeaponWorkbenchBlock extends HorizontalFacingBlock implements Waterloggable
{
	public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	protected static final VoxelShape BOTTOM_LEFT = createCuboidShape(2, 0, 0, 5, 4, 16);
	protected static final VoxelShape BOTTOM_RIGHT = createCuboidShape(11, 0, 0, 14, 4, 16);
	protected static final VoxelShape BASE = createCuboidShape(1, 1, 1, 15, 16, 15);
	protected static final VoxelShape DETAIL = createCuboidShape(0, 8, 0, 16, 10, 16);
	protected static final VoxelShape HANDLE = createCuboidShape(5, 11, 0, 11, 12, 1);
	public static final VoxelShape[] SHAPES = createVoxelShapes(BOTTOM_LEFT, BOTTOM_RIGHT, BASE, DETAIL, HANDLE);
	private static final MutableText CONTAINER_NAME = Text.translatable("container.ammo_knights_workbench");
	private final MapCodec<? extends HorizontalFacingBlock> CODEC = createCodec(WeaponWorkbenchBlock::new);
	public WeaponWorkbenchBlock(Settings setting)
	{
		super(setting);
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(WATERLOGGED, false));
	}
	protected static VoxelShape modifyShapeForDirection(Direction facing, VoxelShape shape)
	{
		Box bb = shape.getBoundingBox();
		
		switch (facing)
		{
			case EAST:
				return VoxelShapes.cuboid(new Box(1 - bb.minZ, bb.minY, 1 - bb.minX, 1 - bb.maxZ, bb.maxY, 1 - bb.maxX));
			case SOUTH:
				return VoxelShapes.cuboid(new Box(1 - bb.maxX, bb.minY, 1 - bb.maxZ, 1 - bb.minX, bb.maxY, 1 - bb.minZ));
			case WEST:
				return VoxelShapes.cuboid(new Box(bb.minZ, bb.minY, bb.minX, bb.maxZ, bb.maxY, bb.maxX));
		}
		return shape;
	}
	public static VoxelShape[] createVoxelShapes(VoxelShape... shapes)
	{
		VoxelShape[] result = new VoxelShape[4];
		
		for (int i = 0; i < 4; i++)
		{
			result[i] = VoxelShapes.empty();
			for (VoxelShape shape : shapes)
			{
				result[i] = VoxelShapes.union(result[i], modifyShapeForDirection(Direction.fromHorizontal(i), shape));
			}
		}
		
		return result;
	}
	@Override
	public @NotNull VoxelShape getOutlineShape(BlockState state, @NotNull BlockView levelIn, @NotNull BlockPos pos, @NotNull ShapeContext context)
	{
		return SHAPES[state.get(FACING).getHorizontal()];
	}
	@Override
	public ItemActionResult onUseWithItem(ItemStack stack, @NotNull BlockState state, World levelIn, @NotNull BlockPos pos, @NotNull PlayerEntity player, @NotNull Hand handIn, @NotNull BlockHitResult hit)
	{
		if (levelIn.isClient)
		{
			return ItemActionResult.SUCCESS;
		}
		player.openHandledScreen(createScreenHandlerFactory(state, levelIn, pos));
		return ItemActionResult.CONSUME;
	}
	@Override
	public NamedScreenHandlerFactory createScreenHandlerFactory(@NotNull BlockState state, @NotNull World levelIn, @NotNull BlockPos pos)
	{
		return new SimpleNamedScreenHandlerFactory((id, inventory, player) ->
			new WeaponWorkbenchContainer(inventory, ScreenHandlerContext.create(levelIn, pos), id), CONTAINER_NAME
		);
	}
	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(FACING, WATERLOGGED);
	}
	@Nullable
	@Override
	public BlockState getPlacementState(ItemPlacementContext context)
	{
		BlockPos blockpos = context.getBlockPos();
		FluidState fluidstate = context.getWorld().getFluidState(blockpos);
		return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite()).with(WATERLOGGED, fluidstate.getRegistryEntry() == Fluids.WATER);
	}
	@Override
	public @NotNull FluidState getFluidState(BlockState state)
	{
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}
	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec()
	{
		return CODEC;
	}
}
