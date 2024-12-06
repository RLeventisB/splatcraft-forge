package net.splatcraft.tileentities;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.util.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkColorTileEntity extends BlockEntity implements IHasTeam
{
    private int color = ColorUtils.DEFAULT;
    private boolean inverted = false;
    private String team = "";

    public InkColorTileEntity(BlockPos pos, BlockState state)
    {
        super(SplatcraftTileEntities.colorTileEntity.get(), pos, state);
    }

    public InkColorTileEntity(BlockEntityType type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
    {
        nbt.putBoolean("Inverted", inverted);
        nbt.putInt("Color", color);
        if (!team.isEmpty())
            nbt.putString("Team", team);
        super.writeNbt(nbt, registryLookup);
    }

    //Nbt Read
    @Override
    public void readNbt(@NotNull NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
    {
        super.readNbt(nbt, registryLookup);
        color = ColorUtils.getColorFromNbt(nbt);
        team = nbt.getString("Team");
        inverted = nbt.getBoolean("Inverted");
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket()
    {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup)
    {
        NbtCompound nbt = new NbtCompound();
        writeNbt(nbt, registryLookup);
        return nbt;
    }

    @Override
    public void handleUpdateTag(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup)
    {
        this.readNbt(tag);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt)
    {
        if (world != null)
        {
            BlockState state = world.getBlockState(getBlockPos());
            world.sendBlockUpdated(this.getPos(), state, state, 2);
            handleUpdateTag(pkt.getTag());
        }
    }

    public int getColor()
    {
        return color;
    }

    public void setColor(int color)
    {
        this.color = color;
    }

    public boolean isInverted()
    {
        return inverted;
    }

    public void setInverted(boolean inverted)
    {
        this.inverted = inverted;
    }

    public String getTeam()
    {
        return team;
    }

    public void setTeam(String team)
    {
        this.team = team;
    }
}
