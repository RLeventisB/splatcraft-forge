package net.splatcraft.util;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3d;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.Splatcraft;
import net.splatcraft.blocks.IColoredBlock;
import net.splatcraft.blocks.InkedBlock;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.data.capabilities.worldink.ChunkInk;
import net.splatcraft.data.capabilities.worldink.ChunkInkCapability;
import net.splatcraft.entities.SpawnShieldEntity;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.mixin.accessors.EntityAccessor;
import net.splatcraft.registries.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InkBlockUtils
{
    public static BlockInkedResult playerInkBlock(@Nullable Player player, Level level, BlockPos pos, int color, Direction direction, InkType inkType, float damage)
    {
        BlockInkedResult inked = inkBlock(level, pos, color, direction, inkType, damage);

        if (player != null && inked == BlockInkedResult.SUCCESS)
        {
            player.awardStat(SplatcraftStats.BLOCKS_INKED);
        }

        return inked;
    }

    public static Direction getRandomInkedFace(Level level, BlockPos pos)
    {
        ChunkInk chunkInk = ChunkInkCapability.get(level, pos);
        ChunkInk.BlockEntry entry = chunkInk.getInk(RelativeBlockPos.fromAbsolute(pos));
        if (entry != null && entry.isInkedAny())
        {
            return Direction.from3DDataValue(Util.getRandom(entry.getActiveIndices(), level.random));
        }
        return null;
    }

    public static boolean clearInk(Level level, BlockPos pos, Direction direction, boolean removePermanent)
    {
        return clearInk(level, pos, direction.get3DDataValue(), removePermanent);
    }

    public static boolean clearInk(Level level, BlockPos pos, int index, boolean removePermanent)
    {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkInk worldInk = ChunkInkCapability.get(chunk);
        RelativeBlockPos relative = RelativeBlockPos.fromAbsolute(pos);

        if (worldInk.isInkedAny(relative))
        {
            if (worldInk.clearInk(relative, index, removePermanent))
            {
                if (!level.isClientSide())
                {
                    if (worldInk.isInkedAny(relative))
                        ChunkInkHandler.addInkToUpdate(level, pos);
                    else
                        ChunkInkHandler.addInkToRemove(level, pos);
                }
                return true;
            }
        }
        return false;
    }

    public static boolean clearBlock(Level level, BlockPos pos, boolean removePermanent)
    {
        ChunkInk worldInk = ChunkInkCapability.get(level, pos);
        RelativeBlockPos relative = RelativeBlockPos.fromAbsolute(pos);
        ChunkInk.BlockEntry entry = worldInk.getInk(relative);
        if (entry != null)
        {
            if (worldInk.clearBlock(relative, removePermanent))
            {
                if (!level.isClientSide())
                    ChunkInkHandler.addInkToRemove(level, pos);
                return true;
            }
        }
        return false;
    }

    public static BlockInkedResult inkBlock(Level level, BlockPos pos, int color, Direction direction, InkType inkType, float damage)
    {
        return inkBlock(level, pos, color, direction.get3DDataValue(), inkType, damage);
    }

    public static BlockInkedResult inkBlock(Level level, BlockPos pos, int color, int index, InkType inkType, float damage)
    {
        if (isUninkable(level, pos, Direction.from3DDataValue(index)))
            return BlockInkedResult.FAIL;

        for (SpawnShieldEntity shieldEntity : level.getEntitiesOfClass(SpawnShieldEntity.class, new AABB(pos)))
            if (!ColorUtils.colorEquals(level, pos, ColorUtils.getEntityColor(shieldEntity), color))
                return BlockInkedResult.FAIL;

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof IColoredBlock coloredBlock)
        {
            BlockInkedResult result = coloredBlock.inkBlock(level, pos, color, damage, inkType);
            if (result != BlockInkedResult.PASS)
                return result;
        }

        if (!SplatcraftGameRules.getLocalizedRule(level, pos, SplatcraftGameRules.INKABLE_GROUND))
            return BlockInkedResult.FAIL;

        LevelChunk chunk = level.getChunkAt(pos);
        ChunkInk worldInk = ChunkInkCapability.get(chunk);
        RelativeBlockPos relative = RelativeBlockPos.fromAbsolute(pos);
        ChunkInk.BlockEntry entry = worldInk.getInk(relative);

        boolean isInked = entry != null && entry.isInked(index);
        if (entry != null && entry.inmutable)
            return BlockInkedResult.IS_PERMANENT;

        boolean sameColor = isInked && entry.color(index) == color;

        if (sameColor && entry.type(index) == inkType)
            return BlockInkedResult.ALREADY_INKED;

        worldInk.ink(relative, index, color, inkType);

        if (SplatcraftGameRules.getLocalizedRule(level, pos.above(), SplatcraftGameRules.INK_DESTROYS_FOLIAGE) &&
            isBlockFoliage(level.getBlockState(pos.above())))
            level.destroyBlock(pos.above(), true);

        if (!level.isClientSide())
            ChunkInkHandler.addInkToUpdate(level, pos);

        return sameColor ? BlockInkedResult.ALREADY_INKED : BlockInkedResult.SUCCESS;
    }

    public static void forEachInkedBlockInBounds(Level level, final AABB bounds, InkedBlockConsumer action)
    {
        int chunkMinX = (int) Math.min(bounds.minX, bounds.maxX) >> 4;
        int chunkMinY = (int) Math.min(bounds.minY, bounds.maxY) >> 4;
        int chunkMinZ = (int) Math.min(bounds.minZ, bounds.maxZ) >> 4;
        int chunkmaxX = (int) Math.max(bounds.minX, bounds.maxX) >> 4;
        int chunkmaxY = (int) Math.max(bounds.minY, bounds.maxY) >> 4;
        int chunkmaxZ = (int) Math.max(bounds.minZ, bounds.maxZ) >> 4;
        for (int x = chunkMinX; x <= chunkmaxX; x++)
//            for (int y = chunkMinY; y <= chunkmaxY; y++)
            for (int z = chunkMinZ; z <= chunkmaxZ; z++)
            {
                ChunkPos chunkPos = new ChunkPos(x, z);
                LevelChunk chunk = level.getChunk(x, z);
                Set<Map.Entry<RelativeBlockPos, ChunkInk.BlockEntry>> uhhh = ChunkInkCapability.get(chunk).getInkInChunk().entrySet();
                List<Map.Entry<RelativeBlockPos, ChunkInk.BlockEntry>> entries;
                synchronized (uhhh)
                {
                    entries = new ArrayList<>(uhhh);
                }
                for (var ink : entries)
                {
                    BlockPos blockPos = ink.getKey().toAbsolute(chunkPos);
                    if (bounds.contains(blockPos.getX(), blockPos.getY(), blockPos.getZ()))
                    {
                        action.accept(blockPos, ink.getValue());
                    }
                }
            }
    }

    public static boolean isBlockFoliage(BlockState state)
    {
        return state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.REPLACEABLE);
    }

    public static BlockState getInkState(InkType inkType)
    {
        return (inkType == null ? InkType.NORMAL : inkType).block.defaultBlockState();
    }

    public static ChunkInk.BlockEntry getInkBlock(Level level, BlockPos pos)
    {
        return ChunkInkCapability.get(level, pos).getInk(RelativeBlockPos.fromAbsolute(pos));
    }

    public static ChunkInk.InkEntry getInkInFace(Level level, BlockPos pos, Direction direction)
    {
        return getInkBlock(level, pos).get(direction.get3DDataValue());
    }

    public static boolean isInked(Level level, BlockPos pos, Direction direction)
    {
        return isInked(level, pos, direction.get3DDataValue());
    }

    public static boolean isInked(Level level, BlockPos pos, int index)
    {
        return ChunkInkCapability.get(level, pos).isInked(RelativeBlockPos.fromAbsolute(pos), index);
    }

    public static boolean isInkedAny(Level level, BlockPos pos)
    {
        return ChunkInkCapability.get(level, pos).isInkedAny(RelativeBlockPos.fromAbsolute(pos));
    }

    public static boolean canInkFromFace(Level level, BlockPos pos, Direction face)
    {
        if (!(level.getBlockState(pos).getBlock() instanceof IColoredBlock) && isUninkable(level, pos, face))
            return false;

        return canInkPassthrough(level, pos.relative(face)) || !level.getBlockState(pos.relative(face)).is(SplatcraftTags.Blocks.BLOCKS_INK);
    }

    public static boolean isUninkable(Level level, BlockPos pos, Direction direction)
    {
        return isUninkable(level, pos, direction, false);
    }

    public static boolean isUninkable(Level level, BlockPos pos, Direction direction, boolean checkGamemode)
    {
        if (InkedBlock.isTouchingLiquid(level, pos, direction))
            return true;

        if (isBlockUninkable(level, pos))
            return true;

        if (!checkGamemode)
            return false;

        if (!SplatcraftGameRules.getLocalizedRule(level, pos, SplatcraftGameRules.BLOCK_DESTROY_INK))
            return false;
        BlockState blockState = level.getBlockState(pos);
        BlockState occludingBlockState = level.getBlockState(pos.relative(direction));
        VoxelShape blockCollision = blockState.getCollisionShape(level, pos).getFaceShape(direction);
        VoxelShape occludingCollision = occludingBlockState.getCollisionShape(level, pos.relative(direction)).getFaceShape(direction.getOpposite());
        return blockState.canOcclude() && !Shapes.joinIsNotEmpty(blockCollision, occludingCollision, BooleanOp.NOT_SAME);
    }

    public static boolean isBlockUninkable(Level level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);

        if (state.is(SplatcraftTags.Blocks.UNINKABLE_BLOCKS))
            return true;

        if (!state.is(SplatcraftTags.Blocks.RENDER_AS_CUBE) && state.getRenderShape() != RenderShape.MODEL)
            return true;

        return canInkPassthrough(level, pos);
    }

    public static boolean canInkPassthrough(Level level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);

        return state.getCollisionShape(level, pos).isEmpty() || level.getBlockState(pos).is(SplatcraftTags.Blocks.INK_PASSTHROUGH);
    }

    public static boolean canSquidHide(LivingEntity entity)
    {
        if (entity instanceof Player player && PlayerCooldown.hasPlayerCooldown(player) && PlayerCooldown.getPlayerCooldown(player) instanceof SuperJumpCommand.SuperJump)
        {
            return false;
        }
        PlayerInfo playerInfo = PlayerInfoCapability.get(entity);
        if (playerInfo == null)
            return false;

        return !entity.isSpectator() && (canSquidSwim(entity) || playerInfo.getClimbedDirection().isPresent() && playerInfo.getSquidSurgeCharge() < 20);
    }

    public static boolean canSquidSwim(LivingEntity entity)
    {
        boolean canSwim = false;

        BlockPos down = entity.getOnPos();
        Block standingBlock = entity.getWorld().getBlockState(down).getBlock();

        if (isInked(entity.getWorld(), down, Direction.UP))
            return ColorUtils.colorEquals(entity.getWorld(), down, ColorUtils.getEntityColor(entity), getInkBlock(entity.getWorld(), down).color(Direction.UP.get3DDataValue()));

        if (standingBlock instanceof IColoredBlock coloredBlock)
            canSwim = coloredBlock.canSwim();

        return canSwim && ColorUtils.colorEquals(entity, entity.getWorld().getBlockEntity(down));
    }

    public static BlockPos getBlockStandingOnPos(Entity entity)
    {
        return getBlockStandingOnPos(entity, 0.6);
    }

    public static BlockPos getBlockStandingOnPos(Entity entity, double maxDepth)
    {
        BlockPos result;
        for (double i = 0; i >= -maxDepth + 0.1; i -= 0.1)
        {
            result = CommonUtils.createBlockPos(entity.getX(), entity.getY() + i, entity.getZ());

            VoxelShape shape = entity.getWorld().getBlockState(result).getCollisionShape(entity.getWorld(), result, CollisionContext.of(entity));
            shape.collide(Direction.Axis.Y, entity.getBoundingBox(), 0.0);

            if (!shape.isEmpty() && shape.bounds().minY <= entity.getY() - result.getY())
                return result;
        }
        return CommonUtils.createBlockPos(entity.getX(), entity.getY() - maxDepth, entity.getZ());
    }

    public static boolean onEnemyInk(LivingEntity entity)
    {
        if (!entity.onGround())
            return false;
        BlockPos pos = entity.getOnPos();

        if (isInked(entity.getWorld(), pos, Direction.UP))
            return !canSquidSwim(entity);
        else if (entity.getWorld().getBlockState(pos).getBlock() instanceof IColoredBlock coloredBlock)
            return coloredBlock.canDamage() && ColorUtils.getInkColor(entity.getWorld(), pos) != -1 && !canSquidSwim(entity);
        else return false;
    }

    public static Direction canSquidClimb(LivingEntity entity, float strafeImpulse, float forwardImpulse, float yRot)
    {
        if (onEnemyInk(entity))
            return null;

        Vec3d inputVector = EntityAccessor.invokeGetInputVector(new Vec3d(Math.signum(strafeImpulse), 0, Math.signum(forwardImpulse)), 0.1f, yRot);
        BlockCollisions<BlockPos> collisions = new BlockCollisions<>(entity.getWorld(), entity, entity.getBoundingBox().expandTowards(inputVector), false, (bro, what) ->
            bro);

        return checkSquidCollisions(entity, collisions, inputVector);
    }

    @Nullable
    private static Direction checkSquidCollisions(LivingEntity entity, BlockCollisions<BlockPos> collisions, Vec3d inputVector)
    {
        while (collisions.hasNext())
        {
            BlockPos collidedBlock = collisions.next();
            Vec3d center = collidedBlock.getCenter();
            Direction direction;
            if (Math.abs(center.x - entity.getX()) > Math.abs(center.z - entity.getZ()))
            {
                direction = center.x > entity.getX() ? Direction.WEST : Direction.EAST;
            }
            else
            {
                direction = center.z > entity.getZ() ? Direction.NORTH : Direction.SOUTH;
            }

            if (isInked(entity.getWorld(), collidedBlock, direction) &&
                ColorUtils.colorEquals(entity.getWorld(), collidedBlock,
                    ColorUtils.getEntityColor(entity),
                    getInkBlock(entity.getWorld(), collidedBlock).color(direction.get3DDataValue())))
            {
                if (inputVector == null || Vec3d.atLowerCornerOf(direction.getNormal()).cross(inputVector).y() != 0)
                    return direction;
            }
        }
        return null;
    }

    public static Direction getSquidClimbingDirection(LivingEntity entity, float strafeImpulse, float forwardImpulse, Direction face)
    {
        Direction blockFaceToCheck = face.getOpposite();
        AABB baseBoundingBox = SplatcraftEntities.INK_SQUID.get().getDimensions().makeBoundingBox(entity.position());
        Vec3d inputVector = EntityAccessor.invokeGetInputVector(new Vec3d(-Math.signum(strafeImpulse), Math.signum(forwardImpulse), 0), 0.1f, face.toYRot());
        BlockCollisions<BlockPos> collisions = new BlockCollisions<>(entity.getWorld(), entity, baseBoundingBox.expandTowards(inputVector), false, (bro, what) ->
            bro);

        Direction otherWallClosion = checkSquidCollisions(entity, collisions, inputVector);
        if (otherWallClosion != null)
            return otherWallClosion;

        inputVector = Vec3d.atLowerCornerOf(blockFaceToCheck.getNormal()).scale(0.01);
        AABB aabb = baseBoundingBox.expandTowards(inputVector);
        collisions = new BlockCollisions<>(entity.getWorld(), entity, aabb, false, (bro, what) ->
            bro);

        return checkSquidCollisions(entity, collisions, null);
    }

    public static InkBlockUtils.InkType getInkType(LivingEntity entity)
    {
        return PlayerInfoCapability.hasCapability(entity) ? PlayerInfoCapability.get(entity).getInkType() : InkType.NORMAL;
    }

    public static InkType getInkTypeFromStack(ItemStack stack)
    {
        if (!stack.isEmpty())
            for (InkType t : InkType.values.values())
                if (t.getRepItem().equals(stack.getItem()))
                    return t;

        return InkType.NORMAL;
    }

    public static boolean hasInkType(ItemStack stack)
    {
        if (!stack.isEmpty())
            for (InkType t : InkType.values.values())
                if (t.getRepItem().equals(stack.getItem()))
                    return true;
        return false;
    }

    public interface InkedBlockConsumer
    {
        void accept(BlockPos pos, ChunkInk.BlockEntry ink);
    }

    public static class InkType implements Comparable<InkType>
    {
        public static final HashMap<ResourceLocation, InkType> values = new HashMap<>();
        public static final InkType NORMAL = new InkType(0, new ResourceLocation(Splatcraft.MODID, "normal"), SplatcraftBlocks.inkedBlock.get());
        public static final InkType GLOWING = new InkType(1, new ResourceLocation(Splatcraft.MODID, "glowing"), SplatcraftItems.splatfestBand.get(), SplatcraftBlocks.glowingInkedBlock.get());
        public static final InkType CLEAR = new InkType(2, new ResourceLocation(Splatcraft.MODID, "clear"), SplatcraftItems.clearBand.get(), SplatcraftBlocks.clearInkedBlock.get());
        private final ResourceLocation name;
        private final Item repItem;
        private final InkedBlock block;
        private final byte id;

        public InkType(int id, ResourceLocation name, Item repItem, InkedBlock inkedBlock)
        {
            values.put(name, this);
            this.id = (byte) id;
            this.name = name;
            this.repItem = repItem;
            this.block = inkedBlock;
        }

        public InkType(int id, ResourceLocation name, InkedBlock inkedBlock)
        {
            this(id, name, Items.AIR, inkedBlock);
        }

        public static InkType fromId(int id)
        {
            return switch (id)
            {
                case 0 -> NORMAL;
                case 1 -> GLOWING;
                case 2 -> CLEAR;
                default -> throw new IllegalStateException("Unexpected value: " + id);
            };
        }

        @Override
        public int compareTo(InkType o)
        {
            return getName().compareTo(o.getName());
        }

        public ResourceLocation getName()
        {
            return name;
        }

        public Item getRepItem()
        {
            return repItem;
        }

        @Override
        public String toString()
        {
            return name.toString();
        }

        public String getSerializedName()
        {
            return getName().toString();
        }

        public byte getId()
        {
            return id;
        }
    }
}