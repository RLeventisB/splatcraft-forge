package net.splatcraft.blocks;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.client.gui.StageMarkerEditorScreen;
import net.splatcraft.dummys.ISplatcraftForgeBlockDummy;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.StageMarkerTileEntity;
import net.splatcraft.util.ClientUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class StageMarkerBlock extends Block implements ISplatcraftForgeBlockDummy, GameMasterBlock, SimpleWaterloggedBlock, EntityBlock
{
	private static final Map<LevelAccessor, Map<ChunkPos, Set<StageMarkerTileEntity>>> CHUNK_MARKER_MAP = new Object2ObjectOpenHashMap<>();
	private static final Map<StageMarkerTileEntity, List<ChunkPos>> MARKER_POSITIONS_MAP = new Object2ObjectOpenHashMap<>();
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public StageMarkerBlock()
	{
		super(BlockBehaviour.Properties.of()
			.strength(-1.0F, 3600000.8F)
			.noOcclusion().noLootTable()
			.mapColor(state -> state.getValue(BlockStateProperties.WATERLOGGED) ? MapColor.WATER : MapColor.NONE)
		);
		registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
	}
	@Override
	protected boolean skipRendering(@NotNull BlockState state, @NotNull BlockState adjacentState, @NotNull Direction direction)
	{
		return true;
	}
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult)
	{
		if (level.isClientSide && player.canUseGameMasterBlocks())
		{
			openEditor(level, pos);
			return InteractionResult.SUCCESS;
		}
		else
		{
			return InteractionResult.CONSUME;
		}
	}
	@OnlyIn(Dist.CLIENT)
	private void openEditor(Level level, @NotNull BlockPos pos)
	{
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof StageMarkerTileEntity marker)
			ClientUtils.getClient().setScreen(new StageMarkerEditorScreen(marker));
	}
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(WATERLOGGED);
	}
	protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, CollisionContext context)
	{
		return context.isHoldingItem(SplatcraftItems.stageMarker.get()) ? Shapes.block() : Shapes.empty();
	}
	protected boolean propagatesSkylightDown(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos)
	{
		return state.getFluidState().isEmpty();
	}
	@Override
	public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState)
	{
		return SplatcraftTileEntities.stageMarketTileEntity.get().create(blockPos, blockState);
	}
	protected @NotNull FluidState getFluidState(BlockState state)
	{
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}
	public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state)
	{
		return super.getCloneItemStack(level, pos, state);
	}
	@Override
	public @NotNull RenderShape getRenderShape(@NotNull BlockState state)
	{
		return RenderShape.INVISIBLE;
	}
	public static List<StageMarkerTileEntity> getMarkersInChunkPos(LevelAccessor level, ChunkPos chunkPos)
	{
		Map<ChunkPos, Set<StageMarkerTileEntity>> zonesInLevel = CHUNK_MARKER_MAP.get(level);
		if (zonesInLevel == null || zonesInLevel.isEmpty())
			return List.of();
		
		Set<StageMarkerTileEntity> zonesInChunk = zonesInLevel.get(chunkPos);
		if (zonesInChunk == null || zonesInChunk.isEmpty())
			return List.of();
		
		return zonesInChunk.stream().toList();
	}
	public static List<StageMarkerTileEntity> getZonesThatContainPos(LevelAccessor level, BlockPos pos)
	{
		ChunkPos chunkPos = new ChunkPos(pos);
		List<StageMarkerTileEntity> zonesInChunk = new ArrayList<>(getMarkersInChunkPos(level, chunkPos));
		if (zonesInChunk.isEmpty())
			return List.of();
		
		zonesInChunk.removeIf(v -> v.isRemoved() || v.getMarkerType() != StageMarkerTileEntity.MarkerType.SPLAT_ZONE);
		
		return zonesInChunk.stream().filter(v ->
		{
			if (!(v.getBlockState().getBlock() instanceof StageMarkerBlock))
				return false;
			
			BlockPos minBoundPos = v.getMinBoundPos();
			
			BlockPos maxBoundPos = v.getMaxBoundPos();
			return
				pos.getX() >= minBoundPos.getX() && pos.getX() <= maxBoundPos.getX() &&
					pos.getY() >= minBoundPos.getY() && pos.getY() <= maxBoundPos.getY() &&
					pos.getZ() >= minBoundPos.getZ() && pos.getZ() <= maxBoundPos.getZ();
		}).toList();
	}
	public static void removeMarker(StageMarkerTileEntity marker)
	{
		if (marker.getLevel() == null || marker.getLevel().isClientSide())
			return;
		
		List<ChunkPos> affectedChunks = MARKER_POSITIONS_MAP.remove(marker);
		if (affectedChunks == null || affectedChunks.isEmpty())
			return;
		
		Map<ChunkPos, Set<StageMarkerTileEntity>> chunkPosMap = CHUNK_MARKER_MAP.get(marker.getLevel());
		if (chunkPosMap == null)
			return;
		
		for (ChunkPos previousPos : affectedChunks)
		{
			Set<StageMarkerTileEntity> markersInChunk = chunkPosMap.get(previousPos);
			if (markersInChunk == null)
				continue;
			
			markersInChunk.remove(marker);
			
			if (markersInChunk.isEmpty())
				chunkPosMap.remove(previousPos);
		}
		
		if (chunkPosMap.isEmpty())
			CHUNK_MARKER_MAP.remove(marker.getLevel());
	}
	public static void updateSplatZoneMap(StageMarkerTileEntity marker)
	{
		if (marker.getLevel() == null || marker.getLevel().isClientSide())
			return;
		
		if (marker.getMarkerType() != StageMarkerTileEntity.MarkerType.SPLAT_ZONE)
			return;
		
		removeMarker(marker);
		
		List<ChunkPos> newPositions = ChunkPos.rangeClosed(new ChunkPos(marker.getMinBoundPos()), new ChunkPos(marker.getMaxBoundPos())).toList();
		
		for (ChunkPos newPos : newPositions)
		{
			CHUNK_MARKER_MAP.computeIfAbsent(marker.getLevel(), v -> new Object2ObjectOpenHashMap<>())
				.computeIfAbsent(newPos, v -> new HashSet<>()).add(marker);
		}
		
		MARKER_POSITIONS_MAP.put(marker, newPositions);
	}
}
