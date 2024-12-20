package net.splatcraft.blocks;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.splatcraft.Splatcraft;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftLoot;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.CrateTileEntity;
import net.splatcraft.util.BlockInkedResult;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class CrateBlock extends Block implements IColoredBlock, BlockEntityProvider
{
    public static final IntProperty STATE = IntProperty.of("state", 0, 4);
    public static final RegistryKey<LootTable> STORAGE_SUNKEN_CRATE = RegistryKey.of(RegistryKeys.LOOT_TABLE, Splatcraft.identifierOf("storage/sunken_crate"));
    public static final RegistryKey<LootTable> STORAGE_EGG_CRATE = RegistryKey.of(RegistryKeys.LOOT_TABLE, Splatcraft.identifierOf("storage/egg_crate"));
    public final boolean isSunken;

    public CrateBlock(String name, boolean isSunken)
    {
        super(AbstractBlock.Settings.create().mapColor(MapColor.BROWN).burnable().instrument(NoteBlockInstrument.BASS).sounds(BlockSoundGroup.WOOD).strength(2.0f));

        this.isSunken = isSunken;

        SplatcraftBlocks.inkColoredBlocks.add(this);
    }

    public static List<ItemStack> generateLoot(World world, CrateTileEntity crate, BlockState state, float luckValue)
    {
        if (!(world instanceof ServerWorld serverWorld))
            return Collections.emptyList();

        BlockPos pos = crate.getPos();

        LootContextParameterSet.Builder contextBuilder = new LootContextParameterSet.Builder(serverWorld);
        return serverWorld.getServer().getReloadableRegistries().getLootTable(crate.getLootTable()).generateLoot(contextBuilder.luck(luckValue)
            .add(LootContextParameters.BLOCK_STATE, state).add(LootContextParameters.TOOL, ItemStack.EMPTY).add(LootContextParameters.ORIGIN, new Vec3d(pos.getX(), pos.getY(), pos.getZ())).build(LootContextTypes.BLOCK));
    }

    @Override
    public void appendTooltip(@NotNull ItemStack stack, @Nullable Item.TooltipContext levelIn, @NotNull List<Text> tooltip, @NotNull TooltipType type)
    {
        super.appendTooltip(stack, levelIn, tooltip, type);
        NbtCompound nbt = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA).copyNbt();
        if (!isSunken && nbt == null)
            return;

        if (isSunken || nbt.contains("LootTable"))
            tooltip.add(Text.translatable("block.splatcraft.crate.loot"));
        else if (nbt.contains("Items", NbtElement.LIST_TYPE))
        {
            DefaultedList<ItemStack> nonnulllist = DefaultedList.ofSize(27, ItemStack.EMPTY);

            Inventories.readNbt(nbt, nonnulllist, levelIn.getRegistryLookup());
            int i = 0;
            int j = 0;

            for (ItemStack itemstack : nonnulllist)
            {
                if (!itemstack.isEmpty())
                {
                    ++j;
                    if (i <= 4)
                    {
                        ++i;
                        MutableText iformattabletextcomponent = itemstack.getName().copy();
                        iformattabletextcomponent.append(" x").append(String.valueOf(itemstack.getCount()));
                        tooltip.add(iformattabletextcomponent);
                    }
                }
            }

            if (j - i > 0)
            {
                tooltip.add(Text.translatable("container.shulkerBox.more", j - i).formatted(Formatting.ITALIC));
            }
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
    {
        builder.add(STATE);
    }

    @Override
    public @NotNull BlockState getStateForNeighborUpdate(@NotNull BlockState stateIn, @NotNull Direction facing, @NotNull BlockState facingState, WorldAccess levelIn, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos)
    {
        if (levelIn.getBlockEntity(currentPos) instanceof CrateTileEntity crateTile)
        {
            return stateIn.with(STATE, crateTile.getState());
        }

        return super.getStateForNeighborUpdate(stateIn, facing, facingState, levelIn, currentPos, facingPos);
    }

    @Override
    public boolean hasComparatorOutput(@NotNull BlockState state)
    {
        return !isSunken;
    }

    @Override
    public int getComparatorOutput(@NotNull BlockState blockState, @NotNull World levelIn, @NotNull BlockPos pos)
    {

        if (isSunken || !(levelIn.getBlockEntity(pos) instanceof CrateTileEntity crateTile))
        {
            return 0;
        }
        ItemStack stack = crateTile.getStack(0);
        return (int) Math.ceil(stack.getCount() / (float) stack.getMaxCount() * 15);
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable BlockEntity te, @NotNull ItemStack stack)
    {
        player.incrementStat(Stats.MINED.getOrCreateStat(this));
        player.addExhaustion(0.005F);

        if (world.getGameRules().getBoolean(SplatcraftGameRules.DROP_CRATE_LOOT) && EnchantmentHelper.getLevel(CommonUtils.getEnchantmentEntry(world, Enchantments.SILK_TOUCH), stack) <= 0 && world.getBlockEntity(pos) instanceof CrateTileEntity crateTileEntity)
        {
            crateTileEntity.dropInventory();
        }
        else
        {
            dropStacks(state, world, pos, te, player, stack);
        }
    }

    @Override
    public @NotNull List<ItemStack> getDroppedStacks(@NotNull BlockState state, LootContextParameterSet.Builder builder)
    {
        ItemStack tool = builder.getOptional(LootContextParameters.TOOL);
        World world = builder.getWorld();
        BlockEntity te = builder.getOptional(LootContextParameters.BLOCK_ENTITY);

        if (te instanceof CrateTileEntity crate)
        {
            boolean silkTouched = tool != null && EnchantmentHelper.getLevel(CommonUtils.getEnchantmentEntry(world, Enchantments.SILK_TOUCH), tool) > 0;

            if (world.getGameRules().getBoolean(SplatcraftGameRules.DROP_CRATE_LOOT) && !silkTouched)
            {
                return crate.getDrops();
            }
        }

        return super.getDroppedStacks(state, builder);
    }

    @Override
    public BlockInkedResult inkBlock(World world, BlockPos pos, InkColor color, float damage, InkBlockUtils.InkType inkType)
    {
        if (world.getBlockEntity(pos) instanceof CrateTileEntity crateTileEntity)
        {
            crateTileEntity.ink(color, damage);
        }

        return BlockInkedResult.FAIL;
    }

    @Override
    public boolean canClimb()
    {
        return false;
    }

    @Override
    public boolean canSwim()
    {
        return false;
    }

    @Override
    public boolean canDamage()
    {
        return false;
    }

    @Override
    public boolean remoteColorChange(World world, BlockPos pos, InkColor newColor)
    {
        return false;
    }

    @Override
    public boolean remoteInkClear(World world, BlockPos pos)
    {
        if (world.getBlockEntity(pos) instanceof CrateTileEntity crate)
        {
            if (crate.getHealth() == crate.getMaxHealth())
            {
                return false;
            }
            crate.resetHealth();
            world.setBlockState(pos, crate.getCachedState().with(STATE, crate.getState()), 2);
            return true;
        }
        return false;
    }

    @Override
    public void onPlaced(@NotNull World world, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity entity, @NotNull ItemStack stack)
    {
        super.onPlaced(world, pos, state, entity, stack);

        if (world.getBlockEntity(pos) instanceof CrateTileEntity crate)
            if (isSunken)
                crate.setLootTable(SplatcraftLoot.STORAGE_SUNKEN_CRATE);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        CrateTileEntity te = SplatcraftTileEntities.crateTileEntity.get().instantiate(pos, state);

        if (te != null)
        {
            te.setMaxHealth(isSunken ? 25 : 20);
            te.resetHealth();
            te.setColor(InkColor.INVALID);
        }

        return te;
    }
}
