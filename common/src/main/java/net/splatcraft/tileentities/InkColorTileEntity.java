package net.splatcraft.tileentities;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.splatcraft.blocks.ISplatcraftForgeBlockDummy;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkColorTileEntity extends BlockEntity implements IHasTeam, ISplatcraftForgeBlockDummy
{
	private InkColor color = ColorUtils.getDefaultColor();
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
		if (!team.isEmpty())
			nbt.putString("Team", team);
		NbtElement element = new NbtCompound();
		// no i wont save them like a normal person instead i will make an ItemColorData for each one of them
		SplatcraftComponents.ItemColorData.CODEC.encode(new SplatcraftComponents.ItemColorData(true, inverted, color), NbtOps.INSTANCE, element);
		nbt.put("ColorData", element);
		super.writeNbt(nbt, registryLookup);
	}
	//Nbt Read
	@Override
	public void readNbt(@NotNull NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		SplatcraftComponents.ItemColorData colorData = SplatcraftComponents.ItemColorData.CODEC.decode(NbtOps.INSTANCE, nbt.get("ColorData")).getOrThrow().getFirst();
		color = colorData.color();
		inverted = colorData.hasInvertedColor();
		team = nbt.getString("Team");
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
		readNbt(tag, registryLookup);
	}
	@Override
	public void onDataPacket(ClientConnection net, BlockEntityUpdateS2CPacket pkt, RegistryWrapper.WrapperLookup lookupProvider)
	{
		if (world != null)
		{
			BlockState state = world.getBlockState(getPos());
			world.updateListeners(getPos(), state, state, 2);
			handleUpdateTag(pkt.getNbt(), lookupProvider);
		}
	}
	public InkColor getInkColor()
	{
		return color;
	}
	public void setColor(InkColor color)
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
