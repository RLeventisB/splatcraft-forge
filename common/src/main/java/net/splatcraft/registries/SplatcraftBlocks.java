package net.splatcraft.registries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.splatcraft.Splatcraft;
import net.splatcraft.blocks.*;

import java.util.ArrayList;

public class SplatcraftBlocks
{
	public static final ArrayList<Block> inkColoredBlocks = new ArrayList<>();
	protected static final DeferredRegister<Block> REGISTRY = Splatcraft.deferredRegistryOf(BuiltInRegistries.BLOCK);
	public static final RegistrySupplier<InkedBlock> inkedBlock = REGISTRY.register("inked_block", InkedBlock::new);
	public static final RegistrySupplier<InkedBlock> glowingInkedBlock = REGISTRY.register("glowing_inked_block", InkedBlock::glowing);
	public static final RegistrySupplier<InkedBlock> clearInkedBlock = REGISTRY.register("clear_inked_block", InkedBlock::new);
	public static final RegistrySupplier<Block> sardiniumBlock = REGISTRY.register("sardinium_block", () -> new MetalBlock(MapColor.TERRACOTTA_WHITE));
	public static final RegistrySupplier<Block> rawSardiniumBlock = REGISTRY.register("raw_sardinium_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).requiresCorrectToolForDrops().strength(5, 6)));
	public static final RegistrySupplier<Block> sardiniumOre = REGISTRY.register("sardinium_ore", OreBlock::new);
	public static final RegistrySupplier<Block> powerEggBlock = REGISTRY.register("power_egg_block", () -> new Block(BlockBehaviour.Properties.of().pushReaction(PushReaction.DESTROY).mapColor(MapColor.COLOR_ORANGE).sound(SoundType.SLIME_BLOCK).strength(0.2f, 0).lightLevel((state) -> 9)));
	public static final RegistrySupplier<CrateBlock> crate = REGISTRY.register("crate", () -> new CrateBlock("crate", false));
	public static final RegistrySupplier<CrateBlock> sunkenCrate = REGISTRY.register("sunken_crate", () -> new CrateBlock("sunken_crate", true));
	public static final RegistrySupplier<Block> ammoKnightsDebris = REGISTRY.register("ammo_knights_debris", () -> new DebrisBlock(MapColor.EMERALD));
	public static final RegistrySupplier<Block> coralite = REGISTRY.register("coralite", () -> new InkStainedBlock.WithUninkedVariant(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).mapColor(MapColor.CLAY).strength(3, 3).requiresCorrectToolForDrops()));
	public static final RegistrySupplier<Block> coraliteStairs = REGISTRY.register("coralite_stairs", () -> new InkStainedStairBlock.WithUninkedVariant(coralite.get().defaultBlockState(), BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).mapColor(MapColor.CLAY).strength(3, 3).requiresCorrectToolForDrops()));
	public static final RegistrySupplier<Block> coraliteSlab = REGISTRY.register("coralite_slab", () -> new InkStainedSlabBlock.WithUninkedVariant(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).mapColor(MapColor.CLAY).strength(3, 3).requiresCorrectToolForDrops()));
	public static final RegistrySupplier<Block> inkVat = REGISTRY.register("ink_vat", () -> new InkVatBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).requiresCorrectToolForDrops()));
	public static final RegistrySupplier<Block> weaponWorkbench = REGISTRY.register("ammo_knights_workbench", () -> new WeaponWorkbenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(2.0f).requiresCorrectToolForDrops()));
	public static final RegistrySupplier<Block> remotePedestal = REGISTRY.register("remote_pedestal", RemotePedestalBlock::new);
	public static final RegistrySupplier<Block> emptyInkwell = REGISTRY.register("empty_inkwell", () -> new EmptyInkwellBlock(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS)));
	public static final RegistrySupplier<Block> inkwell = REGISTRY.register("inkwell", InkwellBlock::new);
	public static final RegistrySupplier<Block> inkedWool = REGISTRY.register("ink_stained_wool", () -> new InkStainedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).ignitedByLava().strength(0.8F).sound(SoundType.WOOL)));
	public static final RegistrySupplier<Block> inkedCarpet = REGISTRY.register("ink_stained_carpet", () -> new InkedCarpetBlock("ink_stained_carpet"));
	public static final RegistrySupplier<Block> inkedGlass = REGISTRY.register("ink_stained_glass", () -> new InkedGlassBlock("ink_stained_glass"));
	public static final RegistrySupplier<Block> inkedGlassPane = REGISTRY.register("ink_stained_glass_pane", InkedGlassPaneBlock::new);
	public static final RegistrySupplier<Block> canvas = REGISTRY.register("canvas", () -> new CanvasBlock("canvas"));
	public static final RegistrySupplier<Block> splatSwitch = REGISTRY.register("splat_switch", SplatSwitchBlock::new);
	public static final RegistrySupplier<SpawnPadBlock> spawnPad = REGISTRY.register("spawn_pad", SpawnPadBlock::new);
	public static final RegistrySupplier<Block> spawnPadEdge = REGISTRY.register("spawn_pad_edge", () -> new SpawnPadBlock.Aux(spawnPad.get()));
	public static final RegistrySupplier<Block> grate = REGISTRY.register("grate", GrateBlock::new);
	public static final RegistrySupplier<Block> grateRamp = REGISTRY.register("grate_ramp", GrateRampBlock::new);
	public static final RegistrySupplier<Block> barrierBar = REGISTRY.register("barrier_bar", BarrierBarBlock::new);
	public static final RegistrySupplier<Block> cautionBarrierBar = REGISTRY.register("caution_barrier_bar", BarrierBarBlock::new);
	public static final RegistrySupplier<Block> platedBarrierBar = REGISTRY.register("plated_barrier_bar", BarrierBarBlock::new);
	public static final RegistrySupplier<Block> tarp = REGISTRY.register("tarp", TarpBlock::new);
	public static final RegistrySupplier<Block> glassCover = REGISTRY.register("glass_cover", TarpBlock.Seethrough::new);
	public static final RegistrySupplier<Block> stageBarrier = REGISTRY.register("stage_barrier", () -> new StageBarrierBlock(false));
	public static final RegistrySupplier<Block> stageVoid = REGISTRY.register("stage_void", () -> new StageBarrierBlock(true));
	public static final RegistrySupplier<Block> allowedColorBarrier = REGISTRY.register("allowed_color_barrier", () -> new ColoredBarrierBlock(false));
	public static final RegistrySupplier<Block> deniedColorBarrier = REGISTRY.register("denied_color_barrier", () -> new ColoredBarrierBlock(true));
	public static boolean noRedstoneConduct(BlockState state, BlockGetter getter, BlockPos pos)
	{
		return false;
	}
}
