package io.github.headlesshq.headlessmc.lwjgl3.runtime;

import io.github.headlesshq.headlessmc.lwjgl.runtime.DummyBuffers;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Patches for the string functions of {@code org.lwjgl.system.MemoryUtil} that work on Java buffers, which are pure
 * Java and applications rely on, e.g. Minecraft reads its shader sources with {@code memASCII(ByteBuffer, int)}.
 * Decoding reads {@code [position + offset, position + offset + length)}, encoding returns a new buffer or writes at
 * the position of the target, UTF-16 uses the native byte order, little endian, like LWJGL on x86 and ARM.
 */
public final class MemoryStringPatches {
    private static final String MEMORY_UTIL = "org/lwjgl/system/MemoryUtil";
    private static final Charset UTF_16 = StandardCharsets.UTF_16LE;

    private MemoryStringPatches() {
        throw new AssertionError();
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memASCII(ByteBuffer buffer) {
        return decode(buffer, 0, buffer.remaining(), StandardCharsets.US_ASCII);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memASCIISafe(ByteBuffer buffer) {
        return buffer == null ? null : decode(buffer, 0, buffer.remaining(), StandardCharsets.US_ASCII);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memASCII(ByteBuffer buffer, int length) {
        return decode(buffer, 0, length, StandardCharsets.US_ASCII);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memASCII(ByteBuffer buffer, int length, int offset) {
        return decode(buffer, offset, length, StandardCharsets.US_ASCII);
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memASCII(CharSequence text) {
        return encode(text, true, StandardCharsets.US_ASCII, 1);
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memASCIISafe(CharSequence text) {
        return text == null ? null : encode(text, true, StandardCharsets.US_ASCII, 1);
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memASCII(CharSequence text, boolean nullTerminated) {
        return encode(text, nullTerminated, StandardCharsets.US_ASCII, 1);
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memASCIISafe(CharSequence text, boolean nullTerminated) {
        return text == null ? null : encode(text, nullTerminated, StandardCharsets.US_ASCII, 1);
    }

    @Patch(owner = MEMORY_UTIL)
    public static int memASCII(CharSequence text, boolean nullTerminated, ByteBuffer target) {
        return encode(text, nullTerminated, target, 0, StandardCharsets.US_ASCII, 1);
    }

    @Patch(owner = MEMORY_UTIL)
    public static int memASCII(CharSequence text, boolean nullTerminated, ByteBuffer target, int offset) {
        return encode(text, nullTerminated, target, offset, StandardCharsets.US_ASCII, 1);
    }

    @Patch(owner = MEMORY_UTIL)
    public static int memLengthASCII(CharSequence text, boolean nullTerminated) {
        return text.toString().getBytes(StandardCharsets.US_ASCII).length + (nullTerminated ? 1 : 0);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memUTF8(ByteBuffer buffer) {
        return decode(buffer, 0, buffer.remaining(), StandardCharsets.UTF_8);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memUTF8Safe(ByteBuffer buffer) {
        return buffer == null ? null : decode(buffer, 0, buffer.remaining(), StandardCharsets.UTF_8);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memUTF8(ByteBuffer buffer, int length) {
        return decode(buffer, 0, length, StandardCharsets.UTF_8);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memUTF8(ByteBuffer buffer, int length, int offset) {
        return decode(buffer, offset, length, StandardCharsets.UTF_8);
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memUTF8(CharSequence text) {
        return encode(text, true, StandardCharsets.UTF_8, 1);
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memUTF8Safe(CharSequence text) {
        return text == null ? null : encode(text, true, StandardCharsets.UTF_8, 1);
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memUTF8(CharSequence text, boolean nullTerminated) {
        return encode(text, nullTerminated, StandardCharsets.UTF_8, 1);
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memUTF8Safe(CharSequence text, boolean nullTerminated) {
        return text == null ? null : encode(text, nullTerminated, StandardCharsets.UTF_8, 1);
    }

    @Patch(owner = MEMORY_UTIL)
    public static int memUTF8(CharSequence text, boolean nullTerminated, ByteBuffer target) {
        return encode(text, nullTerminated, target, 0, StandardCharsets.UTF_8, 1);
    }

    @Patch(owner = MEMORY_UTIL)
    public static int memUTF8(CharSequence text, boolean nullTerminated, ByteBuffer target, int offset) {
        return encode(text, nullTerminated, target, offset, StandardCharsets.UTF_8, 1);
    }

    @Patch(owner = MEMORY_UTIL)
    public static int memLengthUTF8(CharSequence text, boolean nullTerminated) {
        return text.toString().getBytes(StandardCharsets.UTF_8).length + (nullTerminated ? 1 : 0);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memUTF16(ByteBuffer buffer) {
        return decode(buffer, 0, buffer.remaining(), UTF_16);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memUTF16Safe(ByteBuffer buffer) {
        return buffer == null ? null : decode(buffer, 0, buffer.remaining(), UTF_16);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memUTF16(ByteBuffer buffer, int length) {
        return decode(buffer, 0, length, UTF_16);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memUTF16(ByteBuffer buffer, int length, int offset) {
        return decode(buffer, offset, length, UTF_16);
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memUTF16(CharSequence text) {
        return encode(text, true, UTF_16, 2);
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memUTF16Safe(CharSequence text) {
        return text == null ? null : encode(text, true, UTF_16, 2);
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memUTF16(CharSequence text, boolean nullTerminated) {
        return encode(text, nullTerminated, UTF_16, 2);
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memUTF16Safe(CharSequence text, boolean nullTerminated) {
        return text == null ? null : encode(text, nullTerminated, UTF_16, 2);
    }

    @Patch(owner = MEMORY_UTIL)
    public static int memUTF16(CharSequence text, boolean nullTerminated, ByteBuffer target) {
        return encode(text, nullTerminated, target, 0, UTF_16, 2);
    }

    @Patch(owner = MEMORY_UTIL)
    public static int memUTF16(CharSequence text, boolean nullTerminated, ByteBuffer target, int offset) {
        return encode(text, nullTerminated, target, offset, UTF_16, 2);
    }

    @Patch(owner = MEMORY_UTIL)
    public static int memLengthUTF16(CharSequence text, boolean nullTerminated) {
        return text.toString().getBytes(UTF_16).length + (nullTerminated ? 2 : 0);
    }

    @Patch(owner = MEMORY_UTIL)
    public static int memLengthNT1(ByteBuffer buffer) {
        for (int i = buffer.position(); i < buffer.limit(); i++) {
            if (buffer.get(i) == 0) {
                return i - buffer.position();
            }
        }

        return buffer.remaining();
    }

    @Patch(owner = MEMORY_UTIL)
    public static int memLengthNT2(ByteBuffer buffer) {
        for (int i = buffer.position(); i + 1 < buffer.limit(); i += 2) {
            if (buffer.get(i) == 0 && buffer.get(i + 1) == 0) {
                return i - buffer.position();
            }
        }

        return buffer.remaining() & ~1;
    }

    private static String decode(ByteBuffer buffer, int offset, int length, Charset charset) {
        byte[] bytes = new byte[length];
        int start = buffer.position() + offset;
        for (int i = 0; i < length; i++) {
            bytes[i] = buffer.get(start + i);
        }

        return new String(bytes, charset);
    }

    private static ByteBuffer encode(CharSequence text, boolean nullTerminated, Charset charset, int terminator) {
        byte[] bytes = text.toString().getBytes(charset);
        ByteBuffer buffer = DummyBuffers.bytes(bytes.length + (nullTerminated ? terminator : 0));
        buffer.put(bytes);
        buffer.clear();
        return buffer;
    }

    private static int encode(CharSequence text, boolean nullTerminated, ByteBuffer target, int offset, Charset charset,
                              int terminator) {
        byte[] bytes = text.toString().getBytes(charset);
        int start = target.position() + offset;
        for (int i = 0; i < bytes.length; i++) {
            target.put(start + i, bytes[i]);
        }

        if (nullTerminated) {
            for (int i = 0; i < terminator; i++) {
                target.put(start + bytes.length + i, (byte) 0);
            }
        }

        return bytes.length + (nullTerminated ? terminator : 0);
    }

}
