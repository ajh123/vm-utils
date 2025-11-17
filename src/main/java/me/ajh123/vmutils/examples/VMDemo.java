package me.ajh123.vmutils.examples;

import li.cil.sedna.Sedna;
import me.ajh123.vmutils.device.JavaIOSerial;
import me.ajh123.vmutils.machine.R5VirtualMachine;

import li.cil.sedna.buildroot.Buildroot;

public class VMDemo {
    public static void main(String[] args) {
        Sedna.initialize();
        R5VirtualMachine vm = new R5VirtualMachine(
                Buildroot.getFirmware(),
                Buildroot.getLinuxImage(),
                Buildroot.getRootFilesystem(),
                new JavaIOSerial()
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
