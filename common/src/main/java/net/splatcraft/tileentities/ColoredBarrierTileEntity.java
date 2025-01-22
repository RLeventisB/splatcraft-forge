package net.splatcraft.tileentities;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
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
		if (ColorUtils.getEntityColor(entity).isValid() && (getCachedState().getBlock() instanceof ColoredBarrierBlock block &&
			!block.canAllowThrough(getPos(), entity)))
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
	public void readNbt(@NotNull NbtCompound nbt, RegistryWrapper.WrapperLookup wrapperLookup)
	{
		super.readNbt(nbt, wrapperLookup);
		setColor(InkColor.getFromNbt(nbt.get("Color")));
		setTeam(nbt.getString("Team"));
		setInverted(nbt.getBoolean("Inverted"));
	}
	@Override
	public void writeNbt(NbtCompound compound, RegistryWrapper.WrapperLookup wrapperLookup)
	{
		compound.put("Color", getColor().getNbt());
		compound.putString("Team", getTeam());
		compound.putBoolean("Inverted", inverted);
		super.writeNbt(compound, wrapperLookup);
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
