package com.example.billard.ui;

/**
 * Smooth, deterministic 2D value noise that repeats every {@code periodX} by {@code periodY} lattice cells, so
 * textures built from it tile without seams.
 */
final class ValueNoise {

    private final long seed;
    private final int periodX;
    private final int periodY;

    ValueNoise(long seed, int periodX, int periodY) {
        if (periodX < 1 || periodY < 1) {
            throw new IllegalArgumentException("Noise periods must be positive: " + periodX + " x " + periodY);
        }
        this.seed = seed;
        this.periodX = periodX;
        this.periodY = periodY;
    }

    /** Noise at a point in lattice units, between 0 and 1. */
    double at(double x, double y) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = smooth(x - x0);
        double fy = smooth(y - y0);
        double top = lerp(lattice(x0, y0), lattice(x0 + 1, y0), fx);
        double bottom = lerp(lattice(x0, y0 + 1), lattice(x0 + 1, y0 + 1), fx);
        return lerp(top, bottom, fy);
    }

    /** A random value between 0 and 1 for one lattice point, the same every time for the same seed. */
    double lattice(int x, int y) {
        long hash = seed;
        hash = mix(hash + Math.floorMod(x, periodX) * 0x9E3779B97F4A7C15L);
        hash = mix(hash + Math.floorMod(y, periodY) * 0xC2B2AE3D27D4EB4FL);
        return (hash >>> 11) * 0x1.0p-53;
    }

    private static long mix(long value) {
        long z = value;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
