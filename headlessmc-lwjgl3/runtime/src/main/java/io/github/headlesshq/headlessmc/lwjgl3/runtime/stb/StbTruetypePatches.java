package io.github.headlesshq.headlessmc.lwjgl3.runtime.stb;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackRange;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Patches for {@code STBTruetype}: every font loads and packs, with the metrics of a font with 2048 units per em,
 * whose glyphs are half an em wide, e.g. for the early loading screens of Forge and NeoForge.
 */
public final class StbTruetypePatches {
    private static final String STB_TRUETYPE = "org/lwjgl/stb/STBTruetype";
    private static final int UNITS_PER_EM = 2048;
    private static final int ASCENT = 1638;
    private static final int DESCENT = ASCENT - UNITS_PER_EM;
    private static final float ADVANCE = 0.5f;

    private StbTruetypePatches() {
        throw new AssertionError();
    }

    @Patch(owner = STB_TRUETYPE)
    public static boolean stbtt_InitFont(STBTTFontinfo info, ByteBuffer data) {
        return true;
    }

    @Patch(owner = STB_TRUETYPE)
    public static boolean stbtt_InitFont(STBTTFontinfo info, ByteBuffer data, int offset) {
        return true;
    }

    @Patch(owner = STB_TRUETYPE)
    public static int stbtt_GetNumberOfFonts(ByteBuffer data) {
        return 1;
    }

    @Patch(owner = STB_TRUETYPE)
    public static int stbtt_GetFontOffsetForIndex(ByteBuffer data, int index) {
        return index == 0 ? 0 : -1;
    }

    @Patch(owner = STB_TRUETYPE)
    public static int stbtt_FindGlyphIndex(STBTTFontinfo info, int unicode_codepoint) {
        return unicode_codepoint;
    }

    @Patch(owner = STB_TRUETYPE)
    public static float stbtt_ScaleForPixelHeight(STBTTFontinfo info, float pixels) {
        return pixels / UNITS_PER_EM;
    }

    @Patch(owner = STB_TRUETYPE)
    public static float stbtt_ScaleForMappingEmToPixels(STBTTFontinfo info, float pixels) {
        return pixels / UNITS_PER_EM;
    }

    @Patch(owner = STB_TRUETYPE)
    public static void stbtt_GetFontVMetrics(STBTTFontinfo info, IntBuffer ascent, IntBuffer descent, IntBuffer lineGap) {
        put(ascent, ASCENT);
        put(descent, DESCENT);
        put(lineGap, 0);
    }

    @Patch(owner = STB_TRUETYPE)
    public static void stbtt_GetFontVMetrics(STBTTFontinfo info, int[] ascent, int[] descent, int[] lineGap) {
        put(ascent, ASCENT);
        put(descent, DESCENT);
        put(lineGap, 0);
    }

    @Patch(owner = STB_TRUETYPE)
    public static void stbtt_GetCodepointHMetrics(STBTTFontinfo info, int codepoint, IntBuffer advanceWidth, IntBuffer leftSideBearing) {
        put(advanceWidth, (int) (UNITS_PER_EM * ADVANCE));
        put(leftSideBearing, 0);
    }

    @Patch(owner = STB_TRUETYPE)
    public static void stbtt_GetCodepointHMetrics(STBTTFontinfo info, int codepoint, int[] advanceWidth, int[] leftSideBearing) {
        put(advanceWidth, (int) (UNITS_PER_EM * ADVANCE));
        put(leftSideBearing, 0);
    }

    @Patch(owner = STB_TRUETYPE)
    public static void stbtt_GetScaledFontVMetrics(ByteBuffer fontdata, int index, float size, FloatBuffer ascent,
                                                   FloatBuffer descent, FloatBuffer lineGap) {
        put(ascent, size * ASCENT / UNITS_PER_EM);
        put(descent, size * DESCENT / UNITS_PER_EM);
        put(lineGap, 0.0f);
    }

