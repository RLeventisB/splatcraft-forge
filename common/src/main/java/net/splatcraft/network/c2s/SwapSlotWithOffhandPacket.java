package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;
import net.splatcraft.util.CommonUtils;

public class SwapSlotWithOffhandPacket extends PlayC2SPacket
{
    public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(SwapSlotWithOffhandPacket.class);
    final int slot;
    final boolean stopUsing;

    public SwapSlotWithOffhandPacket(int slot, boolean stopUsing)
    {
        this.slot = slot;
        this.stopUsing = stopUsing;
    }

    public static SwapSlotWithOffhandPacket decode(RegistryByteBuf buffer)
    {
        return new SwapSlotWithOffhandPacket(buffer.readInt(), buffer.readBoolean());
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void execute(PlayerEntity player)
    {
        ItemStack stack = player.getOffHandStack();
        player.setStackInHand(Hand.OFF_HAND, player.getInventory().getStack(slot));
        player.getInventory().setStack(slot, stack);
        player.stopUsingItem();
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeInt(slot);
        buffer.writeBoolean(stopUsing);
    }
}
