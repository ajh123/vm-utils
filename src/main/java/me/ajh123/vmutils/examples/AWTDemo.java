package me.ajh123.vmutils.examples;

import li.cil.sedna.buildroot.Buildroot;
import me.ajh123.vmutils.Virtualisation;
import me.ajh123.vmutils.device.terminal.AWTTerminal;
import me.ajh123.vmutils.machine.R5VirtualMachine;

import javax.swing.*;
import java.awt.*;

public class AWTDemo {
    private static AWTTerminal terminal;

    public static void main(String[] args) {
        Virtualisation.initialise();
        // Swing setup
        JFrame frame = new JFrame("AWTTerminal Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                terminal.drawAll(g);
            }
        };

        panel.setFocusable(true);
        panel.requestFocusInWindow();
        panel.setBackground(Color.BLACK);

        frame.add(panel);
        frame.setVisible(true);

        int rows = 25;
        int cols = 80;
        int cellWidth = 12;
        int cellHeight = 20;

        // Create the terminal with the panel as the target
        terminal = new AWTTerminal(rows, cols, panel, cellWidth, cellHeight, Color.WHITE, Color.BLACK);

        panel.addKeyListener(terminal);

        R5VirtualMachine vm = new R5VirtualMachine(
                Buildroot.getFirmware(),
                Buildroot.getLinuxImage(),
                Buildroot.getRootFilesystem(),
                terminal
        );
        try {
            vm.initialize();
            vm.start();
            vm.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
