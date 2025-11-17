package me.ajh123.vmutils.device;

import me.ajh123.vmutils.api.Terminal;

import java.awt.*;
import javax.swing.*;

public class AWTTerminal extends Terminal {
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
    protected void refresh() {
        if (target != null) {
            target.repaint();
        }
    }

    @Override
    protected void refreshCell(int row, int col) {
        if (target != null) {
            target.repaint(col * cellWidth, row * cellHeight, cellWidth, cellHeight);
        }
    }

    public void drawAll(Graphics g) {
        g.setFont(font);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                drawCell(g, r, c);
            }
        }
    }

    private void drawCell(Graphics g, int row, int col) {
        g.setColor(bg[row][col]);
        g.fillRect(col * cellWidth, row * cellHeight, cellWidth, cellHeight);
        g.setColor(fg[row][col]);
        g.drawString(String.valueOf(buffer[row][col]),
                col * cellWidth + 2,
                row * cellHeight + cellHeight - 2);
    }
}

