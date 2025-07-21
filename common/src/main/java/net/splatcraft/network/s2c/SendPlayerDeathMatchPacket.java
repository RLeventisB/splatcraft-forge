package net.splatcraft.network.s2c;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.platform.Components;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.UUID;

public class SendPlayerDeathMatchPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendPlayerDeathMatchPacket.class);
	private final int respawnTime;
	private final UUID killerPlayer;
	private final Vector2f pitchYawDir;
	public static SendPlayerDeathMatchPacket create(int respawnTime, Entity killer, Entity killed)
	{
		if (killer == null)
			return new SendPlayerDeathMatchPacket(respawnTime, (Entity) null, new Vector2f());
		Vec3 killCamDirection = killer.position().subtract(killed.position()).multiply(1, 0, 1);
		return new SendPlayerDeathMatchPacket(respawnTime, killer, new Vector2f(
			-(float) (Mth.atan2(killCamDirection.x, killCamDirection.z) * Mth.RAD_TO_DEG),
			60f
		));
	}
	public SendPlayerDeathMatchPacket(int respawnTime, Entity killer, Vector2f pitchYawDir)
	{
		this(respawnTime, killer instanceof Player ? killer.getUUID() : new UUID(0, 0), pitchYawDir);
	}
	public SendPlayerDeathMatchPacket(int respawnTime, UUID killerPlayer, Vector2f pitchYawDir)
	{
		this.respawnTime = respawnTime;
		this.killerPlayer = killerPlayer;
		this.pitchYawDir = pitchYawDir;
	}
	public static SendPlayerDeathMatchPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new SendPlayerDeathMatchPacket(buffer.readInt(), UUIDUtil.STREAM_CODEC.decode(buffer), CodecUtils.Codecs.VECTOR2F_STREAM_CODEC.decode(buffer));
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeInt(respawnTime);
		UUIDUtil.STREAM_CODEC.encode(buffer, killerPlayer);
		CodecUtils.Codecs.VECTOR2F_STREAM_CODEC.encode(buffer, pitchYawDir);
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute()
	{
		LocalPlayer clientPlayer = ClientUtils.getClientPlayer();
		if (!killerPlayer.equals(new UUID(0, 0)) && pitchYawDir.lengthSquared() != 0)
			ClientUtils.killCamData = Pair.of(killerPlayer, pitchYawDir);
		EntityInfo info = Components.ENTITY_INFO.getOrCreate(clientPlayer);
		
		info.setMatchRespawnTimeLeft(respawnTime);
		info.setIsMatchRespawning(true);
	}
}