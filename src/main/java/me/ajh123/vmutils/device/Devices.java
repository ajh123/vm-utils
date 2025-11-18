package me.ajh123.vmutils.device;

import li.cil.sedna.device.block.ByteBufferBlockDevice;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;

import java.io.ByteArrayInputStream;

public class Devices {
    public static final DeviceRegistry REGISTRY = new DeviceRegistry();

    public static final DeviceRegistry.DeviceType<UART16550A> UART_16550AD = REGISTRY.register(
            "uart16550",
            new DeviceRegistry.DeviceType<>((options, board, memory) -> {
                Integer irq = 0xA;
                if (options.containsKey("irq")) {
                    irq = (Integer) options.get("irq");
                }

                UART16550A uart = new UART16550A();
                uart.getInterrupt().set(irq, board.getInterruptController());
                board.addDevice(uart);
                board.setStandardOutputDevice(uart);

                return uart;
            })
    );

    public static final DeviceRegistry.DeviceType<VirtIOBlockDevice> VIRTIO_BLOCK_DEVICE = REGISTRY.register(
            "virtio-block-device",
            new DeviceRegistry.DeviceType<>((options, board, memory) -> {
                Integer irq = 0x1;
                if (options.containsKey("irq")) {
                    irq = (Integer) options.get("irq");
                }

                byte[] fsBytes = (byte[]) options.get("fsBytes");
                final ByteArrayInputStream bais = new ByteArrayInputStream(fsBytes);

                VirtIOBlockDevice vbd = new VirtIOBlockDevice(
                        board.getMemoryMap(),
                        ByteBufferBlockDevice.createFromStream(bais, true)
                );
                vbd.getInterrupt().set(irq, board.getInterruptController());
                board.addDevice(vbd);
                return vbd;
            })
    );

    public static void initialise() {
        // do nothing except get static members ready
    }
}
