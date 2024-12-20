package net.splatcraft.registries;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.renderer.tileentity.RemotePedestalTileEntityRenderer;
import net.splatcraft.client.renderer.tileentity.StageBarrierTileEntityRenderer;
import net.splatcraft.tileentities.*;
import net.splatcraft.tileentities.container.InkVatContainer;
import net.splatcraft.tileentities.container.WeaponWorkbenchContainer;

import static net.splatcraft.registries.SplatcraftBlocks.*;

public class SplatcraftTileEntities
{
    protected static final DeferredRegister<BlockEntityType<?>> REGISTRY = Splatcraft.deferredRegistryOf(Registries.BLOCK_ENTITY_TYPE);
    protected static final DeferredRegister<ScreenHandlerType<?>> CONTAINER_REGISTRY = Splatcraft.deferredRegistryOf(Registries.SCREEN_HANDLER);
    public static final RegistrySupplier<ScreenHandlerType<InkVatContainer>> inkVatContainer = registerContainer("ink_vat", (a, b) -> new InkVatContainer(a, b));

    @SafeVarargs
    private static <T extends BlockEntity> RegistrySupplier<BlockEntityType<T>> registerTileEntity(String name, BlockEntityType.BlockEntityFactory<T> factoryIn, RegistrySupplier<? extends Block>... allowedBlocks)
    {
        return REGISTRY.register(name, () ->
        {
            Block[] blocks = new Block[allowedBlocks.length];
            for (int i = 0; i < blocks.length; i++)
                blocks[i] = allowedBlocks[i].get();

            return BlockEntityType.Builder.create(factoryIn, blocks).build(null);
        });
    }    public static final RegistrySupplier<BlockEntityType<StageBarrierTileEntity>> stageBarrierTileEntity = registerTileEntity("stage_barrier", StageBarrierTileEntity::new, stageBarrier, stageVoid);

    private static <T extends ScreenHandler> RegistrySupplier<ScreenHandlerType<T>> registerContainer(String name, ScreenHandlerType.Factory<T> factoryIn)
    {
        return CONTAINER_REGISTRY.register(name, () -> new ScreenHandlerType<>(factoryIn, FeatureSet.empty()));
    }

    public static void bindTESR()
    {
        //BlockEntityRenderers.register(inkedTileEntity.get(), InkedBlockTileEntityRenderer::new);
        BlockEntityRendererRegistry.register(stageBarrierTileEntity.get(), StageBarrierTileEntityRenderer::new);
        BlockEntityRendererRegistry.register(colorBarrierTileEntity.get(), context -> (BlockEntityRenderer<ColoredBarrierTileEntity>) (Object) new StageBarrierTileEntityRenderer(context));
        BlockEntityRendererRegistry.register(remotePedestalTileEntity.get(), context -> new RemotePedestalTileEntityRenderer());
    }    public static final RegistrySupplier<ScreenHandlerType<WeaponWorkbenchContainer>> weaponWorkbenchContainer = registerContainer("weapon_workbench", WeaponWorkbenchContainer::new);



    public static final RegistrySupplier<BlockEntityType<InkColorTileEntity>> colorTileEntity = registerTileEntity("color", InkColorTileEntity::new, inkedWool, inkedGlass, inkedGlassPane, inkedCarpet, canvas, splatSwitch, inkwell);



    public static final RegistrySupplier<BlockEntityType<InkVatTileEntity>> inkVatTileEntity = registerTileEntity("ink_vat", InkVatTileEntity::new, inkVat);
    public static final RegistrySupplier<BlockEntityType<RemotePedestalTileEntity>> remotePedestalTileEntity = registerTileEntity("remote_pedestal", RemotePedestalTileEntity::new, remotePedestal);
    public static final RegistrySupplier<BlockEntityType<SpawnPadTileEntity>> spawnPadTileEntity = registerTileEntity("spawn_pad", SpawnPadTileEntity::new, spawnPad);
    public static final RegistrySupplier<BlockEntityType<InkedBlockTileEntity>> inkedTileEntity = registerTileEntity("inked_block", InkedBlockTileEntity::new, inkedBlock, glowingInkedBlock, clearInkedBlock);
    public static final RegistrySupplier<BlockEntityType<CrateTileEntity>> crateTileEntity = registerTileEntity("crate", CrateTileEntity::new, crate, sunkenCrate);
    public static final RegistrySupplier<BlockEntityType<ColoredBarrierTileEntity>> colorBarrierTileEntity = registerTileEntity("color_barrier", ColoredBarrierTileEntity::new, allowedColorBarrier, deniedColorBarrier);
}
