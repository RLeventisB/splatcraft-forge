package net.splatcraft.items.remotes;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.commands.InkColorCommand;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.SaveInfoCapability;
import net.splatcraft.dummys.ISplatcraftForgeItemDummy;
import net.splatcraft.items.IColoredItem;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateStageListPacket;
import net.splatcraft.platform.Components;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.tileentities.IHasTeam;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ColorChangerItem extends RemoteItem implements IColoredItem, ISplatcraftForgeItemDummy
{
	public ColorChangerItem()
	{
		super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON), 3);
		SplatcraftItems.inkColoredItems.add(this);
	}
	public static RemoteResult replaceColor(Level world, BlockPos from, BlockPos to, InkColor color, int mode, InkColor affectedColor, String stage, String affectedTeam)
	{
		if (!world.isInWorldBounds(from) || !world.isInWorldBounds(to))
			return createResult(false, Component.translatable("status.change_color.out_of_world"));
		
		AABB bounds = AABB.encapsulatingFullBlocks(from, to);
		AtomicInteger count = new AtomicInteger();
		int blockTotal = (int) (bounds.getXsize() * bounds.getYsize() * bounds.getZsize());
		
		ColorUtils.forEachColoredBlockInBounds(world, bounds, ((pos, coloredBlock, blockEntity) ->
		{
			InkColor blockColor = coloredBlock.getColor(world, pos);
			
			if (coloredBlock.canRemoteColorChange(world, pos, blockColor, color) && (mode == 0 || (mode == 1) == (affectedTeam.isEmpty() ? blockColor == affectedColor :
				blockEntity instanceof IHasTeam team && team.getTeam().equals(affectedTeam)))
				&& coloredBlock.remoteColorChange(world, pos, color))
			{
				count.getAndIncrement();
			}
		}));
		
		if (mode <= 1 && !affectedTeam.isEmpty() && !stage.isEmpty())
		{
			Object2ObjectMap<String, Stage> stages = SaveInfoCapability.get().stages();
			stages.get(stage).setTeamColor(affectedTeam, color);
			if (!world.isClientSide())
				SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
		}
		
		return createResult(true, Component.translatable("status.change_color.success", count, world.isClientSide() ? ColorUtils.getFormatedColorName(color, false) : InkColorCommand.getColorName(color))).setIntResults(count.get(), blockTotal == 0 ? 0 : count.get() * 15 / blockTotal);
	}
	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag type)
	{
		super.appendHoverText(stack, context, tooltip, type);
		
		DataComponentMap components = stack.getComponents();
		
		if (components.has(SplatcraftComponents.TEAM_ID))
		{
			String teamId = components.get(SplatcraftComponents.TEAM_ID);
			if (!teamId.isEmpty())
			{
				InkColor color = InkColor.INVALID;
				
				if (components.has(SplatcraftComponents.REMOTE_INFO))
				{
					String stage = components.get(SplatcraftComponents.REMOTE_INFO).stageId().get();
					if (SaveInfoCapability.get().stages().containsKey(stage))
					{
						color = SaveInfoCapability.get().stages().get(stage).getTeamColor(teamId);
					}
				}
				tooltip.add(ComponentUtils.mergeStyles(Component.literal(teamId), !color.isValid() ? TARGETS_STYLE : TARGETS_STYLE.withColor(TextColor.fromRgb(color.getColorWithAlpha(255)))));
			}
		}
		
		if (ColorUtils.isColorLocked(stack))
			tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);
		
		if (entity instanceof LivingEntity living &&
			!ColorUtils.isColorLocked(stack) &&
			ColorUtils.getInkColor(stack) != ColorUtils.getEntityColor(living) &&
			Components.ENTITY_INFO.has(living))
		{
			ColorUtils.withInkColor(stack, ColorUtils.getEntityColor(living));
		}
	}
	@Override
	public boolean phOnEntityItemUpdate(ItemStack stack, ItemEntity entity)
	{
		BlockPos pos = entity.blockPosition().below();
		
		if (entity.level().getBlockState(pos).getBlock() instanceof InkwellBlock)
		{
			if (ColorUtils.getInkColor(stack) != ColorUtils.getEffectiveColor(entity.level(), pos))
			{
				ColorUtils.withInkColor(entity.getItem(), ColorUtils.getEffectiveColor(entity.level(), pos));
				ColorUtils.withColorLocked(entity.getItem(), true);
			}
		}
		
		return false;
	}
	@Override
	public RemoteResult onRemoteUse(Level usedOnWorld, BlockPos from, BlockPos to, ItemStack stack, InkColor colorIn, int mode, Collection<ServerPlayer> targets)
	{
		String stage = "";
		String team = "";
		
		DataComponentMap components = stack.getComponents();
		if (!components.has(SplatcraftComponents.TEAM_ID))
		{
			team = components.get(SplatcraftComponents.TEAM_ID);
		}
		if (!components.has(SplatcraftComponents.REMOTE_INFO))
		{
			stage = components.get(SplatcraftComponents.REMOTE_INFO).stageId().get();
		}
		
		return replaceColor(getLevel(usedOnWorld, stack), from, to, ColorUtils.getInkColor(stack), mode, colorIn, stage, team);
	}
}