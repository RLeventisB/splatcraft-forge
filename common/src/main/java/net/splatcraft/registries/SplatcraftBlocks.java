package net.splatcraft.registries;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.splatcraft.Splatcraft;
import net.splatcraft.blocks.*;

import java.util.ArrayList;

public class SplatcraftBlocks
{
    public static final ArrayList<Block> inkColoredBlocks = new ArrayList<>();
    protected static final DeferredRegister<Block> REGISTRY = Splatcraft.deferredRegistryOf(Registries.BLOCK);
    public static final RegistrySupplier<InkedBlock> inkedBlock = REGISTRY.register("inked_block", InkedBlock::new);
    public static final RegistrySupplier<InkedBlock> glowingInkedBlock = REGISTRY.register("glowing_inked_block", InkedBlock::glowing);
    public static final RegistrySupplier<InkedBlock> clearInkedBlock = REGISTRY.register("clear_inked_block", InkedBlock::new);
    public static final RegistrySupplier<Block> sardiniumBlock = REGISTRY.register("sardinium_block", () -> new MetalBlock(MapColor.TERRACOTTA_WHITE));
    public static final RegistrySupplier<Block> rawSardiniumBlock = REGISTRY.register("raw_sardinium_block", () -> new Block(AbstractBlock.Settings.create().mapColor(MapColor.TERRACOTTA_WHITE).requiresTool().strength(5, 6)));
    public static final RegistrySupplier<Block> sardiniumOre = REGISTRY.register("sardinium_ore", OreBlock::new);
    public static final RegistrySupplier<Block> powerEggBlock = REGISTRY.register("power_egg_block", () -> new Block(AbstractBlock.Settings.create().pistonBehavior(PistonBehavior.DESTROY).mapColor(MapColor.ORANGE).sounds(BlockSoundGroup.SLIME).strength(0.2f, 0).luminance((state) -> 9)));
    public static final RegistrySupplier<CrateBlock> crate = REGISTRY.register("crate", () -> new CrateBlock("crate", false));
    public static final RegistrySupplier<CrateBlock> sunkenCrate = REGISTRY.register("sunken_crate", () -> new CrateBlock("sunken_crate", true));
    public static final RegistrySupplier<Block> ammoKnightsDebris = REGISTRY.register("ammo_knights_debris", () -> new DebrisBlock(MapColor.EMERALD_GREEN));
    public static final RegistrySupplier<Block> coralite = REGISTRY.register("coralite", () -> new InkStainedBlock.WithUninkedVariant(AbstractBlock.Settings.create().instrument(NoteBlockInstrument.BASEDRUM).mapColor(MapColor.LIGHT_BLUE_GRAY).strength(3, 3).requiresTool()));
    public static final RegistrySupplier<Block> coraliteStairs = REGISTRY.register("coralite_stairs", () -> new InkStainedStairBlock.WithUninkedVariant(coralite.get().getDefaultState(), AbstractBlock.Settings.create().instrument(NoteBlockInstrument.BASEDRUM).mapColor(MapColor.LIGHT_BLUE_GRAY).strength(3, 3).requiresTool()));
    public static final RegistrySupplier<Block> coraliteSlab = REGISTRY.register("coralite_slab", () -> new InkStainedSlabBlock.WithUninkedVariant(AbstractBlock.Settings.create().instrument(NoteBlockInstrument.BASEDRUM).mapColor(MapColor.LIGHT_BLUE_GRAY).strength(3, 3).requiresTool()));
    public static final RegistrySupplier<Block> inkVat = REGISTRY.register("ink_vat", () -> new InkVatBlock(AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY).strength(2.0f).requiresTool()));
    public static final RegistrySupplier<Block> weaponWorkbench = REGISTRY.register("ammo_knights_workbench", () -> new WeaponWorkbenchBlock(AbstractBlock.Settings.create().mapColor(MapColor.STONE_GRAY).instrument(NoteBlockInstrument.BASEDRUM).strength(2.0f).requiresTool()));
    public static final RegistrySupplier<Block> remotePedestal = REGISTRY.register("remote_pedestal", RemotePedestalBlock::new);
    public static final RegistrySupplier<Block> emptyInkwell = REGISTRY.register("empty_inkwell", () -> new EmptyInkwellBlock(AbstractBlock.Settings.create().instrument(NoteBlockInstrument.HAT).strength(0.3F).sounds(BlockSoundGroup.GLASS)));
    public static final RegistrySupplier<Block> inkwell = REGISTRY.register("inkwell", InkwellBlock::new);
    public static final RegistrySupplier<Block> inkedWool = REGISTRY.register("ink_stained_wool", () -> new InkStainedBlock(AbstractBlock.Settings.create().mapColor(MapColor.WHITE_GRAY).burnable().strength(0.8F).sounds(BlockSoundGroup.WOOL)));
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

    public static boolean noRedstoneConduct(BlockState state, BlockView getter, BlockPos pos)
    {
        return false;
    }

    /*public static class Missmaps
    {
        private static final HashMap<String, RegistrySupplier<? extends Block>> remaps = new HashMap<>()
        {{
            put("inked_wool", inkedWool);
            put("inked_carpet", inkedCarpet);
            put("inked_glass", inkedGlass);
            put("inked_glass_pane", inkedGlassPane);
            put("weapon_workbench", weaponWorkbench);

            put("inked_stairs", inkedBlock);
            put("inked_slab", inkedBlock);
            put("tall_inked_block", inkedBlock);
            put("glowing_inked_stairs", inkedBlock);
            put("glowing_inked_slab", inkedBlock);
            put("tall_glowing_inked_block", inkedBlock);
            put("tall_clear_inked_block", inkedBlock);
        }};

        @SubscribeEvent
        public static void onMissingMappings(final MissingMappingsEvent event)
        {
            for (MissingMappingsEvent.Mapping<Block> block : event.getAllMappings(REGISTRY.getRegistryKey()))
            {
                String key = block.getKey().getPath();
                if (remaps.containsKey(key))
                    block.remap(remaps.get(key).get());
            }
        }
    }*/
}
