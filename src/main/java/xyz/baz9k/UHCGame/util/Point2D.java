package xyz.baz9k.UHCGame.util;

import org.bukkit.Location;
import org.bukkit.World;
import static xyz.baz9k.UHCGame.util.Utils.*;

public class Point2D {
    private final double x;
    private final double z;

    public Point2D(double x, double z) {
        this.x = x;
        this.z = z;
    }

    public double x() { return x; }
    public double z() { return z; }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof Point2D)) return false;
        Point2D p = (Point2D) o;
        return x == p.x && z == p.z;
    }

    public String toString() {
        return String.format("Point2D[%s, %s]", x, z);
    }

    /**
     * Get Euclidean distance between this point to the specified point.
     * @param q
     * @return the Euclidean distance
     */
    public double dist(Point2D q) {
        return dist(q.x, q.z);
    }

    /**
     * Get Euclidean distance between this point to the coordinates.
     * @param X
     * @param Z
     * @return the Euclidean distance
     */
    public double dist(double X, double Z) {
        return Math.hypot(x - X, z - Z);
    }

    /**
     * Add two points like vectors.
     * @param q
     * @return a new Point2D, which is a vector sum of this point and the specified point.
     */
    public Point2D add(Point2D q) {
        return add(q.x, q.z);
    }

    /**
     * Translate a point.
     * @param X
     * @param Z
     * @return a new Point2D, which has the specified coordinates added.
     */
    public Point2D add(double X, double Z) {
        return new Point2D(x + X, z + Z);
    }

    /**
     * Convert to a Bukkit {@link Location}.
     * @param w World of location
     * @param y y-coordinate of location
     * @return the location
     */
    public Location loc(World w, double y) {
        return new Location(w, x, y, z);
    }

    /**
     * Get a point from the specified radius and angle.
     * @param r
     * @param theta
     * @return a new Point2D, in rectangular coordinates
     */
    public static Point2D fromPolar(double r, double theta) {
        return new Point2D(r * Math.cos(theta), r * Math.sin(theta));
    }
    
    /**
     * Check if this point is inside the square with specified center and side length.
     * @param center
     * @param sideLength
     * @return true/false
     */
    public boolean inSquare(Point2D center, double sideLength) {
        double minX = center.x - (sideLength/2);
        double maxX = center.x + (sideLength/2);
        double minZ = center.z - (sideLength/2);
        double maxZ = center.z + (sideLength/2);
        return inSquare(minX, maxX, minZ, maxZ);
    }
    
    /**
     * Check if this point is inside the <i>rectangle</i> bounded by the min and max coordinates.
     * @param minX
     * @param maxX
     * @param minZ
     * @param maxZ
     * @return true/false
     */
    public boolean inSquare(double minX, double maxX, double minZ, double maxZ) {
        return (minX < x && x < maxX && minZ < z && z < maxZ);
    }

    /**
     * Generate a random point between the bounding <i>rectangle</i>.
     * @param minX
     * @param maxX
     * @param minZ
     * @param maxZ
     * @return a new Point2D in the bounding rectangle
     */
    public static Point2D uniformRand(double minX, double maxX, double minZ, double maxZ) {
        double X = rand(minX, maxX);
        double Z = rand(minZ, maxZ);
        return new Point2D(X, Z);
    }

    /**
     * Generate a random point between the bounding square with specified center and side length.
     * @param center
     * @param sideLength
     * @return a new Point2D in the bounding rectangle
     */
    public static Point2D uniformRand(Point2D center, double sideLength) {
        double minX = center.x - (sideLength/2);
        double maxX = center.x + (sideLength/2);
        double minZ = center.z - (sideLength/2);
        double maxZ = center.z + (sideLength/2);
        return uniformRand(minX, maxX, minZ, maxZ);
    }


    /**
     * Generate a random point with a distance between the two specified radii from the center.
     * @param center
     * @param minRadius
     * @param maxRadius
     * @return a new Point2D in the bounding ring
     */
    public static Point2D ringRand(Point2D center, double minRadius, double maxRadius) {
        double theta = rand(0, 2 * Math.PI);
        double radius = rand(minRadius, maxRadius);
        return center.add(fromPolar(radius, theta));
    }
}