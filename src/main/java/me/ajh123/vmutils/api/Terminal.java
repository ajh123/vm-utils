package me.ajh123.vmutils.api;

import java.awt.*;

public abstract class Terminal {
    protected final int rows;
    protected final int cols;
    protected final char[][] buffer;
    protected final Color[][] fg;
    protected final Color[][] bg;
    protected Color currentBg;
    protected Color currentFg;
    protected int cursorRow = 0;
    protected int cursorCol = 0;

    public Terminal(int rows, int cols, Color foreground, Color background) {
        this.rows = rows;
        this.cols = cols;
        buffer = new char[rows][cols];
        fg = new Color[rows][cols];
        bg = new Color[rows][cols];
        currentBg = background;
        currentFg = foreground;
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

    // Put a character at a specific position
    public void putChar(int row, int col, char ch) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return;
        buffer[row][col] = ch;
        fg[row][col] = currentFg;
        bg[row][col] = currentBg;
        refreshCell(row, col);
    }

    // Put a character at the cursor position
    public void putChar(char ch) {
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

    // Abstract rendering hooks
    protected abstract void refresh();           // redraw the entire terminal
    protected abstract void refreshCell(int row, int col); // redraw one cell
}


