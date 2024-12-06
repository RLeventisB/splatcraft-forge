package net.splatcraft.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtCompound;
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
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.blocks.StageBarrierBlock;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.entities.SpawnShieldEntity;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.util.ClientUtils;
import org.jetbrains.annotations.NotNull;

public class StageBarrierTileEntity extends BlockEntity
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

    //@Override
    public void tick()
    {
        if (activeTime > 0)
        {
            activeTime--;
        }

        for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(getBlockPos()).inflate(0.05)))
        {
            if (entity instanceof SpawnShieldEntity)
                continue;

            resetActiveTime();
            if (getBlockState().getBlock() instanceof StageBarrierBlock stageBarrierBlock && stageBarrierBlock.damagesPlayer &&
                entity instanceof Player)
            {
                entity.hurt(level.damageSources().source(SplatcraftDamageTypes.OUT_OF_STAGE), Float.MAX_VALUE);
            }
        }

        if (level.isClientSide && ClientUtils.getClientPlayer().isCreative())
        {
            boolean canRender = true;
            Player player = ClientUtils.getClientPlayer();
            int renderDistance = SplatcraftConfig.Client.barrierRenderDistance.get();

            if (player.distanceToSqr(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()) > renderDistance * renderDistance)
                canRender = false;
            else if (SplatcraftConfig.Client.holdBarrierToRender.get())
            {
                canRender = player.getMainHandItem().is(SplatcraftTags.Items.REVEALS_BARRIERS) ||
                    player.getMainHandItem().is(SplatcraftTags.Items.REVEALS_BARRIERS);
            }
            if (canRender)
                resetActiveTime();
        }
    }

    //@Override
    public double getViewDistance()
    {
        return SplatcraftConfig.Client.barrierRenderDistance.get();
    }

    protected void resetActiveTime()
    {
        activeTime = maxActiveTime;
    }

    @Override
    public void load(@NotNull NbtCompound nbt)
    {
        super.load(nbt);

        if (nbt.contains("ActiveTime"))
        {
            activeTime = nbt.getInt("ActiveTime");
        }
    }

    @Override
    public void saveAdditional(NbtCompound compound)
    {
        compound.putInt("ActiveTime", activeTime);
        super.saveAdditional(compound);
    }

    @Override
    public @NotNull NbtCompound getUpdateTag()
    {
        return new NbtCompound()
        {{
            saveAdditional(this);
        }};
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        // Will get tag from #getUpdateTag
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt)
    {
        if (level != null)
        {
            BlockState state = level.getBlockState(getBlockPos());
            level.sendBlockUpdated(getBlockPos(), state, state, 2);
            handleUpdateTag(pkt.getTag());
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
