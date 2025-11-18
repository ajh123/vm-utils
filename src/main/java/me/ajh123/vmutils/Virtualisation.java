package me.ajh123.vmutils;

import li.cil.sedna.Sedna;
import me.ajh123.vmutils.device.Devices;

public class Virtualisation {
    public static void initialise() {
        Sedna.initialize();
        Devices.initialise();
    }
}
