package me.ajh123.vmutils.utils;

import me.ajh123.vmutils.api.Terminal;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static me.ajh123.vmutils.utils.ColorUtils.*;

public class EscapeCodes {
    public static void handleEscapeSequence(String seqOrig, Terminal.TerminalState state) {
        if (seqOrig == null || seqOrig.isEmpty()) return;

        String seq = seqOrig;

        // handle CSI sequences and others
        // If sequence begins with '[' treat as CSI.
        boolean csi = false;
        if (seq.charAt(0) == '[') {
            csi = true;
            seq = seq.substring(1);
        }

        // Support private-mode ? parameters (e.g. "?25h")
        boolean isPrivate = false;
        if (!seq.isEmpty() && seq.charAt(0) == '?') {
            isPrivate = true;
            seq = seq.substring(1);
        }

        if (!csi) {
            // Non-CSI sequences: ignore unknown non-CSI for now.
            return;
        }

        // final byte is the command letter (or '~' for some sequences)
        char command = seq.charAt(seq.length() - 1);
        String paramStr = seq.substring(0, seq.length() - 1);

        // If there are no parameters for SGR (i.e. paramStr is empty and command == 'm'),
        // treat it as "0" (reset) per spec. More generally, when paramStr is empty we
        // substitute a single "0" so code handling uses reset.
        String[] rawParts = paramStr.isEmpty() ? new String[]{"0"} : paramStr.split(";");
        List<String> partsList = Arrays.asList(rawParts);

        // Helper to parse integer with default (null when missing/invalid)
        java.util.function.Function<Integer, Integer> getParam = (idx) -> {
            if (idx < partsList.size()) {
                String s = partsList.get(idx);
                if (s == null || s.isEmpty()) return null;
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException ex) {
                    return null;
                }
            } else {
                return null;
            }
        };

