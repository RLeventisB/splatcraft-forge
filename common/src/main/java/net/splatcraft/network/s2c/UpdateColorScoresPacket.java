package net.splatcraft.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.crafting.InkVatColorRecipe;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;

import java.util.List;

public class UpdateColorScoresPacket extends PlayS2CPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(UpdateColorScoresPacket.class);
	public static final PacketCodec<RegistryByteBuf, List<InkColor>> COLOR_LIST_CODEC = InkColor.PACKET_CODEC.collect(PacketCodecs.toList());
	List<InkColor> colors;
	boolean add;
	boolean clear;
	public UpdateColorScoresPacket(boolean clear, boolean add, List<InkColor> color)
	{
		this.clear = clear;
		colors = color;
		this.add = add;
	}
	public static UpdateColorScoresPacket decode(RegistryByteBuf buffer)
	{
		return new UpdateColorScoresPacket(buffer.readBoolean(), buffer.readBoolean(), COLOR_LIST_CODEC.decode(buffer));
	}
	@Override
	public Id<? extends CustomPayload> getId()
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
	public void encode(RegistryByteBuf buffer)
	{
		buffer.writeBoolean(clear);
		buffer.writeBoolean(add);
		COLOR_LIST_CODEC.encode(buffer, colors);
	}
}
