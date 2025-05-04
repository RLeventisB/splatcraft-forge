package net.splatcraft.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.blocks.ColoredBarrierBlock;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

public class ColoredBarrierTileEntity extends StageBarrierTileEntity implements IHasTeam
{
	protected InkColor color = ColorUtils.getDefaultColor();
	private boolean inverted = false;
	private String team = "";
	public ColoredBarrierTileEntity(BlockPos pos, BlockState state)
	{
		super(SplatcraftTileEntities.colorBarrierTileEntity.get(), pos, state);
	}
	@Override
	public void onEntityCollide(Entity entity)
	{
		if (ColorUtils.getEntityColor(entity).isValid() && (getBlockState().getBlock() instanceof ColoredBarrierBlock block &&
			!block.canAllowThrough(getBlockPos(), entity)))
			resetActiveTime();
	}
	public InkColor getColor()
	{
		return color;
	}
	public void setColor(InkColor color)
	{
		this.color = color;
	}
	@Override
	public void loadAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider wrapperLookup)
	{
		super.loadAdditional(nbt, wrapperLookup);
		setColor(InkColor.getFromNbt(nbt.get("Color")));
		setTeam(nbt.getString("Team"));
		setInverted(nbt.getBoolean("Inverted"));
	}
	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.@NotNull Provider wrapperLookup)
	{
		compound.put("Color", getColor().getNbt());
		compound.putString("Team", getTeam());
		compound.putBoolean("Inverted", inverted);
		super.saveAdditional(compound, wrapperLookup);
	}
	public boolean isInverted()
	{
		return inverted;
	}
	public void setInverted(boolean inverted)
	{
		this.inverted = inverted;
	}
	@Override
	public String getTeam()
	{
		return team;
	}
	@Override
	public void setTeam(String team)
	{
		this.team = team;
	}
}
