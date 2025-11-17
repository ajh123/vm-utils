package me.ajh123.vmutils.examples;

import li.cil.sedna.Sedna;
import li.cil.sedna.buildroot.Buildroot;
import me.ajh123.vmutils.api.SerialConsole;
import me.ajh123.vmutils.device.AWTTerminal;
import me.ajh123.vmutils.machine.R5VirtualMachine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AWTDemo {
    private static final List<Character> input = new ArrayList<>();
    private static AWTTerminal terminal;

    public static void main(String[] args) {
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

        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                char keyChar = e.getKeyChar();
                input.add(keyChar);
            }
        });

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

        Sedna.initialize();
        R5VirtualMachine vm = new R5VirtualMachine(
                Buildroot.getFirmware(),
                Buildroot.getLinuxImage(),
                Buildroot.getRootFilesystem(),
                new SerialConsole() {
                    @Override
                    public boolean hasInput() throws IOException {
                        return !input.isEmpty();
                    }

                    @Override
                    public byte dequeueInput() throws IOException {
                        return (byte) input.remove(0).charValue();
                    }

                    @Override
                    public void putChar(char c) {
                        terminal.putChar(c);
                    }
                }
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
