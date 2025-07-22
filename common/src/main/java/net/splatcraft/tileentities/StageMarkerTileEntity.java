package net.splatcraft.tileentities;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.blocks.StageMarkerBlock;
import net.splatcraft.data.capabilities.chunkink.ChunkInk.InkEntry;
import net.splatcraft.dummys.ISplatcraftForgeBlockEntityDummy;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.CommonUtils.ReseteableMemoizedSupplier;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkBlockUtils.InkType;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.*;
import java.util.Map.Entry;

public class StageMarkerTileEntity extends BlockEntity implements ISplatcraftForgeBlockEntityDummy
{
	private MarkerType type = MarkerType.SPLAT_ZONE;
	private BlockPos offset = BlockPos.ZERO;
	public int[] intDatas = new int[0];
	public float[] floatDatas = new float[0];
	private boolean active;
	private final ReseteableMemoizedSupplier<List<List<Vector3f>>> renderingCorners = CommonUtils.memoizeResetable(() ->
	{
		// returns a list of list of vectors to render as a line
		// the vectors in a list represent the corners that are facing upwards and are in the border of the zone blocks
		// nvm that is too complex
		
		BlockPos minPos = getMinBoundPos();
		BlockPos maxPos = getMaxBoundPos();
		float maxDepth = maxPos.getY() - minPos.getY() + 1;
		Vec3[] corners = {
			new Vec3(minPos.getX() + 10e-5, maxPos.getY() + 1, minPos.getZ() + 10e-5),
			new Vec3(maxPos.getX() + 1 - 10e-5, maxPos.getY() + 1, minPos.getZ() + 10e-5),
			new Vec3(maxPos.getX() + 1 - 10e-5, maxPos.getY() + 1, maxPos.getZ() + 1 - 10e-5),
			new Vec3(minPos.getX() + 10e-5, maxPos.getY() + 1, maxPos.getZ() + 1 - 10e-5)
		};
		
		ImmutableList.Builder<List<Vector3f>> builder = ImmutableList.builder();
		ImmutableList.Builder<Vector3f> vertexBuilder = ImmutableList.builder();
		BlockPos offsettedPos = getOffsetedPosition();
		for (int i = 0; i < 4; i++)
		{
			Vec3 corner = corners[i];
			float distanceToFloor = InkBlockUtils.getDistanceToFloor(corner, level, maxDepth, null).orElse(maxDepth);
			Vector3f point = new Vector3f((float) (corner.x - offsettedPos.getX()), (float) (corner.y - offsettedPos.getY() - distanceToFloor), (float) (corner.z - offsettedPos.getZ()));
			vertexBuilder.add(point);
		}
		builder.add(vertexBuilder.build());
		return builder.build();
/*
		Iterable<BlockPos> blocks = BlockPos.betweenClosed(getMinBoundPos(), getMaxBoundPos());
		Set<BlockPos> validBlocks = new HashSet<>();

		int maxHeights = 0;
		
		for (BlockPos pos : blocks)
		{
			if (!InkBlockUtils.isUninkable(level, pos, Direction.UP, false) && (pos.getY() == getMaxBoundPos().getY() || level.isEmptyBlock(pos.above())))
			{
				validBlocks.add(new BlockPos(pos));
			}
		}
		
		HashSet<Vector3f>[] builder = new HashSet[maxHeights];
		
		BlockPos minPos = getRelativeMinBoundPos();
		BlockPos maxPos = getRelativeMaxBoundPos();
		int[] ints = {minPos.getZ(), maxPos.getZ()};
		for (int x = minPos.getX(); x <= maxPos.getX(); x++)
		{
			for (int z : ints)
			{
				List<Integer> heights = validBlocks.get(new Vector2i(x, z));
				if (heights == null)
					continue;
				
				for (int i = 0; i < heights.size(); i++)
				{
					int y = heights.get(i);
					
					BlockPos pos = new BlockPos(x, y, z).offset(worldPosition);
					VoxelShape shape = level.getBlockState(pos).getVisualShape(level, pos, CollisionContext.empty());
					if (shape.isEmpty())
						continue;
					
					if (builder[i] == null)
						builder[i] = new HashSet<>();
					
					DiscreteVoxelShape bitShape = ((VoxelShapeAccessor) shape).getShape();
					for (int j = 0; j <= bitShape.getXSize(); j++)
					{
						int bitZ = z == minPos.getZ() ? 0 : bitShape.getZSize();
						float bitHeight = getBitHeight(bitShape, Direction.Axis.Y, bitZ, j);
						bitHeight /= bitShape.getYSize();
						builder[i].add(new Vector3f(x + (float) j / bitShape.getXSize(), y + bitHeight, z + (float) bitZ / bitShape.getZSize()));
					}
				}
			}
		}
		
		ints = new int[] {minPos.getX(), maxPos.getX()};
		for (int z = minPos.getZ(); z <= maxPos.getZ(); z++)
		{
			for (int x : ints)
			{
				List<Integer> heights = validBlocks.get(new Vector2i(x, z));
				if (heights == null)
					continue;
				
				for (int i = 0; i < heights.size(); i++)
				{
					int y = heights.get(i);
					
					BlockPos pos = new BlockPos(x, y, z).offset(worldPosition);
					VoxelShape shape = level.getBlockState(pos).getVisualShape(level, pos, CollisionContext.empty());
					if (shape.isEmpty())
						continue;
					
					if (builder[i] == null)
						builder[i] = new HashSet<>();
					
					DiscreteVoxelShape bitShape = ((VoxelShapeAccessor) shape).getShape();
					for (int j = 0; j <= bitShape.getZSize(); j++)
					{
						int bitX = x == minPos.getX() ? 0 : bitShape.getXSize();
						float bitHeight = getBitHeight(bitShape, Direction.Axis.Y, j, bitX);
						bitHeight /= bitShape.getYSize();
						builder[i].add(new Vector3f(x + (float) bitX / bitShape.getXSize(), y + bitHeight, z + (float) j / bitShape.getZSize()));
					}
				}
			}
		}
		
		return Arrays.stream(builder).filter(Objects::nonNull).map(v -> v.stream().toList()).toList();

*/
/*
		// filter only the blocks that are in a border
		validBlocks.removeIf(pos ->
		{
			for (int x = -1; x < 2; x++)
				for (int z = -1; z < 2; z++)
				{
					if (!validBlocks.contains(pos.offset(x, 0, z)))
						return false;
				}
			return true;
		});
*/
//		List<VoxelShape> shapes = CommonUtils.createShapes(level, validBlocks, worldPosition);
//		return shapes;
		/*shapes.forEach(shape ->
			{
				ImmutableSet.Builder<Vector3f> lineBuilder = ImmutableSet.builder();
				HashSet<Vec3> verticalConnections = new HashSet<>();

//				shape.forAllEdges((x1, y1, z1, x2, y2, z2) ->
//				{
//					Vec3 v1 = new Vec3(x1, y1, z1);
//					Vec3 v2 = new Vec3(x2, y2, z2);
//					Vec3 subtract = v1.subtract(v2);
//					if (!verticalConnections.contains(v1) && !verticalConnections.contains(v2) && subtract.horizontalDistanceSqr() == 0 & subtract.y != 0)
//						if (y1 < y2)
//						{
//							verticalConnections.add(v1);
//						}
//						else
//						{
//							verticalConnections.add(v2);
//						}
//				});
				
				shape.forAllEdges((x1, y1, z1, x2, y2, z2) ->
				{
//					if (verticalConnections.contains(new Vec3(x1, y1, z1)))
//						return;
//
//					if ((x1 == x2 && (shape.min(Direction.Axis.Z, y1, x1) == z1 || shape.max(Direction.Axis.Z, y1, x1) == z2)))
//						return;
//
//					if ((z1 == z2 && (shape.min(Direction.Axis.X, y1, z1) == x1 || shape.max(Direction.Axis.X, y1, z1) == x2)))
//						return;
					
					float maxY = (float) Math.max(y1, y2);
					float z3 = (float) z1;
					float x3 = (float) x1;
					float x4 = (float) x2;
					float z4 = (float) z2;
					lineBuilder.add(new Vector3f(x3, maxY, z3));
					lineBuilder.add(new Vector3f(x3, maxY, z4));
					lineBuilder.add(new Vector3f(x3, maxY, z4));
					lineBuilder.add(new Vector3f(x4, maxY, z4));
					lineBuilder.add(new Vector3f(x4, maxY, z4));
					lineBuilder.add(new Vector3f(x4, maxY, z3));
					lineBuilder.add(new Vector3f(x4, maxY, z3));
					lineBuilder.add(new Vector3f(x3, maxY, z3));
				});
				
				builder.add(lineBuilder.build().stream().toList());
			}
		);
		
		return builder.build();*/
	});
	private final ReseteableMemoizedSupplier<BlockPos> relativePositionSupplier = CommonUtils.memoizeResetable(() -> worldPosition.offset(offset));
	private final ReseteableMemoizedSupplier<BlockPos> minRelativeSupplier = CommonUtils.memoizeResetable(() -> offset.offset(
		Mth.ceil(-intDatas[0] / 2.0f),
		Mth.ceil(-intDatas[1] / 2.0f),
		Mth.ceil(-intDatas[2] / 2.0f)
	));
	private final ReseteableMemoizedSupplier<BlockPos> maxRelativeSupplier = CommonUtils.memoizeResetable(() -> offset.offset(
		Mth.ceil(intDatas[0] / 2.0f),
		Mth.ceil(intDatas[1] / 2.0f),
		Mth.ceil(intDatas[2] / 2.0f)
	));
	private final ReseteableMemoizedSupplier<BlockPos> minBoundSupplier = CommonUtils.memoizeResetable(() -> worldPosition.offset(minRelativeSupplier.get()));
	private final ReseteableMemoizedSupplier<BlockPos> maxBoundSupplier = CommonUtils.memoizeResetable(() -> worldPosition.offset(maxRelativeSupplier.get()));
	public StageMarkerTileEntity(BlockPos pos, BlockState state)
	{
		super(SplatcraftTileEntities.stageMarketTileEntity.get(), pos, state);
		initializeMarkerData();
	}
	public StageMarkerTileEntity(BlockEntityType<? extends StageMarkerTileEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	private void initializeMarkerData()
	{
		switch (type)
		{
			case SPLAT_ZONE ->
			{
				intDatas = new int[4]; // sizeX, sizeY, sizeZ, currentInkColor (0xFFFFFFFF is none)
				floatDatas = new float[0];
			}
			case CLAM_SPAWN_AREA ->
			{
				intDatas = new int[3]; // sizeX, sizeY, sizeZ
				floatDatas = new float[0];
			}
			case RAINMAKER_SPAWN ->
			{
				intDatas = new int[0];
				floatDatas = new float[0];
			}
			case RAINMAKER_PODIUM ->
			{
				intDatas = new int[1]; // checkpoint index
				floatDatas = new float[0];
			}
			case CLAM_BASKET ->
			{
				intDatas = new int[1]; // team id
				floatDatas = new float[0];
			}
		}
		notifyChange();
	}
	public void notifyChange()
	{
		relativePositionSupplier.reset();
		
		switch (type)
		{
			case SPLAT_ZONE ->
			{
				minRelativeSupplier.reset();
				maxRelativeSupplier.reset();
				minBoundSupplier.reset();
				maxBoundSupplier.reset();
				renderingCorners.reset();
				
				StageMarkerBlock.updateSplatZoneMap(this);
			}
			case RAINMAKER_SPAWN ->
			{
			}
			case RAINMAKER_PODIUM ->
			{
			}
			case CLAM_BASKET ->
			{
			}
			case CLAM_SPAWN_AREA ->
			{
			}
		}
	}
	public void tickZone()
	{
		if (!active)
			return;
		
		Optional<InkColor> currentColor = getCurrentColor();
		
		Iterable<BlockPos> blocks = BlockPos.betweenClosed(getMinBoundPos(), getMaxBoundPos());
		List<BlockPos> blocksWithExposedUpFace = new ArrayList<>();
		Map<InkColor, Float> colorCount = new HashMap<>();
		int totalFaces = 0;
		for (BlockPos pos : blocks)
		{
			if (!InkBlockUtils.isUninkable(level, pos, Direction.UP, true))
			{
				blocksWithExposedUpFace.add(new BlockPos(pos));
				InkEntry upFace = InkBlockUtils.getInkInFace(level, pos, Direction.UP);
				if (upFace != null)
				{
					InkColor color = upFace.color();
					colorCount.merge(color, 1f, Float::sum);
				}
				totalFaces += 1;
			}
		}
		
		int finalTotalFaces = totalFaces;
		colorCount.replaceAll((color, count) -> count / finalTotalFaces);
		
		if (colorCount.isEmpty())
		{
			setCurrentColor(null);
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
			return;
		}
		
		InkColor mostCoveredColor = null;
		float mostCoveredPercent = 0f;
		
		for (Entry<InkColor, Float> entry : colorCount.entrySet())
		{
			if (entry.getValue() > mostCoveredPercent)
			{
				mostCoveredColor = entry.getKey();
				mostCoveredPercent = entry.getValue();
			}
		}
		
		if (currentColor.isEmpty())
		{
			if (mostCoveredPercent > 0.7f)
			{
				level.playSound(null, getOffsetedPosition(), SplatcraftSounds.rankedAdvantage, SoundSource.BLOCKS, 0.6f, 1f);
				setCurrentColor(mostCoveredColor);
				level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
				for (BlockPos pos : blocksWithExposedUpFace)
				{
					InkBlockUtils.inkBlock(level, pos, mostCoveredColor, Direction.UP, InkType.NORMAL, 0f);
				}
			}
		}
		else
		{
			if (currentColor.get().getColor() != mostCoveredColor.getColor())
			{
				if (mostCoveredPercent > 0.5f)
				{
					level.playSound(null, getOffsetedPosition(), SplatcraftSounds.rankedNeutral, SoundSource.BLOCKS, 0.6f, 1f);
					setCurrentColor(null);
					level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}
	}
	@Override
	public void setRemoved()
	{
		if (type == MarkerType.SPLAT_ZONE)
			StageMarkerBlock.removeMarker(this);
		super.setRemoved();
	}
	@Override
	public @NotNull CompoundTag getUpdateTag(@NotNull Provider wrapperLookup)
	{
		return saveWithoutMetadata(wrapperLookup);
	}
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this);
	}
	@Override
	public void loadAdditional(@NotNull CompoundTag nbt, @NotNull Provider wrapperLookup)
	{
		BlockPos.CODEC.parse(NbtOps.INSTANCE, nbt.get("RelativePos")).ifSuccess(pos -> offset = pos);
		MarkerType.CODEC.parse(NbtOps.INSTANCE, nbt.get("MarketType")).ifSuccess(type -> this.type = type);
		
		intDatas = nbt.getIntArray("IntData");
		ListTag list = nbt.getList("FloatData", FloatTag.TAG_FLOAT);
		active = nbt.getBoolean("Active");
		
		floatDatas = new float[list.size()];
		for (int i = 0; i < list.size(); i++)
		{
			floatDatas[i] = ((FloatTag) list.get(i)).getAsFloat();
		}
		
		super.loadAdditional(nbt, wrapperLookup);
	}
	@Override
	public void saveAdditional(@NotNull CompoundTag nbt, @NotNull Provider wrapperLookup)
	{
		BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, offset).ifSuccess(
			tag -> nbt.put("RelativePos", tag)
		);
		MarkerType.CODEC.encodeStart(NbtOps.INSTANCE, type).ifSuccess(
			tag -> nbt.put("MarketType", tag)
		);
		nbt.putIntArray("IntData", intDatas);
		ListTag list = new ListTag();
		for (float data : floatDatas)
		{
			list.add(FloatTag.valueOf(data));
		}
		
		nbt.put("FloatData", list);
		nbt.putBoolean("Active", active);
		
		super.saveAdditional(nbt, wrapperLookup);
	}
	public BlockPos getMinBoundPos()
	{
		return minBoundSupplier.get();
	}
	public BlockPos getMaxBoundPos()
	{
		return maxBoundSupplier.get();
	}
	public BlockPos getRelativeMinBoundPos()
	{
		return minRelativeSupplier.get();
	}
	public BlockPos getRelativeMaxBoundPos()
	{
		return maxRelativeSupplier.get();
	}
	public BlockPos getOffsetedPosition()
	{
		return relativePositionSupplier.get();
	}
	@OnlyIn(Dist.CLIENT)
	public List<List<Vector3f>> getRenderingCorners()
	{
		return renderingCorners.get();
	}
	public Optional<InkColor> getCurrentColor()
	{
		return intDatas[3] == 0xFFFFFFFF ? Optional.empty() : Optional.ofNullable(InkColor.constructOrReuse(intDatas[3]));
	}
	private void setCurrentColor(InkColor color)
	{
		if (color == null || color.isInvalid())
		{
			intDatas[3] = 0xFFFFFFFF;
			return;
		}
		intDatas[3] = color.getColor();
	}
	@Override
	public void phOnDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, Provider lookup)
	{
		if (level != null)
		{
			BlockState state = getBlockState();
			level.sendBlockUpdated(getBlockPos(), state, state, 2);
			phHandleUpdateTag(pkt.getTag(), lookup);
		}
	}
	public AABB getAABB()
	{
		return AABB.encapsulatingFullBlocks(getMinBoundPos(), getMaxBoundPos());
	}
	public boolean isActive()
	{
		return active;
	}
	public void setActive(boolean active)
	{
		BlockState state = getBlockState();
		if (level != null && !level.isClientSide())
			level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
		this.active = active;
	}
	public BlockPos getOffset()
	{
		return offset;
	}
	public void setOffset(BlockPos offset)
	{
		BlockState state = getBlockState();
		if (level != null && !level.isClientSide())
			level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
		this.offset = offset;
	}
	public MarkerType getMarkerType()
	{
		return type;
	}
	public void setMarkerType(MarkerType type)
	{
		BlockState state = getBlockState();
		if (level != null && !level.isClientSide())
			level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
		this.type = type;
	}
	public enum MarkerType implements StringRepresentable
	{
		SPLAT_ZONE, RAINMAKER_SPAWN, RAINMAKER_PODIUM, CLAM_BASKET, CLAM_SPAWN_AREA;
		public static Codec<MarkerType> CODEC = StringRepresentable.fromEnum(MarkerType::values);
		public static StreamCodec<ByteBuf, MarkerType> STREAM_CODEC = CodecUtils.createEnumPacketCodec(MarkerType::values);
		@Override
		public @NotNull String getSerializedName()
		{
			return name();
		}
	}
}
