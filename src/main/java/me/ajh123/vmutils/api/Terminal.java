package me.ajh123.vmutils.api;

import me.ajh123.vmutils.utils.ColorUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static me.ajh123.vmutils.utils.EscapeCodes.handleEscapeSequence;

public abstract class Terminal implements SerialConsole {
    protected boolean inEscape = false; // Are we inside an escape sequence?
    protected StringBuilder escapeBuffer = new StringBuilder(); // Stores the ongoing sequence

    protected final TerminalState state;


    public Terminal(int rows, int cols, Color foreground, Color background) {
        this.state = new TerminalState(this, rows, cols, foreground, background);
        clear();
    }

    // Clears the terminal buffer
    public void clear() {
        for (int r = 0; r < this.state.rows; r++) {
            for (int c = 0; c < this.state.cols; c++) {
                this.state.buffer[r][c] = ' ';
                this.state.fg[r][c] = this.state.currentFg;
                this.state.bg[r][c] = this.state.currentBg;
            }
        }
        this.state.cursorRow = 0;
        this.state.cursorCol = 0;
        refresh();
    }

    @Override
    public boolean hasInput() {
        return !this.state.response.isEmpty();
    }

    @Override
    public byte dequeueInput() {
        return (byte) this.state.response.remove(0).charValue();
    }

    // Put a character at the cursor position
    @Override
    public void putChar(char ch) {
        if (inEscape) {
            escapeBuffer.append(ch);
            // Complete when we encounter a final byte (generally a letter) or '~'
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || ch == '~') {
                handleEscapeSequence(escapeBuffer.toString(), state);
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
            this.state.cursorCol = 0;
            this.state.cursorRow++;
            if (this.state.cursorRow >= this.state.rows) scrollUp();
        } else {
            // Store character and its current attributes
            this.state.buffer[this.state.cursorRow][this.state.cursorCol] = ch;
            this.state.fg[this.state.cursorRow][this.state.cursorCol] = effectiveForeground();
            this.state.bg[this.state.cursorRow][this.state.cursorCol] = effectiveBackground();
            this.state.cursorCol++;
            if (this.state.cursorCol >= this.state.cols) {
                this.state.cursorCol = 0;
                this.state.cursorRow++;
                if (this.state.cursorRow >= this.state.rows) scrollUp();
            }
        }
        refresh();
    }

    // Scroll the buffer up by one row
    protected void scrollUp() {
        for (int r = 1; r < this.state.rows; r++) {
            System.arraycopy(this.state.buffer[r], 0, this.state.buffer[r - 1], 0, this.state.cols);
            System.arraycopy(this.state.fg[r], 0, this.state.fg[r - 1], 0, this.state.cols);
            System.arraycopy(this.state.bg[r], 0, this.state.bg[r - 1], 0, this.state.cols);
        }
        for (int c = 0; c < this.state.cols; c++) {
            this.state.buffer[this.state.rows - 1][c] = ' ';
            this.state.fg[this.state.rows - 1][c] = this.state.currentFg;
            this.state.bg[this.state.rows - 1][c] = this.state.currentBg;
        }
        this.state.cursorRow = this.state.rows - 1;
        refresh();
    }

    public Color getCurrentBg() {
        return this.state.currentBg;
    }

    public void setCurrentBg(Color currentBg) {
        this.state.currentBg = currentBg;
    }

    public Color getCurrentFg() {
        return this.state.currentFg;
    }

    public void setCurrentFg(Color currentFg) {
        this.state.currentFg = currentFg;
    }

    public Color getDefaultFg() {
        return this.state.defaultFg;
    }

    public Color getDefaultBg() {
        return this.state.defaultBg;
    }

    // Abstract rendering hooks
    public abstract void refresh();           // redraw the entire terminal
    public abstract void refreshCell(int row, int col); // redraw one cell

    // Compute effective foreground taking attributes into account (bold/inverse)
    protected Color effectiveForeground() {
        Color fgc = this.state.currentFg;
        Color bgc = this.state.currentBg;
        if (this.state.attrInverse) {
            // When inverse, swap fg/bg
            fgc = bgc;
        }
        if (this.state.attrBold) {
            // Brighten color for bold (non-destructive)
            fgc = ColorUtils.brighterIfPossible(fgc);
        }
        return fgc;
    }

    // Compute effective background taking attributes into account (inverse)
    protected Color effectiveBackground() {
        Color bgc = this.state.currentBg;
        Color fgc = this.state.currentFg;
        if (this.state.attrInverse) {
            bgc = fgc;
        }
        return bgc;
    }

    public static class TerminalState {
        public final Terminal parent;
        public final int rows;
        public final int cols;
        public final List<Character> response = new ArrayList<>();
        public final char[][] buffer;
        public final Color[][] fg;
        public final Color[][] bg;
        public Color currentBg;
        public Color currentFg;
        public final Color defaultFg;
        public final Color defaultBg;
        public int cursorRow = 0;
        public int cursorCol = 0;

        // Attribute state

        public boolean attrBold = false;
        public boolean attrUnderline = false;
        public boolean attrInverse = false;
        public boolean cursorVisible = true;

        // Saved state (for CSI s / u)
        public int savedCursorRow = 0;
        public int savedCursorCol = 0;
        public Color savedFg = null;
        public Color savedBg = null;
        public boolean savedBold = false;
        public boolean savedUnderline = false;
        public boolean savedInverse = false;

        public TerminalState(Terminal parent, int rows, int cols, Color defaultFg, Color defaultBg) {
            this.parent = parent;
            this.rows = rows;
            this.cols = cols;
            this.defaultFg = defaultFg;
            this.defaultBg = defaultBg;
            this.currentFg = defaultFg;
            this.currentBg = defaultBg;
            this.buffer = new char[rows][cols];
            this.fg = new Color[rows][cols];
            this.bg = new Color[rows][cols];
        }
    }
}
