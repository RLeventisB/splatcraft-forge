package net.splatcraft.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.blocks.IColoredBlock;
import net.splatcraft.data.capabilities.ChunkInkCapability;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.structs.InkColor;
import net.splatcraft.util.structs.RelativeBlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class InkedBlockTileEntity extends InkColorTileEntity
{
	private BlockState savedState = Blocks.AIR.defaultBlockState();
	private int savedColor = -1;
	private int permanentColor = -1;
	private InkBlockUtils.InkType permanentInkType = InkBlockUtils.InkType.NORMAL;
	public InkedBlockTileEntity(BlockPos pos, BlockState state)
	{
		super(SplatcraftTileEntities.inkedTileEntity.value(), pos, state);
	}
	//Used to port Inked Blocks to World Ink system
	// ok fine
	public static <T extends BlockEntity> void tick(Level world, BlockPos pos, BlockState state, T te)
	{
		if (!world.isClientSide() && te instanceof InkedBlockTileEntity inkedBlock)
		{
			if (inkedBlock.hasSavedState())
			{
				world.setBlock(pos, inkedBlock.savedState, 2);
				if (inkedBlock.hasPermanentColor())
					ChunkInkCapability.getOrCreate(world, pos).markInmutable(RelativeBlockPos.fromAbsolute(pos));
				
				for (int i = 0; i < 6; i++)
				{
					InkBlockUtils.inkBlock(world, pos, inkedBlock.getInkColor(), i, getInkType(state), 0);
				}
				
				if (inkedBlock.hasSavedColor() && inkedBlock.getSavedState().getBlock() instanceof IColoredBlock coloredBlock)
				{
					if (inkedBlock.getSavedState().getBlock() instanceof EntityBlock blockEntityProvider)
						world.setBlockEntity(Objects.requireNonNull(blockEntityProvider.newBlockEntity(pos, inkedBlock.getSavedState())));
					coloredBlock.setColor(world, pos, InkColor.constructOrReuse(inkedBlock.getSavedColor()));
				}
			}
		}
	}
	@Deprecated //Only used for parity purposes
	public static InkBlockUtils.InkType getInkType(BlockState state)
	{
		if (state.is(SplatcraftBlocks.clearInkedBlock.value()))
			return InkBlockUtils.InkType.CLEAR;
		if (state.is(SplatcraftBlocks.glowingInkedBlock.value()))
			return InkBlockUtils.InkType.GLOWING;
		return InkBlockUtils.InkType.NORMAL;
	}
	@Override
	public void setLevel(@NotNull Level world)
	{
		super.setLevel(world);
	}
	//Read NBT
	@Override
	public void loadAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider wrapperLookup)
	{
		super.loadAdditional(nbt, wrapperLookup);
		savedState = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), nbt.getCompound("SavedState"));
		savedColor = nbt.getInt("SavedColor");
		if (nbt.contains("PermanentColor"))
		{
			setPermanentColor(nbt.getInt("PermanentColor"));
			setPermanentInkType(InkBlockUtils.InkType.CODEC.parse(NbtOps.INSTANCE, nbt.get("PermanentInkType")).result().orElse(InkBlockUtils.InkType.NORMAL));
		}
	}
	@Override
	public void saveAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider wrapperLookup)
	{
		nbt.put("SavedState", NbtUtils.writeBlockState(savedState));
		if (hasSavedColor())
			nbt.putInt("SavedColor", savedColor);
		if (hasPermanentColor())
		{
			nbt.putInt("PermanentColor", permanentColor);
			nbt.putString("PermanentInkType", permanentInkType.name());
		}
		super.saveAdditional(nbt, wrapperLookup);
	}
	public BlockState getSavedState()
	{
		return savedState;
	}
	public void setSavedState(BlockState savedState)
	{
		this.savedState = savedState;
	}
	public boolean hasSavedState()
	{
		return savedState != null && savedState.getBlock() != Blocks.AIR;
	}
	public int getSavedColor()
	{
		return savedColor;
	}
	public void setSavedColor(int color)
	{
		savedColor = color;
	}
	public boolean hasSavedColor()
	{
		return savedColor != -1;
	}
	public int getPermanentColor()
	{
		return permanentColor;
	}
	public void setPermanentColor(int permanentColor)
	{
		this.permanentColor = permanentColor;
	}
	public boolean hasPermanentColor()
	{
		return permanentColor != -1;
	}
	public InkBlockUtils.InkType getPermanentInkType()
	{
		return permanentInkType;
	}
	public void setPermanentInkType(InkBlockUtils.InkType permanentInkType)
	{
		this.permanentInkType = permanentInkType;
	}
}