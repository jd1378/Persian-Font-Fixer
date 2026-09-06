package io.github.jd1378.persianfontfixer.rtl;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

/**
 * Contextual shaping: base letters to Arabic Presentation Forms (joined glyphs).
 *
 * The form table is derived from the JDK's own Unicode data rather than transcribed:
 * every presentation form's name says which slot it fills, and its compatibility
 * decomposition says which letter it stands for. That covers Arabic, Persian, Urdu,
 * Sindhi, Uyghur and the rest without a hand-written list.
 */
final class ArabicShaper {
    static final char ZWNJ = '\u200C';
    static final char ZWJ = '\u200D';
    static final char LAM = 'ل';
    private static final char TATWEEL = 'ـ';

    private static final int ISOLATED = 0, FINAL = 1, INITIAL = 2, MEDIAL = 3;

    /** base letter to {isolated, final, initial, medial}; 0 where the form does not exist. */
    private static final Map<Character, char[]> FORMS = new HashMap<Character, char[]>();
    /** alef variant to {isolated, final} of its lam-alef ligature. */
    private static final Map<Character, char[]> LAM_ALEF = new HashMap<Character, char[]>();

    static {
        int[][] ranges = {{0xFB50, 0xFDFF}, {0xFE70, 0xFEFF}};
        for (int[] range : ranges) {
            for (int cp = range[0]; cp <= range[1]; cp++) {
                String name = Character.getName(cp);
                int slot = name == null ? -1 : slotOf(name);
                if (slot < 0) {
                    continue;
                }
                String base = logicalOf((char) cp);
                if (base.length() == 1) {
                    put(FORMS, base.charAt(0), 4, slot, (char) cp);
                } else if (base.length() == 2 && base.charAt(0) == LAM && slot <= FINAL && isAlef(base.charAt(1))) {
                    put(LAM_ALEF, base.charAt(1), 2, slot, (char) cp);
                }
            }
        }
        // Tatweel has no presentation forms but joins on both sides; it draws as itself.
        FORMS.put(TATWEEL, new char[]{TATWEEL, TATWEEL, TATWEEL, TATWEEL});
    }

    private ArabicShaper() {
    }

    private static int slotOf(String name) {
        if (name.endsWith("ISOLATED FORM")) return ISOLATED;
        if (name.endsWith("FINAL FORM")) return FINAL;
        if (name.endsWith("INITIAL FORM")) return INITIAL;
        if (name.endsWith("MEDIAL FORM")) return MEDIAL;
        return -1;
    }

    /** NFKD strips the form tag but also splits hamza/madda letters; NFC puts those back. */
    private static String logicalOf(char form) {
        String decomposed = Normalizer.normalize(String.valueOf(form), Normalizer.Form.NFKD);
        return Normalizer.normalize(decomposed, Normalizer.Form.NFC);
    }

    private static boolean isAlef(char c) {
        return c == 'ا' || c == 'آ' || c == 'أ' || c == 'إ';
    }

    private static void put(Map<Character, char[]> table, char key, int size, int slot, char form) {
        char[] forms = table.get(key);
        if (forms == null) {
            forms = new char[size];
            table.put(key, forms);
        }
        if (forms[slot] == 0) {
            forms[slot] = form;
        }
    }

    static boolean isMark(char c) {
        return Character.getDirectionality(c) == Character.DIRECTIONALITY_NONSPACING_MARK;
    }

    static boolean isShapeable(char c) {
        return FORMS.containsKey(c);
    }

    /** Package-private for tests: the four forms of a letter, or null. */
    static char[] formsOf(char letter) {
        char[] forms = FORMS.get(letter);
        return forms == null ? null : forms.clone();
    }

    private static boolean joinsBackward(char c) {
        if (c == ZWJ) {
            return true;
        }
        char[] forms = FORMS.get(c);
        return forms != null && forms[FINAL] != 0;
    }

    private static int nextLetterIndex(String text, int i) {
        int n = text.length();
        while (i < n && isMark(text.charAt(i))) {
            i++;
        }
        return i;
    }

    /** Replace every letter with the presentation form its neighbours call for. Zero-width joiners are consumed. */
    static String shape(String text) {
        int n = text.length();
        StringBuilder out = new StringBuilder(n);
        boolean prevForward = false; // can the previous letter connect to this one?
        int i = 0;
        while (i < n) {
            char c = text.charAt(i);

            if (isMark(c)) {
                out.append(c);
                i++;
                continue;
            }
            if (c == ZWNJ || c == ZWJ) {
                prevForward = c == ZWJ;
                i++;
                continue;
            }

            char[] forms = FORMS.get(c);
            if (forms == null) {
                out.append(c);
                prevForward = false;
                i++;
                continue;
            }

            int j = nextLetterIndex(text, i + 1);
            char next = j < n ? text.charAt(j) : 0;

            if (c == LAM && next != 0 && LAM_ALEF.containsKey(next)) {
                char[] ligature = LAM_ALEF.get(next);
                out.append(prevForward && ligature[FINAL] != 0 ? ligature[FINAL] : ligature[ISOLATED]);
                out.append(text, i + 1, j); // marks that sat between lam and the alef
                prevForward = false;        // an alef never connects forward
                i = j + 1;
                continue;
            }

            boolean nextBackward = next != 0 && joinsBackward(next);
            if (prevForward && nextBackward && forms[MEDIAL] != 0) {
                out.append(forms[MEDIAL]);
            } else if (prevForward && forms[FINAL] != 0) {
                out.append(forms[FINAL]);
            } else if (nextBackward && forms[INITIAL] != 0) {
                out.append(forms[INITIAL]);
            } else {
                out.append(forms[ISOLATED]);
            }
            prevForward = forms[INITIAL] != 0;
            i++;
        }
        return out.toString();
    }
}
