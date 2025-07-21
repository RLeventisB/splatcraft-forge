package net.splatcraft.network.s2c;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.data.InkColorGroup;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SendColorRegistryPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendColorRegistryPacket.class);
	private static final StreamCodec<RegistryFriendlyByteBuf, BiMap<ResourceLocation, InkColor>> COLORS_STREAM_CODEC =
		ByteBufCodecs.map(HashBiMap::create, ResourceLocation.STREAM_CODEC, InkColor.STREAM_CODEC);
	private static final StreamCodec<RegistryFriendlyByteBuf, Map<ResourceLocation, InkColorGroup>> GROUPS_STREAM_CODEC =
		ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, InkColorGroup.STREAM_CODEC);
	private final BiMap<ResourceLocation, InkColor> colors;
	private final Map<ResourceLocation, InkColorGroup> groups;
	public SendColorRegistryPacket(BiMap<ResourceLocation, InkColor> colors, Map<ResourceLocation, InkColorGroup> groups)
	{
		this.colors = colors;
		this.groups = groups;
	}
	public static SendColorRegistryPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new SendColorRegistryPacket(COLORS_STREAM_CODEC.decode(buffer), GROUPS_STREAM_CODEC.decode(buffer));
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		COLORS_STREAM_CODEC.encode(buffer, colors);
		GROUPS_STREAM_CODEC.encode(buffer, groups);
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute()
	{
		InkColorRegistry.REGISTRY.clear();
		InkColorRegistry.REGISTRY.putAll(colors);
		
		InkColorGroup.setRegistry(groups);
	}
}