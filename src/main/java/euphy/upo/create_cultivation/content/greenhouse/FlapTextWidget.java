package euphy.upo.create_cultivation.content.greenhouse;

import java.util.Arrays;
import java.util.Random;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Flap-display text widget for the greenhouse controller GUI, mirroring
 * Create's {@code FlapDisplaySection}: the text is laid out in fixed-width
 * monospace slots; on change every altered slot "flips" - it cycles random
 * characters from a cycle pool with per-slot phase offsets, stops with a
 * rising probability, and the spin infects neighbouring slots for the wave
 * feel - until the new text settles.
 */
public class FlapTextWidget {

    public static final float MONOSPACE = 7.0f;

    private static final Random RANDOM = new Random();

    private final int slots;
    private final int color;
    private final boolean centered;
    private final String cyclePool;
    /** True = fixed 7px digit cells; false = label mode with natural per-character widths. */
    private final boolean monospace;

    private char[] text;
    private final boolean[] spinning;
    private int spinningTicks;
    private int maxSpinTicks;
    /** Fixed short wave length for the ceremonial first fill. */
    private static final int FIRST_WAVE_TICKS = 5;
    /** False until the first setText; selects the short first wave. */
    private boolean everFilled;
    /** Hard cap of the current wave (first wave is short, later ones full). */
    private int waveCap;

    public FlapTextWidget(int x, int y, int slots, int color, String cyclePool, boolean centered) {
        this(x, y, slots, color, cyclePool, centered, true);
    }

    /**
     * Label mode: one slot per character with the cell width taken from the
     * settled character itself, so the rendered width equals
     * {@link Font#width(String)} of the label and existing layouts hold. The
     * flip cycle pool is built from the label's own characters.
     */
    public static FlapTextWidget forLabel(String text, int color) {
        StringBuilder pool = new StringBuilder();
        text.chars().distinct().forEach(c -> pool.append((char) c));
        String cyclePool = pool.length() > 0 ? pool.toString() : " ";
        return new FlapTextWidget(0, 0, text.length(), color, cyclePool, false, false);
    }

    private FlapTextWidget(int x, int y, int slots, int color, String cyclePool, boolean centered, boolean monospace) {
        this.slots = slots;
        this.color = color;
        this.centered = centered;
        this.cyclePool = cyclePool;
        this.monospace = monospace;
        this.text = new char[slots];
        Arrays.fill(this.text, ' ');
        this.spinning = new boolean[slots];
        this.maxSpinTicks = Math.max(4, (int) (cyclePool.length() * 1.75));
        this.waveCap = this.maxSpinTicks;
    }

    /** Diff-based update: only changed slots start flipping. */
    public void setText(String newText, boolean animate) {
        char[] padded = pad(newText);
        if (!everFilled) {
            // ceremonial first fill: every visible slot flips for a short,
            // fixed wave instead of the full random settle
            waveCap = FIRST_WAVE_TICKS;
            spinningTicks = 0;
            for (int i = 0; i < slots; i++)
                spinning[i] = padded[i] != ' ';
        } else if (animate) {
            boolean any = false;
            for (int i = 0; i < slots; i++) {
                if (padded[i] != text[i]) {
                    spinning[i] = true;
                    any = true;
                }
            }
            if (any) {
                spinningTicks = 0;
                waveCap = maxSpinTicks;
            }
        } else {
            Arrays.fill(spinning, false);
        }
        this.text = padded;
        everFilled = true;
    }

    /** Called once per GUI tick; drives the settle-down of spinning slots. */
    public void tick() {
        boolean any = false;
        for (boolean b : spinning)
            if (b) {
                any = true;
                break;
            }
        if (!any)
            return;

        spinningTicks++;
        // hard cap of the current wave (5 ticks first fill, full wave later)
        if (spinningTicks >= waveCap) {
            Arrays.fill(spinning, false);
            return;
        }
        int increasingChance = Mth.clamp(8 - spinningTicks, 1, 10);
        for (int i = 0; i < slots; i++) {
            if (!spinning[i])
                continue;
            boolean continueSpin = RANDOM.nextInt(increasingChance * maxSpinTicks / 4) != 0;
            // the slot itself settles exactly when the roll says so
            spinning[i] = continueSpin;
            // infection: only a slot that keeps spinning may pull neighbours
            if (continueSpin && RANDOM.nextInt(6) < 2) {
                if (i > 0)
                    spinning[i - 1] = true;
                if (i < slots - 1)
                    spinning[i + 1] = true;
            }
        }
    }

    /**
     * Draws at an absolute screen position. When {@code centered} the text is
     * centered on {@code x + slots * MONOSPACE / 2}.
     */
    public void renderAt(GuiGraphics gui, Font font, int x, int y) {
        int cx = x;
        if (centered && monospace) {
            cx = x + (int) (slots * MONOSPACE / 2f) - (slots * (int) MONOSPACE) / 2;
        }
        for (int i = 0; i < slots; i++) {
            char c = text[i];
            // the cell width always follows the settled character, so the
            // board never jitters while a slot flips through the pool
            int cell = monospace ? (int) MONOSPACE : font.width(String.valueOf(c));
            if (spinning[i]) {
                // per-slot phase (same trick as FlapDisplayRenderer)
                int phase = (int) ((spinningTicks * 1.0f + i * 2.3f) * 0.5f) % cyclePool.length();
                c = cyclePool.charAt(phase);
            }
            String s = String.valueOf(c);
            int w = font.width(s);
            int charX = monospace
                    ? Mth.floor(cx + i * MONOSPACE + (MONOSPACE - w) / 2f)
                    : cx + (cell - w) / 2;
            gui.drawString(font, s, charX, y, spinning[i] ? dim(color) : color, false);
            if (!monospace) {
                cx += cell;
            }
        }
    }

    /** Rendered width in pixels: the 7px cell grid, or the settled label's natural width. */
    public int width(Font font) {
        if (monospace) {
            return (int) (slots * MONOSPACE);
        }
        int w = 0;
        for (int i = 0; i < slots; i++) {
            w += font.width(String.valueOf(text[i]));
        }
        return w;
    }

    private static int dim(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (((argb >>> 16) & 0xFF) * 3) / 4;
        int g = (((argb >>> 8) & 0xFF) * 3) / 4;
        int b = ((argb & 0xFF) * 3) / 4;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private char[] pad(String s) {
        char[] out = new char[slots];
        Arrays.fill(out, ' ');
        int n = Math.min(slots, s.length());
        for (int i = 0; i < n; i++) {
            out[i] = s.charAt(i);
        }
        return out;
    }
}
