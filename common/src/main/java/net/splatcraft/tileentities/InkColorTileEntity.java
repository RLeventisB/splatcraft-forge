package net.splatcraft.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.dummys.ISplatcraftForgeBlockDummy;
import net.splatcraft.dummys.ISplatcraftForgeBlockEntityDummy;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkColorTileEntity extends BlockEntity implements IHasTeam, ISplatcraftForgeBlockDummy, ISplatcraftForgeBlockEntityDummy
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
	public void saveAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider registryLookup)
	{
		if (!team.isEmpty())
			nbt.putString("Team", team);
		// no i wont save them like a normal person instead i will make an ItemColorData for each one of them
		nbt.put("ColorData", SplatcraftComponents.ItemColorData.CODEC.encode(new SplatcraftComponents.ItemColorData(true, inverted, color), NbtOps.INSTANCE, nbt).getOrThrow());
		super.saveAdditional(nbt, registryLookup);
	}
	//Nbt Read
	@Override
	public void loadAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider registryLookup)
	{
		super.loadAdditional(nbt, registryLookup);
		SplatcraftComponents.ItemColorData colorData = SplatcraftComponents.ItemColorData.CODEC.decode(NbtOps.INSTANCE, nbt.get("ColorData")).getOrThrow().getFirst();
		color = colorData.color();
		inverted = colorData.hasInvertedColor();
		team = nbt.getString("Team");
	}
	@Override
	public @Nullable Packet<ClientGamePacketListener> getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this);
	}
	@Override
	public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registryLookup)
	{
		CompoundTag nbt = new CompoundTag();
		saveAdditional(nbt, registryLookup);
		return nbt;
	}
	@Override
	public void phHandleUpdateTag(CompoundTag tag, HolderLookup.Provider registryLookup)
	{
		loadWithComponents(tag, registryLookup);
	}
	@Override
	public void phOnDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider)
	{
		if (level != null)
		{
			BlockState state = level.getBlockState(getBlockPos());
			level.sendBlockUpdated(getBlockPos(), state, state, 2);
			phHandleUpdateTag(pkt.getTag(), lookupProvider);
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
