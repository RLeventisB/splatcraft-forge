package net.splatcraft.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.math.DoubleMath;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.Double2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.doubles.DoubleUnaryOperator;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;
import net.splatcraft.mixin.accessors.VoxelShapeAccessor;
import org.apache.commons.lang3.math.IEEE754rUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import oshi.util.tuples.Quartet;
import oshi.util.tuples.Quintet;
import oshi.util.tuples.Triplet;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CollisionUtils
{
	public static Optional<Quintet<BlockPos, Vec3, Vec3, Boolean, Double>> findFirstBlock(Vec3 position, Vec3 velocity, float radius, List<Pair<BlockPos, VoxelShape>> possibleCollisions, DoubleUnaryOperator orthogonalCoefficientOperator)
	{
		// welcome to oshi.util.tuples usage hell where every return value changes so making records for every single one of these will clutter up the file very quickly (i think)
		// so i will use ambiguously-named methods instead!!! (i miss c# named-tuples :( )
		// also warning!!!! the "position inside the box that is closest to the segment" is sometimes unreliable

		Vec3 reversedPosition = position.reverse();
		float radiusSqrd = radius * radius;

		// iterate for every posible collision, and get the closest block to collide with.
		Triplet<Integer, Double, Quartet<BlockPos, Vec3, Vec3, Boolean>> firstBlockToCollide = findFirstToCollide(possibleCollisions, blockData ->
		{
			// lmao iterate for every aabb in the block's voxelshape, and find the closest one.
			Triplet<Integer, Double, Triplet<Vec3, Vec3, Boolean>> firstBoxToCollide = findFirstToCollide(blockData.getSecond().toAabbs(), aabb ->
			{
				AABB relativeBox = aabb.move(reversedPosition);

				Triplet<Vec3, Vec3, Double> collision = calculateOrthogonalPoint(velocity, relativeBox, orthogonalCoefficientOperator);

				if (collision.getA().distanceToSqr(collision.getB()) > radiusSqrd) return null;

				return Pair.of(collision.getC(), new Triplet<>(
					collision.getA().add(position), // make these points absolute
					collision.getB().add(position),
					relativeBox.contains(Vec3.ZERO)));
			});

			if (firstBoxToCollide == null)
				return null; // no box in the voxelshape collided oops, so we will return nothing too!

			// re-pack the data, giving the current block's position, the absolute closest point to the aabb inside segment, and the absolute closest point to the segment, inside the aabb.
			// i am losing my mind writing this
			Triplet<Vec3, Vec3, Boolean> boxData = firstBoxToCollide.getC();
			Quartet<BlockPos, Vec3, Vec3, Boolean> data = CommonUtils.merge(blockData.getFirst(), boxData);

			return Pair.of(firstBoxToCollide.getB(), data);
		});

		// no collision >:(
		if (firstBlockToCollide == null) return Optional.empty();

		Quartet<BlockPos, Vec3, Vec3, Boolean> data = firstBlockToCollide.getC();
		// return as: block pos, absolute point inside segment, absolute point inside aabb, orthogonal coefficient, and whether the position was inside the box
		return Optional.of(CommonUtils.merge(data, firstBlockToCollide.getB()));
	}
	public static List<Quartet<Entity, Vec3, Vec3, Double>> findEntityCollisions(Vec3 position, Vec3 velocity, float radius, List<Entity> possibleCollisions, DoubleUnaryOperator orthogonalCoefficientOperator)
	{
		Double2ReferenceRBTreeMap<Triplet<Entity, Vec3, Vec3>> collisions = new Double2ReferenceRBTreeMap<>();
		Vec3 reversedPosition = position.reverse();
		float radiusSqrd = radius * radius;

		for (Entity entity : possibleCollisions)
		{
			AABB relativeBox = entity.getBoundingBox().move(reversedPosition);
			Triplet<Vec3, Vec3, Double> result = calculateOrthogonalPoint(velocity, relativeBox, orthogonalCoefficientOperator);

			if (result.getA().distanceToSqr(result.getB()) > radiusSqrd) continue;

			collisions.put(result.getC().doubleValue(), new Triplet<>(entity, result.getA().add(position), result.getB().add(position)));
		}

		return collisions.double2ReferenceEntrySet().stream().map(v ->
			CommonUtils.merge(v.getValue(), v.getDoubleKey())
		).toList();
	}
	public static Optional<Quartet<Entity, Vec3, Vec3, Double>> findFirstEntityCollisions(Vec3 position, Vec3 velocity, float radius, List<Entity> possibleCollisions, DoubleUnaryOperator orthogonalCoefficientOperator)
	{
		Vec3 reversedPosition = position.reverse();
		float radiusSqrd = radius * radius;

		Triplet<Integer, Double, Triplet<Entity, Vec3, Vec3>> firstToCollide = findFirstToCollide(possibleCollisions, entity ->
		{
			AABB relativeBox = entity.getBoundingBox().move(reversedPosition);
			Triplet<Vec3, Vec3, Double> result = calculateOrthogonalPoint(velocity, relativeBox, orthogonalCoefficientOperator);

			if (result.getA().distanceToSqr(result.getB()) > radiusSqrd) return null;

			return Pair.of(result.getC(), new Triplet<>(entity, result.getA().add(position), result.getB().add(position)));
		});

		if (firstToCollide == null) return Optional.empty();

		return Optional.of(CommonUtils.merge(firstToCollide.getC(), firstToCollide.getB()));
	}
	public static <Type, Data> @Nullable Triplet<Integer, Double, Data> findFirstToCollide(@NotNull List<Type> dataSet, @NotNull Function<Type, Pair<Double, Data>> collider)
	{
		double minAdvancedFrame = Double.POSITIVE_INFINITY;
		int selectedIndex = -1;
		Data selectedData = null;
		int dataSetSize = dataSet.size();

		for (int i = 0; i < dataSetSize; i++)
		{
			Type object = dataSet.get(i);
			Pair<Double, Data> data = collider.apply(object);

			if (data == null || data.getFirst() == null || data.getFirst() >= minAdvancedFrame) continue;

			minAdvancedFrame = data.getFirst();
			selectedData = data.getSecond();
			selectedIndex = i;
		}

		if (selectedIndex == -1) return null;

		return new Triplet<>(selectedIndex, minAdvancedFrame, selectedData);
	}
	public static double collide(Direction.Axis movementAxis, AABB collisionBox, Iterable<Pair<BlockPos, VoxelShape>> possibleHits, double desiredOffset, HashSet<BlockPos> collisions)
	{
		List<BlockPos> collidedBlocks = new ArrayList<>();
		for (Pair<BlockPos, VoxelShape> pair : possibleHits)
		{
			if (Math.abs(desiredOffset) < 1.0E-7)
			{
				return 0.0;
			}

			double newOffset = pair.getSecond().collide(movementAxis, collisionBox, desiredOffset + 1);
			if (!DoubleMath.fuzzyEquals(desiredOffset, newOffset, 10E-7))
			{
				collidedBlocks.clear();
			}

			collidedBlocks.add(pair.getFirst());

			desiredOffset = newOffset;
		}

		collisions.addAll(collidedBlocks);

		return desiredOffset;
	}
	public static List<Pair<BlockPos, VoxelShape>> collectWorldColliders(Entity entity, Level level, AABB aabb)
	{
		ImmutableList.Builder<Pair<BlockPos, VoxelShape>> builder = ImmutableList.builderWithExpectedSize(3);

		WorldBorder worldborder = level.getWorldBorder();
		boolean addWorldBorder = entity != null && worldborder.isInsideCloseToBorder(entity, aabb);
		if (addWorldBorder)
		{
			builder.add(Pair.of(new BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), worldborder.getCollisionShape()));
		}

		builder.addAll(
			() -> new BlockCollisions<>(level, entity, aabb, false, (blockPos, shape) -> Pair.of(blockPos.immutable(), shape))
		);
		return builder.build();
	}
	public static List<VoxelShape> collectAndConnectShapes(BlockGetter getter, Collection<BlockPos> blocks, BlockPos localZero)
	{
		if (blocks.isEmpty())
			return List.of();

		// ew
		final Vec3i[] offsets = new Vec3i[] {
			new Vec3i(0, 1, 0),
			new Vec3i(-1, 0, 0),
			new Vec3i(0, 0, 1),
			new Vec3i(0, 0, -1),
			new Vec3i(1, 0, 0),
			new Vec3i(0, -1, 0),
			new Vec3i(0, 1, 1),
			new Vec3i(0, 1, -1),
			new Vec3i(1, 1, 0),
			new Vec3i(0, -1, 1),
			new Vec3i(0, -1, -1),
			new Vec3i(-1, 1, 0),
			new Vec3i(1, 0, 1),
			new Vec3i(-1, 0, 1),
			new Vec3i(-1, 0, -1),
			new Vec3i(-1, -1, 0),
			new Vec3i(1, 0, -1),
			new Vec3i(1, -1, 0),
			new Vec3i(1, 1, -1),
			new Vec3i(-1, -1, -1),
			new Vec3i(1, -1, -1),
			new Vec3i(-1, 1, 1),
			new Vec3i(-1, 1, -1),
			new Vec3i(-1, -1, 1),
			new Vec3i(1, -1, 1),
			new Vec3i(1, 1, 1)
		};

		HashMap<BlockPos, VoxelShape> shapes = blocks.stream().map(v ->
		{
			BlockPos localPos = v.subtract(localZero);
			VoxelShape shape = getter.getBlockState(v).getVisualShape(getter, v, CollisionContext.empty());
			if (shape.isEmpty())
				return null;

			DiscreteVoxelShape bounds = ((VoxelShapeAccessor) shape).getShape();
			if (bounds.getXSize() < 1 || bounds.getZSize() < 1)
			{
				return null;
			}
			return Map.entry(localPos, shape.move(localPos.getX(), localPos.getY(), localPos.getZ()).optimize());
		}).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> x, HashMap::new));

		ImmutableList.Builder<VoxelShape> builder = ImmutableList.builder();

		while (!shapes.isEmpty())
		{
			Map.Entry<BlockPos, VoxelShape> firstEntry = Iterables.getFirst(shapes.entrySet(), null);
			final VoxelShape[] currentShape = {firstEntry.getValue()};
			while (true)
			{
				// TODO: optimize this correctly pls i cannot do Breadth-first search without processing the same block twice
				boolean joined = false;
				List<Map.Entry<BlockPos, VoxelShape>> shapesToAdd = new ArrayList<>();
				for (Map.Entry<BlockPos, VoxelShape> entry : shapes.entrySet())
				{
					if (shapesCollide(currentShape[0], entry.getValue()))
					{
						shapesToAdd.add(entry);
						joined = true;
					}
				}

				shapesToAdd.forEach(v ->
				{
					currentShape[0] = Shapes.joinUnoptimized(currentShape[0], v.getValue(), BooleanOp.OR);
					shapes.remove(v.getKey());
				});
				shapesToAdd.clear();

				if (!joined)
					break;
			}

/*
			while (true)
			{
				boolean searchingAdjacent = true;

				for (Vec3i offset : offsets)
				{
					BlockPos newPos = cursor.offset(offset);

					if (!shapes.containsKey(newPos))
						continue;

					VoxelShape otherShape = shapes.get(newPos);

					if (shapesCollide(currentShape, otherShape))
					{
						currentShape = Shapes.join(currentShape, otherShape, BooleanOp.OR);

						cursor = newPos;
						searchingAdjacent = false;
					}
				}
				if (counter == 10)
				{
//					currentShape = currentShape.optimize();
					counter = -1;
				}

				counter++;

				if (searchingAdjacent)
					break;
			}
*/

			builder.add(currentShape[0].optimize().optimize());
		}

		return builder.build();
	}
	private static boolean shapesCollide(VoxelShape shape1, VoxelShape shape2)
	{
		AtomicBoolean result = new AtomicBoolean(false);

		shape1.forAllBoxes((x1, y1, z1, x2, y2, z2) ->
		{
			if (result.get())
				return;

			shape2.forAllBoxes((x3, y3, z3, x4, y4, z4) ->
			{
				if (result.get())
					return;

				double tolerance = 10e-5;

				if (x1 - x4 <= tolerance && x2 - x3 >= -tolerance && y1 - y4 <= tolerance && y2 - y3 >= -tolerance && z1 - z4 <= tolerance && z2 - z3 >= -tolerance)
					result.set(true);
			});
		});

		return result.get();
	}
	// these methods below come from https://stackoverflow.com/questions/34952680/distance-between-a-ray-and-a-bound-box
	// yes stack overflow (and Raidho Coaxil with 41 of reputation score and 3 bronze badges who probably had access
	// to better search engines than now i suppose because i cant find this code anywhere else) comes to save
	// me from eternal torment
	private static @NotNull Triplet<Double, Vec3, Double> getLineDistance(Vec3 direction, AABB relativeBox, DoubleUnaryOperator rayCoefficientLimiter)
	{
		Triplet<Vec3, Vec3, Double> result = calculateOrthogonalPoint(direction, relativeBox, rayCoefficientLimiter);

		return new Triplet<>(result.getA().distanceTo(result.getB()), result.getB(), result.getC());
	}
	private static @NotNull Triplet<Float, Vector3f, Float> getLineDistance(Vector3f direction, AABB relativeBox, FloatUnaryOperator rayCoefficientLimiter)
	{
		Triplet<Vector3f, Vector3f, Float> result = calculateOrthogonalPoint(direction, relativeBox, rayCoefficientLimiter);

		return new Triplet<>(result.getA().distance(result.getB()), result.getB(), result.getC());
	}
	/**
	 * Calculates the closest point in a infinite 3d line, which contains the origin of the 3d space, to a bounding box that should be relative to the previously mentioned line.
	 * This "closest point" can be tweaked via the orthogonalCoefficientOperator.
	 *
	 * @param direction                     The infinite line's direction.
	 * @param relativeBox                   An AABB that is relative to the previously mentioned line.
	 * @param orthogonalCoefficientOperator A function that is applied to the coefficient obtained from the orthogonal projection between the closest point to the RelativeBox
	 * @return A triplet that contains the closest relative point to the AABB that is inside the line, the closest relative point to the line inside the AABB, and the orthogonal projection coefficient used for the first element.
	 */
	public static Triplet<Vec3, Vec3, Double> calculateOrthogonalPoint(Vec3 direction, AABB relativeBox, DoubleUnaryOperator orthogonalCoefficientOperator)
	{
		Vec3 closestPoint = getClosestPointToDirection(direction, relativeBox);

		if (direction.lengthSqr() == 0)
			return new Triplet<>(Vec3.ZERO, closestPoint, 0.0);

		double t = orthogonalCoefficientOperator.apply(direction.dot(closestPoint) / direction.lengthSqr()); // orthogonal projection??? in my code???? its more likely than you think!
		return new Triplet<>(direction.scale(t), closestPoint, t);
	}
	public static Triplet<Vector3f, Vector3f, Float> calculateOrthogonalPoint(Vector3f rayDirection, AABB relativeBox, FloatUnaryOperator orthogonalCoefficientOperator)
	{
		Vector3f closestPoint = getClosestPointToDirection(rayDirection, relativeBox);

		float t = orthogonalCoefficientOperator.apply(rayDirection.dot(closestPoint) / rayDirection.lengthSquared());
		return new Triplet<>(rayDirection.mul(t), closestPoint, t);
	}
	/**
	 * Some collision thing that returns collision data of an "relative" bounding box with a ray.
	 *
	 * @param rayDirection The normalized direction vector of the ray to check with.
	 * @param relativeBox  An {@link AABB} that has relative coordinates in terms of the ray.
	 * @return A {@link Triplet} that has the distance of the AABB to the ray as the first element, the closest point from the ray to the box, and a delta value that represents the coefficient of the orthogonal projection of the second element, on the ray, because it is a ray this value is only positive or 0.
	 */
	public static Triplet<Double, Vec3, Double> getRayDistance(Vec3 rayDirection, AABB relativeBox)
	{
		return getLineDistance(rayDirection, relativeBox, (v) -> Math.max(v, 0));
	}
	public static Triplet<Float, Vector3f, Float> getRayDistance(Vector3f rayDirection, AABB relativeBox)
	{
		return getLineDistance(rayDirection, relativeBox, (v) -> Math.max(v, 0));
	}
	/**
	 * Some collision thing that returns collision data of an "relative" bounding box with an segment.
	 *
	 * @param segmentVector The segment vector to check with.
	 * @param relativeBox   An {@link AABB} that has relative coordinates in relation to the starting point of the segment.
	 * @return A {@link Triplet} that has the distance of the AABB to the segment as the first element, the closest point from the segment to the box, and a delta value that represents the coefficient of the orthogonal projection of the second element, on the segment, because it is a segment it is clamped between 0 and 1.
	 */
	public static Triplet<Double, Vec3, Double> getSegmentDistance(Vec3 segmentVector, AABB relativeBox)
	{
		return getLineDistance(segmentVector, relativeBox, (v) -> Math.clamp(v, 0, 1));
	}
	public static Triplet<Float, Vector3f, Float> getSegmentDistance(Vector3f segmentVector, AABB relativeBox)
	{
		return getLineDistance(segmentVector, relativeBox, (v) -> Math.clamp(v, 0, 1));
	}
	public static Optional<Triplet<Vec3, Vec3, Double>> isCubeInsideSegmentCollidingWithAABB(Vec3 segmentVector, AABB relativeBox, Vec3 boxSize)
	{
		return isCubeInsideSegmentCollidingWithAABB(segmentVector, relativeBox, boxSize, (v) -> Math.clamp(v, 0, 1));
	}
	public static Optional<Triplet<Vec3, Vec3, Double>> isCubeInsideSegmentCollidingWithAABB(Vec3 segmentVector, AABB relativeBox, Vec3 boxSize, DoubleUnaryOperator orthogonalCoefficientOperator)
	{
		Triplet<Vec3, Vec3, Double> result = calculateOrthogonalPoint(segmentVector, relativeBox, orthogonalCoefficientOperator);

		Vec3 closestPoint = result.getA();

		boolean collided =
			Math.abs(closestPoint.x - result.getB().x) < boxSize.x &&
			Math.abs(closestPoint.y - result.getB().y) < boxSize.y &&
			Math.abs(closestPoint.z - result.getB().z) < boxSize.z;

		if (!collided)
			return Optional.empty();

		return Optional.of(result);
	}
	public static Optional<Triplet<Vector3f, Vector3f, Float>> isCubeInsideSegmentCollidingWithAABB(Vector3f segmentVector, AABB relativeBox, Vector3f boxSize)
	{
		return isCubeInsideSegmentCollidingWithAABB(segmentVector, relativeBox, boxSize, (v) -> Math.clamp(v, 0, 1));
	}
	public static Optional<Triplet<Vector3f, Vector3f, Float>> isCubeInsideSegmentCollidingWithAABB(Vector3f segmentVector, AABB relativeBox, Vector3f boxSize, FloatUnaryOperator orthogonalCoefficientOperator)
	{
		Triplet<Vector3f, Vector3f, Float> result = calculateOrthogonalPoint(segmentVector, relativeBox, orthogonalCoefficientOperator);

		Vector3f closestPoint = result.getA();

		boolean collided =
			Math.abs(closestPoint.x - result.getB().x) < boxSize.x &&
			Math.abs(closestPoint.y - result.getB().y) < boxSize.y &&
			Math.abs(closestPoint.z - result.getB().z) < boxSize.z;

		if (!collided)
			return Optional.empty();

		return Optional.of(result);
	}
	private static @NotNull Vec3 getClosestPointToDirection(Vec3 direction, AABB relativeBox)
	{
		if (direction.lengthSqr() == 0)
		{
			return CommonUtils.limitTo(relativeBox, Vec3.ZERO);
		}

		double tx1 = relativeBox.minX / direction.x;
		double tx2 = relativeBox.maxX / direction.x;
		double ty1 = relativeBox.minY / direction.y;
		double ty2 = relativeBox.maxY / direction.y;
		double tz1 = relativeBox.minZ / direction.z;
		double tz2 = relativeBox.maxZ / direction.z;

		double[] xPair = direction.x < 0 ? new double[] {tx2, tx1} : new double[] {tx1, tx2};
		double[] yPair = direction.y < 0 ? new double[] {ty2, ty1} : new double[] {ty1, ty2};
		double[] zPair = direction.z < 0 ? new double[] {tz2, tz1} : new double[] {tz1, tz2};

		double p1 = Math.max(0.0, IEEE754rUtils.max(xPair[0], yPair[0], zPair[0]));
		double p2 = Math.max(0.0, IEEE754rUtils.min(xPair[1], yPair[1], zPair[1]));

		Vec3 point = direction.scale(Math.min(p1, p2));

		double x = Mth.clamp(point.x, relativeBox.minX, relativeBox.maxX);
		double y = Mth.clamp(point.y, relativeBox.minY, relativeBox.maxY);
		double z = Mth.clamp(point.z, relativeBox.minZ, relativeBox.maxZ);
		return new Vec3(x, y, z);
	}
	private static @NotNull Vector3f getClosestPointToDirection(Vector3f direction, AABB relativeBox)
	{
		float minX = (float) relativeBox.minX;
		float maxX = (float) relativeBox.maxX;
		float minY = (float) relativeBox.minY;
		float maxY = (float) relativeBox.maxY;
		float minZ = (float) relativeBox.minZ;
		float maxZ = (float) relativeBox.maxZ;

		float tx1 = minX / direction.x;
		float tx2 = maxX / direction.x;
		float ty1 = minY / direction.y;
		float ty2 = maxY / direction.y;
		float tz1 = minZ / direction.z;
		float tz2 = maxZ / direction.z;

		float[] xPair = direction.x < 0 ? new float[] {tx2, tx1} : new float[] {tx1, tx2};
		float[] yPair = direction.y < 0 ? new float[] {ty2, ty1} : new float[] {ty1, ty2};
		float[] zPair = direction.z < 0 ? new float[] {tz2, tz1} : new float[] {tz1, tz2};

		float p1 = Math.max(0.0f, IEEE754rUtils.max(xPair[0], yPair[0], zPair[0]));
		float p2 = Math.max(0.0f, IEEE754rUtils.min(xPair[1], yPair[1], zPair[1]));

		Vector3f point = direction.mul(Math.min(p1, p2), new Vector3f());

		float x = Mth.clamp(point.x, minX, maxX);
		float y = Mth.clamp(point.y, minY, maxY);
		float z = Mth.clamp(point.z, minZ, maxZ);
		return new Vector3f(x, y, z);
	}
	public static Vec3 getClosestPoint(Vec3 rayDirection, Vec3 relativePoint)
	{
		double t = Math.max(0.0, rayDirection.dot(relativePoint) / rayDirection.lengthSqr());

		return rayDirection.scale(t);
	}
}
