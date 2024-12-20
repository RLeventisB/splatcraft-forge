package net.splatcraft.client.handlers;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.gui.InkVatScreen;
import net.splatcraft.client.gui.WeaponWorkbenchScreen;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClientSetupHandler
{
    public static void onTextureStitch(Sprite sprite)
    {
        if (!sprite.getAtlasId().equals(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE))
            return;

        List<SpriteContents> copy = new ArrayList<>();
        copy.add(sprite.getContents());
        registerSprite(copy, Splatcraft.identifierOf("block/stage_barrier_fancy"));
        registerSprite(copy, Splatcraft.identifierOf("block/stage_void_fancy"));
        registerSprite(copy, Splatcraft.identifierOf("block/allowed_color_barrier_fancy"));
        registerSprite(copy, Splatcraft.identifierOf("block/denied_color_barrier_fancy"));
        registerSprite(copy, Splatcraft.identifierOf("block/permanent_ink_overlay"));
//      dont know what do these do but ok??? curse of i = 30338
//		int i = 1;
//		while (MinecraftClient.getInstance().getResourceManager().getResource(Splatcraft.identifierOf("textures/block/inked_block" + i + ".png")).isEmpty())
//			registerSprite(copy, Splatcraft.identifierOf("block/inked_block" + (i++)));
//		i = 1;
//		while (MinecraftClient.getInstance().getResourceManager().getResource(Splatcraft.identifierOf("textures/block/glitter" + i + ".png")).isEmpty())
//			registerSprite(copy, Splatcraft.identifierOf("block/glitter" + (i++)));
        registerSprite(copy, Splatcraft.identifierOf("block/inked_block"));
        registerSprite(copy, Splatcraft.identifierOf("block/glitter"));

//        event.getAtlas().sprites = List.copyOf(copy);
    }

    public static void registerSprite(List<SpriteContents> sprites, Identifier location)
    {
        final ResourceFinder TEXTURE_ID_CONVERTER = new ResourceFinder("textures", ".png");
        Identifier ohNo = TEXTURE_ID_CONVERTER.toResourceId(location);
        Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(ohNo);
        if (resource.isPresent())
        {
//            SpriteContents contents = SpriteLoader.fromAtlas(new SpriteAtlasTexture(ohNo, resource.get());
//            sprites.add(contents);
        }
    }

    public static void bindScreenContainers()
    {
        MenuRegistry.registerScreenFactory(SplatcraftTileEntities.inkVatContainer.get(), InkVatScreen::new);
        MenuRegistry.registerScreenFactory(SplatcraftTileEntities.weaponWorkbenchContainer.get(), WeaponWorkbenchScreen::new);
    }

    // todo: me thinks these are handled by the rendering but just in case i will put a todo here
    public static void initItemColors(ItemColors colors)
    {
        colors.register(new InkItemColor(), SplatcraftItems.inkColoredItems.toArray(new Item[0]));
    }

    public static void initBlockColors(BlockColors colors)
    {
        colors.registerColorProvider(new InkBlockColor(), SplatcraftBlocks.inkColoredBlocks.toArray(new Block[0]));
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
            if (i != 0)
                return -1;

            boolean isDefault = !ColorUtils.getInkColor(stack).isValid() && !ColorUtils.isColorLocked(stack);
            InkColor color = (stack.isIn(SplatcraftTags.Items.INK_BANDS) || !stack.isIn(SplatcraftTags.Items.MATCH_ITEMS)) && isDefault && EntityInfoCapability.hasCapability(ClientUtils.getClientPlayer())
                ? ColorUtils.getEntityColor(ClientUtils.getClientPlayer()) : ColorUtils.getInkColor(stack);

            if (ColorUtils.isInverted(stack))
                color = color.getInverted();

            color = ColorUtils.getColorLockedIfConfig(color);

            return color.getColor();
        }
    }

    public static class InkBlockColor implements BlockColorProvider
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

            return color.getColor();
        }
    }
}