        switch (command) {
            case 'm' -> { // SGR - Select Graphic Rendition
                // iterate through parts with index because 38/48 need lookahead
                for (int i = 0; i < partsList.size(); i++) {
                    String p = partsList.get(i);
                    int code;
                    if (p == null || p.isEmpty()) {
                        code = 0;
                    } else {
                        try {
                            code = Integer.parseInt(p);
                        } catch (NumberFormatException ex) {
                            continue;
                        }
                    }
                    switch (code) {
                        case 0 -> { // reset all attributes
                            state.attrBold = false;
                            state.attrUnderline = false;
                            state.attrInverse = false;
                            state.currentFg = state.defaultFg;
                            state.currentBg = state.defaultBg;
                        }
                        case 1 -> state.attrBold = true;
                        case 22 -> state.attrBold = false;
                        case 4 -> state.attrUnderline = true;
                        case 24 -> state.attrUnderline = false;
                        case 7 -> state.attrInverse = true;
                        case 27 -> state.attrInverse = false;
                        case 39 -> state.currentFg = state.defaultFg;
                        case 49 -> state.currentBg = state.defaultBg;
                        // standard foreground 30-37 (use Color.<name> as requested)
                        case 30 -> state.currentFg = ansiColor(state, 0, false);
                        case 31 -> state.currentFg = ansiColor(state, 1, false);
                        case 32 -> state.currentFg = ansiColor(state, 2, false);
                        case 33 -> state.currentFg = ansiColor(state, 3, false);
                        case 34 -> state.currentFg = ansiColor(state, 4, false);
                        case 35 -> state.currentFg = ansiColor(state, 5, false);
                        case 36 -> state.currentFg = ansiColor(state, 6, false);
                        case 37 -> state.currentFg = ansiColor(state, 7, false);
                        // standard background 40-47
                        case 40 -> state.currentBg = ansiColor(state, 0, false);
                        case 41 -> state.currentBg = ansiColor(state, 1, false);
                        case 42 -> state.currentBg = ansiColor(state, 2, false);
                        case 43 -> state.currentBg = ansiColor(state, 3, false);
                        case 44 -> state.currentBg = ansiColor(state, 4, false);
                        case 45 -> state.currentBg = ansiColor(state, 5, false);
                        case 46 -> state.currentBg = ansiColor(state, 6, false);
                        case 47 -> state.currentBg = ansiColor(state, 7, false);
                        // bright foreground 90-97 (brighter variants of the base Color.<name>)
                        case 90 -> state.currentFg = ansiColor(state, 0, true);
                        case 91 -> state.currentFg = ansiColor(state, 1, true);
                        case 92 -> state.currentFg = ansiColor(state, 2, true);
                        case 93 -> state.currentFg = ansiColor(state, 3, true);
                        case 94 -> state.currentFg = ansiColor(state, 4, true);
                        case 95 -> state.currentFg = ansiColor(state, 5, true);
                        case 96 -> state.currentFg = ansiColor(state, 6, true);
                        case 97 -> state.currentFg = ansiColor(state, 7, true);
                        // bright background 100-107
                        case 100 -> state.currentBg = ansiColor(state, 0, true);
                        case 101 -> state.currentBg = ansiColor(state, 1, true);
                        case 102 -> state.currentBg = ansiColor(state, 2, true);
                        case 103 -> state.currentBg = ansiColor(state, 3, true);
                        case 104 -> state.currentBg = ansiColor(state, 4, true);
                        case 105 -> state.currentBg = ansiColor(state, 5, true);
                        case 106 -> state.currentBg = ansiColor(state, 6, true);
                        case 107 -> state.currentBg = ansiColor(state, 7, true);
                        // extended color handling 38/48
                        case 38, 48 -> {
                            boolean isForeground = (code == 38);
                            // Look ahead; ensure there's more parts
                            int nextIndex = i + 1;
                            if (nextIndex < partsList.size()) {
                                String modeStr = partsList.get(nextIndex);
                                if ("5".equals(modeStr)) {
                                    // 256-color palette: 38;5;<n>
                                    int colorIndex = 0;
                                    if (nextIndex + 1 < partsList.size()) {
                                        try {
                                            colorIndex = Integer.parseInt(partsList.get(nextIndex + 1));
                                        } catch (NumberFormatException ignored) {
                                        }
                                    }
                                    Color col = colorFrom256(state, colorIndex);
                                    if (isForeground) state.currentFg = col; else state.currentBg = col;
                                    i += 2; // consumed mode and index
                                } else if ("2".equals(modeStr)) {
                                    // truecolor: 38;2;r;g;b
                                    if (nextIndex + 3 < partsList.size()) {
                                        try {
                                            int r = Integer.parseInt(partsList.get(nextIndex + 1));
                                            int g = Integer.parseInt(partsList.get(nextIndex + 2));
                                            int b = Integer.parseInt(partsList.get(nextIndex + 3));
                                            Color col = new Color(clamp(r), clamp(g), clamp(b));
                                            if (isForeground) state.currentFg = col; else state.currentBg = col;
                                        } catch (NumberFormatException ex) {
                                            // ignore invalid
                                        }
                                    }
                                    i += 4; // consumed mode + r,g,b
                                }
                            }
                        }
                        default -> {
                            // Unknown SGR code: ignore
                        }
                    }
                }
                // After attribute changes, trigger refresh
                state.parent.refresh();
            }
            case 'H', 'f' -> { // Cursor positioning: "[row;colH"
                Integer r = getParam.apply(0);
                Integer c = getParam.apply(1);
                int rr = (r == null) ? 0 : (r - 1);
                int cc = (c == null) ? 0 : (c - 1);
                state.cursorRow = Math.max(0, Math.min(state.rows - 1, rr));
                state.cursorCol = Math.max(0, Math.min(state.cols - 1, cc));
                state.parent.refresh();
            }
            case 'A' -> { // cursor up
                Integer n = getParam.apply(0);
                int delta = (n == null) ? 1 : n;
                state.cursorRow = Math.max(0,state. cursorRow - delta);
                state.parent.refresh();
            }
            case 'B' -> { // cursor down
                Integer n = getParam.apply(0);
                int delta = (n == null) ? 1 : n;
                state.cursorRow = Math.min(state.rows - 1, state.cursorRow + delta);
                state.parent.refresh();
            }
            case 'C' -> { // cursor forward
                Integer n = getParam.apply(0);
                int delta = (n == null) ? 1 : n;
                state.cursorCol = Math.min(state.cols - 1, state.cursorCol + delta);
                state.parent.refresh();
            }
            case 'D' -> { // cursor back
                Integer n = getParam.apply(0);
                int delta = (n == null) ? 1 : n;
                state.cursorCol = Math.max(0, state.cursorCol - delta);
                state.parent.refresh();
            }
            case 's' -> { // save cursor and attributes
                state.savedCursorRow = state.cursorRow;
                state.savedCursorCol = state.cursorCol;
                state.savedFg = state.currentFg;
                state.savedBg = state.currentBg;
                state.savedBold = state.attrBold;
                state.savedUnderline = state.attrUnderline;
                state.savedInverse = state.attrInverse;
            }
            case 'u' -> { // restore cursor and attributes
                state.cursorRow = Math.max(0, Math.min(state.rows - 1, state.savedCursorRow));
                state.cursorCol = Math.max(0, Math.min(state.cols - 1, state.savedCursorCol));
                if (state.savedFg != null) state.currentFg = state.savedFg;
                if (state.savedBg != null) state.currentBg = state.savedBg;
                state.attrBold = state.savedBold;
                state.attrUnderline = state.savedUnderline;
                state.attrInverse = state.savedInverse;
                state.parent.refresh();
            }
            case 'K' -> { // Erase in line
                // params: 0 = cursor to end, 1 = start to cursor, 2 = entire line
                Integer p = getParam.apply(0);
                int mode = (p == null) ? 0 : p;
                int r = state.cursorRow;
                switch (mode) {
                    case 0 -> { // from cursor to end
                        for (int c = state.cursorCol; c < state.cols; c++) {
                            state.buffer[r][c] = ' ';
                            state.fg[r][c] = state.currentFg;
                            state.bg[r][c] = state.currentBg;
                            state.parent.refreshCell(r, c);
                        }
                    }
                    case 1 -> { // from start to cursor
                        for (int c = 0; c <= state.cursorCol; c++) {
                            state.buffer[r][c] = ' ';
                            state.fg[r][c] = state.currentFg;
                            state.bg[r][c] = state.currentBg;
                            state.parent.refreshCell(r, c);
                        }
                    }
                    case 2 -> { // entire line
                        for (int c = 0; c < state.cols; c++) {
                            state.buffer[r][c] = ' ';
                            state.fg[r][c] = state.currentFg;
                            state.bg[r][c] = state.currentBg;
                            state.parent.refreshCell(r, c);
                        }
                    }
                }
                state.parent.refresh();
            }
            case 'J' -> { // Erase in display
                // params: 0 = cursor to end, 1 = start to cursor, 2 = entire screen
                Integer p = getParam.apply(0);
                int mode = (p == null) ? 0 : p;
                switch (mode) {
                    case 0 -> { // cursor to end of screen
                        // clear current line from cursor, then lines below
                        int r = state.cursorRow;
                        for (int c = state.cursorCol; c < state.cols; c++) {
                            state.buffer[r][c] = ' ';
                            state.fg[r][c] = state.currentFg;
                            state.bg[r][c] = state.currentBg;
                            state.parent.refreshCell(r, c);
                        }
                        for (int rr = r + 1; rr < state.rows; rr++) {
                            for (int c = 0; c < state.cols; c++) {
                                state.buffer[rr][c] = ' ';
                                state.fg[rr][c] = state.currentFg;
                                state.bg[rr][c] = state.currentBg;
                                state.parent.refreshCell(rr, c);
                            }
                        }
                    }
                    case 1 -> { // start to cursor
                        int r = state.cursorRow;
                        for (int c = 0; c <= state.cursorCol; c++) {
                            state.buffer[r][c] = ' ';
                            state.fg[r][c] = state.currentFg;
                            state.bg[r][c] = state.currentBg;
                            state.parent.refreshCell(r, c);
                        }
                        for (int rr = 0; rr < r; rr++) {
                            for (int c = 0; c < state.cols; c++) {
                                state.buffer[rr][c] = ' ';
                                state.fg[rr][c] = state.currentFg;
                                state.bg[rr][c] = state.currentBg;
                                state.parent.refreshCell(rr, c);
                            }
                        }
                    }
                    case 2 -> { // entire screen
                        state.parent.clear();
                    }
                }
                state.parent.refresh();
            }
            default -> {
                // Private-mode commands (starting with ?), for example "?25h" (show cursor) and "?25l" (hide cursor)
                if (isPrivate) {
                    if (command == 'h' || command == 'l') {
                        Integer p0 = getParam.apply(0);
                        if (p0 != null) {
                            if (p0 == 25) { // cursor visibility
                                state.cursorVisible = (command == 'h');
                                state.parent.refresh();
                            }
                        }
                    }
                }
                // unsupported command - ignore for now
            }
        }
    }
}
