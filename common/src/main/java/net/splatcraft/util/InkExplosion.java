package net.splatcraft.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.blocks.ColoredBarrierBlock;
import net.splatcraft.entities.IColoredEntity;
import net.splatcraft.entities.InkDropEntity;
import net.splatcraft.entities.SpawnShieldEntity;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendPlayerHitPacket;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.structs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InkExplosion
{
	private final AttackId attackId;
	@Nullable
	private final Entity exploder;
	private final float paintRadius;
	private final List<BlockFace> affectedBlockFaces = Lists.newArrayList();
	private final Vec3 position;
	private final InkBlockUtils.InkType inkType;
	private final RangedValueCollection dmgCalculator;
	private final ItemStack weapon;
	private final InkColor color;
	public InkExplosion(@Nullable Entity source, Vec3 pos, RangedValueCollection damageCalculator, float paintRadius, InkBlockUtils.InkType inkType, ItemStack weapon, AttackId attackId)
	{
		exploder = source;
		this.paintRadius = paintRadius;
		this.attackId = attackId;
		position = pos;
		
		this.inkType = inkType;
		dmgCalculator = damageCalculator;
		this.weapon = weapon;
		color = ColorUtils.getEntityColor(exploder);
	}
	public static Vec3 adjustPosition(final Vec3 pos, Direction normal, Entity entity)
	{
		final float modifier = entity == null ? 0.01f : switch (normal.getAxis())
		{
			case X, Z -> entity.getBbWidth() / 2;
			case Y -> entity.getBbHeight() / 2;
		};
		return pos.relative(normal, modifier);
	}
	public static void createInkExplosion(Entity source, Vec3 pos, float paintRadius, InkBlockUtils.InkType type, ItemStack weapon)
	{
		createInkExplosion(source, pos, paintRadius, null, type, weapon, AttackId.NONE, null);
	}
	public static void createInkExplosion(Entity source, Vec3 pos, float paintRadius, RangedValueCollection damageManager, InkBlockUtils.InkType type, ItemStack weapon)
	{
		createInkExplosion(source, pos, paintRadius, damageManager, type, weapon, AttackId.NONE, null);
	}
	public static void createInkExplosion(Entity source, Vec3 pos, float paintRadius, RangedValueCollection damageManager, InkBlockUtils.InkType type, ItemStack weapon, AttackId attackId)
	{
		createInkExplosion(source, pos, paintRadius, damageManager, type, weapon, attackId, null);
	}
	public static void createInkExplosion(Entity source, Vec3 pos, float paintRadius, RangedValueCollection damageManager, InkBlockUtils.InkType type, ItemStack weapon, AttackId attackId, Consumer<Entity> onHit)
	{
		if (source == null || source.level().isClientSide)
			return;
		
		InkExplosion inksplosion = new InkExplosion(source, pos, damageManager, paintRadius, type, weapon, attackId);
		
		inksplosion.doExplosionA(onHit);
		inksplosion.doExplosionCosmetics(false);
	}
	public static void createInkExplosionWithSound(Entity source, Vec3 pos, float paintRadius, RangedValueCollection damageManager, InkBlockUtils.InkType type, ItemStack weapon, AttackId attackId, @Nullable Vector3f soundPos)
	{
		if (source instanceof ServerPlayer serverPlayer)
		{
			ImmutableList.Builder<Vector3f> builder = ImmutableList.builder();
			
			createInkExplosion(source, pos, paintRadius, damageManager, type, weapon, attackId, e ->
			{
				builder.add(e.getBoundingBox().getCenter().toVector3f());
			});
			
			ImmutableList<Vector3f> positions = builder.build();
			if (positions.isEmpty())
				return;
			
			if (soundPos == null)
				soundPos = positions.getFirst();
			
			SplatcraftPacketHandler.sendToPlayer(
				new SendPlayerHitPacket(positions, soundPos, SplatcraftSounds.shotHit, 0.7f), serverPlayer);
			
			return;
		}
		
		createInkExplosion(source, pos, paintRadius, damageManager, type, weapon, attackId, null);
	}
	public static void doSplashes(@Nullable Entity owner, Vec3 center, SubWeaponSettings.SplashAroundDataRecord splashData, InkColor color, InkBlockUtils.InkType inkType, ItemStack weapon)
	{
		if (owner == null)
			return;
		
		Level world = owner.level();
		RandomSource random = world.getRandom();
		// this is not because i feel this is nice in terms of syntax this is because im a dumbass microoptimizer and i do this in c# too
		Function<Integer, Float> yawGetter = splashData.distributeEvenly() ?
			(count) -> (float) count / splashData.splashCount() :
			(count) -> world.getRandom().nextFloat();
		for (int i = 0; i < splashData.splashCount(); i++)
		{
			createDrop(
				world, owner, center,
				yawGetter.apply(i) * Mth.TWO_PI, -splashData.splashPitchRange().getValue(random.nextFloat()) * Mth.DEG_TO_RAD,
				splashData.splashVelocityRange().getValue(random.nextFloat()),
				splashData.splashPaintRadius(), color, inkType, weapon
			);
		}
	}
	static void createDrop(Level world, Entity owner, Vec3 center, float yaw, float pitch, float speed, float splashSize, InkColor color, InkBlockUtils.InkType type, ItemStack weapon)
	{
		InkDropEntity drop = new InkDropEntity(owner.level(), center, owner, color, type, splashSize, weapon);
		float f = -Mth.sin(yaw) * Mth.cos(pitch);
		float g = -Mth.sin(pitch);
		float h = Mth.cos(yaw) * Mth.cos(pitch);
		drop.shoot(f, g, h, speed, 0);
		
		world.addFreshEntity(drop);
	}
	/**
	 * Does the first part of the explosion (destroy blocks)
	 */
	public void doExplosionA(@Nullable Consumer<Entity> onHit)
	{
		List<BlockFace> set = new ArrayList<>();
		ServerLevel world = (ServerLevel) exploder.level();
		getBlocksInSphereWithNoise(set, world);
		
		affectedBlockFaces.addAll(set);
		if (RangedValueCollection.isInsignificant(dmgCalculator))
			return;
		float radiusSquared = dmgCalculator.getMaxKey() * dmgCalculator.getMaxKey();
		int k1 = Mth.floor(position.x - dmgCalculator.getMaxKey() - 1F);
		int l1 = Mth.floor(position.x + dmgCalculator.getMaxKey() + 1F);
		int i2 = Mth.floor(position.y - dmgCalculator.getMaxKey() - 1F);
		int i1 = Mth.floor(position.y + dmgCalculator.getMaxKey() + 1F);
		int j2 = Mth.floor(position.z - dmgCalculator.getMaxKey() - 1F);
		int j1 = Mth.floor(position.z + dmgCalculator.getMaxKey() + 1F);
		AABB box = new AABB(k1, i2, j2, l1, i1, j1);
		List<Entity> possibleTargets = new ArrayList<>();
		List<SpawnShieldEntity> spawnShields = new ArrayList<>();
		world.getEntities().get(box, (v) ->
		{
			if (!v.isSpectator())
			{
				if (v instanceof LivingEntity || v instanceof IColoredEntity)
				{
					possibleTargets.add(v);
				}
				else if (v instanceof SpawnShieldEntity spawnShield)
				{
					spawnShields.add(spawnShield);
				}
			}
		});
		
		for (Entity entity : possibleTargets)
		{
			AABB boundingBox = entity.getBoundingBox();
			Vec3 closestPos = CommonUtils.limitTo(boundingBox, position);
			
			float distance = (float) position.distanceToSqr(closestPos);
			if (distance > radiusSquared)
				continue;
			
			InkColor targetColor = ColorUtils.getEntityColor(entity);
			if (!targetColor.isValid() || (color != targetColor && targetColor.isValid()))
			{
				Vec3 boundingBoxCenter = boundingBox.getCenter();
				
				// find shields that can protect entities of same color
				boolean spawnShieldBlocked = false;
				for (SpawnShieldEntity shieldEntity : spawnShields)
				{
					AABB shieldBb = shieldEntity.getBoundingBox();
					// if only using shieldBb.contains(boundingBox) some accuracy might be lost!!!
					// since an entity can be damaged even if they're obstructed (but not inside) the shield thingy
					if (shieldEntity.getColor() == ColorUtils.getEntityColor(entity) && shieldBb.contains(position) || shieldBb.clip(position, boundingBoxCenter).isPresent())
					{
						spawnShieldBlocked = true;
						break;
					}
				}
				if (spawnShieldBlocked)
					continue;
				
				float seenPercent = Explosion.getSeenPercent(position, entity);
				if (InkDamageUtils.doSplatDamage(entity, dmgCalculator.getValue(Mth.sqrt(distance)) * seenPercent, exploder, weapon, attackId) && onHit != null)
					onHit.accept(entity);
			}
			
			DyeColor dyeColor = color.getDyeColor();
			
			if (entity instanceof Sheep sheep)
			{
				sheep.setColor(dyeColor);
			}
		}
	}
	private void getBlocksInSphereWithNoise(List<BlockFace> set, ServerLevel world)
	{
		// explosion is inside a block, everything is occluded
		if (!world.noCollision(new AABB(position, position)))
			return;
		
		final float noiseRange = 0.2f;
		int cubeSizeHalf = ((int) Math.ceil(paintRadius + noiseRange) >> 1) + 1;
		FaceMap map = new FaceMap(position, world, paintRadius, noiseRange, world.random);
		
		for (int x = -cubeSizeHalf; x <= cubeSizeHalf; x++)
			for (int y = -cubeSizeHalf; y <= cubeSizeHalf; y++)
				for (int z = -cubeSizeHalf; z <= cubeSizeHalf; z++)
				{
					BlockPos pos = BlockPos.containing(position.x + x, position.y + y, position.z + z);
					BlockState blockState = world.getBlockState(pos);
					
					if (!canPassIfBarrier(color, world, pos, blockState))
						continue;
					
					VoxelShape shape = blockState.getCollisionShape(world, pos);
					Vec3 relativePos = position.subtract(pos.getCenter());
					
					double dist = relativePos.length();
					if (dist <= paintRadius + Mth.SQRT_OF_TWO)
					{
						map.register(pos, blockState, shape);
					}
				}
		
		map.processAndCull();
		set.addAll(map.faces.stream().map(v -> new BlockFace(map.blockPositions.get(v.blockPosIndex), v.faceNormalDir)).collect(Collectors.toSet()));
	}
	private boolean canPassIfBarrier(InkColor color, LevelReader worldView, BlockPos pos, BlockState state)
	{
		if (state.getBlock() instanceof ColoredBarrierBlock barrier)
		{
			// woah weird syntax but this is just extracted ColoredBarrierBlock.canAllowThrough
			return (barrier.getColor(worldView, pos) == color) == !barrier.blocksColor;
		}
		return true;
	}
	/**
	 * Does the second part of the explosion (sound, particles, drop spawn)
	 */
	public void doExplosionCosmetics(boolean spawnParticles)
	{
		Vec3 explosionPos = new Vec3(position.x + 0.5f, position.y + 0.5f, position.z + 0.5f);
		
		Level world = exploder.level();
		
		if (spawnParticles)
		{
			if (paintRadius < 2.0F)
			{
				world.addParticle(ParticleTypes.EXPLOSION, position.x, position.y, position.z, 1.0D, 0.0D, 0.0D);
			}
			else
			{
				world.addParticle(ParticleTypes.EXPLOSION_EMITTER, position.x, position.y, position.z, 1.0D, 0.0D, 0.0D);
			}
		}
		
		int pointsToAward = 0;
		for (BlockFace blockFace : affectedBlockFaces)
		{
			BlockState blockstate = world.getBlockState(blockFace.pos());
			if (!blockstate.isAir())
			{
				float dist = (float) Math.sqrt(blockFace.pos().distToCenterSqr(explosionPos.x, explosionPos.y, explosionPos.z));
				BlockInkedResult result = InkBlockUtils.inkBlock(exploder, world, blockFace.pos(), color, blockFace.face(), inkType, dmgCalculator == null ? 0 : dmgCalculator.getValue(dist));
				if (result == BlockInkedResult.SUCCESS && blockFace.face().equals(Direction.UP))
				{
					pointsToAward++;
				}
			}
		}
		if (exploder instanceof LivingEntity living)
			InkBlockUtils.awardTurfPoints(living, weapon, pointsToAward);
	}
	public Vec3 getPosition()
	{
		return position;
	}
	public static final class FaceData
	{
		// coords are stored as followed:
		// if the axis is x, x, minY, minZ, maxY, maxZ
		// if the axis is y, y, minX, minZ, maxX, maxZ
		// if the axis is z, z, minX, minY, maxX, maxY
		public final int blockPosIndex;
		public final Direction faceNormalDir;
		public final PointData[] corners;
		public final PointData centroid;
		public FaceData(int blockPosIndex, Direction faceNormalDir, double planeCoord, double minCoord1, double maxCoord1, double minCoord2, double maxCoord2)
		{
			this.blockPosIndex = blockPosIndex;
			this.faceNormalDir = faceNormalDir;
			
			// corners should be in order:
			// bottom left
			// bottom right
			// top right
			// top left
			corners = PointData.createFromCorners(switch (faceNormalDir.getAxis())
			{
				case X -> new Vector3d[]
					{
						new Vector3d(planeCoord, minCoord1, minCoord2),
						new Vector3d(planeCoord, minCoord1, maxCoord2),
						new Vector3d(planeCoord, maxCoord1, maxCoord2),
						new Vector3d(planeCoord, maxCoord1, minCoord2)
					};
				case Y -> new Vector3d[]
					{
						new Vector3d(minCoord1, planeCoord, minCoord2),
						new Vector3d(maxCoord1, planeCoord, minCoord2),
						new Vector3d(maxCoord1, planeCoord, maxCoord2),
						new Vector3d(minCoord1, planeCoord, maxCoord2)
					};
				case Z -> new Vector3d[]
					{
						new Vector3d(minCoord1, minCoord2, planeCoord),
						new Vector3d(maxCoord1, minCoord2, planeCoord),
						new Vector3d(maxCoord1, maxCoord2, planeCoord),
						new Vector3d(minCoord1, maxCoord2, planeCoord)
					};
			});
			
			centroid = new PointData(switch (faceNormalDir.getAxis())
			{
				case X -> new Vector3d(planeCoord, (minCoord1 + maxCoord1) / 2, (minCoord2 + maxCoord2) / 2);
				case Y -> new Vector3d((minCoord1 + maxCoord1) / 2, planeCoord, (minCoord2 + maxCoord2) / 2);
				case Z -> new Vector3d((minCoord1 + maxCoord1) / 2, (minCoord2 + maxCoord2) / 2, planeCoord);
			});
		}
		public static List<FaceData> getFacesFromBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int blockPosIndex, Predicate<FaceData> facePredicate)
		{
			List<FaceData> list = new ArrayList<>(3);
			
			// negative X
			if (minX > 0)
				addToListIfValid(list, facePredicate, new FaceData(blockPosIndex, Direction.WEST,
					minX, minY, maxY, maxZ, minZ
				));
			
			// positive x
			if (maxX < 0)
				addToListIfValid(list, facePredicate, new FaceData(blockPosIndex, Direction.EAST,
					maxX, minY, maxY, minZ, maxZ
				));
			
			// negative y
			if (minY > 0)
				addToListIfValid(list, facePredicate, new FaceData(blockPosIndex, Direction.DOWN,
					minY, maxX, minX, minZ, maxZ
				));
			
			// positive y
			if (maxY < 0)
				addToListIfValid(list, facePredicate, new FaceData(blockPosIndex, Direction.UP,
					maxY, minX, maxX, minZ, maxZ
				));
			
			// negative z
			if (minZ > 0)
				addToListIfValid(list, facePredicate, new FaceData(blockPosIndex, Direction.NORTH,
					minZ, minX, maxX, minY, maxY
				));
			
			// positive z
			if (maxZ < 0)
				addToListIfValid(list, facePredicate, new FaceData(blockPosIndex, Direction.SOUTH,
					maxZ, maxX, minX, minY, maxY
				));
			
			return list;
		}
		private static void addToListIfValid(List<FaceData> list, Predicate<FaceData> facePredicate, FaceData faceData)
		{
			if (facePredicate.test(faceData))
				list.add(faceData);
		}
		public PointData getCentroid()
		{
			return centroid;
		}
		@Override
		public int hashCode()
		{
			return Objects.hash(blockPosIndex, faceNormalDir, corners[0], corners[1], corners[2], corners[3]);
		}
		@Override
		public String toString()
		{
			return "FaceData[" +
				"blockPosIndex=" + blockPosIndex + ", " +
				"faceNormalDir=" + faceNormalDir + ", " +
				"centroid=" + centroid + ", " +
				"bottom left=" + corners[0] + ", " +
				"bottom right=" + corners[1] + ", " +
				"top right=" + corners[2] + ", " +
				"top left=" + corners[3] + "]";
		}
		@Override
		public boolean equals(Object o)
		{
			if (this == o) return true;
			if (!(o instanceof FaceData faceData)) return false;
			return blockPosIndex == faceData.blockPosIndex && faceNormalDir == faceData.faceNormalDir && Objects.equals(centroid, faceData.centroid) && Objects.deepEquals(corners, faceData.corners);
		}
		public static final class PointData implements Comparable<PointData>
		{
			public final Vector3d point;
			private boolean obstructed;
			public PointData(Vector3d point)
			{
				this.point = point;
			}
			public static PointData[] createFromCorners(Vector3d[] points)
			{
				return Arrays.stream(points).map(PointData::new).toArray(PointData[]::new);
			}
			@Override
			public boolean equals(Object o)
			{
				if (this == o) return true;
				if (!(o instanceof PointData pointData)) return false;
				return obstructed == pointData.obstructed && Objects.equals(point, pointData.point);
			}
			@Override
			public int hashCode()
			{
				return Objects.hash(point, obstructed);
			}
			@Override
			public String toString()
			{
				return "PointData{" +
					"obstructed=" + obstructed +
					", point=" + point +
					'}';
			}
			public boolean isObstructed()
			{
				return obstructed;
			}
			public void markObstructed()
			{
				obstructed = true;
			}
			@Override
			public int compareTo(@NotNull InkExplosion.FaceData.PointData pointData)
			{
				return Double.compare(point.lengthSquared(), pointData.point.lengthSquared());
			}
		}
	}
	public static class FaceMap
	{
		private final float paintRange;
		private final float noiseRange;
		private final RandomSource random;
		public Vector3d worldOrigin;
		public Level world;
		public ArrayList<FaceData> faces = new ArrayList<>();
		public List<BlockPos> blockPositions = new ArrayList<>();
		public FaceMap(Vec3 point, Level world, float paintRange, float noiseRange, RandomSource random)
		{
			worldOrigin = new Vector3d(point.x, point.y, point.z);
			this.world = world;
			this.paintRange = paintRange;
			this.noiseRange = noiseRange;
			this.random = random;
		}
		public void register(BlockPos pos, BlockState state, VoxelShape shape)
		{
			shape.forAllBoxes((xmin, ymin, zmin, xmax, ymax, zmax) ->
			{
				double minX = -worldOrigin.x + xmin + pos.getX();
				double minY = -worldOrigin.y + ymin + pos.getY();
				double minZ = -worldOrigin.z + zmin + pos.getZ();
				double maxX = -worldOrigin.x + xmax + pos.getX();
				double maxY = -worldOrigin.y + ymax + pos.getY();
				double maxZ = -worldOrigin.z + zmax + pos.getZ();
				
				List<FaceData> facesList = FaceData.getFacesFromBox(minX, minY, minZ, maxX, maxY, maxZ, blockPositions.size(), (FaceData face) -> checkCloseEnoughAndVisible(face, state, pos));
				if (!facesList.isEmpty())
				{
					blockPositions.add(pos);
					faces.addAll(facesList);
				}
			});
		}
		public boolean checkCloseEnoughAndVisible(FaceData face, BlockState blockState, BlockPos pos)
		{
			// is this faster than doing a² + 2ab + b²?????? i should benchmark that but it has been 4 days since i am doing this thing so im tired
			float noiseValue = (random.nextFloat() * 2f - 1) * noiseRange;
			if (face.centroid.point.length() < paintRange + noiseValue)
			{
				// if close enough check if there isn't another block fully occluding the face
				BlockPos forwardPos = pos.relative(face.faceNormalDir);
				
				BlockState occludingBlockState = world.getBlockState(forwardPos);
				VoxelShape blockCollision = blockState.getCollisionShape(world, pos).getFaceShape(face.faceNormalDir);
				VoxelShape occludingCollision = occludingBlockState.getCollisionShape(world, forwardPos).getFaceShape(face.faceNormalDir.getOpposite());
				
				return !Shapes.blockOccudes(blockCollision, occludingCollision, face.faceNormalDir);
			}
			return false;
		}
		public void processAndCull()
		{
			// sort ascending so the first that are processed are the closest, which should occlude the most
			faces.sort(Comparator.comparing(FaceData::getCentroid));
			
			// god fucking lord this was hard to search for
			QuadFrustum frustum = new QuadFrustum();
			List<Integer> obstructedFaces = new ArrayList<>(faces.size());
			
			for (int i = 0; i < faces.size(); i++)
			{
				// this iterares through all faces!!! unless it has been obstructed
				if (obstructedFaces.contains(i))
					continue;
				
				// gets the current face, and creates a frustom that consists of 5 planes: quad plane (the back of the face as a plane,
				// used to check quickly if a point is obstructed) and 4 aditional planes: right, bottom, up, left for more precise checking
				FaceData currentFace = faces.get(i);
				frustum.createFor(currentFace);
				
				for (int j = 0; j < faces.size(); j++)
				{
					FaceData otherFace = faces.get(j);
					
					// if a face was already obstructed (by another face) just skip processing it
					if (obstructedFaces.contains(j) || i == j || !frustum.isAbleToBeObstructed(otherFace))
						continue;
					
					// if a face is obstructed (centroid and all corners are "above" these 5 planes) by the current face,
					// it is added to a list to remove after the entire loop, and skips processing the face
					QuadFrustum.FaceState state = frustum.isFaceObstructed(otherFace);
					if (state == QuadFrustum.FaceState.FULLY_OBSTRUCTED)
					{
						obstructedFaces.add(j);
					}
				}
			}
			
			sortAndRemoveIndices(obstructedFaces);
			
			// ok most of the time removing a face that has it's centroid obstructed but not it's corners is ok because
			// it was skipped by that small epsilon in the plane check so this should be fine
			faces.removeIf(v -> v.centroid.obstructed);
		}
		private void sortAndRemoveIndices(List<Integer> obstructedFaces)
		{
			// start from highest since starting from the lowest shifts the entire list
			obstructedFaces.sort(Comparator.reverseOrder());
			for (int index : obstructedFaces)
			{
				// java when will you create a removeAll(Collection<int>) method pls
				faces.remove(index);
			}
		}
		public BlockPos getBlockPos(FaceData face)
		{
			return blockPositions.get(face.blockPosIndex);
		}
		public interface Vector3Base
		{
			double dot(Vector3d point);
		}
		public static class QuadFrustum
		{
			private final Plane down = new Plane();
			private final Plane right = new Plane();
			private final Plane up = new Plane();
			private final Plane left = new Plane();
			private final QuadPlane backQuad = new QuadPlane();
			public void createFor(FaceData face)
			{
				FaceData.PointData[] midpoints = face.corners;
				down.setFor3Point(midpoints, 0);
				left.setFor3Point(midpoints, 1);
				up.setFor3Point(midpoints, 2);
				right.setFor3Point(midpoints, 3);
				Direction normalDir = face.faceNormalDir;
				backQuad.setForPointAndNormal(face.getCentroid().point, new Vector3i(normalDir.getStepX(), normalDir.getStepY(), normalDir.getStepZ()));
			}
			public boolean isPointObstructed(FaceData.PointData point)
			{
				if (point.isObstructed())
					return true;
				
				boolean pointObstructed =
					left.isAbove(point.point) &&
						up.isAbove(point.point) &&
						right.isAbove(point.point) &&
						down.isAbove(point.point);
				
				if (pointObstructed)
					point.markObstructed();
				
				return pointObstructed;
			}
			public FaceState isFaceObstructed(FaceData otherFace)
			{
				boolean centroid = isPointObstructed(otherFace.centroid);
				boolean c1 = isPointObstructed(otherFace.corners[0]);
				boolean c2 = isPointObstructed(otherFace.corners[1]);
				boolean c3 = isPointObstructed(otherFace.corners[2]);
				boolean c4 = isPointObstructed(otherFace.corners[3]);
				
				if (centroid && c1 && c2 && c3 && c4)
					return FaceState.FULLY_OBSTRUCTED;
				else if (centroid || c1 || c2 || c3 || c4)
					return FaceState.PARTIALLY_OBSTRUCTED;
				return FaceState.UNOBSTRUCTED;
			}
			public boolean isAbleToBeObstructed(FaceData otherFace)
			{
				return backQuad.isAbove(otherFace.centroid.point);
			}
			public enum FaceState
			{
				UNOBSTRUCTED,
				PARTIALLY_OBSTRUCTED,
				FULLY_OBSTRUCTED
			}
		}
		public static class Plane
		{
			public Vector3Base normal;
			public void setFor3Point(FaceData.PointData[] corners, int index)
			{
				Vector3d a = corners[index].point;
				Vector3d b = corners[(index + 1) % 4].point;
				
				// it isn't necessary to normalize since we're only checking if a point is above a plane, not how far it is
				
				// thank you c# system.numerics.plane.CreateFromVertices code for existing
				normal = new Vector3DoubleImpl(a.cross(b, new Vector3d()).normalize());
			}
			public double getDistance(Vector3d point)
			{
				return normal.dot(point);
			}
			public boolean isAbove(Vector3d point)
			{
				// small epsilon so blocks that are on the plane aren't detected as obstructed, but will be processed later
				return getDistance(point) > 10e-6;
			}
			@Override
			public String toString()
			{
				return normal.toString() + " = 0";
			}
		}
		public static class QuadPlane extends Plane
		{
			public double distance;
			public void setForPointAndNormal(Vector3d point, Vector3i normal)
			{
				this.normal = new Vector3IntImpl(normal.negate(new Vector3i()));
				
				// since the dot product of the normal and the point must be 0 (lies in the plane) ax + by + cz = d HOLY FUCKIGN SHIT I AM LEARNGIN geometry
				// note: distance is inverted since normal is inverted to detect points that are in the "back"
				distance = -this.normal.dot(point);
			}
			@Override
			public double getDistance(Vector3d point)
			{
				return normal.dot(point) + distance;
			}
			@Override
			public String toString()
			{
				return normal.toString() + " = " + distance;
			}
		}
		public static class Vector3DoubleImpl implements Vector3Base
		{
			public Vector3d vector;
			public Vector3DoubleImpl(Vector3d vector)
			{
				this.vector = vector;
			}
			@Override
			public double dot(Vector3d point)
			{
				return vector.x * point.x + vector.y * point.y + vector.z * point.z;
			}
			@Override
			public String toString()
			{
				return vector.x + "x + " + vector.y + "y + " + vector.z + "z";
			}
		}
		public static class Vector3IntImpl implements Vector3Base
		{
			public Vector3i vector;
			public Vector3IntImpl(Vector3i vector)
			{
				this.vector = vector;
			}
			@Override
			public double dot(Vector3d point)
			{
				return vector.x * point.x + vector.y * point.y + vector.z * point.z;
			}
			@Override
			public String toString()
			{
				return vector.x + "x + " + vector.y + "y + " + vector.z + "z";
			}
		}
	}
}
