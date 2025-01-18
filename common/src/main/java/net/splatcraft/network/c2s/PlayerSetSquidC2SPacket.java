package net.splatcraft.network.c2s;

import dev.architectury.utils.GameInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.sound.SoundCategory;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.CommonUtils;

public class PlayerSetSquidC2SPacket extends PlayC2SPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(PlayerSetSquidC2SPacket.class);
	private final boolean squid;
	public PlayerSetSquidC2SPacket(boolean squid)
	{
		this.squid = squid;
	}
	public static PlayerSetSquidC2SPacket decode(RegistryByteBuf buffer)
	{
		return new PlayerSetSquidC2SPacket(buffer.readBoolean());
	}
	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
	@Override
	public void encode(RegistryByteBuf buffer)
	{
		buffer.writeBoolean(squid);
	}
	@Override
	public void execute(PlayerEntity player)
	{
		EntityInfo target = EntityInfoCapability.get(player);
		if (squid == target.isSquid() && !GameInstance.getServer().isSingleplayer())
		{
			throw new IllegalStateException(String.format("Squid state did not change for %s (%s)", player.getGameProfile(), squid));
		}
		
		target.setIsSquid(squid);
		if (!squid)
			target.flagSquidCancel();
		player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), squid ? SplatcraftSounds.squidTransform : SplatcraftSounds.squidRevert, SoundCategory.PLAYERS, 0.75F, CommonUtils.nextTriangular(player.getWorld().getRandom(), 0.95f, 0.095f));
		
		SplatcraftPacketHandler.sendToTrackers(new PlayerSetSquidS2CPacket(player.getUuid(), squid), player);
	}
}