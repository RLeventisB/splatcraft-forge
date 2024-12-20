package net.splatcraft.client.particles;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class InkTerrainParticle extends SpriteBillboardParticle
{
    private final BlockPos pos;
    private final float uo;
    private final float vo;

    public InkTerrainParticle(ClientWorld p_108282_, double p_108283_, double p_108284_, double p_108285_, double p_108286_, double p_108287_, double p_108288_, float r, float g, float b)
    {
        this(p_108282_, p_108283_, p_108284_, p_108285_, p_108286_, p_108287_, p_108288_, CommonUtils.createBlockPos(p_108283_, p_108284_, p_108285_), r, g, b);
    }

    public InkTerrainParticle(ClientWorld level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, BlockPos blockPos, float r, float g, float b)
    {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        pos = blockPos;
        setSprite(MinecraftClient.getInstance().getBlockRenderManager().getModels().getModelParticleSprite(SplatcraftBlocks.inkedBlock.get().getDefaultState()));
        gravityStrength = 1.0F;
        red = 0.6F * r;
        green = 0.6F * g;
        blue = 0.6F * b;

        scale /= 2.0F;
        uo = random.nextFloat() * 3.0F;
        vo = random.nextFloat() * 3.0F;
    }

    public @NotNull ParticleTextureSheet getType()
    {
        return ParticleTextureSheet.TERRAIN_SHEET;
    }

    /*protected float getMinU()
    {
        return this.sprite.getFrameU((this.uo + 1.0F) / 4.0F * 16.0F);
    }
    protected float getMaxU()
    {
        return this.sprite.getU(this.uo / 4.0F * 16.0F);
    }
    protected float getMinV()
    {
        return this.sprite.getV(this.vo / 4.0F * 16.0F);
    }
    protected float getMaxV()
    {
        return this.sprite.getV((this.vo + 1.0F) / 4.0F * 16.0F);
    }
    public int getBrightness(float p_108291_)
    {
        int i = super.getBrightness(p_108291_);
        return i == 0 && this.world.isChunkLoaded(this.pos) ? WorldRenderer.getLightmapCoordinates(this.world, this.pos) : i;
    }
    @Environment(EnvType.CLIENT)
    public static class Provider implements ParticleProvider<BlockParticleOption>
    {
        public Particle createParticle(BlockParticleOption p_108304_, @NotNull ClientWorld p_108305_, double p_108306_, double p_108307_, double p_108308_, double p_108309_, double p_108310_, double p_108311_)
        {
            BlockState blockstate = p_108304_.getState();
            return !blockstate.isAir() && !blockstate.is(Blocks.MOVING_PISTON) ? (new net.minecraft.client.particle.TerrainParticle(p_108305_, p_108306_, p_108307_, p_108308_, p_108309_, p_108310_, p_108311_, blockstate)).updateSprite(blockstate, p_108304_.getPos()) : null;
        }
    }
    public Particle updateSprite(BlockState state, BlockPos pos)
    { //FORGE: we cannot assume that the x y z of the particles match the block pos of the block.
        if (pos != null) // There are cases where we are not able to obtain the correct source pos, and need to fallback to the non-model data version
            this.setSprite(MinecraftClient.getInstance().getBlockRenderManager().getModels().getModelParticleSprite(state));
        return this;
    }*/
    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<InkTerrainParticleData>
    {
        public Factory(SpriteProvider sprite)
        {
        }

        @Nullable
        @Override
        public Particle createParticle(InkTerrainParticleData typeIn, @NotNull ClientWorld levelIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
        {
            return new InkTerrainParticle(levelIn, x, y, z, xSpeed, ySpeed, zSpeed, typeIn.red, typeIn.green, typeIn.blue);
        }
    }
}

