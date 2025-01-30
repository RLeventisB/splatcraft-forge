package net.splatcraft.client.handlers;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.splatcraft.client.gui.InkVatScreen;
import net.splatcraft.client.gui.WeaponWorkbenchScreen;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.container.InkVatContainer;
import net.splatcraft.tileentities.container.WeaponWorkbenchContainer;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class ClientSetupHandler
{
	public static <H extends ScreenHandler, S extends Screen & ScreenHandlerProvider<H>> void bindScreenContainers(BiConsumer<ScreenHandlerType<? extends H>, MenuRegistry.ScreenFactory<H, S>> register)
	{
		register.accept((ScreenHandlerType<? extends H>) SplatcraftTileEntities.inkVatContainer.get(), (a, e, i) -> (S) new InkVatScreen((InkVatContainer) a, e, i));
		register.accept((ScreenHandlerType<? extends H>) SplatcraftTileEntities.weaponWorkbenchContainer.get(), (a, e, i) -> (S) new WeaponWorkbenchScreen((WeaponWorkbenchContainer) a, e, i));
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
		colors.registerColorProvider(new ColoredTileEntityColor(), SplatcraftBlocks.inkColoredBlocks.toArray(new Block[0]));
	}
	// https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/test/java/net/minecraftforge/debug/client/CustomTASTest.java
    /*@SubscribeEvent
    public static void registerSpriteLoaders(RegisterSpriteLoadersEvent event)
    {
        event.register("weapon_loader", new WeaponLoader()); // so the gal deco texture has this!!! idk why but ok here it is
    }*/
	protected static class InkItemColor implements ItemColorProvider
	{
		@Override
		public int getColor(@NotNull ItemStack stack, int i)
		{
			if (i != 0 || !stack.contains(SplatcraftComponents.ITEM_COLOR_DATA))
				return -1;
			
			SplatcraftComponents.ItemColorData colorData = stack.get(SplatcraftComponents.ITEM_COLOR_DATA);
			boolean isDefault = colorData.color().isInvalid() && !colorData.colorLocked();
			InkColor color = (stack.isIn(SplatcraftTags.Items.INK_BANDS) || !stack.isIn(SplatcraftTags.Items.MATCH_ITEMS)) && isDefault && EntityInfoCapability.hasCapability(ClientUtils.getClientPlayer())
				? ColorUtils.getEntityColor(ClientUtils.getClientPlayer()) : colorData.color();
			color = ColorUtils.getColorLockedIfConfig(color);
			
			if (ColorUtils.isInverted(stack))
				color = color.getInverted();
			
			return color.getColorWithAlpha(255);
		}
	}
	public static class ColoredTileEntityColor implements BlockColorProvider
	{
		@Override
		public int getColor(@NotNull BlockState blockState, @Nullable BlockRenderView iBlockDisplayReader, @Nullable BlockPos blockPos, int i)
		{
			if (i != 0 || iBlockDisplayReader == null || blockPos == null)
				return -1;
			
			BlockEntity te = iBlockDisplayReader.getBlockEntity(blockPos);
			
			if (te == null)
				return -1;
			
			InkColor color = ColorUtils.getInkColor(te);
			
			if (ColorUtils.isInverted(te.getWorld(), blockPos))
				color = color.getInverted();
			
			color = ColorUtils.getColorLockedIfConfig(color);
			
			if (!color.isValid())
				return 0xFFFFFF;
			
			return color.getColorWithAlpha(255);
		}
	}
}
