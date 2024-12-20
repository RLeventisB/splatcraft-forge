package net.splatcraft.registries;

import dev.architectury.registry.item.ItemPropertiesRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.DispenserBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.inktanks.ArmoredInkTankModel;
import net.splatcraft.client.models.inktanks.ClassicInkTankModel;
import net.splatcraft.client.models.inktanks.InkTankJrModel;
import net.splatcraft.client.models.inktanks.InkTankModel;
import net.splatcraft.dispenser.PlaceBlockDispenseBehavior;
import net.splatcraft.entities.subs.CurlingBombEntity;
import net.splatcraft.items.*;
import net.splatcraft.items.remotes.ColorChangerItem;
import net.splatcraft.items.remotes.InkDisruptorItem;
import net.splatcraft.items.remotes.RemoteItem;
import net.splatcraft.items.remotes.TurfScannerItem;
import net.splatcraft.items.weapons.*;
import net.splatcraft.util.ColorUtils;

import java.util.*;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class SplatcraftItems
{
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIAL_REGISTRY = Splatcraft.deferredRegistryOf(Registries.ARMOR_MATERIAL);
    public static final DeferredRegister<Item> REGISTRY = Splatcraft.deferredRegistryOf(Registries.ITEM);
    public static final List<WeaponBaseItem<?>> weapons = new ArrayList<>();
    public static final ArrayList<Item> inkColoredItems = new ArrayList<>();
    public static final Identifier SPEED_MOD_IDENTIFIER = Identifier.ofVanilla("generic.movement_speed");
    //Armor Materials
    public static final RegistrySupplier<ArmorMaterial> INK_CLOTH = createArmorMaterial("ink_cloth", SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 0, 0, 0, null);
    //Vanity
    public static final RegistrySupplier<Item> inkClothHelmet = REGISTRY.register("ink_cloth_helmet", () -> new ColoredArmorItem(INK_CLOTH, ArmorItem.Type.HELMET));
    public static final RegistrySupplier<Item> inkClothChestplate = REGISTRY.register("ink_cloth_chestplate", () -> new ColoredArmorItem(INK_CLOTH, ArmorItem.Type.CHESTPLATE));
    public static final RegistrySupplier<Item> inkClothLeggings = REGISTRY.register("ink_cloth_leggings", () -> new ColoredArmorItem(INK_CLOTH, ArmorItem.Type.LEGGINGS));
    public static final RegistrySupplier<Item> inkClothBoots = REGISTRY.register("ink_cloth_boots", () -> new ColoredArmorItem(INK_CLOTH, ArmorItem.Type.BOOTS));
    public static final RegistrySupplier<ArmorMaterial> ARMORED_INK_TANK_MATERIAL = createArmorMaterial("armored_ink_tank", SoundEvents.ITEM_ARMOR_EQUIP_IRON, 3, 0, 0.05f, null);
    public static final RegistrySupplier<InkTankItem> armoredInkTank = REGISTRY.register("armored_ink_tank", () -> new InkTankItem("armored_ink_tank", 85, ARMORED_INK_TANK_MATERIAL));
    public static final RegistrySupplier<ArmorMaterial> DEFAULT_INK_TANK_MATERIAL = createArmorMaterial("unprotected_ink_tank", SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 0, 0, 0, null);
    //Shooters
    public static final RegistrySupplier<ShooterItem> splattershot = ShooterItem.create(REGISTRY, "splattershot", "splattershot");
    public static final RegistrySupplier<ShooterItem> ancientSplattershot = ShooterItem.create(REGISTRY, splattershot, "ancient_splattershot", true);
    public static final RegistrySupplier<ShooterItem> tentatekSplattershot = ShooterItem.create(REGISTRY, "splattershot", "tentatek_splattershot");
    public static final RegistrySupplier<ShooterItem> wasabiSplattershot = ShooterItem.create(REGISTRY, "splattershot", "wasabi_splattershot");
    public static final RegistrySupplier<ShooterItem> splattershotJr = ShooterItem.create(REGISTRY, "splattershot_jr", "splattershot_jr");
    public static final RegistrySupplier<ShooterItem> kensaSplattershotJr = ShooterItem.create(REGISTRY, "splattershot_jr", "kensa_splattershot_jr");
    public static final RegistrySupplier<ShooterItem> aerosprayMG = ShooterItem.create(REGISTRY, "aerospray", "aerospray_mg");
    public static final RegistrySupplier<ShooterItem> aerosprayRG = ShooterItem.create(REGISTRY, "aerospray", "aerospray_rg");
    public static final RegistrySupplier<ShooterItem> gal52 = ShooterItem.create(REGISTRY, "52_gal", "52_gal");
    public static final RegistrySupplier<ShooterItem> gal52Deco = ShooterItem.create(REGISTRY, "52_gal", "52_gal_deco");
    public static final RegistrySupplier<ShooterItem> kensaGal52 = ShooterItem.create(REGISTRY, "52_gal", "kensa_52_gal");
    public static final RegistrySupplier<ShooterItem> gal96 = ShooterItem.create(REGISTRY, "96_gal", "96_gal");
    public static final RegistrySupplier<ShooterItem> gal96Deco = ShooterItem.create(REGISTRY, "96_gal", "96_gal_deco");
    public static final RegistrySupplier<ShooterItem> nzap85 = ShooterItem.create(REGISTRY, "n-zap", "n-zap85");
    public static final RegistrySupplier<ShooterItem> nzap89 = ShooterItem.create(REGISTRY, "n-zap", "n-zap89");
    public static final RegistrySupplier<ShooterItem> jet_squelcher = ShooterItem.create(REGISTRY, "jet_squelcher", "jet_squelcher");
    public static final RegistrySupplier<ShooterItem> splash_o_matic = ShooterItem.create(REGISTRY, "splash_o_matic", "splash_o_matic");
    //Blasters
    public static final RegistrySupplier<BlasterItem> blaster = BlasterItem.createBlaster(REGISTRY, "blaster", "blaster");
    public static final RegistrySupplier<BlasterItem> grimBlaster = BlasterItem.createBlaster(REGISTRY, "blaster", "grim_blaster");
    public static final RegistrySupplier<BlasterItem> rangeBlaster = BlasterItem.createBlaster(REGISTRY, "range_blaster", "range_blaster");
    public static final RegistrySupplier<BlasterItem> grimRangeBlaster = BlasterItem.createBlaster(REGISTRY, "range_blaster", "grim_range_blaster");
    public static final RegistrySupplier<BlasterItem> clashBlaster = BlasterItem.createBlaster(REGISTRY, "clash_blaster", "clash_blaster");
    public static final RegistrySupplier<BlasterItem> clashBlasterNeo = BlasterItem.createBlaster(REGISTRY, "clash_blaster", "clash_blaster_neo");
    public static final RegistrySupplier<BlasterItem> lunaBlaster = BlasterItem.createBlaster(REGISTRY, "luna_blaster", "luna_blaster");
    public static final RegistrySupplier<BlasterItem> rapidBlaster = BlasterItem.createBlaster(REGISTRY, "rapid_blaster", "rapid_blaster");
    public static final RegistrySupplier<BlasterItem> rapidBlasterPro = BlasterItem.createBlaster(REGISTRY, "rapid_blaster_pro", "rapid_blaster_pro");
    //Rollers
    public static final RegistrySupplier<RollerItem> splatRoller = RollerItem.create(REGISTRY, "splat_roller", "splat_roller");
    public static final RegistrySupplier<RollerItem> krakOnSplatRoller = RollerItem.create(REGISTRY, "splat_roller", "krak_on_splat_roller");
    public static final RegistrySupplier<RollerItem> coroCoroSplatRoller = RollerItem.create(REGISTRY, "splat_roller", "corocoro_splat_roller");
    public static final RegistrySupplier<RollerItem> carbonRoller = RollerItem.create(REGISTRY, "carbon_roller", "carbon_roller");
    public static final RegistrySupplier<RollerItem> dynamoRoller = RollerItem.create(REGISTRY, "dynamo_roller", "dynamo_roller");
    public static final RegistrySupplier<RollerItem> inkbrush = RollerItem.create(REGISTRY, "inkbrush", "inkbrush");
    public static final RegistrySupplier<RollerItem> octobrush = RollerItem.create(REGISTRY, "octobrush", "octobrush");
    public static final RegistrySupplier<RollerItem> kensaOctobrush = RollerItem.create(REGISTRY, "octobrush", "kensa_octobrush");
    //Chargers
    public static final RegistrySupplier<ChargerItem> splatCharger = ChargerItem.create(REGISTRY, "splat_charger", "splat_charger");
    public static final RegistrySupplier<ChargerItem> bentoSplatCharger = ChargerItem.create(REGISTRY, "splat_charger", "bento_splat_charger");
    public static final RegistrySupplier<ChargerItem> kelpSplatCharger = ChargerItem.create(REGISTRY, "splat_charger", "kelp_splat_charger");
    public static final RegistrySupplier<ChargerItem> eLiter4K = ChargerItem.create(REGISTRY, "e_liter", "e_liter_4k");
    public static final RegistrySupplier<ChargerItem> eliter3K = ChargerItem.create(REGISTRY, "e_liter", "e_liter_3k");
    public static final RegistrySupplier<ChargerItem> bamboozler14mk1 = ChargerItem.create(REGISTRY, "bamboozler_14", "bamboozler_14_mk1");
    public static final RegistrySupplier<ChargerItem> bamboozler14mk2 = ChargerItem.create(REGISTRY, "bamboozler_14", "bamboozler_14_mk2");
    public static final RegistrySupplier<ChargerItem> classicSquiffer = ChargerItem.create(REGISTRY, "squiffer", "classic_squiffer");
    //Dualies
    public static final RegistrySupplier<DualieItem> splatDualie = DualieItem.create(REGISTRY, "splat_dualies", "splat_dualies");
    public static final RegistrySupplier<DualieItem> enperrySplatDualie = DualieItem.create(REGISTRY, "splat_dualies", "enperry_splat_dualies");
    public static final RegistrySupplier<DualieItem> dualieSquelcher = DualieItem.create(REGISTRY, "dualie_squelchers", "dualie_squelchers");
    public static final RegistrySupplier<DualieItem> gloogaDualie = DualieItem.create(REGISTRY, "glooga_dualies", "glooga_dualies");
    public static final RegistrySupplier<DualieItem> gloogaDualieDeco = DualieItem.create(REGISTRY, "glooga_dualies", "glooga_dualies_deco");
    public static final RegistrySupplier<DualieItem> kensaGloogaDualie = DualieItem.create(REGISTRY, "glooga_dualies", "kensa_glooga_dualies");
    //Sloshers
    public static final RegistrySupplier<SlosherItem> slosher = SlosherItem.create(REGISTRY, "slosher", "slosher", SlosherItem.Type.DEFAULT);
    public static final RegistrySupplier<SlosherItem> classicSlosher = SlosherItem.create(REGISTRY, slosher, "classic_slosher");
    public static final RegistrySupplier<SlosherItem> sodaSlosher = SlosherItem.create(REGISTRY, slosher, "soda_slosher");
    public static final RegistrySupplier<SlosherItem> triSlosher = SlosherItem.create(REGISTRY, "tri_slosher", "tri_slosher", SlosherItem.Type.DEFAULT);
    public static final RegistrySupplier<SlosherItem> explosher = SlosherItem.create(REGISTRY, "explosher", "explosher", SlosherItem.Type.EXPLODING);
    //Splatlings
    public static final RegistrySupplier<SplatlingItem> miniSplatling = SplatlingItem.create(REGISTRY, "mini_splatling", "mini_splatling");
    public static final RegistrySupplier<SplatlingItem> refurbishedMiniSplatling = SplatlingItem.create(REGISTRY, "mini_splatling", "refurbished_mini_splatling");
    public static final RegistrySupplier<SplatlingItem> heavySplatling = SplatlingItem.create(REGISTRY, "heavy_splatling", "heavy_splatling");
    public static final RegistrySupplier<SplatlingItem> heavySplatlingDeco = SplatlingItem.create(REGISTRY, "heavy_splatling", "heavy_splatling_deco");
    public static final RegistrySupplier<SplatlingItem> heavySplatlingRemix = SplatlingItem.create(REGISTRY, "heavy_splatling", "heavy_splatling_remix");
    public static final RegistrySupplier<SplatlingItem> classicHeavySplatling = SplatlingItem.create(REGISTRY, "heavy_splatling", "classic_heavy_splatling");
    public static final RegistrySupplier<SplatlingItem> nautilus_47 = SplatlingItem.create(REGISTRY, "nautilus", "nautilus_47");
    public static final RegistrySupplier<SplatlingItem> nautilus_79 = SplatlingItem.create(REGISTRY, "nautilus", "nautilus_79");
    //Ink Tanks
    public static final RegistrySupplier<InkTankItem> inkTank = REGISTRY.register("ink_tank", () -> new InkTankItem("ink_tank", 100));
    public static final RegistrySupplier<InkTankItem> classicInkTank = REGISTRY.register("classic_ink_tank", () -> new InkTankItem("classic_ink_tank", 100));
    public static final RegistrySupplier<InkTankItem> inkTankJr = REGISTRY.register("ink_tank_jr", () -> new InkTankItem("ink_tank_jr", 110));
    //Sub Weapons
    public static final RegistrySupplier<SubWeaponItem> splatBomb = REGISTRY.register("splat_bomb", () -> new SubWeaponItem(SplatcraftEntities.SPLAT_BOMB, "splat_bomb"));
    public static final RegistrySupplier<SubWeaponItem> splatBomb2 = REGISTRY.register("splat_bomb_2", () -> new SubWeaponItem(SplatcraftEntities.SPLAT_BOMB, "splat_bomb").setSecret(true));
    public static final RegistrySupplier<SubWeaponItem> burstBomb = REGISTRY.register("burst_bomb", () -> new SubWeaponItem(SplatcraftEntities.BURST_BOMB, "burst_bomb"));
    public static final RegistrySupplier<SubWeaponItem> suctionBomb = REGISTRY.register("suction_bomb", () -> new SubWeaponItem(SplatcraftEntities.SUCTION_BOMB, "suction_bomb"));
    public static final RegistrySupplier<SubWeaponItem> curlingBomb = REGISTRY.register("curling_bomb", () -> new CurlingSubWeaponItem(SplatcraftEntities.CURLING_BOMB, "curling_bomb", CurlingBombEntity::onItemUseTick));
    //Materials
    public static final RegistrySupplier<Item> sardinium = REGISTRY.register("sardinium", () -> new Item(new Item.Settings()));
    public static final RegistrySupplier<Item> sardiniumBlock = REGISTRY.register("sardinium_block", () -> new BlockItem(SplatcraftBlocks.sardiniumBlock.get()));
    public static final RegistrySupplier<Item> rawSardinium = REGISTRY.register("raw_sardinium", () -> new Item(new Item.Settings()));
    public static final RegistrySupplier<Item> sardiniumOre = REGISTRY.register("sardinium_ore", () -> new BlockItem(SplatcraftBlocks.sardiniumOre.get()));
    public static final RegistrySupplier<Item> rawSardiniumBlock = REGISTRY.register("raw_sardinium_block", () -> new BlockItem(SplatcraftBlocks.rawSardiniumBlock.get()));
    public static final RegistrySupplier<Item> coralite = REGISTRY.register("coralite", () -> new ColoredBlockItem(SplatcraftBlocks.coralite.get()).setMatchColor(false).clearsToSelf());
    public static final RegistrySupplier<Item> coraliteSlab = REGISTRY.register("coralite_slab", () -> new ColoredBlockItem(SplatcraftBlocks.coraliteSlab.get()).setMatchColor(false).clearsToSelf());
    public static final RegistrySupplier<Item> coraliteStairs = REGISTRY.register("coralite_stairs", () -> new ColoredBlockItem(SplatcraftBlocks.coraliteStairs.get()).setMatchColor(false).clearsToSelf());
    public static final RegistrySupplier<Item> powerEgg = REGISTRY.register("power_egg", () -> new Item(new Item.Settings()));
    public static final RegistrySupplier<Item> powerEggCan = REGISTRY.register("power_egg_can", PowerEggCanItem::new);
    public static final RegistrySupplier<Item> powerEggBlock = REGISTRY.register("power_egg_block", () -> new BlockItem(SplatcraftBlocks.powerEggBlock.get()));
    public static final RegistrySupplier<Item> emptyInkwell = REGISTRY.register("empty_inkwell", () -> new BlockItem(SplatcraftBlocks.emptyInkwell.get()));
    //Map Items
    public static final RegistrySupplier<Item> inkwell = REGISTRY.register("inkwell", () -> new ColoredBlockItem(SplatcraftBlocks.inkwell.get(), 16, emptyInkwell.get()));
    public static final RegistrySupplier<Item> ammoKnightsScrap = REGISTRY.register("ammo_knights_scrap", () -> new Item(new Item.Settings()));
    public static final RegistrySupplier<Item> blueprint = REGISTRY.register("blueprint", BlueprintItem::new);
    public static final RegistrySupplier<Item> kensaPin = REGISTRY.register("toni_kensa_pin", () -> new Item(new Item.Settings().rarity(Rarity.UNCOMMON)));
    //Remotes
    public static final RegistrySupplier<RemoteItem> turfScanner = REGISTRY.register("turf_scanner", TurfScannerItem::new);
    public static final RegistrySupplier<RemoteItem> inkDisruptor = REGISTRY.register("ink_disruptor", InkDisruptorItem::new);
    public static final RegistrySupplier<RemoteItem> colorChanger = REGISTRY.register("color_changer", ColorChangerItem::new);
    //Filters
    public static final RegistrySupplier<FilterItem> emptyFilter = REGISTRY.register("filter", FilterItem::new);
    public static final RegistrySupplier<FilterItem> pastelFilter = REGISTRY.register("pastel_filter", FilterItem::new);
    public static final RegistrySupplier<FilterItem> organicFilter = REGISTRY.register("organic_filter", FilterItem::new);
    public static final RegistrySupplier<FilterItem> neonFilter = REGISTRY.register("neon_filter", FilterItem::new);
    public static final RegistrySupplier<FilterItem> enchantedFilter = REGISTRY.register("enchanted_filter", () -> new FilterItem(Rarity.UNCOMMON, true, false));
    public static final RegistrySupplier<FilterItem> overgrownFilter = REGISTRY.register("overgrown_filter", FilterItem::new);
    public static final RegistrySupplier<FilterItem> midnightFilter = REGISTRY.register("midnight_filter", FilterItem::new);
    public static final RegistrySupplier<FilterItem> creativeFilter = REGISTRY.register("creative_filter", () -> new FilterItem(Rarity.RARE, false, true));
    //Crafting Stations
    public static final RegistrySupplier<Item> inkVat = REGISTRY.register("ink_vat", () -> new BlockItem(SplatcraftBlocks.inkVat.get()));
    public static final RegistrySupplier<Item> weaponWorkbench = REGISTRY.register("ammo_knights_workbench", () -> new BlockItem(SplatcraftBlocks.weaponWorkbench.get()));
    public static final RegistrySupplier<Item> spawnPad = REGISTRY.register("spawn_pad", () -> new ColoredBlockItem(SplatcraftBlocks.spawnPad.get(), 1));
    public static final RegistrySupplier<Item> grate = REGISTRY.register("grate", () -> new BlockItem(SplatcraftBlocks.grate.get()));
    public static final RegistrySupplier<Item> grateRamp = REGISTRY.register("grate_ramp", () -> new BlockItem(SplatcraftBlocks.grateRamp.get()));
    public static final RegistrySupplier<Item> barrierBar = REGISTRY.register("barrier_bar", () -> new BlockItem(SplatcraftBlocks.barrierBar.get()));
    public static final RegistrySupplier<Item> platedBarrierBar = REGISTRY.register("plated_barrier_bar", () -> new BlockItem(SplatcraftBlocks.platedBarrierBar.get()));
    public static final RegistrySupplier<Item> cautionBarrierBar = REGISTRY.register("caution_barrier_bar", () -> new BlockItem(SplatcraftBlocks.cautionBarrierBar.get()));
    public static final RegistrySupplier<Item> tarp = REGISTRY.register("tarp", () -> new BlockItem(SplatcraftBlocks.tarp.get()));
    public static final RegistrySupplier<Item> glassCover = REGISTRY.register("glass_cover", () -> new BlockItem(SplatcraftBlocks.glassCover.get()));
    public static final RegistrySupplier<Item> canvas = REGISTRY.register("canvas", () -> new ColoredBlockItem(SplatcraftBlocks.canvas.get()).setMatchColor(false));
    public static final RegistrySupplier<Item> squidBumper = REGISTRY.register("squid_bumper", SquidBumperItem::new);
    public static final RegistrySupplier<Item> sunkenCrate = REGISTRY.register("sunken_crate", () -> new BlockItem(SplatcraftBlocks.sunkenCrate.get()));
    public static final RegistrySupplier<Item> crate = REGISTRY.register("crate", () -> new BlockItem(SplatcraftBlocks.crate.get()));
    //Redstone Components
    public static final RegistrySupplier<Item> remotePedestal = REGISTRY.register("remote_pedestal", () -> new ColoredBlockItem(SplatcraftBlocks.remotePedestal.get()));
    public static final RegistrySupplier<Item> splatSwitch = REGISTRY.register("splat_switch", () -> new BlockItem(SplatcraftBlocks.splatSwitch.get()));
    //Ink Stained Blocks
    public static final RegistrySupplier<Item> inkedWool = REGISTRY.register("ink_stained_wool", () -> new ColoredBlockItem(SplatcraftBlocks.inkedWool.get(), new Item.Settings(), Items.WHITE_WOOL));
    public static final RegistrySupplier<Item> inkedCarpet = REGISTRY.register("ink_stained_carpet", () -> new ColoredBlockItem(SplatcraftBlocks.inkedCarpet.get(), new Item.Settings(), Items.WHITE_CARPET));
    public static final RegistrySupplier<Item> inkedGlass = REGISTRY.register("ink_stained_glass", () -> new ColoredBlockItem(SplatcraftBlocks.inkedGlass.get(), new Item.Settings(), Items.GLASS));
    public static final RegistrySupplier<Item> inkedGlassPane = REGISTRY.register("ink_stained_glass_pane", () -> new ColoredBlockItem(SplatcraftBlocks.inkedGlassPane.get(), new Item.Settings(), Items.GLASS_PANE));
    //Barriers
    public static final RegistrySupplier<Item> allowedColorBarrier = REGISTRY.register("allowed_color_barrier", () -> new ColoredBlockItem(SplatcraftBlocks.allowedColorBarrier.get()));
    public static final RegistrySupplier<Item> deniedColorBarrier = REGISTRY.register("denied_color_barrier", () -> new ColoredBlockItem(SplatcraftBlocks.deniedColorBarrier.get()));
    public static final RegistrySupplier<Item> stageBarrier = REGISTRY.register("stage_barrier", () -> new BlockItem(SplatcraftBlocks.stageBarrier.get()));
    public static final RegistrySupplier<Item> stageVoid = REGISTRY.register("stage_void", () -> new BlockItem(SplatcraftBlocks.stageVoid.get()));
    //Gear
    public static final RegistrySupplier<Item> splatfestBand = REGISTRY.register("splatfest_band", () -> new Item(new Item.Settings().maxCount(1)));
    public static final RegistrySupplier<Item> clearBand = REGISTRY.register("clear_ink_band", () -> new Item(new Item.Settings().maxCount(1)));
    public static final RegistrySupplier<Item> waxApplicator = REGISTRY.register("wax_applicator", InkWaxerItem::new);
    public static final RegistrySupplier<Item> superJumpLure = REGISTRY.register("super_jump_lure", JumpLureItem::new);
    public static final RegistrySupplier<Item> stagePad = REGISTRY.register("stage_pad", StagePadItem::new);

    public static Item register(String id, Supplier<Item> supplier)
    {
        return Registry.register(Registries.ITEM, Splatcraft.identifierOf(id), supplier.get());
    }

    public static void postRegister()
    {
        SplatcraftItemGroups.colorTabItems.addAll(new ArrayList<>()
        {{
            add(inkwell.get());
            add(spawnPad.get());
            add(squidBumper.get());
            add(canvas.get());
            add(coralite.get());
            add(coraliteSlab.get());
            add(coraliteStairs.get());
            add(inkedWool.get());
            add(inkedCarpet.get());
            add(inkedGlass.get());
            add(inkedGlassPane.get());
            add(inkedGlassPane.get());
            add(allowedColorBarrier.get());
            add(deniedColorBarrier.get());
        }});

        DispenserBlock.registerBehavior(emptyInkwell.get(), new PlaceBlockDispenseBehavior());
        DispenserBlock.registerBehavior(inkwell.get(), new PlaceBlockDispenseBehavior());
    }

    public static RegistrySupplier<ArmorMaterial> createArmorMaterial(String name, RegistryEntry<SoundEvent> equipSound, int armor, float toughness, float knockbackResistance, Ingredient repairIngredient)
    {
        Identifier id = Splatcraft.identifierOf(name);
        return ARMOR_MATERIAL_REGISTRY.register(id, () ->
            new ArmorMaterial(Map.of(
                ArmorItem.Type.HELMET, armor,
                ArmorItem.Type.CHESTPLATE, armor,
                ArmorItem.Type.LEGGINGS, armor,
                ArmorItem.Type.BOOTS, armor)
                , 0, equipSound, () -> repairIngredient, Collections.singletonList(new ArmorMaterial.Layer(id, "", false)), toughness, knockbackResistance));
    }

    @Environment(EnvType.CLIENT)
    public static void registerModelProperties()
    {
        Identifier activeProperty = Splatcraft.identifierOf("active");
        Identifier modeProperty = Splatcraft.identifierOf("mode");
        Identifier inkProperty = Splatcraft.identifierOf("ink");
        Identifier isLeftProperty = Splatcraft.identifierOf("is_left");
        Identifier unfoldedProperty = Splatcraft.identifierOf("unfolded");

        for (RemoteItem remote : RemoteItem.remotes)
        {
            ItemPropertiesRegistry.register(remote, activeProperty, remote.getActiveProperty());
            ItemPropertiesRegistry.register(remote, modeProperty, remote.getModeProperty());
        }

        for (InkTankItem tank : InkTankItem.inkTanks)
        {
            ItemPropertiesRegistry.register(tank, inkProperty, (stack, level, entity, seed) -> InkTankItem.getInkAmount(stack) / tank.capacity);
        }

        for (DualieItem dualie : DualieItem.dualies)
        {
            ItemPropertiesRegistry.register(dualie, isLeftProperty, dualie.getIsLeft());
        }

        for (RollerItem roller : RollerItem.rollers)
        {
            ItemPropertiesRegistry.register(roller, unfoldedProperty, roller.getUnfolded());
        }

        ClampedModelPredicateProvider coloredProperty = (stack, level, entity, seed) -> !ColorUtils.getInkColor(stack).isValid() ? 0 : 1;
        ItemPropertiesRegistry.register(canvas.get(), Splatcraft.identifierOf("inked"), coloredProperty);
        ItemPropertiesRegistry.register(coralite.get(), Splatcraft.identifierOf("colored"), coloredProperty);
        ItemPropertiesRegistry.register(coraliteSlab.get(), Splatcraft.identifierOf("colored"), coloredProperty);
        ItemPropertiesRegistry.register(coraliteStairs.get(), Splatcraft.identifierOf("colored"), coloredProperty);
    }

    @Environment(EnvType.CLIENT)
    public static void registerArmorModels()
    {
        inkTank.get().setArmorModel(new InkTankModel(MinecraftClient.getInstance().getEntityModelLoader().getModelPart(InkTankModel.LAYER_LOCATION)));
        classicInkTank.get().setArmorModel(new ClassicInkTankModel(MinecraftClient.getInstance().getEntityModelLoader().getModelPart(ClassicInkTankModel.LAYER_LOCATION)));
        inkTankJr.get().setArmorModel(new InkTankJrModel(MinecraftClient.getInstance().getEntityModelLoader().getModelPart(InkTankJrModel.LAYER_LOCATION)));
        armoredInkTank.get().setArmorModel(new ArmoredInkTankModel(MinecraftClient.getInstance().getEntityModelLoader().getModelPart(ArmoredInkTankModel.LAYER_LOCATION)));
    }

    public static class Missmaps
    {
        private static final HashMap<String, RegistrySupplier<? extends Item>> remaps = new HashMap<>()
        {{
            put("inked_wool", inkedWool);
            put("inked_carpet", inkedCarpet);
            put("inked_glass", inkedGlass);
            put("inked_glass_pane", inkedGlassPane);
            put("weapon_workbench", weaponWorkbench);
            put("ink_polisher", waxApplicator);
        }};
    }
}
