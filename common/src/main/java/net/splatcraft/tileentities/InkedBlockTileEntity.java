package net.splatcraft.tileentities;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.splatcraft.blocks.IColoredBlock;
import net.splatcraft.data.capabilities.chunkink.ChunkInkCapability;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkColor;
import net.splatcraft.util.RelativeBlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class InkedBlockTileEntity extends InkColorTileEntity
{
	private BlockState savedState = Blocks.AIR.getDefaultState();
	private int savedColor = -1;
	private int permanentColor = -1;
	private InkBlockUtils.InkType permanentInkType = InkBlockUtils.InkType.NORMAL;
	public InkedBlockTileEntity(BlockPos pos, BlockState state)
	{
		super(SplatcraftTileEntities.inkedTileEntity.get(), pos, state);
	}
	//Used to port Inked Blocks to World Ink system
	// ok fine
	public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T te)
	{
		if (!world.isClient() && te instanceof InkedBlockTileEntity inkedBlock)
		{
			if (inkedBlock.hasSavedState())
			{
				world.setBlockState(pos, inkedBlock.savedState, 2);
				if (inkedBlock.hasPermanentColor())
					ChunkInkCapability.get(world, pos).markInmutable(RelativeBlockPos.fromAbsolute(pos));
				
				for (int i = 0; i < 6; i++)
				{
					InkBlockUtils.inkBlock(world, pos, inkedBlock.getInkColor(), i, getInkType(state), 0);
				}
				
				if (inkedBlock.hasSavedColor() && inkedBlock.getSavedState().getBlock() instanceof IColoredBlock coloredBlock)
				{
					if (inkedBlock.getSavedState().getBlock() instanceof BlockEntityProvider blockEntityProvider)
						world.addBlockEntity(Objects.requireNonNull(blockEntityProvider.createBlockEntity(pos, inkedBlock.getSavedState())));
					coloredBlock.setColor(world, pos, InkColor.constructOrReuse(inkedBlock.getSavedColor()));
				}
			}
		}
	}
	@Deprecated //Only used for parity purposes
	public static InkBlockUtils.InkType getInkType(BlockState state)
	{
		if (state.isOf(SplatcraftBlocks.clearInkedBlock.get()))
			return InkBlockUtils.InkType.CLEAR;
		if (state.isOf(SplatcraftBlocks.glowingInkedBlock.get()))
			return InkBlockUtils.InkType.GLOWING;
		return InkBlockUtils.InkType.NORMAL;
	}
	@Override
	public void setWorld(@NotNull World world)
	{
		super.setWorld(world);
	}
	//Read NBT
	@Override
	public void readNbt(@NotNull NbtCompound nbt, RegistryWrapper.WrapperLookup wrapperLookup)
	{
		super.readNbt(nbt, wrapperLookup);
		savedState = NbtHelper.toBlockState(world.createCommandRegistryWrapper(RegistryKeys.BLOCK), nbt.getCompound("SavedState"));
		savedColor = nbt.getInt("SavedColor");
		if (nbt.contains("PermanentColor"))
		{
			setPermanentColor(nbt.getInt("PermanentColor"));
			setPermanentInkType(InkBlockUtils.InkType.IDENTIFIER_MAP.getOrDefault(Identifier.of(nbt.getString("PermanentInkType")), InkBlockUtils.InkType.NORMAL));
		}
	}
	@Override
	public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup wrapperLookup)
	{
		nbt.put("SavedState", NbtHelper.fromBlockState(savedState));
		if (hasSavedColor())
			nbt.putInt("SavedColor", savedColor);
		if (hasPermanentColor())
		{
			nbt.putInt("PermanentColor", permanentColor);
			nbt.putString("PermanentInkType", permanentInkType.getSerializedName());
		}
		super.writeNbt(nbt, wrapperLookup);
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