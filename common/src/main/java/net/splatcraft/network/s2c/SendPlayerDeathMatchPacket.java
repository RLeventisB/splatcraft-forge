package net.splatcraft.network.s2c;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.platform.Components;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.UUID;

public class SendPlayerDeathMatchPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendPlayerDeathMatchPacket.class);
	private final int respawnTime;
	private final UUID killerPlayer;
	private final Vector3f killCamDirection;
	public SendPlayerDeathMatchPacket(int respawnTime, Entity killer, Vector3f killCamDirection)
	{
		this(respawnTime, killer instanceof Player ? killer.getUUID() : new UUID(0, 0), killCamDirection);
	}
	public SendPlayerDeathMatchPacket(int respawnTime, UUID killerPlayer, Vector3f killCamDirection)
	{
		this.respawnTime = respawnTime;
		this.killerPlayer = killerPlayer;
		this.killCamDirection = killCamDirection;
	}
	public static SendPlayerDeathMatchPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new SendPlayerDeathMatchPacket(buffer.readInt(), UUIDUtil.STREAM_CODEC.decode(buffer), ByteBufCodecs.VECTOR3F.decode(buffer));
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
		ByteBufCodecs.VECTOR3F.encode(buffer, killCamDirection);
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute()
	{
		LocalPlayer clientPlayer = ClientUtils.getClientPlayer();
		ClientUtils.killCamData = Pair.of(killerPlayer, killCamDirection);
		EntityInfo info = Components.ENTITY_INFO.getOrCreate(clientPlayer);
		
		info.setMatchRespawnTimeLeft(respawnTime);
		info.setIsMatchRespawning(true);
	}
}