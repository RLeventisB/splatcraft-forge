package net.splatcraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.commands.arguments.HighlightTypeArgument;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.data.capabilities.chunkink.ChunkInkCapability;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.structs.RelativeBlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchWaxCommand
{
	private static final DynamicCommandExceptionType ERROR_BLOCK_INVALID = new DynamicCommandExceptionType(
		result -> Component.translatableEscape("argument.block.id.invalid", result)
	);
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(
			Commands.literal("batchwax").requires(commandSource -> commandSource.hasPermission(2))
				.then(Commands.argument("wax", BoolArgumentType.bool())
					.then(Commands.argument("cornerA", BlockPosArgument.blockPos())
						.then(Commands.argument("cornerB", BlockPosArgument.blockPos())
							.executes(
								context -> batchWax(
									context.getSource(),
									BoolArgumentType.getBool(context, "wax"),
									BlockPosArgument.getBlockPos(context, "cornerA"),
									BlockPosArgument.getBlockPos(context, "cornerB"),
									null
								)
							)
							.then(Commands.argument("mask", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.BLOCK))
								.executes(context -> batchWax(
									context.getSource(),
									BoolArgumentType.getBool(context, "wax"),
									BlockPosArgument.getBlockPos(context, "cornerA"),
									BlockPosArgument.getBlockPos(context, "cornerB"),
									ResourceOrTagKeyArgument.getResourceOrTagKey(context, "mask", Registries.BLOCK, ERROR_BLOCK_INVALID)
								))
							))))
				.then(Commands.literal("check")
					.then(Commands.argument("cornerA", BlockPosArgument.blockPos())
						.then(Commands.argument("cornerB", BlockPosArgument.blockPos())
							.executes(context ->
								highlightWax(context.getSource(),
									BlockPosArgument.getBlockPos(context, "cornerA"),
									BlockPosArgument.getBlockPos(context, "cornerB"),
									HighlightTypeArgument.HighlightType.NONE)
							)
							.then(Commands.argument("highlightType", HighlightTypeArgument.highlightType())
								.executes(context ->
									highlightWax(context.getSource(),
										BlockPosArgument.getBlockPos(context, "cornerA"),
										BlockPosArgument.getBlockPos(context, "cornerB"),
										HighlightTypeArgument.getHighlightType(context, "highlightType"))
								)
							))))
		);
	}
	private static int batchWax(CommandSourceStack source, boolean doWax, BlockPos pos1, BlockPos pos2, ResourceOrTagKeyArgument.Result<Block> mask)
	{
		AtomicInteger count = new AtomicInteger();
		source.sendSuccess(() ->
		{
			for (BlockPos pos : BlockPos.betweenClosed(pos1, pos2))
			{
				if (source.getLevel().isEmptyBlock(pos) || (mask != null && !mask.test(source.getLevel().getBlockState(pos).getBlockHolder())))
					continue;
				
				RelativeBlockPos offset = RelativeBlockPos.fromAbsolute(pos);
				ChunkInk worldInk = ChunkInkCapability.getOrCreate(source.getLevel(), pos);
				ChunkInk.BlockEntry ink = worldInk.getInk(offset);
				if (ink == null || ink.immutable != doWax)
				{
					if (doWax)
						worldInk.markInmutable(offset);
					else
						worldInk.markMutable(offset);
					
					ChunkInkHandler.addInkToUpdate(source.getLevel(), new BlockPos(pos));
					BlockState state = source.getLevel().getBlockState(pos);
					source.getLevel().sendBlockUpdated(pos, state, state, 0);
					count.getAndIncrement();
				}
			}
			return Component.literal((doWax ? "Waxed " : "Unwaxed ") + count + " blocks.");
		}, true);
		return count.get();
	}
	private static int highlightWax(CommandSourceStack source, BlockPos pos1, BlockPos pos2, HighlightTypeArgument.HighlightType highlightType)
	{
		AtomicInteger count = new AtomicInteger();
		List<BlockPos> waxedPositions = new ArrayList<>();
		ServerLevel level = source.getLevel();
		source.sendSuccess(() ->
		{
			for (BlockPos pos : BlockPos.betweenClosed(pos1, pos2))
			{
				RelativeBlockPos offset = RelativeBlockPos.fromAbsolute(pos);
				ChunkInk worldInk = ChunkInkCapability.get(level, pos);
				if (worldInk == null)
					continue;
				
				ChunkInk.BlockEntry inkEntry = worldInk.getInk(offset);
				if (inkEntry != null && inkEntry.immutable)
				{
					count.getAndIncrement();
					switch (highlightType)
					{
						case PARTICLE:
							Vec3 center = pos.getCenter();
							createParticle(source, center.x, center.y, center.z, Blocks.BARRIER);
							
							break;
						case EDGE_PARTICLE:
							waxedPositions.add(new BlockPos(pos));
							break;
					}
				}
			}
			return Component.literal("Found " + count + " waxed blocks.");
		}, true);
		
		if (highlightType == HighlightTypeArgument.HighlightType.EDGE_PARTICLE)
		{
			for (VoxelShape shape : CommonUtils.createShapes(level, waxedPositions, pos1))
			{
				shape.forAllEdges((x1, y1, z1, x2, y2, z2) ->
				{
					if (x1 != x2)
					{
						float distance = (float) (x2 - x1);
						float step = CommonUtils.calculateStep(distance, 0.5f);
						for (float i = 0; i - distance < 10e-5f; i += step)
						{
							createParticle(source,
								pos1.getX() + x1 + i,
								pos1.getY() + y1,
								pos1.getZ() + z1, Blocks.REDSTONE_WIRE
							);
						}
					}
					else if (y1 != y2)
					{
						float distance = (float) (y2 - y1);
						float step = CommonUtils.calculateStep(distance, 0.5f);
						for (float i = 0; i - distance < 10e-5f; i += step)
						{
							createParticle(source,
								pos1.getX() + x1,
								pos1.getY() + y1 + i,
								pos1.getZ() + z1, Blocks.REDSTONE_WIRE
							);
						}
					}
					else
					{
						float distance = (float) (z2 - z1);
						float step = CommonUtils.calculateStep(distance, 0.5f);
						for (float i = 0; i - distance < 10e-5f; i += step)
						{
							createParticle(source,
								pos1.getX() + x1,
								pos1.getY() + y1,
								pos1.getZ() + z1 + i, Blocks.REDSTONE_WIRE
							);
						}
					}
				});
			}
		}
		return count.get();
	}
	private static int createParticle(CommandSourceStack source, double x, double y, double z, Block block)
	{
		return source.getLevel().sendParticles(
			new BlockParticleOption(ParticleTypes.BLOCK_MARKER, block.defaultBlockState()),
			x, y, z, 1, 0, 0, 0, 0);
	}
}
