package io.github.headlesshq.headlessmc.lwjgl.runtime.openal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * The OpenAL state that applications set and query back: source parameters and playback state, queued buffers, the
 * format of buffer data, listener parameters, strings and device attributes.
 * <p>Nothing is played, so a source that is played stops right away and all its buffers are processed, unless it
 * loops. Applications see their sounds finish and free their sources, e.g. Minecraft polls
 * {@code AL_SOURCE_STATE} to recycle its channels, and streams refill their buffers until the stream ends.</p>
 */
public final class AlState {
    public static final int AL_NO_ERROR = 0;
    public static final int AL_INVALID_NAME = 0xA001;
    public static final int AL_INVALID_ENUM = 0xA002;
    public static final int AL_INVALID_VALUE = 0xA003;
    public static final int AL_INVALID_OPERATION = 0xA004;
    public static final int AL_OUT_OF_MEMORY = 0xA005;
    public static final int AL_VENDOR = 0xB001;
    public static final int AL_VERSION = 0xB002;
    public static final int AL_RENDERER = 0xB003;
    public static final int AL_EXTENSIONS = 0xB004;
    private static final int AL_SOURCE_RELATIVE = 0x202;
    private static final int AL_CONE_INNER_ANGLE = 0x1001;
    private static final int AL_CONE_OUTER_ANGLE = 0x1002;
    private static final int AL_PITCH = 0x1003;
    private static final int AL_LOOPING = 0x1007;
    private static final int AL_BUFFER = 0x1009;
    private static final int AL_GAIN = 0x100A;
    private static final int AL_MIN_GAIN = 0x100D;
    private static final int AL_MAX_GAIN = 0x100E;
    private static final int AL_SOURCE_STATE = 0x1010;
    private static final int AL_INITIAL = 0x1011;
    private static final int AL_PLAYING = 0x1012;
    private static final int AL_PAUSED = 0x1013;
    private static final int AL_STOPPED = 0x1014;
    private static final int AL_BUFFERS_QUEUED = 0x1015;
    private static final int AL_BUFFERS_PROCESSED = 0x1016;
    private static final int AL_REFERENCE_DISTANCE = 0x1020;
    private static final int AL_ROLLOFF_FACTOR = 0x1021;
    private static final int AL_MAX_DISTANCE = 0x1023;
    private static final int AL_SOURCE_TYPE = 0x1027;
    private static final int AL_STATIC = 0x1028;
    private static final int AL_STREAMING = 0x1029;
    private static final int AL_UNDETERMINED = 0x1030;
    private static final int AL_FREQUENCY = 0x2001;
    private static final int AL_BITS = 0x2002;
    private static final int AL_CHANNELS = 0x2003;
    private static final int AL_SIZE = 0x2004;
    private static final int AL_FORMAT_MONO8 = 0x1100;
    private static final int AL_FORMAT_MONO16 = 0x1101;
    private static final int AL_FORMAT_STEREO8 = 0x1102;
    private static final int AL_FORMAT_STEREO16 = 0x1103;
    private static final int AL_FORMAT_MONO_FLOAT32 = 0x10010;
    private static final int AL_FORMAT_STEREO_FLOAT32 = 0x10011;
    private static final int ALC_NO_ERROR = 0;
    private static final int ALC_INVALID_DEVICE = 0xA001;
    private static final int ALC_INVALID_CONTEXT = 0xA002;
    private static final int ALC_INVALID_ENUM = 0xA003;
    private static final int ALC_INVALID_VALUE = 0xA004;
    private static final int ALC_OUT_OF_MEMORY = 0xA005;
    private static final int ALC_FREQUENCY = 0x1007;
    private static final int ALC_REFRESH = 0x1008;
    private static final int ALC_SYNC = 0x1009;
    private static final int ALC_MONO_SOURCES = 0x1010;
    private static final int ALC_STEREO_SOURCES = 0x1011;
    private static final int ALC_MAJOR_VERSION = 0x1000;
    private static final int ALC_MINOR_VERSION = 0x1001;
    private static final int ALC_ATTRIBUTES_SIZE = 0x1002;
    private static final int ALC_ALL_ATTRIBUTES = 0x1003;
    private static final int ALC_DEFAULT_DEVICE_SPECIFIER = 0x1004;
    private static final int ALC_DEVICE_SPECIFIER = 0x1005;
    private static final int ALC_EXTENSIONS = 0x1006;
    private static final int ALC_CAPTURE_DEVICE_SPECIFIER = 0x310;
    private static final int ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER = 0x311;
    private static final int ALC_ALL_DEVICES_SPECIFIER = 0x1013;
    private static final int ALC_DEFAULT_ALL_DEVICES_SPECIFIER = 0x1012;
    private static final String DEVICE = "HeadlessMc";
    private static final int FREQUENCY = 48000;
    private static final int[] ATTRIBUTES = {
        ALC_FREQUENCY, FREQUENCY, ALC_REFRESH, 50, ALC_SYNC, 0, ALC_MONO_SOURCES, 255, ALC_STEREO_SOURCES, 1, 0
    };
    private static final Map<Integer, Source> SOURCES = new HashMap<>();
    private static final Map<Integer, int[]> BUFFERS = new HashMap<>();
    private static final Map<Integer, float[]> LISTENER = new HashMap<>();

