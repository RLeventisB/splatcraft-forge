package net.splatcraft.network.c2s;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.SaveInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateStageListPacket;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

public class RequestSetStageRulePacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(RequestSetStageRulePacket.class);
	final String stageId;
	final String ruleId;
	final Boolean value;
	public RequestSetStageRulePacket(String stageId, String ruleId, Boolean value)
	{
		this.stageId = stageId;
		this.ruleId = ruleId;
		this.value = value;
	}
	public static RequestSetStageRulePacket decode(RegistryFriendlyByteBuf buffer)
	{
		int valueIndex = buffer.readInt();
		return new RequestSetStageRulePacket(buffer.readUtf(), buffer.readUtf(), valueIndex == 0 ? null : valueIndex == 1);
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeInt(value == null ? 0 : value ? 1 : 2);
		buffer.writeUtf(stageId);
		buffer.writeUtf(ruleId);
	}
	@Override
	public void execute(Player player)
	{
		Object2ObjectOpenHashMap<String, Stage> stages = SaveInfoCapability.get().stages();
		
		Stage stage = stages.get(stageId);
		stage.applySetting(ruleId.replace(Splatcraft.MODID + ".", ""), value);
		
		SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
	}
}
