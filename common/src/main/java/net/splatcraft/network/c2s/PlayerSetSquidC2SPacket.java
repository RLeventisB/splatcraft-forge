package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.splatcraft.data.capabilities.structs.SquidInfo;
import net.splatcraft.handlers.SquidFormHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.platform.Components;
import net.splatcraft.platform.Services;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

public class PlayerSetSquidC2SPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(PlayerSetSquidC2SPacket.class);
	private final byte data;
	public PlayerSetSquidC2SPacket(boolean squid, boolean checkStorage)
	{
		data = (byte) ((squid ? 1 : 0) | (checkStorage ? 2 : 0));
	}
	public PlayerSetSquidC2SPacket(byte data)
	{
		this.data = data;
	}
	public static PlayerSetSquidC2SPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new PlayerSetSquidC2SPacket(buffer.readByte());
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeByte(data);
	}
	@Override
	public void execute(ServerPlayer player)
	{
		@NotNull SquidInfo target = Components.SQUID_INFO.getOrCreate(player);
		boolean squid = (data & 1) == 1;
		boolean chargeStorage = (data & 2) == 2;
		if (squid == target.isSquid() && !Services.PLATFORM.getServerInstance().isSingleplayer())
		{
			throw new IllegalStateException(String.format("Squid state did not change for %s (%s)", player.getGameProfile(), squid));
		}

		SquidFormHandler.setSquid(player, squid);

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), squid ? SplatcraftSounds.squidTransform : SplatcraftSounds.squidRevert, SoundSource.PLAYERS, 0.75F, CommonUtils.nextTriangular(player.level().getRandom(), 0.95f, 0.095f));

		SplatcraftPacketHandler.sendToTrackers(new PlayerSetSquidS2CPacket(player.getUUID(), squid), player);
	}
}