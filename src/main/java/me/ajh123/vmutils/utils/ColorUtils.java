package me.ajh123.vmutils.utils;

import me.ajh123.vmutils.api.Terminal;

import java.awt.*;

public class ColorUtils {
    // Safe brightening: try to make color brighter while avoiding full white
    public static Color brighterIfPossible(Color c) {
        if (c == null) return c;
        // Use Color.brighter() once to keep behavior predictable
        Color brighter = c.brighter();
        // If brighter equals the same (rare), fall back to small component increase
        if (brighter.equals(c)) {
            int r = Math.min(255, (int) (c.getRed() * 1.2 + 10));
            int g = Math.min(255, (int) (c.getGreen() * 1.2 + 10));
            int b = Math.min(255, (int) (c.getBlue() * 1.2 + 10));
            return new Color(r, g, b);
        }
        return brighter;
    }

    // Clamp 0-255
    public static int clamp(int v) {
        if (v < 0) return 0;
        return Math.min(v, 255);
    }

    // Map 0..7 base ANSI color to Color; bright flag gives brighter variations.
    // Use Java built-in Color.<name> for base (non-dark) palette as requested.
    public static Color ansiColor(Terminal.TerminalState state, int idx, boolean bright) {
        // Base ANSI colors using Java Color constants
        Color base;
        switch (idx) {
            case 0 -> base = Color.BLACK;
            case 1 -> base = Color.RED;
            case 2 -> base = Color.GREEN;
            case 3 -> base = Color.YELLOW;
            case 4 -> base = Color.BLUE;
            case 5 -> base = Color.MAGENTA;
            case 6 -> base = Color.CYAN;
            case 7 -> base = Color.WHITE;
            default -> base = state.defaultFg;
        }
        if (bright) {
            // Use .brighter() for bright variants
            return base.brighter();
        } else {
            return base;
        }
    }

    // Convert 256-color index to java.awt.Color
    public static Color colorFrom256(Terminal.TerminalState state, int index) {
        if (index < 0) index = 0;
        if (index > 255) index = 255;
        if (index < 16) {
            // system colors: map 0-7 standard, 8-15 bright
            int base = index % 8;
            boolean bright = index >= 8;
            return ansiColor(state, base, bright);
        } else if (index <= 231) {
            // 6x6x6 color cube
            int idx = index - 16;
            int b = idx % 6;
            int g = (idx / 6) % 6;
            int r = (idx / 36) % 6;
            int R = r == 0 ? 0 : 55 + r * 40;
            int G = g == 0 ? 0 : 55 + g * 40;
            int B = b == 0 ? 0 : 55 + b * 40;
            return new Color(R, G, B);
        } else {
            // grayscale ramp 232-255
            int gray = 8 + (index - 232) * 10;
            gray = clamp(gray);
            return new Color(gray, gray, gray);
        }
    }
}
