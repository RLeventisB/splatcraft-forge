package net.splatcraft.network.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.handlers.JumpLureHudHandler;
import net.splatcraft.util.InkColor;

import java.util.ArrayList;
import java.util.UUID;

public class SendJumpLureDataPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = new Type<>(Splatcraft.identifierOf("send_jump_lure_data_packet"));
	final InkColor color;
	final boolean canJumpToSpawn;
	final BlockPos spawnPosition;
	final ArrayList<UUID> uuids;
	public SendJumpLureDataPacket(InkColor color, boolean canJumpToSpawn, ArrayList<UUID> uuids, BlockPos spawnPosition)
	{
		this.color = color;
		this.canJumpToSpawn = canJumpToSpawn;
		this.uuids = uuids;
		this.spawnPosition = spawnPosition;
	}
	public static SendJumpLureDataPacket decode(RegistryFriendlyByteBuf buffer)
	{
		InkColor color = InkColor.constructOrReuse(buffer.readInt());
		boolean canJump = buffer.readBoolean();
		BlockPos spawnPosition = buffer.readBlockPos();
		int uuidCount = buffer.readInt();
		ArrayList<UUID> uuids = new ArrayList<>();
		for (int i = 0; i < uuidCount; i++)
			uuids.add(buffer.readUUID());
		
		return new SendJumpLureDataPacket(color, canJump, uuids, spawnPosition);
	}
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeInt(color.getColor());
		buffer.writeBoolean(canJumpToSpawn);
		buffer.writeBlockPos(spawnPosition);
		buffer.writeInt(uuids.size());
		for (UUID uuid : uuids)
			buffer.writeUUID(uuid);
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute()
	{
		JumpLureHudHandler.updateTargetData(new JumpLureHudHandler.SuperJumpTargets(uuids, canJumpToSpawn, color, spawnPosition));
	}
}