    private AlState() {
        throw new AssertionError();
    }

    private static Source source(int source) {
        Source result = SOURCES.get(source);
        if (result == null) {
            result = new Source();
            SOURCES.put(source, result);
        }

        return result;
    }

    public static synchronized void deleteSource(int source) {
        SOURCES.remove(source);
    }

    public static synchronized void deleteBuffer(int buffer) {
        BUFFERS.remove(buffer);
    }

    public static synchronized void play(int source) {
        Source state = source(source);
        if (state.looping) {
            state.state = AL_PLAYING;
        } else {
            state.state = AL_STOPPED;
            state.processed = state.queue.size();
        }
    }

    public static synchronized void pause(int source) {
        Source state = source(source);
        if (state.state == AL_PLAYING) {
            state.state = AL_PAUSED;
        }
    }

    public static synchronized void stop(int source) {
        Source state = source(source);
        if (state.state != AL_INITIAL) {
            state.state = AL_STOPPED;
            state.processed = state.queue.size();
        }
    }

    public static synchronized void rewind(int source) {
        Source state = source(source);
        state.state = AL_INITIAL;
        state.processed = 0;
    }

    public static synchronized void queue(int source, int buffer) {
        Source state = source(source);
        state.buffer = 0;
        state.queue.addLast(buffer);
        if (state.state == AL_STOPPED) {
            state.processed = state.queue.size();
        }
    }

    /**
     * Removes the oldest processed buffer from the queue.
     *
     * @return the buffer, 0 if no buffer is processed.
     */
    public static synchronized int unqueue(int source) {
        Source state = source(source);
        if (state.processed <= 0 || state.queue.isEmpty()) {
            return 0;
        }

        state.processed--;
        return state.queue.removeFirst();
    }

    public static synchronized void sourcei(int source, int pname, int value) {
        Source state = source(source);
        switch (pname) {
            case AL_BUFFER:
                state.buffer = value;
                state.queue.clear();
                state.processed = 0;
                if (value != 0) {
                    state.queue.add(value);
                }

                break;
            case AL_LOOPING:
                state.looping = value != 0;
                break;
            default:
                state.values.put(pname, new float[]{value});
                break;
        }
    }

    public static synchronized void sourcef(int source, int pname, float... values) {
        if (pname == AL_LOOPING || pname == AL_BUFFER) {
            sourcei(source, pname, (int) values[0]);
        } else {
            source(source).values.put(pname, values.clone());
        }
    }

    public static synchronized int getSourcei(int source, int pname) {
        Source state = source(source);
        switch (pname) {
            case AL_SOURCE_STATE:
                return state.state;
            case AL_BUFFER:
                return state.buffer;
            case AL_LOOPING:
                return state.looping ? 1 : 0;
            case AL_BUFFERS_QUEUED:
                return state.queue.size();
            case AL_BUFFERS_PROCESSED:
                return state.processed;
            case AL_SOURCE_TYPE:
                return state.buffer != 0 ? AL_STATIC : state.queue.isEmpty() ? AL_UNDETERMINED : AL_STREAMING;
            default:
                return Math.round(getSourcef(source, pname)[0]);
        }
    }

    public static synchronized float[] getSourcef(int source, int pname) {
        float[] value = source(source).values.get(pname);
        if (value != null) {
            return value.clone();
        }

        switch (pname) {
            case AL_PITCH:
            case AL_GAIN:
            case AL_MAX_GAIN:
            case AL_REFERENCE_DISTANCE:
            case AL_ROLLOFF_FACTOR:
                return new float[]{1.0f};
            case AL_CONE_INNER_ANGLE:
            case AL_CONE_OUTER_ANGLE:
                return new float[]{360.0f};
            case AL_MAX_DISTANCE:
                return new float[]{Float.MAX_VALUE};
            case AL_MIN_GAIN:
            case AL_SOURCE_RELATIVE:
                return new float[]{0.0f};
            default:
                return new float[]{0.0f, 0.0f, 0.0f};
        }
    }

