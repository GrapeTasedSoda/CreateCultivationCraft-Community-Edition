package euphy.upo.create_cultivation.content.greenhouse;

import euphy.upo.create_cultivation.content.climate.ClimateUnits;
import euphy.upo.create_cultivation.infrastructure.network.GreenhouseSnapshotPayload;

import net.minecraft.util.Mth;

/**
 * Server-authoritative crop-optimum setpoint solver. Weighted scan over the
 * climate scale in 0.5 steps: every crop row votes with its plant count on
 * the steps inside its optimal interval; the highest achievable vote total is
 * located first, then the longest plateau of that total, and its centre is
 * returned as the setpoint (x10 fixed point). Mirrors the interaction of the
 * old client-side preview so both sides agree.
 */
public final class CropOptimumSolver {

    private CropOptimumSolver() {}

    /** Best temperature setpoint (x10), or {@code Integer.MIN_VALUE} if no crop votes. */
    public static int bestTempC10(GreenhouseSnapshotPayload.CropRow[] rows) {
        return solve(rows, true, ClimateUnits.TEMP_C_MIN, ClimateUnits.TEMP_C_MAX);
    }

    /** Best humidity setpoint (x10), or {@code Integer.MIN_VALUE} if no crop votes. */
    public static int bestHum10(GreenhouseSnapshotPayload.CropRow[] rows) {
        return solve(rows, false, ClimateUnits.HUMIDITY_MIN, ClimateUnits.HUMIDITY_MAX);
    }

    private static int solve(GreenhouseSnapshotPayload.CropRow[] rows, boolean temp, float lo, float hi) {
        int steps = Math.round((hi - lo) / 0.5f);
        int[] weight = new int[steps + 1];
        boolean anyVote = false;
        for (GreenhouseSnapshotPayload.CropRow row : rows) {
            int optMin = temp ? row.tempOptMin() : row.humOptMin();
            int optMax = temp ? row.tempOptMax() : row.humOptMax();
            if (optMin == Integer.MIN_VALUE || optMax == Integer.MIN_VALUE)
                continue;
            anyVote = true;
            int a = Mth.clamp((int) Math.ceil((optMin / 10f - lo) / 0.5f), 0, steps);
            int b = Mth.clamp((int) Math.floor((optMax / 10f - lo) / 0.5f), 0, steps);
            for (int s = a; s <= b; s++)
                weight[s] += Math.max(1, row.count());
        }
        if (!anyVote)
            return Integer.MIN_VALUE;

        int best = 0;
        for (int w : weight)
            best = Math.max(best, w);

        // longest contiguous plateau at the best score; centre of it wins
        int runStart = -1, runLen = 0, bestStart = 0, bestLen = 0;
        for (int s = 0; s <= steps; s++) {
            if (weight[s] == best) {
                if (runStart < 0)
                    runStart = s;
                runLen = s - runStart + 1;
                if (runLen > bestLen) {
                    bestLen = runLen;
                    bestStart = runStart;
                }
            } else {
                runStart = -1;
                runLen = 0;
            }
        }
        int centre = bestStart + (bestLen - 1) / 2;
        return Math.round((lo + centre * 0.5f) * 10);
    }
}
