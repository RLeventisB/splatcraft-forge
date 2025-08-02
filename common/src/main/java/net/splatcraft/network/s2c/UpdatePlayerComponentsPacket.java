package net.splatcraft.network.s2c;

import com.google.common.collect.ImmutableList;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.platform.ComponentData;
import net.splatcraft.platform.Components;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class UpdatePlayerComponentsPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdatePlayerComponentsPacket.class);
	final UUID target;
	final byte componentFlag;
	final List<Object> components;
	protected UpdatePlayerComponentsPacket(UUID player, byte componentFlag, List<Object> components)
	{
		target = player;
		this.componentFlag = componentFlag;
		this.components = components;
	}
	public UpdatePlayerComponentsPacket(LivingEntity target, byte componentFlag)
	{
		this(target.getUUID(), componentFlag,
			Components.Bits.getComponents(componentFlag, LivingEntity.class)
				.stream().map(v -> v.getOrCreate(target)).toList());
	}
	public static UpdatePlayerComponentsPacket decode(RegistryFriendlyByteBuf buffer)
	{
		UUID player = buffer.readUUID();
		if (player.equals(Util.NIL_UUID))
			return new UpdatePlayerComponentsPacket(player, (byte) 0, List.of());
		
		byte componentFlag = buffer.readByte();
		
		ImmutableList.Builder<Object> componentsBuilder = ImmutableList.builder();
		Iterator<ComponentData<LivingEntity, Object>> iterator = Components.Bits.getComponentIterator(componentFlag, LivingEntity.class);
		while (iterator.hasNext())
		{
			ComponentData<LivingEntity, Object> componentData = iterator.next();
			componentsBuilder.add(componentData.getStreamCodec().decode(buffer));
		}
		
		return new UpdatePlayerComponentsPacket(player, componentFlag, componentsBuilder.build());
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
		buffer.writeByte(componentFlag);
		
		Iterator<ComponentData<LivingEntity, Object>> iterator = Components.Bits.getComponentIterator(componentFlag, LivingEntity.class);
		int i = 0;
		while (iterator.hasNext())
		{
			ComponentData<LivingEntity, Object> componentData = iterator.next();
			componentData.getStreamCodec().encode(buffer, components.get(i));
			i++;
		}
	}
	@Override
	public void execute()
	{
		if (componentFlag == 0)
			return;
		
		Player target = Minecraft.getInstance().level.getPlayerByUUID(this.target);
		
		if (target == null)
			return;
		
		Iterator<ComponentData<LivingEntity, Object>> iterator = Components.Bits.getComponentIterator(componentFlag, LivingEntity.class);
		int i = 0;
		while (iterator.hasNext())
		{
			ComponentData<LivingEntity, Object> componentData = iterator.next();
			componentData.set(target, components.get(i));
			i++;
		}
	}
}
