package net.splatcraft.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.action.EntityAction;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class UpdateEntityActionOnlyPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateEntityActionOnlyPacket.class);
	UUID target;
	Tag nbt;
	public UpdateEntityActionOnlyPacket(UUID player, Tag nbt)
	{
		target = player;
		this.nbt = nbt;
	}
	public static UpdateEntityActionOnlyPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new UpdateEntityActionOnlyPacket(buffer.readUUID(), buffer.readNbt());
	}
	public static UpdateEntityActionOnlyPacket create(LivingEntity target)
	{
		EntityAction action = EntityAction.getEntityAction(target);
		if (action == null)
			return new UpdateEntityActionOnlyPacket(target.getUUID(), EndTag.INSTANCE);
		return new UpdateEntityActionOnlyPacket(target.getUUID(), EntityAction.SERIALIZER_CODEC.encodeStart(NbtOps.INSTANCE, action).getOrThrow());
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeUUID(target);
		buffer.writeNbt(nbt);
	}
	@Override
	public void execute()
	{
		Player target = Minecraft.getInstance().level.getPlayerByUUID(this.target);
		
		if (target == null)
			return;
		
		if (nbt == null || Objects.equals(nbt, EndTag.INSTANCE))
		{
			EntityAction.setEntityAction(target, null, true, true);
			return;
		}
		
		EntityAction.setEntityAction(target, EntityAction.SERIALIZER_CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow(), true, true);
	}
}
