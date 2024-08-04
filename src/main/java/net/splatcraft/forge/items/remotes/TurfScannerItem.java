package net.splatcraft.forge.items.remotes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.splatcraft.forge.blocks.IColoredBlock;
import net.splatcraft.forge.data.SplatcraftTags;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInk;
import net.splatcraft.forge.handlers.ScoreboardHandler;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.s2c.SendScanTurfResultsPacket;
import net.splatcraft.forge.registries.SplatcraftItemGroups;
import net.splatcraft.forge.registries.SplatcraftStats;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.InkBlockUtils;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class TurfScannerItem extends RemoteItem
{
	public TurfScannerItem()
	{
		super(new Properties().stacksTo(1), 2);
		SplatcraftItemGroups.addGeneralItem(this);
	}
	public static TurfScanResult scanTurf(Level level, Level outputWorld, BlockPos blockpos, BlockPos blockpos1, int mode, Collection<ServerPlayer> targets)
	{
		BlockPos minPos = new BlockPos(Math.min(blockpos.getX(), blockpos1.getX()), Math.min(blockpos1.getY(), blockpos.getY()), Math.min(blockpos.getZ(), blockpos1.getZ()));
		BlockPos maxPos = new BlockPos(Math.max(blockpos.getX(), blockpos1.getX()), Math.max(blockpos1.getY(), blockpos.getY()), Math.max(blockpos.getZ(), blockpos1.getZ()));
		
		if (!level.isInWorldBounds(minPos) || !level.isInWorldBounds(maxPos))
			return new TurfScanResult(false, Component.translatable("status.scan_turf.out_of_stage"));
		
		if (level.isClientSide)
		{
			return new TurfScanResult(true, null);
		}
		TreeMap<Integer, Integer> scores = new TreeMap<>();
		int facesTotal = 0;
		int affectedBlockTotal = 0;
		
		if (mode == 0)
		{
			for (int x = minPos.getX(); x <= maxPos.getX(); x++)
			{
				for (int z = minPos.getZ(); z <= maxPos.getZ(); z++)
				{
					BlockPos checkPos = getTopSolidOrLiquidBlock(new BlockPos(x, 0, z), level, minPos.getY(), maxPos.getY() + 1);
					BlockState checkState = level.getBlockState(checkPos);
					
					if (checkPos.getY() > maxPos.getY() || !checkState.blocksMotion() || checkState.liquid())
						continue;
					
					int color;
					ChunkInk.BlockEntry entry = InkBlockUtils.getInkBlock(level, checkPos);
					
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
					else if (level.getBlockState(checkPos).is(SplatcraftTags.Blocks.SCAN_TURF_SCORED) &&
						level.getBlockState(checkPos).getBlock() instanceof IColoredBlock coloredBlock)
					{
						color = coloredBlock.getColor(level, checkPos);
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
						BlockState checkState = level.getBlockState(checkPos);
						boolean isWall = false;
						
						for (int j = 1; j <= 2; j++)
						{
							if (level.isOutsideBuildHeight(checkPos.above(j)))
								break;
							if (!InkBlockUtils.canInkPassthrough(level, checkPos.above(j)))
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
						
						int color;
						ChunkInk.BlockEntry entry = InkBlockUtils.getInkBlock(level, checkPos);
						
						if (entry.isInkedAny())
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
						else if (level.getBlockState(checkPos).is(SplatcraftTags.Blocks.SCAN_TURF_SCORED) &&
							level.getBlockState(checkPos).getBlock() instanceof IColoredBlock coloredBlock)
						{
							color = coloredBlock.getColor(level, checkPos);
							if (scores.containsKey(color))
								scores.replace(color, scores.get(color) + 6);
							else scores.put(color, 6);
							facesTotal += 6;
						}
					}
				}
			}
		}
		
		Integer[] colors = new Integer[scores.size()];
		Float[] colorScores = new Float[scores.size()];
		
		int winner = -1;
		float winnerScore = -1;
		int i = 0;
		for (Map.Entry<Integer, Integer> entry : scores.entrySet())
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
			int color = ColorUtils.getPlayerColor(player);
			
			if (scores.containsKey(color) && player instanceof ServerPlayer serverPlayer)
				SplatcraftStats.SCAN_TURF_TRIGGER.trigger(serverPlayer, scores.get(color), color == winner);
			
			if (color == winner)
				player.awardStat(SplatcraftStats.TURF_WARS_WON);
			
			ScoreboardHandler.updatePlayerScore(ScoreboardHandler.TURF_WAR_SCORE, player, scores.getOrDefault(color, 0));
			
			if (!ScoreboardHandler.hasColorCriterion(color))
				continue;
			
			ObjectiveCriteria criterion = color == winner ? ScoreboardHandler.getColorWins(color) : ScoreboardHandler.getColorLosses(color);
			outputWorld.getScoreboard().forAllObjectives(criterion, player.getScoreboardName(), score -> score.add(1));
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
		
		return (TurfScanResult) new TurfScanResult(true, Component.translatable("commands.scanturf.success", facesTotal), scores, facesTotal).setIntResults(winner, (int) ((float) affectedBlockTotal / facesTotal * 15));
	}
	private static BlockPos getTopSolidOrLiquidBlock(BlockPos pos, Level level, int min, int max)
	{
		LevelChunk chunk = level.getChunkAt(pos);
		
		BlockPos blockpos = new BlockPos(pos.getX(), Math.min(chunk.getHighestFilledSectionIndex() + 16, max), pos.getZ());
		while (level.isInWorldBounds(blockpos) && blockpos.getY() >= min)
		{
			BlockState state = chunk.getBlockState(blockpos);
			
			if (state.is(SplatcraftTags.Blocks.SCAN_TURF_IGNORED) || !InkBlockUtils.canInkPassthrough(level, blockpos) ||
				state.blocksMotion())
			{
				break;
			}
			blockpos = blockpos.below();
		}
		
		return blockpos;
	}
	@Override
	public RemoteResult onRemoteUse(Level usedOnWorld, BlockPos posA, BlockPos posB, ItemStack stack, int colorIn, int mode, Collection<ServerPlayer> targets)
	{
		return scanTurf(getLevel(usedOnWorld, stack), usedOnWorld, posA, posB, mode, targets);
	}
	public static class TurfScanResult extends RemoteResult
	{
		final TreeMap<Integer, Integer> scores;
		final int totalBlocksScanned;
		public TurfScanResult(boolean success, Component output)
		{
			this(success, output, new TreeMap<>(), 0);
		}
		public TurfScanResult(boolean success, Component output, TreeMap<Integer, Integer> scores, int scanVolume)
		{
			super(success, output);
			this.scores = scores;
			this.totalBlocksScanned = scanVolume;
		}
		public TreeMap<Integer, Integer> getScores()
		{
			return scores;
		}
		public int getScanVolume()
		{
			return totalBlocksScanned;
		}
	}
}
