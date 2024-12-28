package net.splatcraft.data.capabilities.saveinfo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.splatcraft.data.PlaySession;
import net.splatcraft.data.Stage;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateStageListPacket;
import net.splatcraft.util.InkColor;

import java.util.List;
import java.util.Map;

public record SaveInfo(Map<String, PlaySession> playSessions, Map<String, Stage> stages, List<InkColor> colorScores)
{
	public static final Codec<SaveInfo> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		Codec.unboundedMap(Codec.STRING, PlaySession.CODEC).fieldOf("PlaySessions").forGetter(SaveInfo::playSessions),
		Codec.unboundedMap(Codec.STRING, Stage.CODEC).fieldOf("Stages").forGetter(SaveInfo::stages),
		Codec.list(InkColor.CODEC).fieldOf("Stages").forGetter(SaveInfo::colorScores)
	).apply(inst, SaveInfo::new));
	public void addInitializedColorScores(InkColor... colors)
	{
		for (InkColor color : colors)
		{
			if (!colorScores.contains(color))
			{
				colorScores.add(color);
			}
		}
	}
	public void removeColorScore(InkColor color)
	{
		colorScores.remove(color);
	}
	public Map<String, Stage> getStages()
	{
		return stages;
	}
	public boolean createOrEditStage(World stageLevel, String stageId, BlockPos corner1, BlockPos corner2, Text stageName)
	{
		if (stageLevel.isClient())
			return false;
		
		if (stages.containsKey(stageId))
		{
			Stage stage = stages.get(stageId);
			stage.setStageName(stageName);
			stage.updateBounds(stageLevel, corner1, corner2);
			stage.dimID = stageLevel.getDimension().effects();
		}
		else
			stages.put(stageId, new Stage(stageLevel, corner1, corner2, stageId, stageName));
		
		SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
		return true;
	}
	public boolean createStage(World world, String stageId, BlockPos corner1, BlockPos corner2, Text stageName)
	{
		if (world.isClient())
			return false;
		
		if (stages.containsKey(stageId))
			return false;
		
		stages.put(stageId, new Stage(world, corner1, corner2, stageId, stageName));
		SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
		return true;
	}
	public boolean createStage(World world, String stageId, BlockPos corner1, BlockPos corner2)
	{
		return createStage(world, stageId, corner1, corner2, Text.literal(stageId));
	}
}