package net.splatcraft.data;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.World;
import net.splatcraft.data.capabilities.playerinfo.EntityInfo;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class PlaySession
{
	public static final Codec<PlaySession> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		Codecs.GAME_PROFILE_WITH_PROPERTIES.listOf().fieldOf("Players").forGetter(v -> v.players),
		StageGameMode.CODEC.fieldOf("GameMode").forGetter(v -> v.gameMode),
		Codec.STRING.fieldOf("StageId").forGetter(v -> v.stageId),
		Codec.INT.fieldOf("Timer").forGetter(v -> v.timer),
		RegistryKey.createCodec(RegistryKeys.WORLD).fieldOf("World").forGetter(v -> v.world)
	).apply(inst, PlaySession::new));
	public final List<GameProfile> players;
	public final StageGameMode gameMode;
	private final String stageId;
	public int timer; // wahoo world has done irreparable damage to my brain
	public RegistryKey<World> world;
	public PlaySession(World world, Collection<ServerPlayerEntity> players, Stage stage, StageGameMode gameMode)
	{
		this.players = players.stream().map(PlayerEntity::getGameProfile).toList();
		players.forEach((player) ->
		{
			EntityInfo playerInfo = EntityInfoCapability.get(player);
			playerInfo.setPlaying(true);
			players.add(player);
		});
		
		this.gameMode = gameMode;
		stageId = stage.id;
		this.world = world.getRegistryKey();
		timer = gameMode.DEFAULT_TIME;
	}
	public PlaySession(List<GameProfile> players, StageGameMode gameMode, String stageId, Integer timer, RegistryKey<World> world)
	{
		this.players = players;
		this.gameMode = gameMode;
		this.stageId = stageId;
		this.timer = timer;
		this.world = world;
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
		return obj instanceof PlaySession that && Objects.equals(players, that.players) &&
			Objects.equals(stageId, that.stageId) &&
			Objects.equals(gameMode, that.gameMode);
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
}
