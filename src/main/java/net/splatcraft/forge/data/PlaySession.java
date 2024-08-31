package net.splatcraft.forge.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.data.capabilities.saveinfo.SaveInfoCapability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class PlaySession
{
    public final List<ServerPlayer> players;
    public final StageGameMode gameMode;
    private final String stageId;
    private final Stage stage;
    public int timer; // wahoo world has done irreparable damage to my brain
    public Level level;

    public PlaySession(Level level, Collection<ServerPlayer> players, Stage stage, StageGameMode gameMode)
    {
        this.players = new ArrayList<>(players.size());
        for (ServerPlayer player : players)
        {
            PlayerInfo playerInfo = PlayerInfoCapability.get(player);
            if (playerInfo != null) // ya know i didnt like look at the forge documentation a i am scared day 1 that this capability returns null or something
            {
                playerInfo.setPlaying(true);
                players.add(player);
            }
        }

        this.gameMode = gameMode;
        this.stageId = stage.id;
        this.stage = stage;
        this.level = level;
        timer = gameMode.DEFAULT_TIME;
    }

    /**
     * Ticks all the play session related actions.
     *
     * @return false if the play session has ended, otherwise, true
     */
    public boolean tick()
    {
        if (players.isEmpty() || (players.size() == 1 && gameMode != StageGameMode.RECON))
        {
            return false;
        }
        if (timer == 0 && !gameMode.overtimeChecker.apply(this))
        {
            return false;
        }
        gameMode.tick.accept(this);
        timer--;
        return true;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        return obj instanceof PlaySession that && Objects.equals(this.players, that.players) &&
                Objects.equals(this.stageId, that.stageId) &&
                Objects.equals(this.gameMode, that.gameMode);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(players, stageId, gameMode);
    }

    @Override
    public String toString()
    {
        return "PlaySession[" +
                "players=" + players + ", " +
                "stageId=" + stageId + ", " +
                "gameMode=" + gameMode + ']';
    }

    public CompoundTag saveTag()
    {
        CompoundTag tag = new CompoundTag();

        tag.putString("LevelId", level.dimensionTypeId().location().toString());
        tag.putString("StageId", stageId);
        tag.putString("GameMode", gameMode.name());
        tag.putString("LevelKey", gameMode.name());
        tag.putInt("Timer", timer);
        tag.putInt("PlayerCount", players.size());
        for (int i = 0; i < players.size(); i++)
        {
            var player = players.get(i);
            tag.putUUID("Player[{" + i + "}]", player.getUUID());
        }

        return tag;
    }

    public static PlaySession fromTag(MinecraftServer server, CompoundTag tag)
    {
        int playerCount = tag.getInt("PlayerCount");
        List<ServerPlayer> players = new ArrayList<>(playerCount);

        Level level = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(tag.getString("LevelId"))));
        for (int i = 0; i < playerCount; i++)
        {
            players.set(i, (ServerPlayer) level.getPlayerByUUID(tag.getUUID("Player[{" + i + "}]")));
        }

        return new PlaySession(level, players, SaveInfoCapability.get(server).getStages().get(tag.getString("StageId")), StageGameMode.valueOf(tag.getString("GameMode")));
    }
}
