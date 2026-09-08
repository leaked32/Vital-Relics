package com.example.vitalrelics.common;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Version-independent selection; adapters retain the actual player's mining context. */
public final class MiningSkills {
    private MiningSkills() {}
    public record Pos(int x, int y, int z) {
        public Pos offset(int x, int y, int z) { return new Pos(this.x + x, this.y + y, this.z + z); }
        public double distanceSquared(Pos other) {
            double dx = x - other.x, dy = y - other.y, dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }
    public interface Context {
        boolean valid();
        boolean isLog(Pos pos);
        boolean breakBlock(Pos pos);
    }
    public static final int MAX_BLOCKS = 512;
    public static final double MAX_AREA_RADIUS = 8;

    public static void apply(Context context, Pos origin, boolean tree, double areaRange) {
        Set<Pos> attempted = new HashSet<>();
        attempted.add(origin);
        int count = 0;
        if (tree) {
            ArrayDeque<Pos> queue = new ArrayDeque<>();
            queue.add(origin);
            while (!queue.isEmpty() && count < MAX_BLOCKS && context.valid()) {
                Pos current = queue.removeFirst();
                for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                    Pos next = current.offset(x, y, z);
                    // Bound connected log traversal; foliage decays naturally after the logs are removed.
                    if (next.distanceSquared(origin) > 32 * 32 || !attempted.add(next) || !context.isLog(next)) continue;
                    if (count >= MAX_BLOCKS || !context.valid()) return;
                    count++;
                    if (context.breakBlock(next)) queue.addLast(next);
                }
            }
        }
        if (!Double.isFinite(areaRange) || areaRange <= 0) return;
        double radius = Math.min(areaRange, MAX_AREA_RADIUS);
        int extent = (int) Math.ceil(radius);
        // Nearest blocks first, with no block-state/type restriction.
        java.util.List<Pos> area = new java.util.ArrayList<>();
        for (int x = -extent; x <= extent; x++) for (int y = -extent; y <= extent; y++) for (int z = -extent; z <= extent; z++) {
            Pos next = origin.offset(x, y, z);
            if (next.distanceSquared(origin) <= radius * radius && !next.equals(origin)) area.add(next);
        }
        area.sort(java.util.Comparator.comparingDouble(origin::distanceSquared));
        for (Pos next : area) {
            if (count >= MAX_BLOCKS || !context.valid()) return;
            // Tree traversal visited nonlogs too; only skip logs already removed, via native air check.
            if (context.breakBlock(next)) count++;
        }
    }
}
