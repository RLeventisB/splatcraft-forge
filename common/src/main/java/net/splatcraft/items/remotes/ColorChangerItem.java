package net.splatcraft.items.remotes;

import net.minecraft.component.ComponentMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.Texts;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.commands.InkColorCommand;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.items.IColoredItem;
import net.splatcraft.items.ISplatcraftForgeItemDummy;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateStageListPacket;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.tileentities.IHasTeam;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ColorChangerItem extends RemoteItem implements IColoredItem, ISplatcraftForgeItemDummy
{
    public ColorChangerItem()
    {
        super(new Settings().maxCount(1).rarity(Rarity.UNCOMMON), 3);
        SplatcraftItems.inkColoredItems.add(this);
    }

    public static RemoteResult replaceColor(World world, BlockPos from, BlockPos to, InkColor color, int mode, InkColor affectedColor, String stage, String affectedTeam)
    {
        if (!world.isInBuildLimit(from) || !world.isInBuildLimit(to))
            return createResult(false, Text.translatable("status.change_color.out_of_world"));

        Box bounds = Box.enclosing(from, to);
        AtomicInteger count = new AtomicInteger();
        int blockTotal = (int) (bounds.getLengthX() * bounds.getLengthY() * bounds.getLengthZ());

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
            Map<String, Stage> stages = (world.isClient() ? ClientUtils.clientStages : SaveInfoCapability.get().getStages());
            stages.get(stage).setTeamColor(affectedTeam, color);
            if (!world.isClient())
                SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
        }

        return createResult(true, Text.translatable("status.change_color.success", count, world.isClient() ? ColorUtils.getFormatedColorName(color, false) : InkColorCommand.getColorName(color))).setIntResults(count.get(), blockTotal == 0 ? 0 : count.get() * 15 / blockTotal);
    }

    @Override
    public void appendTooltip(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Text> tooltip, @NotNull TooltipType type)
    {
        super.appendTooltip(stack, context, tooltip, type);

        ComponentMap components = stack.getComponents();

        if (components.contains(SplatcraftComponents.TEAM_ID))
        {
            String teamId = components.get(SplatcraftComponents.TEAM_ID);
            if (!teamId.isEmpty())
            {
                InkColor color = InkColor.INVALID;

                if (components.contains(SplatcraftComponents.REMOTE_INFO))
                {
                    String stage = components.get(SplatcraftComponents.REMOTE_INFO).stageId().get();
                    if (ClientUtils.clientStages.containsKey(stage))
                    {
                        color = ClientUtils.clientStages.get(stage).getTeamColor(teamId);
                    }
                }
                tooltip.add(Texts.setStyleIfAbsent(Text.literal(teamId), !color.isValid() ? TARGETS_STYLE : TARGETS_STYLE.withColor(TextColor.fromRgb(color.getColorWithAlpha(255)))));
            }
        }

        if (ColorUtils.isColorLocked(stack))
            tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull World world, @NotNull Entity entity, int itemSlot, boolean isSelected)
    {
        super.inventoryTick(stack, world, entity, itemSlot, isSelected);

        if (entity instanceof PlayerEntity player && !ColorUtils.isColorLocked(stack) && ColorUtils.getInkColor(stack) != ColorUtils.getEntityColor(player)
            && EntityInfoCapability.hasCapability(player))
        {
            ColorUtils.setInkColor(stack, ColorUtils.getEntityColor(player));
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity)
    {
        BlockPos pos = entity.getBlockPos().down();

        if (entity.getWorld().getBlockState(pos).getBlock() instanceof InkwellBlock)
        {
            if (ColorUtils.getInkColor(stack) != ColorUtils.getInkColorOrInverted(entity.getWorld(), pos))
            {
                ColorUtils.setInkColor(entity.getStack(), ColorUtils.getInkColorOrInverted(entity.getWorld(), pos));
                ColorUtils.setColorLocked(entity.getStack(), true);
            }
        }

        return false;
    }

    @Override
    public RemoteResult onRemoteUse(World usedOnWorld, BlockPos from, BlockPos to, ItemStack stack, InkColor colorIn, int mode, Collection<ServerPlayerEntity> targets)
    {
        String stage = "";
        String team = "";

        ComponentMap components = stack.getComponents();
        if (!components.contains(SplatcraftComponents.TEAM_ID))
        {
            team = components.get(SplatcraftComponents.TEAM_ID);
        }
        if (!components.contains(SplatcraftComponents.REMOTE_INFO))
        {
            stage = components.get(SplatcraftComponents.REMOTE_INFO).stageId().get();
        }

        return replaceColor(getLevel(usedOnWorld, stack), from, to, ColorUtils.getInkColor(stack), mode, colorIn, stage, team);
    }
}