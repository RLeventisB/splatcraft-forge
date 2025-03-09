package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.util.CommonUtils;

public class SwapSlotWithOffhandPacket extends PlayC2SPacket
{
    public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SwapSlotWithOffhandPacket.class);
    final int slot;
    final boolean stopUsing;

    public SwapSlotWithOffhandPacket(int slot, boolean stopUsing)
    {
        this.slot = slot;
        this.stopUsing = stopUsing;
    }

    public static SwapSlotWithOffhandPacket decode(RegistryFriendlyByteBuf buffer)
    {
        return new SwapSlotWithOffhandPacket(buffer.readInt(), buffer.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return ID;
    }

    @Override
    public void execute(Player player)
    {
        ItemStack stack = player.getOffhandItem();
        player.setItemInHand(InteractionHand.OFF_HAND, player.getInventory().getItem(slot));
        player.getInventory().setItem(slot, stack);
        player.releaseUsingItem();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeInt(slot);
        buffer.writeBoolean(stopUsing);
    }
}
