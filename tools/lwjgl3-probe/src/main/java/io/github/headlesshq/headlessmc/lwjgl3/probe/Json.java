package io.github.headlesshq.headlessmc.lwjgl3.probe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader and writer for the recordings.
 * Numbers are read as {@link Long} or {@link Double}, objects as {@link LinkedHashMap}, arrays as {@link ArrayList}.
 */
final class Json {
    private Json() {
        throw new AssertionError();
    }

    static String write(Object value) {
        StringBuilder builder = new StringBuilder();
        write(builder, value);
        return builder.toString();
    }

    private static void write(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String string) {
            string(builder, string);
        } else if (value instanceof Float number && !Float.isFinite(number)
            || value instanceof Double decimal && !Double.isFinite(decimal)) {
            string(builder, value.toString());
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Character character) {
            builder.append((int) character);
        } else if (value instanceof Map<?, ?> map) {
            builder.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }

                first = false;
                string(builder, String.valueOf(entry.getKey()));
                builder.append(':');
                write(builder, entry.getValue());
            }

            builder.append('}');
        } else if (value instanceof Collection<?> collection) {
            builder.append('[');
            boolean first = true;
            for (Object element : collection) {
                if (!first) {
                    builder.append(',');
                }

                first = false;
                write(builder, element);
            }

            builder.append(']');
        } else {
            string(builder, value.toString());
        }
    }

    private static void string(StringBuilder builder, String string) {
        builder.append('"');
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                }
            }
        }

        builder.append('"');
    }

    static Object read(String json) {
        Reader reader = new Reader(json);
        Object value = reader.value();
        reader.whitespace();
        if (reader.index != json.length()) {
            throw reader.error("Trailing characters");
        }

        return value;
    }

    private static final class Reader {
        private final String json;
        private int index;

        private Reader(String json) {
            this.json = json;
        }

        private Object value() {
            whitespace();
            char c = peek();
            return switch (c) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            Map<String, Object> result = new LinkedHashMap<>();
            index++;
            whitespace();
            if (peek() == '}') {
                index++;
                return result;
            }

            while (true) {
                whitespace();
                String key = string();
                whitespace();
                expect(':');
                result.put(key, value());
                whitespace();
                if (peek() == ',') {
                    index++;
                } else {
                    expect('}');
                    return result;
                }
            }
        }

        private List<Object> array() {
            List<Object> result = new ArrayList<>();
            index++;
            whitespace();
            if (peek() == ']') {
                index++;
                return result;
            }

            while (true) {
                result.add(value());
                whitespace();
                if (peek() == ',') {
                    index++;
                } else {
                    expect(']');
                    return result;
                }
            }
        }

        private String string() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (true) {
                char c = json.charAt(index++);
                if (c == '"') {
                    return builder.toString();
                } else if (c == '\\') {
                    char escaped = json.charAt(index++);
                    switch (escaped) {
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'u' -> {
                            builder.append((char) Integer.parseInt(json.substring(index, index + 4), 16));
                            index += 4;
                        }
                        default -> builder.append(escaped);
                    }
                } else {
                    builder.append(c);
                }
            }
        }

        private Object number() {
            int start = index;
            while (index < json.length() && "+-0123456789.eE".indexOf(json.charAt(index)) >= 0) {
                index++;
            }

            String number = json.substring(start, index);
            if (number.isEmpty()) {
                throw error("Unexpected character");
            }

            return number.contains(".") || number.contains("e") || number.contains("E") ? (Object) Double.parseDouble(number) : (Object) Long.parseLong(number);
        }

        private Object literal(String literal, Object value) {
            if (!json.startsWith(literal, index)) {
                throw error("Expected " + literal);
            }

            index += literal.length();
            return value;
        }

        private void expect(char c) {
            if (peek() != c) {
                throw error("Expected " + c);
            }

            index++;
        }

        private char peek() {
            if (index >= json.length()) {
                throw error("Unexpected end");
            }

            return json.charAt(index);
        }

        private void whitespace() {
            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at " + index + " in " + json);
        }
    }

}
