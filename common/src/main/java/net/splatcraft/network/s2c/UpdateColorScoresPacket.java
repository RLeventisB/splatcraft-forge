package net.splatcraft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.splatcraft.crafting.InkVatColorRecipe;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UpdateColorScoresPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateColorScoresPacket.class);
	public static final StreamCodec<RegistryFriendlyByteBuf, List<InkColor>> COLOR_LIST_CODEC = InkColor.PACKET_CODEC.apply(ByteBufCodecs.list());
	List<InkColor> colors;
	boolean add;
	boolean clear;
	public UpdateColorScoresPacket(boolean clear, boolean add, List<InkColor> color)
	{
		this.clear = clear;
		colors = color;
		this.add = add;
	}
	public static UpdateColorScoresPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new UpdateColorScoresPacket(buffer.readBoolean(), buffer.readBoolean(), COLOR_LIST_CODEC.decode(buffer));
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void execute()
	{
		if (clear)
		{
			ScoreboardHandler.clearColorCriteria();
			InkVatColorRecipe.getOmniList().clear();
		}

		if (add)
		{
			for (InkColor color : colors)
			{
				ScoreboardHandler.createColorCriterion(color);
			}
		}
		else
		{
			for (InkColor color : colors)
			{
				ScoreboardHandler.removeColorCriterion(color);
			}
		}
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeBoolean(clear);
		buffer.writeBoolean(add);
		COLOR_LIST_CODEC.encode(buffer, colors);
	}
}
