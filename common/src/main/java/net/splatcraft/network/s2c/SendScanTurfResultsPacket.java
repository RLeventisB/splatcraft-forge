package net.splatcraft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.Splatcraft;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class SendScanTurfResultsPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = new Type<>(Splatcraft.identifierOf("send_scarf_turf_results_packet"));
	InkColor[] colors;
	Float[] scores;
	int length;
	public SendScanTurfResultsPacket(InkColor[] colors, Float[] scores)
	{
		this.colors = colors;
		this.scores = scores;
		length = Math.min(colors.length, scores.length);
	}
	public static SendScanTurfResultsPacket decode(RegistryFriendlyByteBuf buffer)
	{
		ArrayList<InkColor> colorList = new ArrayList<>();
		ArrayList<Float> scoreList = new ArrayList<>();
		int length = buffer.readInt();
		for (int i = 0; i < length; i++)
		{
			colorList.add(InkColor.constructOrReuse(buffer.readInt()));
			scoreList.add(buffer.readFloat());
		}

		return new SendScanTurfResultsPacket(colorList.toArray(new InkColor[0]), scoreList.toArray(new Float[0]));
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeInt(length);
		for (int i = 0; i < length; i++)
		{
			buffer.writeInt(colors[i].getColor());
			buffer.writeFloat(scores[i]);
		}
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute()
	{
		Player player = ClientUtils.getClientPlayer();
		InkColor winner = InkColor.INVALID;
		float winnerScore = -1;

		for (int i = 0; i < colors.length; i++)
		{
			player.displayClientMessage(Component.translatable("status.scan_turf.score", ColorUtils.getFormatedColorName(colors[i], false), String.format("%.1f", scores[i])), false);
			if (winnerScore < scores[i])
			{
				winnerScore = scores[i];
				winner = colors[i];
			}
		}

		if (winner.isValid())
		{
			player.displayClientMessage(Component.translatable("status.scan_turf.winner", ColorUtils.getFormatedColorName(winner, false)), false);
		}
	}
}
