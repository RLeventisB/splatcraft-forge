package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.platform.Services;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

public class PlayerSetSquidC2SPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(PlayerSetSquidC2SPacket.class);
	private final boolean squid;
	public PlayerSetSquidC2SPacket(boolean squid)
	{
		this.squid = squid;
	}
	public static PlayerSetSquidC2SPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new PlayerSetSquidC2SPacket(buffer.readBoolean());
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeBoolean(squid);
	}
	@Override
	public void execute(Player player)
	{
		EntityInfo target = EntityInfoCapability.get(player);
		if (squid == target.isSquid() && !Services.PLATFORM.getServerInstance().isSingleplayer())
		{
			throw new IllegalStateException(String.format("Squid state did not change for %s (%s)", player.getGameProfile(), squid));
		}
		
		target.setIsSquid(squid);
		if (!squid)
			target.flagSquidCancel();
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), squid ? SplatcraftSounds.squidTransform : SplatcraftSounds.squidRevert, SoundSource.PLAYERS, 0.75F, CommonUtils.nextTriangular(player.level().getRandom(), 0.95f, 0.095f));
		
		SplatcraftPacketHandler.sendToTrackers(new PlayerSetSquidS2CPacket(player.getUUID(), squid), player);
	}
}