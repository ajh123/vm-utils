package me.ajh123.vmutils.device;

import me.ajh123.vmutils.api.SerialConsole;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class JavaIOSerial implements SerialConsole {
    private final BufferedReader br;

    public JavaIOSerial() {
        InputStreamReader isr = new InputStreamReader(System.in);
        this.br = new BufferedReader(isr);
    }

    @Override
    public boolean hasInput() throws IOException {
        return br.ready();
    }

    @Override
    public byte dequeueInput() throws IOException {
        return (byte) br.read();
    }

    @Override
    public void putChar(char c) {
        System.out.print(c);
    }
}
