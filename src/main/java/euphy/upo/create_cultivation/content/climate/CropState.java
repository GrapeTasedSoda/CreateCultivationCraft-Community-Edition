package euphy.upo.create_cultivation.content.climate;

/**
 * Growth modifier a crop receives under a climate (greenhouse or ambient).
 * The ladder is: both dimensions optimal &gt; exactly one optimal &gt; both
 * merely surviving &gt; at least one outside its survival range.
 */
public enum CropState {
    /** Inside both optimal ranges: the highest growth and yield bonus. */
    OPTIMAL,
    /** Exactly one dimension optimal, the other inside survival: reduced bonus. */
    PARTIAL,
    /** Inside both survival ranges but neither optimal: no bonus, still grows. */
    SURVIVAL_ONLY,
    /** Outside at least one survival range: the crop does not grow at all. */
    FAIL
}
