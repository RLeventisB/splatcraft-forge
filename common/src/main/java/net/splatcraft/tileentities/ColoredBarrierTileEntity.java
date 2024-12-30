package net.splatcraft.tileentities;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.blocks.ColoredBarrierBlock;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.entities.SpawnShieldEntity;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.util.ClientUtils;
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
	public void tick()
	{
		if (activeTime > 0)
		{
			activeTime--;
		}
		
		for (Entity entity : world.getEntitiesByClass(Entity.class, new Box(getPos()).expand(0.05), entity -> !(entity instanceof SpawnShieldEntity)))
		{
			if (ColorUtils.getEntityColor(entity).isValid() && (getCachedState().getBlock() instanceof ColoredBarrierBlock block &&
				!block.canAllowThrough(getPos(), entity)))
				resetActiveTime();
		}
		
		if (world.isClient && ClientUtils.getClientPlayer().isCreative())
		{
			boolean canRender = true;
			PlayerEntity player = ClientUtils.getClientPlayer();
			int renderDistance = SplatcraftConfig.get("splatcraft.barrierRenderDistance");
			
			if (player.squaredDistanceTo(getPos().toCenterPos()) > renderDistance * renderDistance)
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
