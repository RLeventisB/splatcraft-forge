package net.splatcraft.blocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
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

public class CrateBlock extends Block implements IColoredBlock, EntityBlock
{
	public static final IntegerProperty STATE = IntegerProperty.create("state", 0, 4);
	public static final ResourceKey<LootTable> STORAGE_SUNKEN_CRATE = ResourceKey.create(Registries.LOOT_TABLE, Splatcraft.identifierOf("storage/sunken_crate"));
	public static final ResourceKey<LootTable> STORAGE_EGG_CRATE = ResourceKey.create(Registries.LOOT_TABLE, Splatcraft.identifierOf("storage/egg_crate"));
	public final boolean isSunken;
	public CrateBlock(String name, boolean isSunken)
	{
		super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).ignitedByLava().instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).strength(2.0f));
		
		this.isSunken = isSunken;
		
		SplatcraftBlocks.inkColoredBlocks.add(this);
	}
	public static List<ItemStack> generateLoot(Level world, CrateTileEntity crate, BlockState state, float luckValue)
	{
		if (!(world instanceof ServerLevel serverWorld))
			return Collections.emptyList();
		
		BlockPos pos = crate.getBlockPos();
		
		LootParams.Builder contextBuilder = new LootParams.Builder(serverWorld);
		return serverWorld.getServer().reloadableRegistries().getLootTable(crate.getLootTable()).getRandomItems(contextBuilder.withLuck(luckValue)
			.withParameter(LootContextParams.BLOCK_STATE, state).withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withParameter(LootContextParams.ORIGIN, new Vec3(pos.getX(), pos.getY(), pos.getZ())).create(LootContextParamSets.BLOCK));
	}
	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable Item.TooltipContext levelIn, @NotNull List<Component> tooltip, @NotNull TooltipFlag type)
	{
		super.appendHoverText(stack, levelIn, tooltip, type);
		CompoundTag nbt = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(new CompoundTag())).copyTag();
		if (!isSunken && nbt == null)
			return;
		
		if (isSunken || nbt.contains("LootTable"))
			tooltip.add(Component.translatable("block.splatcraft.crate.loot"));
		else if (nbt.contains("Items", Tag.TAG_LIST))
		{
			NonNullList<ItemStack> nonnulllist = NonNullList.withSize(27, ItemStack.EMPTY);
			
			ContainerHelper.loadAllItems(nbt, nonnulllist, levelIn.registries());
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
						MutableComponent iformattabletextcomponent = itemstack.getHoverName().copy();
						iformattabletextcomponent.append(" x").append(String.valueOf(itemstack.getCount()));
						tooltip.add(iformattabletextcomponent);
					}
				}
			}
			
			if (j - i > 0)
			{
				tooltip.add(Component.translatable("container.shulkerBox.more", j - i).withStyle(ChatFormatting.ITALIC));
			}
		}
	}
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(STATE);
	}
	@Override
	public @NotNull BlockState updateShape(@NotNull BlockState stateIn, @NotNull Direction facing, @NotNull BlockState facingState, LevelAccessor levelIn, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos)
	{
		if (levelIn.getBlockEntity(currentPos) instanceof CrateTileEntity crateTile)
		{
			return stateIn.setValue(STATE, crateTile.getState());
		}
		
		return super.updateShape(stateIn, facing, facingState, levelIn, currentPos, facingPos);
	}
	@Override
	public boolean hasAnalogOutputSignal(@NotNull BlockState state)
	{
		return !isSunken;
	}
	@Override
	public int getAnalogOutputSignal(@NotNull BlockState blockState, @NotNull Level levelIn, @NotNull BlockPos pos)
	{
		
		if (isSunken || !(levelIn.getBlockEntity(pos) instanceof CrateTileEntity crateTile))
		{
			return 0;
		}
		ItemStack stack = crateTile.getItem(0);
		return (int) Math.ceil(stack.getCount() / (float) stack.getMaxStackSize() * 15);
	}
	@Override
	public void playerDestroy(Level world, Player player, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable BlockEntity te, @NotNull ItemStack stack)
	{
		player.awardStat(Stats.BLOCK_MINED.get(this));
		player.causeFoodExhaustion(0.005F);
		
		if (world.getGameRules().getBoolean(SplatcraftGameRules.DROP_CRATE_LOOT) && EnchantmentHelper.getItemEnchantmentLevel(CommonUtils.getEnchantmentEntry(world, Enchantments.SILK_TOUCH), stack) <= 0 && world.getBlockEntity(pos) instanceof CrateTileEntity crateTileEntity)
		{
			crateTileEntity.dropInventory();
		}
		else
		{
			dropResources(state, world, pos, te, player, stack);
		}
	}
	@Override
	public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.Builder builder)
	{
		ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
		Level world = builder.getLevel();
		BlockEntity te = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		
		if (te instanceof CrateTileEntity crate)
		{
			boolean silkTouched = tool != null && EnchantmentHelper.getItemEnchantmentLevel(CommonUtils.getEnchantmentEntry(world, Enchantments.SILK_TOUCH), tool) > 0;
			
			if (world.getGameRules().getBoolean(SplatcraftGameRules.DROP_CRATE_LOOT) && !silkTouched)
			{
				return crate.getDrops();
			}
		}
		
		return super.getDrops(state, builder);
	}
	@Override
	public BlockInkedResult inkBlock(Level world, BlockPos pos, InkColor color, float damage, InkBlockUtils.InkType inkType)
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
	public boolean remoteColorChange(Level world, BlockPos pos, InkColor newColor)
	{
		return false;
	}
	@Override
	public boolean remoteInkClear(Level world, BlockPos pos)
	{
		if (world.getBlockEntity(pos) instanceof CrateTileEntity crate)
		{
			if (crate.getHealth() == crate.getMaxHealth())
			{
				return false;
			}
			crate.resetHealth();
			world.setBlock(pos, crate.getBlockState().setValue(STATE, crate.getState()), 2);
			return true;
		}
		return false;
	}
	@Override
	public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity entity, @NotNull ItemStack stack)
	{
		super.setPlacedBy(world, pos, state, entity, stack);
		
		if (world.getBlockEntity(pos) instanceof CrateTileEntity crate)
			if (isSunken)
				crate.setLootTable(SplatcraftLoot.STORAGE_SUNKEN_CRATE);
	}
	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
	{
		CrateTileEntity te = SplatcraftTileEntities.crateTileEntity.get().create(pos, state);
		
		if (te != null)
		{
			te.setMaxHealth(isSunken ? 25 : 20);
			te.resetHealth();
			te.setColor(InkColor.INVALID);
		}
		
		return te;
	}
}
