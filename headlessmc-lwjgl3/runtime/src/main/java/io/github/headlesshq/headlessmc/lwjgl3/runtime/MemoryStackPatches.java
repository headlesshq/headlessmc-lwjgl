package io.github.headlesshq.headlessmc.lwjgl3.runtime;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl.runtime.StructMemory;
import org.lwjgl.CLongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;

import java.nio.ByteBuffer;

/**
 * Patches for the {@code MemoryStack} methods that return memory holding a value: encoded strings, like
 * {@code stack.UTF8(text)} or {@code stackASCII(text)}, and pointers and C longs, like {@code stack.pointers(a, b)}.
 * They return new memory with the value, like {@code MemoryUtil.memUTF8(text)}, instead of an empty buffer. The memory
 * is not taken from the stack, which is a dummy, it is freed by the garbage collector.
 */
public final class MemoryStackPatches {
    private static final String MEMORY_STACK = "org/lwjgl/system/MemoryStack";

    private MemoryStackPatches() {
        throw new AssertionError();
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer ASCII(MemoryStack stack, CharSequence text) {
        return MemoryStringPatches.memASCII(text, true);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer ASCII(MemoryStack stack, CharSequence text, boolean nullTerminated) {
        return MemoryStringPatches.memASCII(text, nullTerminated);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer ASCIISafe(MemoryStack stack, CharSequence text) {
        return text == null ? null : MemoryStringPatches.memASCII(text, true);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer ASCIISafe(MemoryStack stack, CharSequence text, boolean nullTerminated) {
        return text == null ? null : MemoryStringPatches.memASCII(text, nullTerminated);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer stackASCII(CharSequence text) {
        return MemoryStringPatches.memASCII(text, true);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer stackASCII(CharSequence text, boolean nullTerminated) {
        return MemoryStringPatches.memASCII(text, nullTerminated);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer stackASCIISafe(CharSequence text) {
        return text == null ? null : MemoryStringPatches.memASCII(text, true);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer stackASCIISafe(CharSequence text, boolean nullTerminated) {
        return text == null ? null : MemoryStringPatches.memASCII(text, nullTerminated);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer UTF8(MemoryStack stack, CharSequence text) {
        return MemoryStringPatches.memUTF8(text, true);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer UTF8(MemoryStack stack, CharSequence text, boolean nullTerminated) {
        return MemoryStringPatches.memUTF8(text, nullTerminated);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer UTF8Safe(MemoryStack stack, CharSequence text) {
        return text == null ? null : MemoryStringPatches.memUTF8(text, true);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer UTF8Safe(MemoryStack stack, CharSequence text, boolean nullTerminated) {
        return text == null ? null : MemoryStringPatches.memUTF8(text, nullTerminated);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer stackUTF8(CharSequence text) {
        return MemoryStringPatches.memUTF8(text, true);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer stackUTF8(CharSequence text, boolean nullTerminated) {
        return MemoryStringPatches.memUTF8(text, nullTerminated);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer stackUTF8Safe(CharSequence text) {
        return text == null ? null : MemoryStringPatches.memUTF8(text, true);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer stackUTF8Safe(CharSequence text, boolean nullTerminated) {
        return text == null ? null : MemoryStringPatches.memUTF8(text, nullTerminated);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer UTF16(MemoryStack stack, CharSequence text) {
        return MemoryStringPatches.memUTF16(text, true);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer UTF16(MemoryStack stack, CharSequence text, boolean nullTerminated) {
        return MemoryStringPatches.memUTF16(text, nullTerminated);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer UTF16Safe(MemoryStack stack, CharSequence text) {
        return text == null ? null : MemoryStringPatches.memUTF16(text, true);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer UTF16Safe(MemoryStack stack, CharSequence text, boolean nullTerminated) {
        return text == null ? null : MemoryStringPatches.memUTF16(text, nullTerminated);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer stackUTF16(CharSequence text) {
        return MemoryStringPatches.memUTF16(text, true);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer stackUTF16(CharSequence text, boolean nullTerminated) {
        return MemoryStringPatches.memUTF16(text, nullTerminated);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer stackUTF16Safe(CharSequence text) {
        return text == null ? null : MemoryStringPatches.memUTF16(text, true);
    }

    @Patch(owner = MEMORY_STACK)
    public static ByteBuffer stackUTF16Safe(CharSequence text, boolean nullTerminated) {
        return text == null ? null : MemoryStringPatches.memUTF16(text, nullTerminated);
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer pointers(MemoryStack stack, long a, long b) {
        return pointerBuffer(new long[]{a, b});
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer pointers(MemoryStack stack, long a, long b, long c) {
        return pointerBuffer(new long[]{a, b, c});
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer pointers(MemoryStack stack, long a, long b, long c, long d) {
        return pointerBuffer(new long[]{a, b, c, d});
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer pointers(MemoryStack stack, Pointer a, Pointer b) {
        return pointerBuffer(new long[]{a.address(), b.address()});
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer pointers(MemoryStack stack, Pointer a, Pointer b, Pointer c) {
        return pointerBuffer(new long[]{a.address(), b.address(), c.address()});
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer pointers(MemoryStack stack, Pointer a, Pointer b, Pointer c, Pointer d) {
        return pointerBuffer(new long[]{a.address(), b.address(), c.address(), d.address()});
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer pointers(MemoryStack stack, long... values) {
        return pointerBuffer(values.clone());
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer pointers(MemoryStack stack, Pointer value) {
        return pointerBuffer(new long[]{value.address()});
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer pointers(MemoryStack stack, Pointer... values) {
        long[] addresses = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            addresses[i] = values[i].address();
        }

        return pointerBuffer(addresses);
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer stackPointers(long a, long b) {
        return pointerBuffer(new long[]{a, b});
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer stackPointers(long a, long b, long c) {
        return pointerBuffer(new long[]{a, b, c});
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer stackPointers(long a, long b, long c, long d) {
        return pointerBuffer(new long[]{a, b, c, d});
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer stackPointers(Pointer a, Pointer b) {
        return pointerBuffer(new long[]{a.address(), b.address()});
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer stackPointers(Pointer a, Pointer b, Pointer c) {
        return pointerBuffer(new long[]{a.address(), b.address(), c.address()});
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer stackPointers(Pointer a, Pointer b, Pointer c, Pointer d) {
        return pointerBuffer(new long[]{a.address(), b.address(), c.address(), d.address()});
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer stackPointers(long... values) {
        return pointerBuffer(values.clone());
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer stackPointers(Pointer value) {
        return pointerBuffer(new long[]{value.address()});
    }

    @Patch(owner = MEMORY_STACK)
    public static PointerBuffer stackPointers(Pointer... values) {
        long[] addresses = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            addresses[i] = values[i].address();
        }

        return pointerBuffer(addresses);
    }

    @Patch(owner = MEMORY_STACK)
    public static CLongBuffer clongs(MemoryStack stack, long a, long b) {
        return cLongBuffer(new long[]{a, b});
    }

    @Patch(owner = MEMORY_STACK)
    public static CLongBuffer clongs(MemoryStack stack, long a, long b, long c) {
        return cLongBuffer(new long[]{a, b, c});
    }

    @Patch(owner = MEMORY_STACK)
    public static CLongBuffer clongs(MemoryStack stack, long a, long b, long c, long d) {
        return cLongBuffer(new long[]{a, b, c, d});
    }

    @Patch(owner = MEMORY_STACK)
    public static CLongBuffer clongs(MemoryStack stack, long... values) {
        return cLongBuffer(values.clone());
    }

    @Patch(owner = MEMORY_STACK)
    public static CLongBuffer stackCLongs(long a, long b) {
        return cLongBuffer(new long[]{a, b});
    }

    @Patch(owner = MEMORY_STACK)
    public static CLongBuffer stackCLongs(long a, long b, long c) {
        return cLongBuffer(new long[]{a, b, c});
    }

    @Patch(owner = MEMORY_STACK)
    public static CLongBuffer stackCLongs(long a, long b, long c, long d) {
        return cLongBuffer(new long[]{a, b, c, d});
    }

    @Patch(owner = MEMORY_STACK)
    public static CLongBuffer stackCLongs(long... values) {
        return cLongBuffer(values.clone());
    }

    private static PointerBuffer pointerBuffer(long[] values) {
        PointerBuffer buffer = PointerBuffer.create(StructMemory.allocate(values.length * Long.BYTES));
        buffer.put(values).flip();
        return buffer;
    }

    private static CLongBuffer cLongBuffer(long[] values) {
        CLongBuffer buffer = CLongBuffer.create(StructMemory.allocate(values.length * Long.BYTES));
        buffer.put(values).flip();
        return buffer;
    }

}