    @Patch(owner = STB_TRUETYPE)
    public static void stbtt_GetScaledFontVMetrics(ByteBuffer fontdata, int index, float size, float[] ascent,
                                                   float[] descent, float[] lineGap) {
        put(ascent, size * ASCENT / UNITS_PER_EM);
        put(descent, size * DESCENT / UNITS_PER_EM);
        put(lineGap, 0.0f);
    }

    @Patch(owner = STB_TRUETYPE)
    public static boolean stbtt_PackBegin(STBTTPackContext spc, ByteBuffer pixels, int width, int height,
                                          int stride_in_bytes, int padding, long alloc_context) {
        return true;
    }

    @Patch(owner = STB_TRUETYPE)
    public static boolean stbtt_PackBegin(STBTTPackContext spc, ByteBuffer pixels, int width, int height,
                                          int stride_in_bytes, int padding) {
        return true;
    }

    @Patch(owner = STB_TRUETYPE)
    public static boolean stbtt_PackFontRange(STBTTPackContext spc, ByteBuffer fontdata, int font_index,
                                              float font_size, int first_unicode_char_in_range,
                                              STBTTPackedchar.Buffer chardata_for_range) {
        pack(font_size, chardata_for_range);
        return true;
    }

    @Patch(owner = STB_TRUETYPE)
    public static boolean stbtt_PackFontRanges(STBTTPackContext spc, ByteBuffer fontdata, int font_index,
                                               STBTTPackRange.Buffer ranges) {
        for (int i = ranges.position(); i < ranges.limit(); i++) {
            STBTTPackRange range = ranges.get(i);
            pack(range.font_size(), range.chardata_for_range());
        }

        return true;
    }

    /**
     * Glyphs are empty boxes on the atlas, advancing the cursor by half the font size.
     */
    private static void pack(float fontSize, STBTTPackedchar.Buffer chars) {
        if (chars == null) {
            return;
        }

        float advance = Math.abs(fontSize) * ADVANCE;
        for (int i = chars.position(); i < chars.limit(); i++) {
            chars.get(i).xadvance(advance).xoff2(advance);
        }
    }

    @Patch(owner = STB_TRUETYPE)
    public static void stbtt_GetPackedQuad(STBTTPackedchar.Buffer chardata, int pw, int ph, int char_index,
                                           FloatBuffer xpos, FloatBuffer ypos, STBTTAlignedQuad q, boolean align_to_integer) {
        float x = xpos.get(xpos.position());
        float advance = quad(chardata, char_index, x, ypos.get(ypos.position()), q);
        xpos.put(xpos.position(), x + advance);
    }

    @Patch(owner = STB_TRUETYPE)
    public static void stbtt_GetPackedQuad(STBTTPackedchar.Buffer chardata, int pw, int ph, int char_index,
                                           float[] xpos, float[] ypos, STBTTAlignedQuad q, boolean align_to_integer) {
        float advance = quad(chardata, char_index, xpos[0], ypos[0], q);
        xpos[0] += advance;
    }

    private static float quad(STBTTPackedchar.Buffer chardata, int index, float x, float y, STBTTAlignedQuad q) {
        float advance = chardata == null || index < 0 || index >= chardata.capacity() ? 0.0f : chardata.get(index).xadvance();
        if (q != null) {
            long address = q.address();
            MemoryUtil.memPutFloat(address + STBTTAlignedQuad.X0, x);
            MemoryUtil.memPutFloat(address + STBTTAlignedQuad.Y0, y);
            MemoryUtil.memPutFloat(address + STBTTAlignedQuad.X1, x + advance);
            MemoryUtil.memPutFloat(address + STBTTAlignedQuad.Y1, y);
        }

        return advance;
    }

    private static void put(IntBuffer buffer, int value) {
        if (buffer != null) {
            buffer.put(buffer.position(), value);
        }
    }

    private static void put(int[] array, int value) {
        if (array != null && array.length > 0) {
            array[0] = value;
        }
    }

    private static void put(FloatBuffer buffer, float value) {
        if (buffer != null) {
            buffer.put(buffer.position(), value);
        }
    }

    private static void put(float[] array, float value) {
        if (array != null && array.length > 0) {
            array[0] = value;
        }
    }

}
