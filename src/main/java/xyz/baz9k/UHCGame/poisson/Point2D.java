package xyz.baz9k.UHCGame.poisson;

import org.bukkit.Location;
import org.bukkit.World;

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

    public double dist(Point2D q) {
        return dist(q.x, q.z);
    }
    public double dist(double X, double Z) {
        return Math.hypot(x - X, z - Z);
    }

    public Point2D add(Point2D q) {
        return add(q.x, q.z);
    }
    public Point2D add(double X, double Z) {
        return new Point2D(x + X, z + Z);
    }

    public Location loc(World w, double y) {
        return new Location(w, x, y, z);
    }
}
