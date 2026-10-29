package io.github.headlesshq.headlessmc.lwjgl3.runtime.stb;

import io.github.headlesshq.headlessmc.lwjgl.runtime.DummyBuffers;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl.runtime.StructMemory;
import org.lwjgl.stb.STBIIOCallbacks;
import org.lwjgl.stb.STBIReadCallback;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Patches for {@code STBImage}: images are not decoded, but their real size and channel count are read from the PNG
 * header, so that applications lay out textures, atlases and animations as usual. The pixels are a dummy buffer of
 * the right size. Images that are not PNGs are reported as 16x16 RGBA. Images read with callbacks only have their
 * header read.
 */
public final class StbImagePatches {
    private static final String STB_IMAGE = "org/lwjgl/stb/STBImage";
    private static final int DEFAULT_SIZE = 16;
    private static final int RGBA = 4;
    private static final int HEADER_SIZE = 26;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private StbImagePatches() {
        throw new AssertionError();
    }

    /**
     * @return width, height and channel count of the image.
     */
    static int[] header(ByteBuffer image) {
        if (image != null && image.remaining() >= HEADER_SIZE) {
            int start = image.position();
            boolean png = true;
            for (int i = 0; i < PNG_SIGNATURE.length && png; i++) {
                png = image.get(start + i) == PNG_SIGNATURE[i];
            }

            if (png) {
                int width = bigEndianInt(image, start + 16);
                int height = bigEndianInt(image, start + 20);
                if (width > 0 && height > 0) {
                    return new int[]{width, height, channels(image.get(start + 25))};
                }
            }
        }

        return new int[]{DEFAULT_SIZE, DEFAULT_SIZE, RGBA};
    }

    /**
     * PNG stores integers big endian, independent of the byte order of the buffer.
     */
    private static int bigEndianInt(ByteBuffer buffer, int index) {
        return (buffer.get(index) & 0xFF) << 24 | (buffer.get(index + 1) & 0xFF) << 16
            | (buffer.get(index + 2) & 0xFF) << 8 | buffer.get(index + 3) & 0xFF;
    }

    private static int channels(byte colorType) {
        switch (colorType) {
            case 0:
                return 1;
            case 4:
                return 2;
            case 2:
            case 3:
                return 3;
            default:
                return RGBA;
        }
    }

    private static int[] load(ByteBuffer image, IntBuffer x, IntBuffer y, IntBuffer channels) {
        int[] header = header(image);
        put(x, header[0]);
        put(y, header[1]);
        put(channels, header[2]);
        return header;
    }

    private static int[] load(ByteBuffer image, int[] x, int[] y, int[] channels) {
        int[] header = header(image);
        put(x, header[0]);
        put(y, header[1]);
        put(channels, header[2]);
        return header;
    }

    private static long pixels(int[] header, int desiredChannels) {
        return (long) header[0] * header[1] * (desiredChannels == 0 ? header[2] : desiredChannels);
    }

    @Patch(owner = STB_IMAGE)
    public static ByteBuffer stbi_load_from_memory(ByteBuffer buffer, IntBuffer x, IntBuffer y, IntBuffer channels_in_file, int desired_channels) {
        return DummyBuffers.bytes(pixels(load(buffer, x, y, channels_in_file), desired_channels));
    }

    @Patch(owner = STB_IMAGE)
    public static ByteBuffer stbi_load_from_memory(ByteBuffer buffer, int[] x, int[] y, int[] channels_in_file, int desired_channels) {
        return DummyBuffers.bytes(pixels(load(buffer, x, y, channels_in_file), desired_channels));
    }

    @Patch(owner = STB_IMAGE)
    public static ShortBuffer stbi_load_16_from_memory(ByteBuffer buffer, IntBuffer x, IntBuffer y, IntBuffer channels_in_file, int desired_channels) {
        return DummyBuffers.shorts(pixels(load(buffer, x, y, channels_in_file), desired_channels));
    }

    @Patch(owner = STB_IMAGE)
    public static ShortBuffer stbi_load_16_from_memory(ByteBuffer buffer, int[] x, int[] y, int[] channels_in_file, int desired_channels) {
        return DummyBuffers.shorts(pixels(load(buffer, x, y, channels_in_file), desired_channels));
    }

    @Patch(owner = STB_IMAGE)
    public static FloatBuffer stbi_loadf_from_memory(ByteBuffer buffer, IntBuffer x, IntBuffer y, IntBuffer channels_in_file, int desired_channels) {
        return DummyBuffers.floats(pixels(load(buffer, x, y, channels_in_file), desired_channels));
    }

