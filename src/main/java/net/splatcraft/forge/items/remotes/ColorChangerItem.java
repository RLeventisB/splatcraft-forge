package net.splatcraft.forge.items.remotes;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.splatcraft.forge.blocks.InkwellBlock;
import net.splatcraft.forge.commands.InkColorCommand;
import net.splatcraft.forge.data.Stage;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.forge.items.IColoredItem;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.s2c.UpdateStageListPacket;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.tileentities.IHasTeam;
import net.splatcraft.forge.util.ClientUtils;
import net.splatcraft.forge.util.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ColorChangerItem extends RemoteItem implements IColoredItem
{
    public ColorChangerItem()
    {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON), 3);
        SplatcraftItems.inkColoredItems.add(this);
    }

    public static RemoteResult replaceColor(Level level, BlockPos from, BlockPos to, int color, int mode, int affectedColor, String stage, String affectedTeam)
    {
        if (!level.isInWorldBounds(from) || !level.isInWorldBounds(to))
            return createResult(false, Component.translatable("status.change_color.out_of_world"));

        AABB bounds = new AABB(from, to);
        AtomicInteger count = new AtomicInteger();
        int blockTotal = (int) (bounds.getXsize() * bounds.getYsize() * bounds.getZsize());

        ColorUtils.forEachColoredBlockInBounds(level, bounds, ((pos, coloredBlock, blockEntity) ->
        {
            int blockColor = coloredBlock.getColor(level, pos);

            if (coloredBlock.canRemoteColorChange(level, pos, blockColor, color) && (mode == 0 || (mode == 1) == (affectedTeam.isEmpty() ? blockColor == affectedColor :
                    blockEntity instanceof IHasTeam team && team.getTeam().equals(affectedTeam)))
                    && coloredBlock.remoteColorChange(level, pos, color))
            {
                count.getAndIncrement();
            }
        }));

        if (mode <= 1 && !affectedTeam.isEmpty() && !stage.isEmpty())
        {
            HashMap<String, Stage> stages = (level.isClientSide() ? ClientUtils.clientStages : SaveInfoCapability.get(level.getServer()).getStages());
            stages.get(stage).setTeamColor(affectedTeam, color);
            if (!level.isClientSide())
                SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
        }

        return createResult(true, Component.translatable("status.change_color.success", count, level.isClientSide() ? ColorUtils.getFormatedColorName(color, false) : InkColorCommand.getColorName(color))).setIntResults(count.get(), blockTotal == 0 ? 0 : count.get() * 15 / blockTotal);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
    {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag nbt = stack.getOrCreateTag();

        if (nbt.contains("Team") && !nbt.getString("Team").isEmpty())
        {
            int color = -1;

            if (nbt.contains("Stage") && ClientUtils.clientStages.containsKey(nbt.getString("Stage")))
                color = ClientUtils.clientStages.get(nbt.getString("Stage")).getTeamColor(nbt.getString("Team"));
            tooltip.add(ComponentUtils.mergeStyles(Component.literal(nbt.getString("Team")), color <= -1 ? TARGETS_STYLE : TARGETS_STYLE.withColor(TextColor.fromRgb(color))));
        }

        if (ColorUtils.isColorLocked(stack))
            tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int itemSlot, boolean isSelected)
    {
        super.inventoryTick(stack, level, entity, itemSlot, isSelected);

        if (entity instanceof Player player && !ColorUtils.isColorLocked(stack) && ColorUtils.getInkColor(stack) != ColorUtils.getPlayerColor(player)
                && PlayerInfoCapability.hasCapability(player))
        {
            ColorUtils.setInkColor(stack, ColorUtils.getPlayerColor(player));
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity)
    {
        BlockPos pos = entity.blockPosition().below();

        if (entity.level().getBlockState(pos).getBlock() instanceof InkwellBlock)
        {
            if (ColorUtils.getInkColor(stack) != ColorUtils.getInkColorOrInverted(entity.level(), pos))
            {
                ColorUtils.setInkColor(entity.getItem(), ColorUtils.getInkColorOrInverted(entity.level(), pos));
                ColorUtils.setColorLocked(entity.getItem(), true);
            }
        }

        return false;
    }

    @Override
    public RemoteResult onRemoteUse(Level usedOnWorld, BlockPos from, BlockPos to, ItemStack stack, int colorIn, int mode, Collection<ServerPlayer> targets)
    {
        String stage = "";
        String team = "";

        if (stack.hasTag())
        {
            team = stack.getTag().getString("Team");
            stage = stack.getTag().getString("Stage");
        }

        return replaceColor(getLevel(usedOnWorld, stack), from, to, ColorUtils.getInkColor(stack), mode, colorIn, stage, team);
    }
}