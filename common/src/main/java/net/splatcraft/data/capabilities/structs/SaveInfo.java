package net.splatcraft.data.capabilities.structs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.data.PlaySession;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.SaveInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateStageListPacket;
import net.splatcraft.platform.Services;
import net.splatcraft.platform.event.TickEvents;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.structs.InkColor;

public record SaveInfo(Object2ObjectMap<String, Stage> stages,
                       Object2ObjectMap<String, PlaySession> playSessions,
                       ObjectList<InkColor> colorScores)
{
	// todo: make playsessions load after the stages, not at the same time
	public static final Codec<SaveInfo> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		CodecUtils.hashMapCodec(Codec.STRING, Stage.CODEC).fieldOf("stages").forGetter(SaveInfo::stages),
		CodecUtils.hashMapCodec(Codec.STRING, PlaySession.CODEC.codec()).fieldOf("play_sessions").forGetter(SaveInfo::playSessions),
		CodecUtils.arrayList(InkColor.HEX_CODEC).fieldOf("color_scores").forGetter(SaveInfo::colorScores)
	).apply(inst, SaveInfo::new));
	public SaveInfo()
	{
		this(new Object2ObjectOpenHashMap<>(), new Object2ObjectOpenHashMap<>(), new ObjectArrayList<>());
	}
	public static void registerEvents()
	{
		Services.PLATFORM.registerListener(TickEvents.ServerAfter.class, SaveInfo::tickPlaySessions);
		if (Services.PLATFORM.isClientSide())
			Services.PLATFORM.registerListener(TickEvents.ClientLevelAfter.class, SaveInfo::tickPlaySessionsClient);
	}
	@OnlyIn(Dist.CLIENT)
	private static void tickPlaySessionsClient(ClientLevel world)
	{
		if (Services.PLATFORM.isClientSide() && Minecraft.getInstance().isLocalServer()) // singleplayer saveinfo is shared
			return;
		
		SaveInfo info = SaveInfoCapability.get();
		for (PlaySession session : info.playSessions().values())
		{
			Stage stage = Stage.getStage(session.stageId);
			if (stage != null && stage.worldKey == ClientUtils.getClient().level.dimension())
				session.tick(null);
		}
	}
	private static void tickPlaySessions(MinecraftServer server)
	{
		SaveInfo info = SaveInfoCapability.get();
		for (PlaySession session : info.playSessions().values())
		{
			session.tick(server);
		}
	}
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
	public boolean createOrEditStage(MinecraftServer server, ResourceKey<Level> worldKey, String stageId, BlockPos corner1, BlockPos corner2, Component stageName)
	{
		if (stages.containsKey(stageId))
		{
			Stage stage = stages.get(stageId);
			stage.worldKey = worldKey;
			stage.setStageName(stageName);
			stage.updateBounds(stage.getStageWorld(server), corner1, corner2);
		}
		else
			stages.put(stageId, new Stage(server, worldKey, corner1, corner2, stageId, stageName));
		
		SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
		return true;
	}
	public boolean createStage(ServerLevel world, String stageId, BlockPos corner1, BlockPos corner2, Component stageName)
	{
		if (world.isClientSide())
			return false;
		
		if (stages.containsKey(stageId))
			return false;
		
		stages.put(stageId, new Stage(world.getServer(), world.dimension(), corner1, corner2, stageId, stageName));
		SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
		return true;
	}
	public boolean createStage(ServerLevel world, String stageId, BlockPos corner1, BlockPos corner2)
	{
		return createStage(world, stageId, corner1, corner2, Component.literal(stageId));
	}
}