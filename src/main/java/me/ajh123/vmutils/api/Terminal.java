package me.ajh123.vmutils.api;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Terminal implements SerialConsole {
    protected final List<Character> response = new ArrayList<>();
    protected final int rows;
    protected final int cols;
    protected final char[][] buffer;
    protected final Color[][] fg;
    protected final Color[][] bg;
    protected Color currentBg;
    protected Color currentFg;
    protected final Color defaultFg;
    protected final Color defaultBg;
    protected int cursorRow = 0;
    protected int cursorCol = 0;
    protected boolean inEscape = false; // Are we inside an escape sequence?
    protected StringBuilder escapeBuffer = new StringBuilder(); // Stores the ongoing sequence

    public Terminal(int rows, int cols, Color foreground, Color background) {
        this.rows = rows;
        this.cols = cols;
        buffer = new char[rows][cols];
        fg = new Color[rows][cols];
        bg = new Color[rows][cols];
        currentBg = background;
        currentFg = foreground;
        defaultFg = foreground;   // store the supplied defaults
        defaultBg = background;
        clear();
    }

    // Clears the terminal buffer
    public void clear() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                buffer[r][c] = ' ';
                fg[r][c] = currentFg;
                bg[r][c] = currentBg;
            }
        }
        cursorRow = 0;
        cursorCol = 0;
        refresh();
    }

    @Override
    public boolean hasInput() {
        return !response.isEmpty();
    }

    @Override
    public byte dequeueInput() {
        return (byte) response.remove(0).charValue();
    }

    // Put a character at the cursor position
    @Override
    public void putChar(char ch) {
        if (inEscape) {
            escapeBuffer.append(ch);
            // Check if this completes an escape sequence (usually ends with a letter)
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) {
                handleEscapeSequence(escapeBuffer.toString());
                escapeBuffer.setLength(0); // reset
                inEscape = false;
            }
            return; // do not put this char in the buffer
        }

        if (ch == '\u001B') { // Start of an escape sequence
            inEscape = true;
            escapeBuffer.setLength(0); // clear
            return;
        }

        if (ch == '\n') {
            cursorCol = 0;
            cursorRow++;
            if (cursorRow >= rows) scrollUp();
        } else {
            buffer[cursorRow][cursorCol] = ch;
            fg[cursorRow][cursorCol] = currentFg;
            bg[cursorRow][cursorCol] = currentBg;
            cursorCol++;
            if (cursorCol >= cols) {
                cursorCol = 0;
                cursorRow++;
                if (cursorRow >= rows) scrollUp();
            }
        }
        refresh();
    }

    // Scroll the buffer up by one row
    protected void scrollUp() {
        for (int r = 1; r < rows; r++) {
            System.arraycopy(buffer[r], 0, buffer[r - 1], 0, cols);
            System.arraycopy(fg[r], 0, fg[r - 1], 0, cols);
            System.arraycopy(bg[r], 0, bg[r - 1], 0, cols);
        }
        for (int c = 0; c < cols; c++) {
            buffer[rows - 1][c] = ' ';
            fg[rows - 1][c] = currentFg;
            bg[rows - 1][c] = currentBg;
        }
        cursorRow = rows - 1;
        refresh();
    }

    public Color getCurrentBg() {
        return currentBg;
    }

    public void setCurrentBg(Color currentBg) {
        this.currentBg = currentBg;
    }

    public Color getCurrentFg() {
        return currentFg;
    }

    public void setCurrentFg(Color currentFg) {
        this.currentFg = currentFg;
    }

    public Color getDefaultFg() {
        return defaultFg;
    }

    public Color getDefaultBg() {
        return defaultBg;
    }

    // Abstract rendering hooks
    protected abstract void refresh();           // redraw the entire terminal
    protected abstract void refreshCell(int row, int col); // redraw one cell

    protected void handleEscapeSequence(String seq) {
        if (seq.isEmpty()) return;

        // Strip leading '[' if present
        if (seq.charAt(0) == '[') {
            seq = seq.substring(1);
        }

        char command = seq.charAt(seq.length() - 1); // last character
        String[] seqParts = seq.substring(0, seq.length() - 1).split(";");

        System.out.println(Arrays.toString(seqParts)+"/"+seq);

        if (command == 'm') {
            // Color sequence
            for (String code : seqParts) {
                int c = Integer.parseInt(code.isEmpty() ? "0" : code);
                switch (c) {
                    case 0 -> { currentFg = defaultFg; currentBg = defaultBg; } // Reset
                    case 30 -> currentFg = Color.BLACK;
                    case 31 -> currentFg = Color.RED;
                    case 32 -> currentFg = Color.GREEN;
                    case 33 -> currentFg = Color.YELLOW;
                    case 34 -> currentFg = Color.BLUE;
                    case 35 -> currentFg = Color.MAGENTA;
                    case 36 -> currentFg = Color.CYAN;
                    case 37 -> currentFg = Color.WHITE;
                    case 40 -> currentBg = Color.BLACK;
                    case 41 -> currentBg = Color.RED;
                    case 42 -> currentBg = Color.GREEN;
                    case 43 -> currentBg = Color.YELLOW;
                    case 44 -> currentBg = Color.BLUE;
                    case 45 -> currentBg = Color.MAGENTA;
                    case 46 -> currentBg = Color.CYAN;
                    case 47 -> currentBg = Color.WHITE;
                }
            }
        } else if (command == 'H' || command == 'f') {
            // Cursor positioning: "[row;colH"
            int r = seqParts.length > 0 && !seqParts[0].isEmpty() ? Integer.parseInt(seqParts[0]) - 1 : 0;
            int c = seqParts.length > 1 && !seqParts[1].isEmpty() ? Integer.parseInt(seqParts[1]) - 1 : 0;
            cursorRow = Math.max(0, Math.min(rows - 1, r));
            cursorCol = Math.max(0, Math.min(cols - 1, c));
        } else if (command == 'J') {
            clear();
        }
    }
}


