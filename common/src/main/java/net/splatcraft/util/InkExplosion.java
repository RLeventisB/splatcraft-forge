package net.splatcraft.util;

import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.explosion.Explosion;
import net.splatcraft.blocks.ColoredBarrierBlock;
import net.splatcraft.entities.InkDropEntity;
import net.splatcraft.entities.SpawnShieldEntity;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class InkExplosion
{
	private final double x;
	private final double y;
	private final double z;
	private final AttackId attackId;
	@Nullable
	private final Entity exploder;
	private final float paintRadius;
	private final List<BlockFace> affectedBlockPositions = Lists.newArrayList();
	private final Vec3d position;
	private final InkBlockUtils.InkType inkType;
	private final DamageRangesRecord dmgCalculator;
	private final ItemStack weapon;
	private final InkColor color;
	public InkExplosion(@Nullable Entity source, double x, double y, double z, DamageRangesRecord damageCalculator, float paintRadius, InkBlockUtils.InkType inkType, ItemStack weapon, AttackId attackId)
	{
		exploder = source;
		this.paintRadius = paintRadius;
		this.x = x;
		this.y = y;
		this.z = z;
		this.attackId = attackId;
		position = new Vec3d(this.x, this.y, this.z);
		
		this.inkType = inkType;
		dmgCalculator = damageCalculator;
		this.weapon = weapon;
		color = ColorUtils.getEntityColor(exploder);
	}
	public static Vec3d adjustPosition(Vec3d pos, Vector3f normal)
	{
		float modifier = 0.01f;
		return pos.add(normal.x() * modifier, normal.y() * modifier, normal.z() * modifier);
	}
	public static void createInkExplosion(Entity source, Vec3d pos, float paintRadius, float damageRadius, float damage, InkBlockUtils.InkType type, ItemStack weapon)
	{
		createInkExplosion(source, pos, paintRadius, DamageRangesRecord.createSimpleLerped(damage, damageRadius), type, weapon, AttackId.NONE);
	}
	public static void createInkExplosion(Entity source, Vec3d pos, float paintRadius, float damageRadius, float closeDamage, float farDamage, InkBlockUtils.InkType type, ItemStack weapon)
	{
		createInkExplosion(source, pos, paintRadius, DamageRangesRecord.createSimpleLerped(closeDamage, farDamage, damageRadius), type, weapon, AttackId.NONE);
	}
	public static void createInkExplosion(Entity source, Vec3d pos, float paintRadius, DamageRangesRecord damageManager, InkBlockUtils.InkType type, ItemStack weapon, AttackId attackId)
	{
		if (source == null || source.getWorld().isClient)
			return;
		
		InkExplosion inksplosion = new InkExplosion(source, pos.x, pos.y, pos.z, damageManager, paintRadius, type, weapon, attackId);
		
		inksplosion.doExplosionA();
		inksplosion.doExplosionCosmetics(false);
	}
	public static void doSplashes(@Nullable Entity owner, Vec3d center, SubWeaponSettings.SplashAroundDataRecord splashData, InkColor color, InkBlockUtils.InkType inkType)
	{
		if (owner == null)
			return;
		
		World world = owner.getWorld();
		Random random = world.getRandom();
		// this is not because i feel this is nice in terms of syntax this is because im a dumbass microoptimizer and i do this in c# too
		Function<Integer, Float> yawGetter = splashData.distributeEvenly() ?
			(count) -> (float) count / splashData.splashCount() :
			(count) -> world.getRandom().nextFloat();
		for (int i = 0; i < splashData.splashCount(); i++)
		{
			createDrop(
				world, owner, center,
				yawGetter.apply(i) * MathHelper.TAU, -splashData.splashPitchRange().getValue(random.nextFloat()) * MathHelper.PI,
				splashData.splashVelocityRange().getValue(random.nextFloat()),
				splashData.splashPaintRadius(), color, inkType
			);
		}
	}
	static void createDrop(World world, Entity owner, Vec3d center, float yaw, float pitch, float speed, float splashSize, InkColor color, InkBlockUtils.InkType type)
	{
		InkDropEntity drop = new InkDropEntity(owner.getWorld(), center, owner, color, type, splashSize);
		float f = -MathHelper.sin(yaw) * MathHelper.cos(pitch);
		float g = -MathHelper.sin(pitch);
		float h = MathHelper.cos(yaw) * MathHelper.cos(pitch);
		drop.setVelocity(f, g, h, speed, 0);
		
		world.spawnEntity(drop);
	}
	/**
	 * Does the first part of the explosion (destroy blocks)
	 */
	public void doExplosionA()
	{
		List<BlockFace> set = new ArrayList<>();
		ServerWorld world = (ServerWorld) exploder.getWorld();
		getBlocksInSphereWithNoise(set, world);
		
		affectedBlockPositions.addAll(set);
		if (dmgCalculator.isInsignificant())
			return;
		float radiusSquared = dmgCalculator.getMaxDistance() * dmgCalculator.getMaxDistance();
		int k1 = MathHelper.floor(x - dmgCalculator.getMaxDistance() - 1F);
		int l1 = MathHelper.floor(x + dmgCalculator.getMaxDistance() + 1F);
		int i2 = MathHelper.floor(y - dmgCalculator.getMaxDistance() - 1F);
		int i1 = MathHelper.floor(y + dmgCalculator.getMaxDistance() + 1F);
		int j2 = MathHelper.floor(z - dmgCalculator.getMaxDistance() - 1F);
		int j1 = MathHelper.floor(z + dmgCalculator.getMaxDistance() + 1F);
		Box box = new Box(k1, i2, j2, l1, i1, j1);
		List<LivingEntity> livingEntities = new ArrayList<>();
		List<SpawnShieldEntity> spawnShields = new ArrayList<>();
		world.getEntityLookup().forEachIntersects(box, (v) ->
		{
			if (!v.isSpectator())
			{
				if (v instanceof LivingEntity living)
				{
					livingEntities.add(living);
				}
				else if (v instanceof SpawnShieldEntity spawnShield)
				{
					spawnShields.add(spawnShield);
				}
			}
		});
		
		for (LivingEntity entity : livingEntities)
		{
			Box boundingBox = entity.getBoundingBox();
			Vec3d closestPos = new Vec3d(MathHelper.clamp(x, boundingBox.minX, boundingBox.maxX), MathHelper.clamp(y, boundingBox.minY, boundingBox.maxY), MathHelper.clamp(z, boundingBox.minZ, boundingBox.maxZ));
			
			float distance = (float) position.squaredDistanceTo(closestPos);
			if (distance > radiusSquared) // still collides even in the center isn't in radius
				continue;
			InkColor targetColor = ColorUtils.getEntityColor(entity);
			if (!targetColor.isValid() || (color != targetColor && targetColor.isValid()))
			{
				Vec3d boundingBoxCenter = boundingBox.getCenter();
				
				// find shields that can protect entities of same color
				boolean spawnShieldBlocked = false;
				for (SpawnShieldEntity shieldEntity : spawnShields)
				{
					Box shieldBb = shieldEntity.getBoundingBox();
					// if using shieldBb.contains(boundingBox) some accuracy might be lost!!! since an entity can be damage even if they're obstructed (but not inside) the shield thingy
					if (shieldEntity.getColor() == ColorUtils.getEntityColor(entity) && shieldBb.contains(position) || shieldBb.raycast(position, boundingBoxCenter).isPresent())
					{
						spawnShieldBlocked = true;
						break;
					}
				}
				if (spawnShieldBlocked)
					continue;
				
				float seenPercent = Explosion.getExposure(position, entity);
				InkDamageUtils.doSplatDamage(entity, dmgCalculator.getDamage(MathHelper.sqrt(distance)) * seenPercent, exploder, weapon, attackId);
			}
			
			DyeColor dyeColor = color.getDyeColor();
			
			if (dyeColor != null && entity instanceof SheepEntity sheep)
			{
				sheep.setColor(dyeColor);
			}
		}
	}
	private void getBlocksInSphereWithNoise(List<BlockFace> set, ServerWorld world)
	{
		// explosion is inside a block, everything is occluded
		if (!world.isSpaceEmpty(new Box(position, position)))
			return;
		
		final float noiseRange = 0.2f;
		int cubeSizeHalf = ((int) Math.ceil(paintRadius + noiseRange) >> 1) + 1;
		FaceMap map = new FaceMap(position, world, paintRadius, noiseRange, world.random);
		
		for (int x = -cubeSizeHalf; x <= cubeSizeHalf; x++)
			for (int y = -cubeSizeHalf; y <= cubeSizeHalf; y++)
				for (int z = -cubeSizeHalf; z <= cubeSizeHalf; z++)
				{
					BlockPos pos = CommonUtils.createBlockPos(position.x + x, position.y + y, position.z + z);
					BlockState blockState = world.getBlockState(pos);
					
					if (InkBlockUtils.isBlockUninkable(world, pos) || !canPassIfBarrier(color, world, pos, blockState))
						continue;
					
					VoxelShape shape = blockState.getCollisionShape(world, pos);
					Vec3d relativePos = position.subtract(pos.toCenterPos());
					
					double dist = relativePos.length();
					if (dist <= paintRadius + MathHelper.SQUARE_ROOT_OF_TWO)
					{
						map.register(pos, shape);
					}
				}
		
		map.processAndCull();
		set.addAll(map.faces.stream().map(v -> new BlockFace(map.blockPositions.get(v.blockPosIndex), v.faceNormalDir)).collect(Collectors.toSet()));
	}
	private boolean canPassIfBarrier(InkColor color, WorldView worldView, BlockPos pos, BlockState state)
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
		Vec3d explosionPos = new Vec3d(x + 0.5f, y + 0.5f, z + 0.5f);
		
		World world = exploder.getWorld();
		
		if (spawnParticles)
		{
			if (paintRadius < 2.0F)
			{
				world.addParticle(ParticleTypes.EXPLOSION, x, y, z, 1.0D, 0.0D, 0.0D);
			}
			else
			{
				world.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1.0D, 0.0D, 0.0D);
			}
		}
		
		for (BlockFace blockFace : affectedBlockPositions)
		{
			BlockState blockstate = world.getBlockState(blockFace.pos());
			if (!blockstate.isAir())
			{
				float dist = (float) Math.sqrt(blockFace.pos().getSquaredDistanceFromCenter(explosionPos.x, explosionPos.y, explosionPos.z));
				InkBlockUtils.inkBlock(exploder, world, blockFace.pos(), color, blockFace.face(), inkType, dmgCalculator.getDamage(dist));
			}
		}
	}
	public Vec3d getPosition()
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
		public static List<FaceData> getFacesFromBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int blockPosIndex, Predicate<FaceData> distanceChecker)
		{
			List<FaceData> list = new ArrayList<>(3);
			
			// negative X
			if (minX > 0)
				addToListIfClose(list, distanceChecker, new FaceData(blockPosIndex, Direction.WEST,
					minX, minY, maxY, maxZ, minZ
				));
			
			// positive x
			if (maxX < 0)
				addToListIfClose(list, distanceChecker, new FaceData(blockPosIndex, Direction.EAST,
					maxX, minY, maxY, minZ, maxZ
				));
			
			// negative y
			if (minY > 0)
				addToListIfClose(list, distanceChecker, new FaceData(blockPosIndex, Direction.DOWN,
					minY, maxX, minX, minZ, maxZ
				));
			
			// positive y
			if (maxY < 0)
				addToListIfClose(list, distanceChecker, new FaceData(blockPosIndex, Direction.UP,
					maxY, minX, maxX, minZ, maxZ
				));
			
			// negative z
			if (minZ > 0)
				addToListIfClose(list, distanceChecker, new FaceData(blockPosIndex, Direction.NORTH,
					minZ, minX, maxX, minY, maxY
				));
			
			// positive z
			if (maxZ < 0)
				addToListIfClose(list, distanceChecker, new FaceData(blockPosIndex, Direction.SOUTH,
					maxZ, maxX, minX, minY, maxY
				));
			
			return list;
		}
		private static void addToListIfClose(List<FaceData> list, Predicate<FaceData> distanceChecker, FaceData faceData)
		{
			if (distanceChecker.test(faceData))
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
		private final Random random;
		public Vector3d worldOrigin;
		public World world;
		public ArrayList<FaceData> faces = new ArrayList<>();
		public List<BlockPos> blockPositions = new ArrayList<>();
		public FaceMap(Vec3d point, World world, float paintRange, float noiseRange, Random random)
		{
			worldOrigin = new Vector3d(point.x, point.y, point.z);
			this.world = world;
			this.paintRange = paintRange;
			this.noiseRange = noiseRange;
			this.random = random;
		}
		public void register(BlockPos pos, VoxelShape shape)
		{
			shape.forEachBox((xmin, ymin, zmin, xmax, ymax, zmax) ->
			{
				double minX = -worldOrigin.x + xmin + pos.getX();
				double minY = -worldOrigin.y + ymin + pos.getY();
				double minZ = -worldOrigin.z + zmin + pos.getZ();
				double maxX = -worldOrigin.x + xmax + pos.getX();
				double maxY = -worldOrigin.y + ymax + pos.getY();
				double maxZ = -worldOrigin.z + zmax + pos.getZ();
				
				List<FaceData> facesList = FaceData.getFacesFromBox(minX, minY, minZ, maxX, maxY, maxZ, blockPositions.size(), this::checkCloseEnough);
				if (!facesList.isEmpty())
				{
					blockPositions.add(pos);
					faces.addAll(facesList);
				}
			});
		}
		public boolean checkCloseEnough(FaceData data)
		{
			float noiseValue = (random.nextFloat() * 2f - 1) * noiseRange;
			// is this faster than doing a² + 2ab + b²?????? i should benchmark that but it has been 4 days since i am doing this thing so im tired
			return data.centroid.point.length() < paintRange + noiseValue;
		}
		public void processAndCull()
		{
			// sort ascending so the first that are processed are the closest, which should occlude the most
			faces.sort(Comparator.comparing(FaceData::getCentroid));
			
			// god fucking lord this was hard to search for
			QuadFrustum frustum = new QuadFrustum();
			List<Integer> obstructedFaces = new ArrayList<>(faces.size());
			HashSet<Integer> facesToProcess = IntStream.range(0, faces.size()).boxed().collect(Collectors.toCollection(HashSet::new));
			
			for (int i = 0; i < faces.size(); i++)
			{
				// this iterares through all faces!!! unless it has been already processed, or obstructed
				if (!facesToProcess.contains(i))
					continue;
				
				// gets the current face, and creates a frustom that consists of 5 planes: quad plane (the back of the face as a plane,
				// used to check quickly if a point is obstructed) and 4 aditional planes: right, bottom, up, left for more precise checking
				FaceData currentFace = faces.get(i);
				frustum.createFor(currentFace);
				Vector3d[] corners = Arrays.stream(currentFace.corners).map(v -> v.point).toArray(Vector3d[]::new);
				CommonUtils.spawnTestText(world, CommonUtils.vec3dFromVector3dc(worldOrigin.add(corners[0], new Vector3d())), "0", 100);
				CommonUtils.spawnTestText(world, CommonUtils.vec3dFromVector3dc(worldOrigin.add(corners[1], new Vector3d())), "1", 100);
				CommonUtils.spawnTestText(world, CommonUtils.vec3dFromVector3dc(worldOrigin.add(corners[2], new Vector3d())), "2", 100);
				CommonUtils.spawnTestText(world, CommonUtils.vec3dFromVector3dc(worldOrigin.add(corners[3], new Vector3d())), "3", 100);
				
				for (int j = 0; j < faces.size(); j++)
				{
					FaceData otherFace = faces.get(j);
					
					// if a face was already obstructed (by another face) just skip processing it
					if (obstructedFaces.contains(j) || currentFace == otherFace)
						continue;
					
					// if a face is obstructed (centroid and all corners are "above" these 5 planes) by the current face,
					// it is added to a list to remove after the entire loop, and skips processing the face
					if (frustum.isFaceObstructed(otherFace))
					{
						obstructedFaces.add(j);
						facesToProcess.remove(j);
					}
				}
			}
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
		public enum Space // for oct-trees, if implemented
		{
			POSX_POSY_POSZ(1),
			POSX_POSY_NEGZ(2),
			POSX_NEGY_POSZ(3),
			POSX_NEGY_NEGZ(4),
			NEGX_POSY_POSZ(5),
			NEGX_POSY_NEGZ(6),
			NEGX_NEGY_POSZ(7),
			NEGX_NEGY_NEGZ(8);
			public final byte id;
			Space(int id)
			{
				this.id = (byte) id;
			}
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
			private final QuadPlane quad = new QuadPlane();
			public void createFor(FaceData face)
			{
				FaceData.PointData[] midpoints = face.corners;
				down.setFor3Point(midpoints, 0);
				left.setFor3Point(midpoints, 1);
				up.setFor3Point(midpoints, 2);
				right.setFor3Point(midpoints, 3);
				quad.setForPointAndNormal(face.getCentroid().point, new Vector3i(face.faceNormalDir.getOffsetX(), face.faceNormalDir.getOffsetY(), face.faceNormalDir.getOffsetZ()));
			}
			public boolean isPointObstructed(FaceData.PointData point, Vector3d centroid)
			{
				if (point.isObstructed())
					return true;
				// this is true if the quad is not in and adyacent or something idk how to explain it
				boolean lowerTolerance = Math.abs(quad.normal.dot(centroid)) > 10e-3;
				
				boolean pointObstructed =
					quad.isAboveOrIn(point.point, lowerTolerance) &&
						left.isAboveOrIn(point.point, lowerTolerance) &&
						up.isAboveOrIn(point.point, lowerTolerance) &&
						right.isAboveOrIn(point.point, lowerTolerance) &&
						down.isAboveOrIn(point.point, lowerTolerance);
				
				if (pointObstructed)
					point.markObstructed();
				
				return pointObstructed;
			}
			public boolean isFaceObstructed(FaceData otherFace)
			{
				Vector3d centroidPoint = otherFace.centroid.point;
				boolean centroid = isPointObstructed(otherFace.centroid, centroidPoint);
				boolean c1 = isPointObstructed(otherFace.corners[0], centroidPoint);
				boolean c2 = isPointObstructed(otherFace.corners[1], centroidPoint);
				boolean c3 = isPointObstructed(otherFace.corners[2], centroidPoint);
				boolean c4 = isPointObstructed(otherFace.corners[3], centroidPoint);
				
				return centroid && c1 && c2 && c3 && c4;
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
			public boolean isAboveOrIn(Vector3d point, boolean lowerTolerance)
			{
				// small epsilon vectors that are literally on the plane arent "9.34537e-10" units above because of floating point precision
				// however! i rewrote all of this to use doubles instead so that's less common but anyways
				double distance = getDistance(point);
				return lowerTolerance ? distance + 10e-7 >= 0 : distance - 10e-6 > 0;
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
				distance = this.normal.dot(point);
			}
			@Override
			public double getDistance(Vector3d point)
			{
				return normal.dot(point) - distance;
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
