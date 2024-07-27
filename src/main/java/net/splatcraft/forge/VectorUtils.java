package net.splatcraft.forge;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

public class VectorUtils
{
	public static Vector3f XN = new Vector3f(-1.0F, 0.0F, 0.0F);
	public static Vector3f XP = new Vector3f(1.0F, 0.0F, 0.0F);
	public static Vector3f YN = new Vector3f(0.0F, -1.0F, 0.0F);
	public static Vector3f YP = new Vector3f(0.0F, 1.0F, 0.0F);
	public static Vector3f ZN = new Vector3f(0.0F, 0.0F, -1.0F);
	public static Vector3f ZP = new Vector3f(0.0F, 0.0F, 1.0F);
	public static Vector3f ZERO = new Vector3f(0.0F, 0.0F, 0.0F);
	public static Quaternionf rotationDegrees(Vector3f vector3f, float deg)
	{
		return new Quaternionf(new AxisAngle4d(deg * Mth.DEG_TO_RAD, vector3f));
	}
	public static Quaternionf rotation(Vector3f vector3f, float rad)
	{
		return new Quaternionf(new AxisAngle4d(rad, vector3f));
	}
	public static void transform(Vector3f vector3f, Quaternionf p_122252_)
	{
		Quaternionf quaternion = new Quaternionf(p_122252_);
		quaternion.mul(new Quaternionf(vector3f.x(), vector3f.y(), vector3f.z(), 0.0F));
		Quaternionf quaternion1 = new Quaternionf(p_122252_);
		quaternion1.conjugate();
		quaternion.mul(quaternion1);
		vector3f.set(quaternion.x(), quaternion.y(), quaternion.z());
	}
	public static void transform(Vector3f vector3f, Matrix3f p_122250_)
	{
		float f = vector3f.x;
		float f1 = vector3f.y;
		float f2 = vector3f.z;
		vector3f.x = p_122250_.m00 * f + p_122250_.m01 * f1 + p_122250_.m02 * f2;
		vector3f.y = p_122250_.m10 * f + p_122250_.m11 * f1 + p_122250_.m12 * f2;
		vector3f.z = p_122250_.m20 * f + p_122250_.m21 * f1 + p_122250_.m22 * f2;
	}
	public static void transform(Vector4f vector4f, Matrix4f p_123608_)
	{
		float f = vector4f.x;
		float f1 = vector4f.y;
		float f2 = vector4f.z;
		float f3 = vector4f.w;
		vector4f.x = p_123608_.m00() * f + p_123608_.m01() * f1 + p_123608_.m02() * f2 + p_123608_.m03() * f3;
		vector4f.y = p_123608_.m10() * f + p_123608_.m11() * f1 + p_123608_.m12() * f2 + p_123608_.m13() * f3;
		vector4f.z = p_123608_.m20() * f + p_123608_.m21() * f1 + p_123608_.m22() * f2 + p_123608_.m23() * f3;
		vector4f.w = p_123608_.m30() * f + p_123608_.m31() * f1 + p_123608_.m32() * f2 + p_123608_.m33() * f3;
	}
	public static Vector4f transform(Vector4f vector4f, Quaternionf p_123610_)
	{
		Quaternionf quaternion = new Quaternionf(p_123610_);
		quaternion.mul(new Quaternionf(vector4f.x(), vector4f.y(), vector4f.z(), 0.0F));
		Quaternionf quaternion1 = new Quaternionf(p_123610_);
		quaternion1.conjugate();
		quaternion.mul(quaternion1);
		vector4f.set(quaternion.x(), quaternion.y(), quaternion.z());
		return vector4f;
	}
	public static Vec3 lerp(double progress, Vec3 position, Vec3 lastPosition)
	{
		double x = Mth.lerp(progress, position.x(), lastPosition.x());
		double y = Mth.lerp(progress, position.y(), lastPosition.y());
		double z = Mth.lerp(progress, position.z(), lastPosition.z());
		return new Vec3(x, y, z);
	}
}
