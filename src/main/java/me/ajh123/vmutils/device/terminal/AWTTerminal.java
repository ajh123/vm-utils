package me.ajh123.vmutils.device.terminal;

import me.ajh123.vmutils.api.Terminal;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class AWTTerminal extends Terminal implements KeyListener {
    private final Component target;   // Component to draw onto
    private final int cellWidth;
    private final int cellHeight;
    private final Font font;

    public AWTTerminal(int rows, int cols, Component target, int cellWidth, int cellHeight, Color foreground, Color background) {
        super(rows, cols, foreground, background);
        this.target = target;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.font = new Font("Monospaced", Font.PLAIN, cellHeight - 2);
    }

    @Override
    public void refresh() {
        if (target != null) {
            target.repaint();
        }
    }

    @Override
    public void refreshCell(int row, int col) {
        if (target != null) {
            target.repaint(col * cellWidth, row * cellHeight, cellWidth, cellHeight);
        }
    }

    public void drawAll(Graphics g) {
        g.setFont(font);
        for (int r = 0; r < state.rows; r++) {
            for (int c = 0; c < state.cols; c++) {
                drawCell(g, r, c);
            }
        }
    }

    private void drawCell(Graphics g, int row, int col) {
        g.setColor(state.bg[row][col]);
        g.fillRect(col * cellWidth, row * cellHeight, cellWidth, cellHeight);
        g.setColor(state.fg[row][col]);
        g.drawString(String.valueOf(state.buffer[row][col]),
                col * cellWidth + 2,
                row * cellHeight + cellHeight - 2);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // we only need this method to make Java happy
    }

    @Override
    public void keyPressed(KeyEvent e) {
        char keyChar = e.getKeyChar();
        state.response.add(keyChar);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // we only need this method to make Java happy
    }
}

