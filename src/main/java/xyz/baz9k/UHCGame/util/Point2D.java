package xyz.baz9k.UHCGame.util;

import org.bukkit.Location;
import org.bukkit.World;
import static xyz.baz9k.UHCGame.util.Utils.*;

public record Point2D(double x, double z) {

    /**
     * Get Euclidean distance between this point to the specified point.
     * @param q other point
     * @return the Euclidean distance
     */
    public double dist(Point2D q) {
        return dist(q.x, q.z);
    }

    /**
     * Get Euclidean distance between this point to the coordinates.
     * @param X other X
     * @param Z other Z
     * @return the Euclidean distance
     */
    public double dist(double X, double Z) {
        return Math.hypot(x - X, z - Z);
    }

    /**
     * Add two points like vectors.
     * @param q other point
     * @return a new point, the vector sum of the points
     */
    public Point2D plus(Point2D q) {
        return plus(q.x, q.z);
    }

    /**
     * Translate a point.
     * @param X x to translate
     * @param Z z to translate
     * @return a new point, the translation of this point
     */
    public Point2D plus(double X, double Z) {
        return new Point2D(x + X, z + Z);
    }

    /**
     * Subtract two points like vectors.
     * @param q other point
     * @return a new point, the difference between the two points
     */
    public Point2D minus(Point2D q) {
        return minus(q.x, q.z);
    }

    /**
     * Translate a point in the negative direction.
     * @param X x to translate back
     * @param Z z to translate back
     * @return a new point, the translation of this point
     */
    public Point2D minus(double X, double Z) {
        return plus(-X, -Z);
    }

    /**
     * Add to a point some polar point.
     * @param r radius
     * @param theta angle
     * @return a new point, the vector sum of the points
     */
    public Point2D addPolar(double r, double theta) {
        return plus(fromPolar(r, theta));
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
     * Get a point from the specified {@link Location}.
     * @param l Location
     * @return the location projected onto the XZ plane
     */
    public static Point2D fromLocation(Location l) {
        return new Point2D(l.getX(), l.getZ());
    }

    /**
     * Get a point from the specified radius and angle.
     * @param r radius
     * @param theta angle
     * @return a new Point2D, in rectangular coordinates
     */
    public static Point2D fromPolar(double r, double theta) {
        return new Point2D(r * Math.cos(theta), r * Math.sin(theta));
    }
    
    /**
     * Check if this point is inside the square with specified center and side length.
     * @param center Center of square to check
     * @param sideLength Side length of square to check
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
     * @param minX Lowest x coordinate
     * @param maxX Highest x coordinate
     * @param minZ Lowest z coordinate
     * @param maxZ Highest z coordinate
     * @return true/false
     */
    public boolean inSquare(double minX, double maxX, double minZ, double maxZ) {
        return (minX < x && x < maxX && minZ < z && z < maxZ);
    }

    /**
     * Generate a random point between the bounding <i>rectangle</i>.
     * @param minX Lowest x coordinate
     * @param maxX Highest x coordinate
     * @param minZ Lowest z coordinate
     * @param maxZ Highest z coordinate
     * @return a new point in the bounding rectangle
     */
    public static Point2D uniformRand(double minX, double maxX, double minZ, double maxZ) {
        double X = rand(minX, maxX);
        double Z = rand(minZ, maxZ);
        return new Point2D(X, Z);
    }

    /**
     * Generate a random point between the bounding square with specified center and side length.
     * @param center Center of rectangle
     * @param sideLength Side length of rectangle
     * @return a new point in the bounding rectangle
     */
    public static Point2D uniformRand(Point2D center, double sideLength) {
        double minX = center.x - (sideLength/2);
        double maxX = center.x + (sideLength/2);
        double minZ = center.z - (sideLength/2);
        double maxZ = center.z + (sideLength/2);
        return uniformRand(minX, maxX, minZ, maxZ);
    }


    /**
     * Generate a random point within an annulus around a given center.
     * @param center Center of ring
     * @param minRadius Minimum radius of ring
     * @param maxRadius Maximum radius of ring
     * @return a new point in the bounding ring
     */
    public static Point2D ringRand(Point2D center, double minRadius, double maxRadius) {
        double theta = rand(0, 2 * Math.PI);
        double radius = Math.sqrt(rand(Math.pow(minRadius, 2), Math.pow(maxRadius, 2)));
        return center.addPolar(radius, theta);
    }
}