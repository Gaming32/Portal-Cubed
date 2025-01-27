package com.fusionflux.portalcubed.util;


import net.minecraft.util.Pair;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

//This is from https://github.com/qouteall/ImmersivePortalsMod/blob/1.18/q_misc_util/src/main/java/qouteall/q_misc_util/my_util/DQuaternion.java,
public class IPQuaternion {
    public final double x;
    public final double y;
    public final double z;
    public final double w;

    // represents no rotation
    public static final IPQuaternion identity = new IPQuaternion(0, 0, 0, 1);

    public IPQuaternion(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /**
     * Converts from Minecraft's mutable quaternion to immutable IPQuaternion
     */
    public static IPQuaternion fromMcQuaternion(Quaternion quaternion) {
        return new IPQuaternion(
                quaternion.getX(), quaternion.getY(), quaternion.getZ(), quaternion.getW()
        );
    }

    /**
     * @return the axis that the rotation is being performed along
     */
    public Vec3d getRotatingAxis() {
        return new Vec3d(x, y, z).normalize();
    }

    public double getRotatingAngleRadians() {
        return Math.acos(w) * 2;
    }

    public double getRotatingAngleDegrees() {
        return Math.toDegrees(getRotatingAngleRadians());
    }

    /**
     * @return Converts to Minecraft's quaternion
     */
    public Quaternion toMcQuaternion() {
        return new Quaternion(
                (float) x, (float) y, (float) z, (float) w
        );
    }

    /**
     * Create a new quaternion.
     *
     * @param rotatingAxis the axis that it rotates along, must be normalized
     * @param degrees      the rotating angle in degrees
     * @return the result
     */
    public static IPQuaternion rotationByDegrees(
            Vec3d rotatingAxis,
            double degrees
    ) {
        return rotationByRadians(
                rotatingAxis, Math.toRadians(degrees)
        );
    }

    /**
     * Create a new quaternion.
     *
     * @param axis          the axis that it rotates along, must be normalized
     * @param rotationAngle the rotating angle in radians
     * @return the result
     */
    public static IPQuaternion rotationByRadians(
            Vec3d axis,
            double rotationAngle
    ) {
        double s = Math.sin(rotationAngle / 2.0F);
        return new IPQuaternion(
                axis.getX() * s,
                axis.getY() * s,
                axis.getZ() * s,
                Math.cos(rotationAngle / 2.0F)
        );
    }

    /**
     * Perform the rotation onto an immutable vector
     */
    public Vec3d rotate(Vec3d vec) {
        IPQuaternion result = this.hamiltonProduct(new IPQuaternion(vec.x, vec.y, vec.z, 0))
                .hamiltonProduct(getConjugated());

        return new Vec3d(result.x, result.y, result.z);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getW() {
        return w;
    }

    /**
     * @return the hamilton product of "this" and "other".
     * Equivalent to firstly do "other" rotation and then do "this" rotation
     */
    public IPQuaternion hamiltonProduct(IPQuaternion other) {
        double x1 = this.getX();
        double y1 = this.getY();
        double z1 = this.getZ();
        double w1 = this.getW();
        double x2 = other.getX();
        double y2 = other.getY();
        double z2 = other.getZ();
        double w2 = other.getW();
        return new IPQuaternion(
                w1 * x2 + x1 * w2 + y1 * z2 - z1 * y2,
                w1 * y2 - x1 * z2 + y1 * w2 + z1 * x2,
                w1 * z2 + x1 * y2 - y1 * x2 + z1 * w2,
                w1 * w2 - x1 * x2 - y1 * y2 - z1 * z2
        );
    }

    // firstly apply "this" and then "other"
    public IPQuaternion combine(IPQuaternion other) {
        return other.hamiltonProduct(this);
    }

    /**
     * @return the inverse rotation transformation
     */
    public IPQuaternion getConjugated() {
        return new IPQuaternion(
                -x, -y, -z, w
        );
    }

    /**
     * vector multiplication
     */
    public IPQuaternion multiply(double val) {
        return new IPQuaternion(
                x * val, y * val, z * val, w * val
        );
    }

    /**
     * vector add
     */
    public IPQuaternion add(IPQuaternion q) {
        return new IPQuaternion(
                x + q.x, y + q.y, z + q.z, w + q.w
        );
    }

    /**
     * vector dot product
     */
    public double dotProduct(IPQuaternion q) {
        return getX() * q.getX() +
                getY() * q.getY() +
                getZ() * q.getZ() +
                getW() * q.getW();
    }

    /**
     * quaternions are normalized 4D vectors
     */
    public IPQuaternion getNormalized() {
        double lenSq = dotProduct(this);
        if (lenSq != 0) {
            // no fastInverseSqrt. precision is the most important
            double len = Math.sqrt(lenSq);
            return this.multiply(1.0 / len);
        }
        else {
            return new IPQuaternion(0, 0, 0, 0);
        }
    }

    public static IPQuaternion getCameraRotation(double pitch, double yaw) {
        IPQuaternion r1 = rotationByDegrees(new Vec3d(1, 0, 0), pitch);
        IPQuaternion r2 = rotationByDegrees(new Vec3d(0, 1, 0), yaw + 180);
        IPQuaternion result = r1.hamiltonProduct(r2);
        return result;
    }

    // should be the same as the above
    public static IPQuaternion getCameraRotation1(double pitch, double yaw) {
        double p = Math.toRadians(pitch) / 2;
        double y = Math.toRadians(yaw) / 2;
        return new IPQuaternion(
                -Math.sin(p) * Math.sin(y),
                Math.cos(p) * Math.cos(y),
                Math.sin(p) * Math.cos(y),
                -Math.cos(p) * Math.sin(y)
        );
    }

    /**
     * Is two quaternions roughly the same
     */
    public static boolean isClose(IPQuaternion a, IPQuaternion b, double valve) {
        if (a.getW() * b.getW() < 0) {
            a = a.multiply(-1);
        }
        double da = a.getX() - b.getX();
        double db = a.getY() - b.getY();
        double dc = a.getZ() - b.getZ();
        double dd = a.getW() - b.getW();
        return da * da + db * db + dc * dc + dd * dd < valve;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IPQuaternion that = (IPQuaternion) o;
        return Double.compare(that.x, x) == 0 &&
                Double.compare(that.y, y) == 0 &&
                Double.compare(that.z, z) == 0 &&
                Double.compare(that.w, w) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, w);
    }

    @Override
    public String toString() {
        Vec3d rotatingAxis = getRotatingAxis();
        return String.format("Rotates %.3f degrees along (%.3f %.3f %.3f) Quaternion:(%.3f %.3f %.3f %.3f)",
                getRotatingAngleDegrees(), rotatingAxis.x, rotatingAxis.y, rotatingAxis.z, x, y, z, w
        );
    }

    /**
     * Interpolate the quaternion. It's the same as interpolating along a line in the 4D sphere surface
     */
    public static IPQuaternion interpolate(
            IPQuaternion a,
            IPQuaternion b,
            double t
    ) {

        double dot = a.dotProduct(b);

        if (dot < 0.0f) {
            a = a.multiply(-1);
            dot = -dot;
        }

        double DOT_THRESHOLD = 0.9995;
        if (dot > DOT_THRESHOLD) {
            // If the inputs are too close for comfort, linearly interpolate
            // and normalize the result.

            return a.multiply(1 - t).add(b.multiply(t)).getNormalized();
        }

        double theta_0 = Math.acos(dot);
        double theta = theta_0 * t;
        double sin_theta = Math.sin(theta);
        double sin_theta_0 = Math.sin(theta_0);

        double s0 = Math.cos(theta) - dot * sin_theta / sin_theta_0;
        double s1 = sin_theta / sin_theta_0;

        return a.multiply(s0).add(b.multiply(s1));
    }

    /**
     * The inverse of {@link IPQuaternion#getCameraRotation1(double, double)}
     * Also roughly works for non-camera transformation
     */
    public static Pair<Double, Double> getPitchYawFromRotation(IPQuaternion quaternion) {
        double x = quaternion.getX();
        double y = quaternion.getY();
        double z = quaternion.getZ();
        double w = quaternion.getW();

        double cosYaw = 2 * (y * y + z * z) - 1;
        double sinYaw = -(x * z + y * w) * 2;

        double cosPitch = 1 - 2 * (x * x + z * z);
        double sinPitch = (x * w + y * z) * 2;

        return new Pair<>(
                Math.toDegrees(Math.atan2(sinPitch, cosPitch)),
                Math.toDegrees(Math.atan2(sinYaw, cosYaw))
        );
    }

    // x, y, z are the 3 rows of the matrix
    // http://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/
    // only works if the matrix is rotation only
    public static IPQuaternion matrixToQuaternion(
            Vec3d x, Vec3d y, Vec3d z
    ) {
        double m00 = x.getX();
        double m11 = y.getY();
        double m22 = z.getZ();

        double m12 = z.getY();
        double m21 = y.getZ();

        double m20 = x.getZ();
        double m02 = z.getX();

        double m01 = y.getX();
        double m10 = x.getY();

        double tr = m00 + m11 + m22;

        double qx, qy, qz, qw;

        if (tr > 0) {
            double S = Math.sqrt(tr + 1.0) * 2; // S=4*qw
            qw = 0.25 * S;
            qx = (m21 - m12) / S;
            qy = (m02 - m20) / S;
            qz = (m10 - m01) / S;
        }
        else if ((m00 > m11) && (m00 > m22)) {
            double S = Math.sqrt(1.0 + m00 - m11 - m22) * 2; // S=4*qx
            qw = (m21 - m12) / S;
            qx = 0.25 * S;
            qy = (m01 + m10) / S;
            qz = (m02 + m20) / S;
        }
        else if (m11 > m22) {
            double S = Math.sqrt(1.0 + m11 - m00 - m22) * 2; // S=4*qy
            qw = (m02 - m20) / S;
            qx = (m01 + m10) / S;
            qy = 0.25 * S;
            qz = (m12 + m21) / S;
        }
        else {
            double S = Math.sqrt(1.0 + m22 - m00 - m11) * 2; // S=4*qz
            qw = (m10 - m01) / S;
            qx = (m02 + m20) / S;
            qy = (m12 + m21) / S;
            qz = 0.25 * S;
        }

        return new IPQuaternion(qx, qy, qz, qw);
    }

    public static IPQuaternion getRotationBetween(Vec3d from, Vec3d to) {
        from = from.normalize();
        to = to.normalize();
        Vec3d axis = from.crossProduct(to).normalize();
        double cos = from.dotProduct(to);
        double angle = Math.acos(cos);
        return IPQuaternion.rotationByRadians(axis, angle);
    }
}
