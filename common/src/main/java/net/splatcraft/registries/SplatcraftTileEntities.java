package net.splatcraft.registries;

import com.google.common.base.Suppliers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.renderer.tileentity.RemotePedestalTileEntityRenderer;
import net.splatcraft.client.renderer.tileentity.StageBarrierTileEntityRenderer;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.platform.Services;
import net.splatcraft.tileentities.*;
import net.splatcraft.tileentities.container.InkVatContainer;
import net.splatcraft.tileentities.container.WeaponWorkbenchContainer;

import static net.splatcraft.registries.SplatcraftBlocks.*;

// why does this file fuck up the formatter?????
public class SplatcraftTileEntities
{
	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = Splatcraft.deferredRegistryOf(BuiltInRegistries.BLOCK_ENTITY_TYPE);
	public static final DeferredRegister<MenuType<?>> CONTAINER_REGISTRY = Splatcraft.deferredRegistryOf(BuiltInRegistries.MENU);
	@SafeVarargs
	private static <T extends BlockEntity> RegistrySupplier<BlockEntityType<T>> registerTileEntity(String name, BlockEntityType.BlockEntitySupplier<T> factoryIn, RegistrySupplier<? extends Block>... allowedBlocks)
	{
		return REGISTRY.register(name, Suppliers.memoize(() ->
		{
			Block[] blocks = new Block[allowedBlocks.length];
			for (int i = 0; i < blocks.length; i++)
				blocks[i] = allowedBlocks[i].value();
			
			return BlockEntityType.Builder.of(factoryIn, blocks).build(null);
		}));
	}
	private static <T extends AbstractContainerMenu> RegistrySupplier<MenuType<T>> registerContainer(String name, MenuType.MenuSupplier<T> factoryIn)
	{
		return CONTAINER_REGISTRY.register(name, () -> new MenuType<>(factoryIn, FeatureFlagSet.of()));
	}
	@OnlyIn(Dist.CLIENT)
	public static void bindTESR()
	{
		Services.PLATFORM.registerBlockEntityRenderer(stageBarrierTileEntity, StageBarrierTileEntityRenderer::new);
		Services.PLATFORM.registerBlockEntityRenderer(colorBarrierTileEntity, context -> (BlockEntityRenderer<ColoredBarrierTileEntity>) (Object) new StageBarrierTileEntityRenderer(context));
		Services.PLATFORM.registerBlockEntityRenderer(remotePedestalTileEntity, context -> new RemotePedestalTileEntityRenderer());
	}
	public static final RegistrySupplier<MenuType<InkVatContainer>> inkVatContainer = registerContainer("ink_vat", InkVatContainer::new);
	public static final RegistrySupplier<MenuType<WeaponWorkbenchContainer>> weaponWorkbenchContainer = registerContainer("weapon_workbench", WeaponWorkbenchContainer::new);
	public static final RegistrySupplier<BlockEntityType<StageBarrierTileEntity>> stageBarrierTileEntity = registerTileEntity("stage_barrier", StageBarrierTileEntity::new, stageBarrier, stageVoid);
	public static final RegistrySupplier<BlockEntityType<InkColorTileEntity>> colorTileEntity = registerTileEntity("color", InkColorTileEntity::new, inkedWool, inkedGlass, inkedGlassPane, inkedCarpet, canvas, splatSwitch, inkwell);
	public static final RegistrySupplier<BlockEntityType<InkVatTileEntity>> inkVatTileEntity = registerTileEntity("ink_vat", InkVatTileEntity::new, inkVat);
	public static final RegistrySupplier<BlockEntityType<RemotePedestalTileEntity>> remotePedestalTileEntity = registerTileEntity("remote_pedestal", RemotePedestalTileEntity::new, remotePedestal);
	public static final RegistrySupplier<BlockEntityType<SpawnPadTileEntity>> spawnPadTileEntity = registerTileEntity("spawn_pad", SpawnPadTileEntity::new, spawnPad);
	public static final RegistrySupplier<BlockEntityType<InkedBlockTileEntity>> inkedTileEntity = registerTileEntity("inked_block", InkedBlockTileEntity::new, inkedBlock, glowingInkedBlock, clearInkedBlock);
	public static final RegistrySupplier<BlockEntityType<CrateTileEntity>> crateTileEntity = registerTileEntity("crate", CrateTileEntity::new, crate, sunkenCrate);
	public static final RegistrySupplier<BlockEntityType<ColoredBarrierTileEntity>> colorBarrierTileEntity = registerTileEntity("color_barrier", ColoredBarrierTileEntity::new, allowedColorBarrier, deniedColorBarrier);
}
