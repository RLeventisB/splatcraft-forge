package net.splatcraft.tileentities;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.blocks.StageBarrierBlock;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.dummys.ISplatcraftForgeBlockEntityDummy;
import net.splatcraft.entities.SpawnShieldEntity;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.util.ClientUtils;
import org.jetbrains.annotations.NotNull;

public class StageBarrierTileEntity extends BlockEntity implements ISplatcraftForgeBlockEntityDummy
{
	public final int maxActiveTime = 20;
	protected int activeTime = maxActiveTime;
	public StageBarrierTileEntity(BlockPos pos, BlockState state)
	{
		super(SplatcraftTileEntities.stageBarrierTileEntity.get(), pos, state);
	}
	public StageBarrierTileEntity(BlockEntityType<? extends StageBarrierTileEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	//	@Override
	public void tick()
	{
		if (activeTime > 0)
		{
			activeTime--;
		}
		
		for (Entity entity : world.getEntitiesByClass(Entity.class, new Box(getPos()).expand(0.05), entity -> !(entity instanceof SpawnShieldEntity)))
		{
			resetActiveTime();
			if (getCachedState().getBlock() instanceof StageBarrierBlock stageBarrierBlock && stageBarrierBlock.damagesPlayer &&
				entity instanceof PlayerEntity)
			{
				entity.damage(SplatcraftDamageTypes.of(world, SplatcraftDamageTypes.OUT_OF_STAGE), Float.MAX_VALUE);
			}
		}
		
		if (world.isClient && ClientUtils.getClientPlayer().isCreative())
		{
			boolean canRender = true;
			PlayerEntity player = ClientUtils.getClientPlayer();
			int renderDistance = SplatcraftConfig.get("splatcraft.barrierRenderDistance");
			
			if (player.squaredDistanceTo(getPos().getX(), getPos().getY(), getPos().getZ()) > renderDistance * renderDistance)
				canRender = false;
			else if (SplatcraftConfig.get("splatcraft.holdBarrierToRender"))
			{
				canRender = player.getMainHandStack().isIn(SplatcraftTags.Items.REVEALS_BARRIERS) ||
					player.getMainHandStack().isIn(SplatcraftTags.Items.REVEALS_BARRIERS);
			}
			if (canRender)
				addActiveTime();
		}
	}
	protected void resetActiveTime()
	{
		activeTime = maxActiveTime;
	}
	protected void addActiveTime()
	{
		activeTime = Math.min(maxActiveTime, activeTime + 3);
	}
	@Override
	public void readNbt(@NotNull NbtCompound nbt, RegistryWrapper.WrapperLookup wrapperLookup)
	{
		super.readNbt(nbt, wrapperLookup);
		
		if (nbt.contains("ActiveTime"))
		{
			activeTime = nbt.getInt("ActiveTime");
		}
	}
	@Override
	public void writeNbt(NbtCompound compound, RegistryWrapper.WrapperLookup wrapperLookup)
	{
		compound.putInt("ActiveTime", activeTime);
		super.writeNbt(compound, wrapperLookup);
	}
	@Override
	public @NotNull NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup wrapperLookup)
	{
		return new NbtCompound()
		{{
			writeNbt(this, wrapperLookup);
		}};
	}
	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket()
	{
		// Will get tag from #toInitialChunkDataNbt
		return BlockEntityUpdateS2CPacket.create(this);
	}
	@Override
	public void phOnDataPacket(ClientConnection net, BlockEntityUpdateS2CPacket pkt, RegistryWrapper.WrapperLookup lookup)
	{
		if (world != null)
		{
			BlockState state = world.getBlockState(getPos());
			world.updateListeners(getPos(), state, state, 2);
			phHandleUpdateTag(pkt.getNbt(), lookup);
		}
	}
	public float getMaxActiveTime()
	{
		return maxActiveTime;
	}
	public float getActiveTime()
	{
		return activeTime;
	}
}
