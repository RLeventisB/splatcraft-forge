package net.splatcraft.tileentities;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.items.remotes.RemoteItem;
import net.splatcraft.registries.SplatcraftTileEntities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemotePedestalTileEntity extends InkColorTileEntity implements SidedInventory, CommandOutput
{
	protected ItemStack remote = ItemStack.EMPTY;
	protected int signal = 0;
	protected int remoteResult = 0;
	public RemotePedestalTileEntity(BlockPos pos, BlockState state)
	{
		super(SplatcraftTileEntities.remotePedestalTileEntity.get(), pos, state);
	}
	public void onPowered()
	{
		if (!(remote.getItem() instanceof RemoteItem remote))
		{
			signal = 0;
			return;
		}
		
		RemoteItem.RemoteResult result = remote.onRemoteUse(world, this.remote, getInkColor(), pos.toCenterPos(), null);
		signal = result.getComparatorResult();
		remoteResult = result.getCommandResult();
	}
	@Override
	public @NotNull NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup)
	{
		return new NbtCompound()
		{{
			writeNbt(this, lookup);
		}};
	}
	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket()
	{
		// Will get tag from #toInitialChunkDataNbt
		return BlockEntityUpdateS2CPacket.create(this);
	}
	@Override
	public void phOnDataPacket(ClientConnection net, BlockEntityUpdateS2CPacket pkt, RegistryWrapper.WrapperLookup wrapperLookup)
	{
		if (world != null)
		{
			BlockState state = world.getBlockState(getPos());
			world.updateListeners(getPos(), state, state, 2);
			phHandleUpdateTag(pkt.getNbt(), wrapperLookup);
		}
	}
	@Override
	public int @NotNull [] getAvailableSlots(@NotNull Direction direction)
	{
		return new int[] {0};
	}
	@Override
	public boolean canInsert(int i, ItemStack itemStack, @Nullable Direction direction)
	{
		return itemStack.isIn(SplatcraftTags.Items.REMOTES);
	}
	@Override
	public boolean canExtract(int i, @NotNull ItemStack itemStack, @NotNull Direction direction)
	{
		return true;
	}
	@Override
	public int size()
	{
		return 1;
	}
	@Override
	public boolean isEmpty()
	{
		return remote.isEmpty();
	}
	@Override
	public @NotNull ItemStack getStack(int i)
	{
		return remote;
	}
	@Override
	public @NotNull ItemStack removeStack(int i, int count)
	{
		return remote.split(count);
	}
	@Override
	public @NotNull ItemStack removeStack(int i)
	{
		ItemStack copy = remote.copy();
		remote = ItemStack.EMPTY;
		return copy;
	}
	@Override
	public void setStack(int i, @NotNull ItemStack itemStack)
	{
		remote = itemStack;
	}
	@Override
	public boolean canPlayerUse(@NotNull PlayerEntity player)
	{
		if (world.getBlockEntity(getPos()) != this)
			return false;
		return !(player.squaredDistanceTo((double) getPos().getX() + 0.5D, (double) getPos().getY() + 0.5D, (double) getPos().getZ() + 0.5D) > 64.0D);
	}
	@Override
	public void clear()
	{
		remote = ItemStack.EMPTY;
	}
	@Override
	public void readNbt(@NotNull NbtCompound nbt, RegistryWrapper.WrapperLookup lookup)
	{
		super.readNbt(nbt, lookup);
		
		signal = nbt.getInt("Signal");
		
		if (nbt.contains("Remote"))
			remote = ItemStack.fromNbtOrEmpty(lookup, nbt.getCompound("Remote"));
		
		if (nbt.contains("RemoteResult"))
			remoteResult = nbt.getInt("RemoteResult");
	}
	@Override
	public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup)
	{
		nbt.putInt("Signal", signal);
		
		if (!remote.isEmpty())
			nbt.put("Remote", remote.encode(lookup, new NbtCompound()));
		if (remoteResult != 0)
			nbt.putInt("RemoteResult", remoteResult);
		
		super.writeNbt(nbt, lookup);
	}
	public int getSignal()
	{
		return signal;
	}
	@Override
	public void sendMessage(@NotNull Text message)
	{
	}
	@Override
	public boolean shouldReceiveFeedback()
	{
		return false;
	}
	@Override
	public boolean shouldTrackOutput()
	{
		return false;
	}
	@Override
	public boolean shouldBroadcastConsoleToOps()
	{
		return false;
	}
}
