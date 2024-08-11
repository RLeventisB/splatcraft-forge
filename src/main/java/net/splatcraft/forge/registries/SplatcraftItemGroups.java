package net.splatcraft.forge.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.data.InkColorTags;
import net.splatcraft.forge.items.ColoredBlockItem;
import net.splatcraft.forge.util.ColorUtils;

import java.util.ArrayList;
import java.util.List;

import static net.splatcraft.forge.registries.SplatcraftItems.*;
@Mod.EventBusSubscriber
public class SplatcraftItemGroups {

    protected static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Splatcraft.MODID);

    public static final RegistryObject<CreativeModeTab> GROUP_GENERAL = REGISTRY.register("splatcraft_general", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> sardiniumBlock.get().getDefaultInstance())
            .title(Component.translatable("itemGroup.splatcraft_general"))
            .displayItems((parameters, output) ->
            {
                //Materials
                output.accept(sardinium.get());
                output.accept(sardiniumBlock.get());
                output.accept(rawSardinium.get());
                output.accept(rawSardiniumBlock.get());
                output.accept(sardiniumOre.get());
                output.accept(powerEgg.get());
                output.accept(powerEggCan.get());
                output.accept(powerEggBlock.get());
                output.accept(emptyInkwell.get());
                output.accept(ammoKnightsScrap.get());
                output.accept(blueprint.get());
                output.accept(kensaPin.get());

                //Remotes
                output.accept(stagePad.get());
                output.accept(turfScanner.get());
                output.accept(inkDisruptor.get());
                output.accept(colorChanger.get());
                output.accept(remotePedestal.get());

                //Gear
                output.accept(superJumpLure.get());
                output.accept(splatfestBand.get());
                output.accept(clearBand.get());
                output.accept(waxApplicator.get());

                //Filters
                output.accept(emptyFilter.get());
                output.accept(pastelFilter.get());
                output.accept(organicFilter.get());
                output.accept(neonFilter.get());
                output.accept(overgrownFilter.get());
                output.accept(midnightFilter.get());
                output.accept(enchantedFilter.get());
                output.accept(creativeFilter.get());

                //Crafting Stations
                output.accept(inkVat.get());
                output.accept(weaponWorkbench.get());

                //Colored Items
                output.acceptAll(ColorUtils.getColorVariantsForItem(inkwell.get(), true, true, true));
                output.acceptAll(ColorUtils.getColorVariantsForItem(spawnPad.get(), true, true, false));
                output.acceptAll(ColorUtils.getColorVariantsForItem(squidBumper.get(), true, true, false));
                output.acceptAll(ColorUtils.getColorVariantsForItem(inkedWool.get(), true, true, false));
                output.acceptAll(ColorUtils.getColorVariantsForItem(inkedCarpet.get(), true, true, false));
                output.acceptAll(ColorUtils.getColorVariantsForItem(inkedGlass.get(), true, true, false));
                output.acceptAll(ColorUtils.getColorVariantsForItem(inkedGlassPane.get(), true, true, false));

                //Decor Blocks
                output.accept(canvas.get());
                output.accept(coralite.get());
                output.accept(coraliteSlab.get());
                output.accept(coraliteStairs.get());
                output.accept(grate.get());
                output.accept(grateRamp.get());
                output.accept(barrierBar.get());
                output.accept(platedBarrierBar.get());
                output.accept(cautionBarrierBar.get());
                output.accept(tarp.get());
                output.accept(glassCover.get());
                output.accept(crate.get());
                output.accept(sunkenCrate.get());
                output.accept(splatSwitch.get());

                //Stage Barriers
                output.accept(stageBarrier.get());
                output.accept(stageVoid.get());
                output.acceptAll(ColorUtils.getColorVariantsForItem(allowedColorBarrier.get(), true, true, false));
                output.acceptAll(ColorUtils.getColorVariantsForItem(deniedColorBarrier.get(), true, true, false));


            }).build());


    public static final RegistryObject<CreativeModeTab> GROUP_WEAPONS = REGISTRY.register("splatcraft_weapons", () -> CreativeModeTab.builder()
            .withTabsBefore(GROUP_GENERAL.getKey())
            .icon(() -> ColorUtils.setInkColor(splattershot.get().getDefaultInstance(), ColorUtils.ORANGE))
            .title(Component.translatable("itemGroup.splatcraft_weapons"))
            .displayItems((parameters, output) ->
        {
                output.acceptAll(weapons.stream().filter(weapon -> !weapon.isSecret).map(Item::getDefaultInstance).toList());

                output.accept(inkClothHelmet.get());
                output.accept(inkClothChestplate.get());
                output.accept(inkClothLeggings.get());
                output.accept(inkClothBoots.get());
            }).build());

    public static final ArrayList<Item> colorTabItems = new ArrayList<>();

    public static final RegistryObject<CreativeModeTab> GROUP_COLORS = REGISTRY.register("splatcraft_colors", () -> CreativeModeTab.builder()
            .withTabsBefore(GROUP_WEAPONS.getKey())
            .icon(() -> ColorUtils.setInkColor(inkwell.get().getDefaultInstance(), ColorUtils.ORANGE))
            .title(Component.translatable("itemGroup.splatcraft_colors"))
            .displayItems((parameters, output) ->
                    {
                        for(Item item : colorTabItems)
            {
                            for(int color : InkColorTags.CREATIVE_TAB_COLORS.getAll().stream().sorted().toList())
                                output.accept(ColorUtils.setColorLocked(ColorUtils.setInkColor(new ItemStack(item), color), true));
                            if(!(item instanceof ColoredBlockItem coloredBlockItem) || coloredBlockItem.matchesColor())
                                output.accept(ColorUtils.setInverted(new ItemStack(item), true));
                        }
				}
            ).build());


    @SubscribeEvent
    public void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS)
        {
            event.accept(splatSwitch.get());
            event.accept(remotePedestal.get());
        }
    }
}
