package io.github.jd1378.persianfontfixer.rtl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RtlTextTest {

    private static String cps(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%04X", (int) text.charAt(i)));
        }
        return sb.toString();
    }

    // shaping (pure right-to-left lines are simply reversed after shaping)

    @Test
    void lamAlefLigature() {
        assertEquals("FEE1 FEFC FEB3", cps(RtlText.toVisual("سلام")));
    }

    @Test
    void persianLetters() {
        assertEquals("FEE5 FE8D FEAE FBFE FE8D", cps(RtlText.toVisual("ایران")));
        assertEquals("FE8F FE8E FE98 FB90", cps(RtlText.toVisual("کتاب")));
        assertEquals("FB92 FEAE FE92 FEE0 FB94", cps(RtlText.toVisual("گلبرگ")));
    }

    @Test
    void zwnjBreaksTheJoin() {
        assertEquals("FEA9 FEED FEAD FBFD FEE3", cps(RtlText.toVisual("می‌رود")));
    }

    @Test
    void allahShapesAsPlainLetters() {
        assertEquals("FEEA FEE0 FEDF FE8D", cps(RtlText.toVisual("الله")));
    }

    @Test
    void combiningMarksTrailTheirLetter() {
        String muhammad = "مُحَمَّد";
        assertEquals("FEAA FEE4 064E 0651 FEA4 064E FEE3 064F", cps(RtlText.toVisual(muhammad)));
    }

    @Test
    void otherArabicScriptLanguages() {
        assertEquals("ﻢﻜﻴﻠﻋ ﻡﻼﺴﻟﺍ", RtlText.toVisual("السلام عليكم")); // Arabic
        assertEquals("ﺮﭨﺎﻤﭨ", RtlText.toVisual("ٹماٹر"));               // Urdu
        assertEquals("ﻮﻟﻮﭜ", RtlText.toVisual("ڀولو"));                 // Sindhi
        assertEquals("ﺭﯘﻐﻳﯘﺋ", RtlText.toVisual("ئۇيغۇر"));            // Uyghur
    }

    @Test
    void formTableMatchesUnicode() {
        assertArrayEquals(new char[]{'ﺏ', 'ﺐ', 'ﺑ', 'ﺒ'}, ArabicShaper.formsOf('ب'));
        assertArrayEquals(new char[]{'ﺍ', 'ﺎ', 0, 0}, ArabicShaper.formsOf('ا'));
        assertArrayEquals(new char[]{'ﺁ', 'ﺂ', 0, 0}, ArabicShaper.formsOf('آ'));
        assertArrayEquals(new char[]{'ﮎ', 'ﮏ', 'ﮐ', 'ﮑ'}, ArabicShaper.formsOf('ک'));
        assertArrayEquals(new char[]{'ﯼ', 'ﯽ', 'ﯾ', 'ﯿ'}, ArabicShaper.formsOf('ی'));
        assertArrayEquals(new char[]{'ﮤ', 'ﮥ', 0, 0}, ArabicShaper.formsOf('ۀ'));
        assertNull(ArabicShaper.formsOf('a'));
        assertEquals("ﻵ", ArabicShaper.shape("لآ"));
        assertEquals("ﻼ", ArabicShaper.shape("بلا").substring(1));
    }

    // reordering

    @Test
    void pureLtrIsUntouched() {
        String line = "def f(): return 1";
        assertEquals(line, RtlText.toVisual(line));
        assertEquals("", RtlText.toVisual(""));
    }

    @Test
    void digitsKeepTheirOrder() {
        assertEquals("ﺩﻮﺑ ۱۴۰۳ ﻝﺎﺳ", RtlText.toVisual("سال ۱۴۰۳ بود"));
        assertEquals("ﺩﻮﺑ 1403 ﻝﺎﺳ", RtlText.toVisual("سال 1403 بود"));
    }

    @Test
    void hebrewIsOutOfScope() {
        assertEquals("שלום עולם", RtlText.toVisual("שלום עולם"));
    }

    @Test
    void mixedLineKeepsLatinInPlace() {
        assertEquals("hello ﺎﯿﻧﺩ ﻡﻼﺳ", RtlText.toVisual("hello سلام دنیا"));
        assertEquals("Steve ﻡﻼﺳ", RtlText.toVisual("سلام Steve")); // RTL base: first word sits rightmost
        assertEquals("Steve ﻡﻼﺳ", RtlText.toVisual("Steve سلام"));
    }

    @Test
    void rtlLineMirrorsBrackets() {
        assertEquals("(ﺖﺴﺗ)", RtlText.toVisual("(تست)"));
    }

    @Test
    void trailingPunctuationOfAnRtlLineMovesToTheLeft() {
        assertEquals("!ﻡﻼﺳ", RtlText.toVisual("سلام!"));
    }

    @Test
    void alreadyShapedInputIsOnlyReordered() {
        assertEquals("ﻡﻼﺳ", RtlText.toVisual("ﺳﻼﻡ"));
    }

    @Test
    void hasArabic() {
        assertTrue(RtlText.hasArabic("سلام"));
        assertTrue(RtlText.hasArabic("hi سلام"));
        assertFalse(RtlText.hasArabic("hi שלום"));
        assertFalse(RtlText.hasArabic("def f(): pass"));
        assertFalse(RtlText.hasArabic("۱۲۳")); // digits alone carry no direction
    }
}
