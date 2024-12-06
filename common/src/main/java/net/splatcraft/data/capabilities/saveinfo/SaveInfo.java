package net.splatcraft.data.capabilities.saveinfo;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.splatcraft.data.PlaySession;
import net.splatcraft.data.Stage;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateStageListPacket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SaveInfo
{
    public final HashMap<String, PlaySession> playSessions = new HashMap<>();
    private final HashMap<String, Stage> stages = new HashMap<>();
    boolean stagesLoaded = false;
    private ArrayList<Integer> colorScores = new ArrayList<>();

    public Collection<Integer> getInitializedColorScores()
    {
        return colorScores;
    }

    public void addInitializedColorScores(Integer... colors)
    {
        for (Integer color : colors)
        {
            if (!colorScores.contains(color))
            {
                colorScores.add(color);
            }
        }
    }

    public void removeColorScore(Integer color)
    {
        colorScores.remove(color);
    }

    public HashMap<String, Stage> getStages()
    {
        return stages;
    }

    public boolean createOrEditStage(Level stageLevel, String stageId, BlockPos corner1, BlockPos corner2, Component stageName)
    {
        if (stageLevel.isClientSide())
            return false;

        if (stages.containsKey(stageId))
        {
            Stage stage = stages.get(stageId);
            stage.seStagetName(stageName);
            stage.updateBounds(stageLevel, corner1, corner2);
            stage.dimID = stageLevel.dimension().location();
        }
        else
            stages.put(stageId, new Stage(stageLevel, corner1, corner2, stageId, stageName));

        SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
        return true;
    }

    public boolean createStage(Level level, String stageId, BlockPos corner1, BlockPos corner2, Component stageName)
    {
        if (level.isClientSide())
            return false;

        if (stages.containsKey(stageId))
            return false;

        stages.put(stageId, new Stage(level, corner1, corner2, stageId, stageName));
        SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
        return true;
    }

    public boolean createStage(Level level, String stageId, BlockPos corner1, BlockPos corner2)
    {
        return createStage(level, stageId, corner1, corner2, Component.literal(stageId));
    }

    public NbtCompound writeNBT(NbtCompound nbt)
    {
        int[] arr = new int[colorScores.size()];
        for (int i = 0; i < colorScores.size(); i++)
        {
            arr[i] = colorScores.get(i);
        }

        nbt.putIntArray("StoredCriteria", arr);

        NbtCompound stageNbt = new NbtCompound();

        for (Map.Entry<String, Stage> e : stages.entrySet())
            stageNbt.put(e.getKey(), e.getValue().writeData());

        nbt.put("Stages", stageNbt);

        NbtCompound playSessions = new NbtCompound();

        for (Map.Entry<String, PlaySession> e : this.playSessions.entrySet())
            playSessions.put(e.getKey(), e.getValue().saveTag());

        nbt.put("PlaySessions", playSessions);

        return nbt;
    }

    public void readNBT(NbtCompound nbt)
    {
        colorScores = new ArrayList<>();
        ScoreboardHandler.clearColorCriteria();

        for (int i : nbt.getIntArray("StoredCriteria"))
        {
            colorScores.add(i);
            ScoreboardHandler.createColorCriterion(i);
        }

        stages.clear();
        for (String key : nbt.getCompound("Stages").getAllKeys())
            stages.put(key, new Stage(nbt.getCompound("Stages").getCompound(key), key));

        playSessions.clear();
        for (String key : nbt.getCompound("PlaySessions").getAllKeys())
            playSessions.put(key, PlaySession.fromTag(Minecraft.getInstance().getSingleplayerServer(), nbt.getCompound("PlaySessions").getCompound(key)));
    }
}