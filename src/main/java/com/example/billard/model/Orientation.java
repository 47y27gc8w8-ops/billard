package com.example.billard.model;

/**
 * How a ball is turned in space, as a unit quaternion. World axes: x to the right and y down the table, as on
 * screen, and z up out of the cloth toward the viewer. On the ball itself, z points at the face with its number.
 */
public record Orientation(double w, double x, double y, double z) {

    /** Number face up, toward the viewer. */
    public static final Orientation UPRIGHT = new Orientation(1, 0, 0, 0);

    /** Turned as a ball does when it rolls over {@code travel} on the cloth without slipping. */
    public Orientation rolled(Vec2 travel, double radius) {
        double distance = travel.length();
        if (distance == 0) {
            return this;
        }
        // The axis lies flat on the cloth, square to the travel: up × travel.
        return turnedAbout(-travel.y() / distance, travel.x() / distance, 0, distance / radius);
    }

    /** Turned about the vertical axis; positive angles turn clockwise on screen. */
    public Orientation spun(double radians) {
        return radians == 0 ? this : turnedAbout(0, 0, 1, radians);
    }

    /**
     * The rotation as a row-major 3×3 matrix that takes directions fixed to the ball into the world. Column 2 is
     * where the number face points; entry 8 above 0 means it faces the viewer.
     */
    public double[] matrix() {
        return new double[]{
                1 - 2 * (y * y + z * z), 2 * (x * y - w * z), 2 * (x * z + w * y),
                2 * (x * y + w * z), 1 - 2 * (x * x + z * z), 2 * (y * z - w * x),
                2 * (x * z - w * y), 2 * (y * z + w * x), 1 - 2 * (x * x + y * y)};
    }

    /** This rotation followed by one of {@code angle} radians about a unit world axis. */
    private Orientation turnedAbout(double ax, double ay, double az, double angle) {
        double half = angle / 2;
        double s = Math.sin(half);
        double qw = Math.cos(half);
        double qx = ax * s;
        double qy = ay * s;
        double qz = az * s;
        return new Orientation(
                qw * w - qx * x - qy * y - qz * z,
                qw * x + qx * w + qy * z - qz * y,
                qw * y - qx * z + qy * w + qz * x,
                qw * z + qx * y - qy * x + qz * w).normalized();
    }

    /** Rescaled to unit length, so rounding errors from many small turns do not build up. */
    private Orientation normalized() {
        double length = Math.sqrt(w * w + x * x + y * y + z * z);
        return new Orientation(w / length, x / length, y / length, z / length);
    }
}
