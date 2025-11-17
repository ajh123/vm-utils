package me.ajh123.vmutils.device;

import me.ajh123.vmutils.api.SerialConsole;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class JavaIOSerial implements SerialConsole {
    private final InputStreamReader isr;

    public JavaIOSerial() {
        isr = new InputStreamReader(System.in);
    }

    @Override
    public boolean hasInput() throws IOException {
        return isr.ready();
    }

    @Override
    public byte dequeueInput() throws IOException {
        return (byte) isr.read();
    }

    @Override
    public void putChar(char c) {
        System.out.print(c);
    }
}
