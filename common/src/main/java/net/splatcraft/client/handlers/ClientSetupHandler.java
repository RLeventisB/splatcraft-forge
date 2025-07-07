package net.splatcraft.client.handlers;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.client.gui.InkVatScreen;
import net.splatcraft.client.gui.WeaponWorkbenchScreen;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.platform.services.MenuScreenFactory;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.container.InkVatContainer;
import net.splatcraft.tileentities.container.WeaponWorkbenchContainer;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class ClientSetupHandler
{
	public static <H extends AbstractContainerMenu, S extends Screen & MenuAccess<H>> void bindScreenContainers(BiConsumer<MenuType<? extends H>, MenuScreenFactory<H, S>> register)
	{
		register.accept((MenuType<? extends H>) SplatcraftTileEntities.inkVatContainer.get(), (a, e, i) -> (S) new InkVatScreen((InkVatContainer) a, e, i));
		register.accept((MenuType<? extends H>) SplatcraftTileEntities.weaponWorkbenchContainer.get(), (a, e, i) -> (S) new WeaponWorkbenchScreen((WeaponWorkbenchContainer) a, e, i));
	}
	// todo: me thinks these are handled by the rendering but just in case i will put a todo here
	public static void initItemColors(ItemColors colors)
	{
		SplatcraftItems.inkColoredItems.add(SplatcraftItems.splatfestBand.get());
		SplatcraftItems.inkColoredItems.add(SplatcraftItems.clearBand.get());
		
		colors.register(new InkItemColor(), SplatcraftItems.inkColoredItems.toArray(new Item[0]));
	}
	public static void initBlockColors(BlockColors colors)
	{
		colors.register(new ColoredTileEntityColor(), SplatcraftBlocks.inkColoredBlocks.toArray(new Block[0]));
	}
	// https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/test/java/net/minecraftforge/debug/client/CustomTASTest.java
    /*@SubscribeEvent
    public static void registerSpriteLoaders(RegisterSpriteLoadersEvent event)
    {
        event.register("weapon_loader", new WeaponLoader()); // so the gal deco texture has this!!! idk why but ok here it is
    }*/
	protected static class InkItemColor implements ItemColor
	{
		@Override
		public int getColor(@NotNull ItemStack stack, int i)
		{
			if (i != 0 || !stack.has(SplatcraftComponents.ITEM_COLOR_DATA))
				return -1;
			
			SplatcraftComponents.ItemColorData colorData = stack.get(SplatcraftComponents.ITEM_COLOR_DATA);
			boolean isDefault = colorData.color().isInvalid() && !colorData.colorLocked();
			InkColor color = (stack.is(SplatcraftTags.Items.INK_BANDS) || !stack.is(SplatcraftTags.Items.MATCH_ITEMS)) && isDefault && EntityInfoCapability.hasCapability(ClientUtils.getClientPlayer())
				? ColorUtils.getEntityColor(ClientUtils.getClientPlayer()) : colorData.color();
			color = ColorUtils.getColorLockedIfConfig(color);
			
			if (ColorUtils.isInverted(stack))
				color = color.getInverted();
			
			return color.getColorWithAlpha(255);
		}
	}
	public static class ColoredTileEntityColor implements BlockColor
	{
		@Override
		public int getColor(@NotNull BlockState blockState, @Nullable BlockAndTintGetter iBlockDisplayReader, @Nullable BlockPos blockPos, int i)
		{
			if (i != 0 || iBlockDisplayReader == null || blockPos == null)
				return -1;
			
			BlockEntity te = iBlockDisplayReader.getBlockEntity(blockPos);
			
			if (te == null)
				return -1;
			
			InkColor color = ColorUtils.getInkColor(te);
			
			if (ColorUtils.isInverted(te.getLevel(), blockPos))
				color = color.getInverted();
			
			color = ColorUtils.getColorLockedIfConfig(color);
			
			if (!color.isValid())
				return 0xFFFFFF;
			
			return color.getColorWithAlpha(255);
		}
	}
}
