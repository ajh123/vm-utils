package me.ajh123.vmutils.api;

import java.io.IOException;

public interface SerialConsole {
    boolean hasInput() throws IOException;
    byte dequeueInput() throws IOException;
    void putChar(char c);
}
