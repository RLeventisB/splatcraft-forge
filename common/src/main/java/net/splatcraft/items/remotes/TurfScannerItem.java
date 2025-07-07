package net.splatcraft.items.remotes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.splatcraft.blocks.IColoredBlock;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendScanTurfResultsPacket;
import net.splatcraft.registries.SplatcraftStats;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.structs.InkColor;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class TurfScannerItem extends RemoteItem
{
	public TurfScannerItem()
	{
		super(new Properties().stacksTo(1), 2);
	}
	public static TurfScanResult scanTurf(Level world, Level outputWorld, BlockPos blockpos, BlockPos blockpos1, int mode, Collection<ServerPlayer> targets)
	{
		BlockPos minPos = new BlockPos(Math.min(blockpos.getX(), blockpos1.getX()), Math.min(blockpos1.getY(), blockpos.getY()), Math.min(blockpos.getZ(), blockpos1.getZ()));
		BlockPos maxPos = new BlockPos(Math.max(blockpos.getX(), blockpos1.getX()), Math.max(blockpos1.getY(), blockpos.getY()), Math.max(blockpos.getZ(), blockpos1.getZ()));

		if (!world.isInWorldBounds(minPos) || !world.isInWorldBounds(maxPos))
			return new TurfScanResult(false, Component.translatable("status.scan_turf.out_of_world"));

		if (world.isClientSide())
		{
			return new TurfScanResult(true, null);
		}
		TreeMap<InkColor, Integer> scores = new TreeMap<>();
		int facesTotal = 0;
		int affectedBlockTotal = 0;

		if (mode == 0)
		{
			for (int x = minPos.getX(); x <= maxPos.getX(); x++)
			{
				for (int z = minPos.getZ(); z <= maxPos.getZ(); z++)
				{
					BlockPos checkPos = getTopSolidOrLiquidBlock(x, z, world, minPos.getY(), maxPos.getY() + 1);
					if (checkPos == null)
						continue;

					BlockState checkState = world.getBlockState(checkPos);

					if (checkPos.getY() > maxPos.getY() || !checkState.blocksMotion() || checkState.liquid())
						continue;

					InkColor color;
					ChunkInk.BlockEntry entry = InkBlockUtils.getInkBlock(world, checkPos);

					if (entry != null && entry.isInkedAny())
					{
						for (byte i = 0; i < 6; i++)
						{
							if (entry.isInked(i))
							{
								color = entry.color(i);

								if (scores.containsKey(color))
									scores.replace(color, scores.get(color) + 1);
								else scores.put(color, 1);
								facesTotal++;
							}
						}
					}
					else if (world.getBlockState(checkPos).is(SplatcraftTags.Blocks.SCAN_TURF_SCORED) &&
						world.getBlockState(checkPos).getBlock() instanceof IColoredBlock coloredBlock)
					{
						color = coloredBlock.getColor(world, checkPos);
						if (scores.containsKey(color))
							scores.replace(color, scores.get(color) + 6);
						else scores.put(color, 6);
						facesTotal += 6;
					}
				}
			}
		}
		else if (mode == 1)
		{
			for (int x = minPos.getX(); x <= maxPos.getX(); x++)
			{
				for (int z = minPos.getZ(); z <= maxPos.getZ(); z++)
				{
					for (int y = minPos.getY(); y <= maxPos.getY(); y++)
					{
						BlockPos checkPos = new BlockPos(x, y, z);
						BlockState checkState = world.getBlockState(checkPos);
						boolean isWall = false;

						for (int j = 1; j <= 2; j++)
						{
							if (world.isOutsideBuildHeight(checkPos.above(j)))
								break;
							if (!InkBlockUtils.canInkPassthrough(world, checkPos.above(j)))
							{
								isWall = true;
								break;
							}

							if (j > maxPos.getY())
								break;
						}

						if (isWall)
							continue;

						if (!checkState.blocksMotion() || checkState.liquid())
							continue;

						InkColor color;
						ChunkInk.BlockEntry entry = InkBlockUtils.getInkBlock(world, checkPos);

						if (entry != null && entry.isInkedAny())
						{
							for (byte i = 0; i < 6; i++)
							{
								if (entry.isInked(i))
								{
									color = entry.color(i);

									if (scores.containsKey(color))
										scores.replace(color, scores.get(color) + 1);
									else scores.put(color, 1);
									facesTotal++;
								}
							}
						}
						else if (world.getBlockState(checkPos).is(SplatcraftTags.Blocks.SCAN_TURF_SCORED) &&
							world.getBlockState(checkPos).getBlock() instanceof IColoredBlock coloredBlock)
						{
							color = coloredBlock.getColor(world, checkPos);
							if (scores.containsKey(color))
								scores.replace(color, scores.get(color) + 6);
							else scores.put(color, 6);
							facesTotal += 6;
						}
					}
				}
			}
		}

		InkColor[] colors = new InkColor[scores.size()];
		Float[] colorScores = new Float[scores.size()];

		InkColor winner = InkColor.INVALID;
		float winnerScore = -1;
		int i = 0;
		for (Map.Entry<InkColor, Integer> entry : scores.entrySet())
		{
			colors[i] = entry.getKey();
			colorScores[i] = entry.getValue() / (float) facesTotal * 100;

			if (winnerScore < entry.getValue())
			{
				winner = entry.getKey();
				winnerScore = entry.getValue();
			}

			i++;
		}

		for (Player player : targets == ALL_TARGETS ? outputWorld.players() : targets)
		{
			InkColor color = ColorUtils.getEntityColor(player);

			if (scores.containsKey(color) && player instanceof ServerPlayer serverPlayer)
				SplatcraftStats.SCAN_TURF_TRIGGER.get().trigger(serverPlayer, scores.get(color), color == winner);

			if (color == winner)
				player.awardStat(SplatcraftStats.TURF_WARS_WON);

			ScoreboardHandler.updatePlayerScore(Stats.CUSTOM.get(ScoreboardHandler.TURF_WAR_SCORE), player, scores.getOrDefault(color, 0));

			if (!ScoreboardHandler.hasColorCriterion(color))
				continue;

			ObjectiveCriteria criterion = color == winner ? ScoreboardHandler.getColorWins(color) : ScoreboardHandler.getColorLosses(color);
			outputWorld.getScoreboard().forAllObjectives(criterion, player, score -> score.add(1));
		}

		if (scores.isEmpty())
		{
			return new TurfScanResult(false, Component.translatable("status.scan_turf.no_ink"));
		}
		else
		{
			SendScanTurfResultsPacket packet = new SendScanTurfResultsPacket(colors, colorScores);
			if (targets == ALL_TARGETS)
				SplatcraftPacketHandler.sendToDim(packet, outputWorld.dimension());
			else for (ServerPlayer target : targets)
				SplatcraftPacketHandler.sendToPlayer(packet, target);
		}

		return (TurfScanResult) new TurfScanResult(true, Component.translatable("commands.scanturf.success", facesTotal), scores, facesTotal).setIntResults(winner.getColor(), (int) ((float) affectedBlockTotal / facesTotal * 15));
	}
	public static BlockPos getTopSolidOrLiquidBlock(int x, int z, Level world, int min, int max)
	{
		ChunkAccess chunk = world.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));

		int highestNonEmptySection = chunk.getHighestFilledSectionIndex();
		if (highestNonEmptySection == -1)
			return null;

		BlockPos.MutableBlockPos blockpos = new BlockPos(x, Math.min(SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(highestNonEmptySection)) + 16, max), z).mutable();
		while (world.isInWorldBounds(blockpos) && blockpos.getY() >= min)
		{
			BlockState state = chunk.getBlockState(blockpos);

			if (state.is(SplatcraftTags.Blocks.SCAN_TURF_IGNORED) || !InkBlockUtils.canInkPassthrough(world, blockpos) ||
				state.blocksMotion())
			{
				return blockpos;
			}
			blockpos.move(0, -1, 0);
		}

		return blockpos;
	}
	@Override
	public RemoteResult onRemoteUse(Level usedOnWorld, BlockPos posA, BlockPos posB, ItemStack stack, InkColor colorIn, int mode, Collection<ServerPlayer> targets)
	{
		return scanTurf(getLevel(usedOnWorld, stack), usedOnWorld, posA, posB, mode, targets);
	}
	public static class TurfScanResult extends RemoteResult
	{
		final TreeMap<InkColor, Integer> scores;
		final int totalBlocksScanned;
		public TurfScanResult(boolean success, Component output)
		{
			this(success, output, new TreeMap<>(), 0);
		}
		public TurfScanResult(boolean success, Component output, TreeMap<InkColor, Integer> scores, int scanVolume)
		{
			super(success, output);
			this.scores = scores;
			totalBlocksScanned = scanVolume;
		}
		public TreeMap<InkColor, Integer> getScores()
		{
			return scores;
		}
		public int getScanVolume()
		{
			return totalBlocksScanned;
		}
	}
}