    @Patch(owner = STB_IMAGE)
    public static FloatBuffer stbi_loadf_from_memory(ByteBuffer buffer, int[] x, int[] y, int[] channels_in_file, int desired_channels) {
        return DummyBuffers.floats(pixels(load(buffer, x, y, channels_in_file), desired_channels));
    }

    @Patch(owner = STB_IMAGE)
    public static boolean stbi_info_from_memory(ByteBuffer buffer, IntBuffer x, IntBuffer y, IntBuffer comp) {
        load(buffer, x, y, comp);
        return true;
    }

    @Patch(owner = STB_IMAGE)
    public static boolean stbi_info_from_memory(ByteBuffer buffer, int[] x, int[] y, int[] comp) {
        load(buffer, x, y, comp);
        return true;
    }

    /**
     * Reads the header of an image with the read callback.
     */
    static ByteBuffer read(STBIIOCallbacks callbacks, long user) {
        ByteBuffer header = StructMemory.allocate(HEADER_SIZE);
        STBIReadCallback read = callbacks == null ? null : callbacks.read();
        if (read == null) {
            header.limit(0);
            return header;
        }

        long address = StructMemory.address(header);
        int total = 0;
        while (total < HEADER_SIZE) {
            int count = read.invoke(user, address + total, HEADER_SIZE - total);
            if (count <= 0) {
                break;
            }

            total += count;
        }

        header.limit(total);
        return header;
    }

    @Patch(owner = STB_IMAGE)
    public static ByteBuffer stbi_load_from_callbacks(STBIIOCallbacks clbk, long user, IntBuffer x, IntBuffer y, IntBuffer channels_in_file, int desired_channels) {
        return stbi_load_from_memory(read(clbk, user), x, y, channels_in_file, desired_channels);
    }

    @Patch(owner = STB_IMAGE)
    public static ByteBuffer stbi_load_from_callbacks(STBIIOCallbacks clbk, long user, int[] x, int[] y, int[] channels_in_file, int desired_channels) {
        return stbi_load_from_memory(read(clbk, user), x, y, channels_in_file, desired_channels);
    }

    @Patch(owner = STB_IMAGE)
    public static ShortBuffer stbi_load_16_from_callbacks(STBIIOCallbacks clbk, long user, IntBuffer x, IntBuffer y, IntBuffer channels_in_file, int desired_channels) {
        return stbi_load_16_from_memory(read(clbk, user), x, y, channels_in_file, desired_channels);
    }

    @Patch(owner = STB_IMAGE)
    public static ShortBuffer stbi_load_16_from_callbacks(STBIIOCallbacks clbk, long user, int[] x, int[] y, int[] channels_in_file, int desired_channels) {
        return stbi_load_16_from_memory(read(clbk, user), x, y, channels_in_file, desired_channels);
    }

    @Patch(owner = STB_IMAGE)
    public static FloatBuffer stbi_loadf_from_callbacks(STBIIOCallbacks clbk, long user, IntBuffer x, IntBuffer y, IntBuffer channels_in_file, int desired_channels) {
        return stbi_loadf_from_memory(read(clbk, user), x, y, channels_in_file, desired_channels);
    }

    @Patch(owner = STB_IMAGE)
    public static FloatBuffer stbi_loadf_from_callbacks(STBIIOCallbacks clbk, long user, int[] x, int[] y, int[] channels_in_file, int desired_channels) {
        return stbi_loadf_from_memory(read(clbk, user), x, y, channels_in_file, desired_channels);
    }

    @Patch(owner = STB_IMAGE)
    public static boolean stbi_info_from_callbacks(STBIIOCallbacks clbk, long user, IntBuffer x, IntBuffer y, IntBuffer comp) {
        return stbi_info_from_memory(read(clbk, user), x, y, comp);
    }

    @Patch(owner = STB_IMAGE)
    public static boolean stbi_info_from_callbacks(STBIIOCallbacks clbk, long user, int[] x, int[] y, int[] comp) {
        return stbi_info_from_memory(read(clbk, user), x, y, comp);
    }

    private static void put(IntBuffer buffer, int value) {
        if (buffer != null) {
            buffer.put(buffer.position(), value);
        }
    }

    private static void put(int[] array, int value) {
        if (array != null) {
            array[0] = value;
        }
    }

}
