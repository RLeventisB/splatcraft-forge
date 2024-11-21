package net.splatcraft.forge.client.handlers;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterTextureAtlasSpriteLoadersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.textures.ForgeTextureMetadata;
import net.minecraftforge.client.textures.ITextureAtlasSpriteLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.SplatcraftConfig;
import net.splatcraft.forge.client.gui.InkVatScreen;
import net.splatcraft.forge.client.gui.WeaponWorkbenchScreen;
import net.splatcraft.forge.data.SplatcraftTags;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.registries.SplatcraftBlocks;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.registries.SplatcraftTileEntities;
import net.splatcraft.forge.util.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("deprecation")
@Mod.EventBusSubscriber(modid = Splatcraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetupHandler
{
    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Post event)
    {
        if (!event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS))
            return;

        List<SpriteContents> copy = new ArrayList<>(event.getAtlas().sprites);
        registerSprite(copy, new ResourceLocation(Splatcraft.MODID, "block/stage_barrier_fancy"));
        registerSprite(copy, new ResourceLocation(Splatcraft.MODID, "block/stage_void_fancy"));
        registerSprite(copy, new ResourceLocation(Splatcraft.MODID, "block/allowed_color_barrier_fancy"));
        registerSprite(copy, new ResourceLocation(Splatcraft.MODID, "block/denied_color_barrier_fancy"));
        registerSprite(copy, new ResourceLocation(Splatcraft.MODID, "block/permanent_ink_overlay"));
//      dont know what do these do but ok??? curse of i = 30338
//		int i = 1;
//		while (Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation(Splatcraft.MODID, "textures/block/inked_block" + i + ".png")).isEmpty())
//			registerSprite(copy, new ResourceLocation(Splatcraft.MODID, "block/inked_block" + (i++)));
//		i = 1;
//		while (Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation(Splatcraft.MODID, "textures/block/glitter" + i + ".png")).isEmpty())
//			registerSprite(copy, new ResourceLocation(Splatcraft.MODID, "block/glitter" + (i++)));
        registerSprite(copy, new ResourceLocation(Splatcraft.MODID, "block/inked_block"));
        registerSprite(copy, new ResourceLocation(Splatcraft.MODID, "block/glitter"));

        event.getAtlas().sprites = List.copyOf(copy);
    }

    public static void registerSprite(List<SpriteContents> sprites, ResourceLocation location)
    {
        final FileToIdConverter TEXTURE_ID_CONVERTER = new FileToIdConverter("textures", ".png");
        ResourceLocation ohNo = TEXTURE_ID_CONVERTER.idToFile(location);
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(ohNo);
        if (resource.isPresent())
        {
            SpriteContents contents = SpriteLoader.loadSprite(ohNo, resource.get());
            sprites.add(contents);
        }
    }

    public static void bindScreenContainers()
    {
        MenuScreens.register(SplatcraftTileEntities.inkVatContainer.get(), InkVatScreen::new);
        MenuScreens.register(SplatcraftTileEntities.weaponWorkbenchContainer.get(), WeaponWorkbenchScreen::new);
    }

    @SubscribeEvent
    public static void initItemColors(RegisterColorHandlersEvent.Item event)
    {
        ItemColors colors = event.getItemColors();

        SplatcraftItems.inkColoredItems.add(SplatcraftItems.splatfestBand.get());
        SplatcraftItems.inkColoredItems.add(SplatcraftItems.clearBand.get());

        colors.register(new InkItemColor(), SplatcraftItems.inkColoredItems.toArray(new Item[0]));
    }

    @SubscribeEvent
    public static void initBlockColors(RegisterColorHandlersEvent.Block event)
    {
        BlockColors colors = event.getBlockColors();

        colors.register(new InkBlockColor(), SplatcraftBlocks.inkColoredBlocks.toArray(new Block[0]));
    }

    protected static class InkItemColor implements ItemColor
    {
        @Override
        public int getColor(@NotNull ItemStack stack, int i)
        {
            if (i != 0)
                return -1;

            boolean isDefault = ColorUtils.getInkColor(stack) == -1 && !ColorUtils.isColorLocked(stack);
            int color = (stack.is(SplatcraftTags.Items.INK_BANDS) || !stack.is(SplatcraftTags.Items.MATCH_ITEMS)) && isDefault && PlayerInfoCapability.hasCapability(Minecraft.getInstance().player)
                ? ColorUtils.getEntityColor(Minecraft.getInstance().player) : ColorUtils.getInkColor(stack);

            if (SplatcraftConfig.Client.colorLock.get())
                color = ColorUtils.getLockedColor(color);
            else if (ColorUtils.isInverted(stack))
                color = 0xFFFFFF - color;

            return color;
        }
    }

    public static class InkBlockColor implements BlockColor
    {
        @Override
        public int getColor(@NotNull BlockState blockState, @Nullable BlockAndTintGetter iBlockDisplayReader, @Nullable BlockPos blockPos, int i)
        {
            if (i != 0 || iBlockDisplayReader == null || blockPos == null)
                return -1;

            BlockEntity te = iBlockDisplayReader.getBlockEntity(blockPos);

            if (te == null)
                return -1;

            int color = ColorUtils.getInkColor(te);
            if (SplatcraftConfig.Client.colorLock.get())
                color = ColorUtils.getLockedColor(color);
            else if (ColorUtils.isInverted(te.getLevel(), blockPos))
                color = 0xFFFFFF - color;

            if (color == -1)
                return 0xFFFFFF;

            return color;
        }
    }

    // https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/test/java/net/minecraftforge/debug/client/CustomTASTest.java
    @SubscribeEvent
    public static void registerTextureAtlasSpriteLoaders(RegisterTextureAtlasSpriteLoadersEvent event)
    {
        event.register("weapon_loader", new WeaponLoader()); // so the gal deco texture has this!!! idk why but ok here it is
    }

    public static class WeaponLoader implements ITextureAtlasSpriteLoader
    {
        @Override
        public SpriteContents loadContents(ResourceLocation name, Resource resource, FrameSize frameSize, NativeImage image, AnimationMetadataSection meta, ForgeTextureMetadata forgeMeta)
        {
            return new WeaponSpriteContents(name, frameSize, image, meta, forgeMeta);
        }

        @Override
        public @NotNull TextureAtlasSprite makeSprite(ResourceLocation atlasName, SpriteContents contents, int atlasWidth, int atlasHeight, int spriteX, int spriteY, int mipmapLevel)
        {
            final class TASprite extends TextureAtlasSprite
            {
                public TASprite(ResourceLocation atlasName, SpriteContents contents, int width, int height, int x, int y)
                {
                    super(atlasName, contents, width, height, x, y);
                }
            }

            return new TASprite(atlasName, contents, atlasWidth, atlasHeight, spriteX, spriteY);
        }

        public static class WeaponSpriteContents extends SpriteContents
        {
            public WeaponSpriteContents(ResourceLocation name, FrameSize size, NativeImage image, AnimationMetadataSection meta, @Nullable ForgeTextureMetadata forgeMeta)
            {
                super(name, size, image, meta, forgeMeta);
            }
        }
    }
}