    public static synchronized void bufferData(int buffer, int format, int size, int frequency) {
        int bits;
        int channels;
        switch (format) {
            case AL_FORMAT_MONO8:
                bits = 8;
                channels = 1;
                break;
            case AL_FORMAT_STEREO8:
                bits = 8;
                channels = 2;
                break;
            case AL_FORMAT_STEREO16:
                bits = 16;
                channels = 2;
                break;
            case AL_FORMAT_MONO_FLOAT32:
                bits = 32;
                channels = 1;
                break;
            case AL_FORMAT_STEREO_FLOAT32:
                bits = 32;
                channels = 2;
                break;
            case AL_FORMAT_MONO16:
            default:
                bits = 16;
                channels = 1;
                break;
        }

        BUFFERS.put(buffer, new int[]{frequency, bits, channels, size});
    }

    public static synchronized int getBufferi(int buffer, int pname) {
        int[] data = BUFFERS.get(buffer);
        switch (pname) {
            case AL_FREQUENCY:
                return data == null ? 0 : data[0];
            case AL_BITS:
                return data == null ? 16 : data[1];
            case AL_CHANNELS:
                return data == null ? 1 : data[2];
            case AL_SIZE:
                return data == null ? 0 : data[3];
            default:
                return 0;
        }
    }

    public static synchronized void listener(int pname, float... values) {
        LISTENER.put(pname, values.clone());
    }

    /**
     * @return the listener parameter, the gain is 1, position, velocity and orientation are 0.
     */
    public static synchronized float[] getListener(int pname) {
        float[] value = LISTENER.get(pname);
        if (value != null) {
            return value.clone();
        }

        return pname == AL_GAIN ? new float[]{1.0f} : new float[6];
    }

    public static String getString(int param) {
        switch (param) {
            case AL_NO_ERROR:
                return "No Error";
            case AL_INVALID_NAME:
                return "Invalid Name";
            case AL_INVALID_ENUM:
                return "Invalid Enum";
            case AL_INVALID_VALUE:
                return "Invalid Value";
            case AL_INVALID_OPERATION:
                return "Invalid Operation";
            case AL_OUT_OF_MEMORY:
                return "Out of Memory";
            case AL_VENDOR:
            case AL_RENDERER:
                return DEVICE;
            case AL_VERSION:
                return "1.1 HeadlessMc";
            case AL_EXTENSIONS:
                return "";
            default:
                return null;
        }
    }

    public static String alcGetString(int param) {
        switch (param) {
            case ALC_NO_ERROR:
                return "No Error";
            case ALC_INVALID_DEVICE:
                return "Invalid Device";
            case ALC_INVALID_CONTEXT:
                return "Invalid Context";
            case ALC_INVALID_ENUM:
                return "Invalid Enum";
            case ALC_INVALID_VALUE:
                return "Invalid Value";
            case ALC_OUT_OF_MEMORY:
                return "Out of Memory";
            case ALC_DEFAULT_DEVICE_SPECIFIER:
            case ALC_DEVICE_SPECIFIER:
            case ALC_ALL_DEVICES_SPECIFIER:
            case ALC_DEFAULT_ALL_DEVICES_SPECIFIER:
            case ALC_CAPTURE_DEVICE_SPECIFIER:
            case ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER:
                return DEVICE;
            case ALC_EXTENSIONS:
                return "";
            default:
                return null;
        }
    }

    /**
     * @return the values of an {@code alcGetIntegerv} query: OpenAL 1.1 at 48 kHz with 255 mono and 1 stereo source.
     */
    public static int[] alcGetIntegers(int param) {
        switch (param) {
            case ALC_MAJOR_VERSION:
            case ALC_MINOR_VERSION:
                return new int[]{1};
            case ALC_ATTRIBUTES_SIZE:
                return new int[]{ATTRIBUTES.length};
            case ALC_ALL_ATTRIBUTES:
                return ATTRIBUTES.clone();
            default:
                for (int i = 0; i + 1 < ATTRIBUTES.length; i += 2) {
                    if (ATTRIBUTES[i] == param) {
                        return new int[]{ATTRIBUTES[i + 1]};
                    }
                }

                return new int[]{0};
        }
    }

    private static final class Source {
        private final Map<Integer, float[]> values = new HashMap<>();
        private final Deque<Integer> queue = new ArrayDeque<>();
        private int state = AL_INITIAL;
        private boolean looping;
        private int buffer;
        private int processed;
    }

}
