package io.github.jd1378.persianfontfixer.rtl;

import java.text.Bidi;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Logical text to what a renderer that neither shapes nor reorders must draw.
 *
 * Minecraft's font renderer (with a left-to-right language selected) draws every code point
 * as an isolated glyph, left to right. {@link #toVisual} does both missing jobs up front:
 * contextual shaping, then the Unicode Bidirectional Algorithm's reordering.
 *
 * The output is display-only and must never be stored as the message.
 */
public final class RtlText {
    private static final int CACHE_SIZE = 512;

    private static final Map<Character, Character> MIRRORED = new HashMap<Character, Character>();

    static {
        String pairs = "()[]{}<>«»‹›";
        for (int i = 0; i < pairs.length(); i += 2) {
            MIRRORED.put(pairs.charAt(i), pairs.charAt(i + 1));
            MIRRORED.put(pairs.charAt(i + 1), pairs.charAt(i));
        }
    }

    @SuppressWarnings("serial")
    private static final Map<String, String> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, String>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > CACHE_SIZE;
                }
            });

    private RtlText() {
    }

    /** Whether the text contains any Arabic-script letter (Persian, Arabic and the languages sharing their script). */
    public static boolean hasArabic(CharSequence text) {
        for (int i = 0, n = text.length(); i < n; i++) {
            if (Character.getDirectionality(text.charAt(i)) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return true;
            }
        }
        return false;
    }

    /** One line, logical order, to the visual form. Text without Arabic-script letters is returned as-is. */
    public static String toVisual(String line) {
        if (line.isEmpty() || !hasArabic(line)) {
            return line;
        }
        String cached = CACHE.get(line);
        if (cached == null) {
            cached = compose(line);
            CACHE.put(line, cached);
        }
        return cached;
    }

    private static String compose(String line) {
        String shaped = ArabicShaper.shape(line);
        int n = shaped.length();
        Bidi bidi = new Bidi(shaped, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);

        byte[] levels = new byte[n];
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            levels[i] = (byte) bidi.getLevelAt(i);
            order[i] = i;
        }
        Bidi.reorderVisually(levels, 0, order, 0, n);
        restoreMarkOrder(shaped, levels, order);

        StringBuilder out = new StringBuilder(n);
        for (int v = 0; v < n; v++) {
            int k = order[v];
            char c = shaped.charAt(k);
            if ((levels[k] & 1) == 1) {
                Character mirror = MIRRORED.get(c);
                if (mirror != null) {
                    c = mirror;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * Reversing a right-to-left run puts each combining mark in front of its letter. A renderer
     * that draws marks as standalone glyphs would then show them on the wrong side, so every
     * mark group is flipped back to trail the letter it belongs to.
     */
    private static void restoreMarkOrder(String shaped, byte[] levels, Integer[] order) {
        int n = order.length;
        int i = 0;
        while (i < n) {
            if (isRtlMark(shaped, levels, order[i])) {
                int j = i;
                while (j < n && isRtlMark(shaped, levels, order[j])) {
                    j++;
                }
                if (j < n && (levels[order[j]] & 1) == 1) {
                    j++; // take in the base letter
                    reverse(order, i, j);
                }
                i = j;
            } else {
                i++;
            }
        }
    }

    private static boolean isRtlMark(String shaped, byte[] levels, int k) {
        return (levels[k] & 1) == 1 && ArabicShaper.isMark(shaped.charAt(k));
    }

    private static void reverse(Integer[] a, int from, int to) {
        for (int lo = from, hi = to - 1; lo < hi; lo++, hi--) {
            Integer t = a[lo];
            a[lo] = a[hi];
            a[hi] = t;
        }
    }
}
