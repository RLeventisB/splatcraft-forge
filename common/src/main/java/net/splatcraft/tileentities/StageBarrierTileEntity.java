package net.splatcraft.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.blocks.StageBarrierBlock;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.dummys.ISplatcraftForgeBlockEntityDummy;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.entities.SpawnShieldEntity;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.util.ClientUtils;
import org.jetbrains.annotations.NotNull;

public class StageBarrierTileEntity extends BlockEntity implements ISplatcraftForgeBlockEntityDummy, InkProjectileListener
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
	public void tick()
	{
		if (activeTime > 0)
		{
			activeTime--;
		}
		
		for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(getBlockPos()).inflate(0.05), entity -> !(entity instanceof SpawnShieldEntity)))
		{
			onEntityCollide(entity);
		}
		
		if (level.isClientSide)
			tickClient();
	}
	public void onEntityCollide(Entity entity)
	{
		resetActiveTime();
		if (getBlockState().getBlock() instanceof StageBarrierBlock stageBarrierBlock && stageBarrierBlock.damagesPlayer &&
			entity instanceof Player)
		{
			entity.hurt(SplatcraftDamageTypes.of(level, SplatcraftDamageTypes.OUT_OF_STAGE), Float.MAX_VALUE);
		}
	}
	@OnlyIn(Dist.CLIENT)
	public void tickClient()
	{
		if (ClientUtils.getClientPlayer().isCreative())
		{
			boolean canRender = true;
			Player player = ClientUtils.getClientPlayer();
			int renderDistance = SplatcraftConfig.get("splatcraft.barrierRenderDistance");
			
			if (player.distanceToSqr(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()) > renderDistance * renderDistance)
				canRender = false;
			else if (SplatcraftConfig.get("splatcraft.holdBarrierToRender"))
			{
				canRender = player.getMainHandItem().is(SplatcraftTags.Items.REVEALS_BARRIERS) ||
					player.getMainHandItem().is(SplatcraftTags.Items.REVEALS_BARRIERS);
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
	public void loadAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider wrapperLookup)
	{
		super.loadAdditional(nbt, wrapperLookup);
		
		if (nbt.contains("ActiveTime"))
		{
			activeTime = nbt.getInt("ActiveTime");
		}
	}
	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.@NotNull Provider wrapperLookup)
	{
		compound.putInt("ActiveTime", activeTime);
		super.saveAdditional(compound, wrapperLookup);
	}
	@Override
	public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider wrapperLookup)
	{
		return new CompoundTag()
		{{
			saveAdditional(this, wrapperLookup);
		}};
	}
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket()
	{
		// Will get tag from #toInitialChunkDataNbt
		return ClientboundBlockEntityDataPacket.create(this);
	}
	@Override
	public void phOnDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookup)
	{
		if (level != null)
		{
			BlockState state = level.getBlockState(getBlockPos());
			level.sendBlockUpdated(getBlockPos(), state, state, 2);
			phHandleUpdateTag(pkt.getTag(), lookup);
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
	@Override
	public void onCollide(InkProjectileEntity projectile, @NotNull BlockHitResult result)
	{
		onEntityCollide(projectile);
	}
}
