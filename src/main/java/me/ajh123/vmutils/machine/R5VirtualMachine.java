package me.ajh123.vmutils.machine;

import li.cil.sedna.api.device.PhysicalMemory;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import li.cil.sedna.device.memory.Memory;
import li.cil.sedna.device.rtc.GoldfishRTC;
import li.cil.sedna.device.rtc.SystemTimeRealTimeCounter;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import li.cil.sedna.riscv.R5Board;
import me.ajh123.vmutils.api.SerialConsole;
import me.ajh123.vmutils.api.VirtualMachine;
import me.ajh123.vmutils.device.DeviceRegistry;
import me.ajh123.vmutils.device.Devices;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class R5VirtualMachine extends VirtualMachine {
    private static final int DEFAULT_MEMORY_BYTES = 32 * 1024 * 1024;
    private static final int KERNEL_LOAD_ADDRESS = 0x200000;
    private static final int CPU_STEP_CYCLES = 1_000; // cycles per small step
    private static final long NANOS_PER_MS = 1_000_000L;

    private final R5Board board;
    private final PhysicalMemory memory;
    private final DeviceRegistry.DeviceType<UART16550A> uart;
    private final GoldfishRTC rtc;
    private final SerialConsole viewer;

    private final DeviceRegistry.DeviceType<VirtIOBlockDevice> hdd; // created from cached rootfs bytes

    // lifecycle state
    private volatile boolean running = false;
    private volatile boolean initialized = false;

    public R5VirtualMachine(final InputStream firmware,
                            final InputStream kernel,
                            final InputStream rootfs,
                            final SerialConsole viewer
    ) {
        super(firmware, kernel, rootfs);
        Objects.requireNonNull(firmware, "firmware");
        Objects.requireNonNull(kernel, "kernel");
        Objects.requireNonNull(rootfs, "rootfs");
        this.viewer = Objects.requireNonNull(viewer, "viewer");


        this.board = new R5Board();
        this.memory = Memory.create(DEFAULT_MEMORY_BYTES);
        this.uart = Devices.UART_16550AD;
        this.rtc = new GoldfishRTC(SystemTimeRealTimeCounter.get());
        this.hdd = Devices.VIRTIO_BLOCK_DEVICE;
    }

    @Override
    public void initialize() throws IOException {
        if (initialized) return;

        // Map devices (device-local memory offset 0 corresponds to RAM start)
        board.addDevice(0x80000000L, memory);

        // Wire interrupts and devices
        uart.attach(new HashMap<>(), board, memory);

        rtc.getInterrupt().set(0xB, board.getInterruptController());
        board.addDevice(rtc);

        Map<String, Object> hddOptions = new HashMap<>();
        hddOptions.put("fsBytes", this.rootfsBytes);
        hdd.attach(hddOptions, board, memory);


        board.setBootArguments("root=/dev/vda ro");

        board.reset();

        // load firmware and kernel into RAM (device-local offsets)
        loadProgramBytes(memory, firmwareBytes, 0);
        loadProgramBytes(memory, kernelBytes, KERNEL_LOAD_ADDRESS);

        initialized = true;
    }

    @Override
    public void start() throws IOException {
        if (!initialized) {
            initialize();
        }
        if (running) return;

        board.initialize();
        board.setRunning(true);
        running = true;
    }

    @Override
    public void stop() {
        running = false;
        board.setRunning(false);
    }

    @Override
    public void reset() throws IOException {
        // stop execution if running
        stop();

        // reload images
        loadProgramBytes(memory, firmwareBytes, 0);
        loadProgramBytes(memory, kernelBytes, KERNEL_LOAD_ADDRESS);

        // re-initialize CPU/board state
        board.initialize();
        board.setRunning(false);
    }

    @Override
    public void shutdown() {
        stop();
        // if hdd or other devices expose close/release, do so here (not assumed in this generic wrapper).
    }

    // ---------- Main run loop ----------

    /**
     * Run the VM; this method returns when the VM stops or the thread is interrupted.
     * Caller responsibility: call start() before run() to ensure board initialized and running flag set.
     */
    public void run() throws Exception {
        final int cpuFrequency = board.getCpu().getFrequency(); // cycles/sec
        final int cyclesPerMs = Math.max(1, cpuFrequency / 1000);
        long nextTick = System.nanoTime();

        Optional<UART16550A> uart_port_opt = uart.getDevice();

        while (board.isRunning() && running && !Thread.currentThread().isInterrupted()) {
            // Run ~1 ms worth of CPU cycles, stepping in CPU_STEP_CYCLES increments
            int cyclesRemaining = cyclesPerMs;
            while (cyclesRemaining > 0 && !Thread.currentThread().isInterrupted() && running) {
                board.step(CPU_STEP_CYCLES);
                cyclesRemaining -= CPU_STEP_CYCLES;

                if (uart_port_opt.isPresent()) {
                    UART16550A uart_port = uart_port_opt.get();
                    // Drain UART output
                    int value;
                    while ((value = uart_port.read()) != -1) {
                        viewer.putChar((char) (value & 0xFF));
                    }

                    // Feed input into UART
                    try {
                        while (viewer.hasInput() && uart_port.canPutByte()) {
                            uart_port.putByte(viewer.dequeueInput());
                        }
                    } catch (IOException ioe) {
                        // If terminal input fails, stop the VM with a clear error
                        stop();
                        throw new IOException("UART input failed", ioe);
                    }
                }
            }

            // If guest requested a restart, reload images and reinitialize CPU
            if (board.isRestarting() && !Thread.currentThread().isInterrupted()) {
                try {
                    loadProgramBytes(memory, firmwareBytes, 0);
                    loadProgramBytes(memory, kernelBytes, KERNEL_LOAD_ADDRESS);
                    board.initialize();
                } catch (IOException ioe) {
                    stop();
                    throw ioe;
                }
            }

            // Use precise sleep to approximate 1ms pacing
            nextTick += NANOS_PER_MS;
            long nanosToSleep = nextTick - System.nanoTime();
            if (nanosToSleep > 0) {
                try {
                    Thread.sleep(nanosToSleep / NANOS_PER_MS, (int) (nanosToSleep % NANOS_PER_MS));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                // We're behind schedule; advance nextTick now to avoid accumulating large negative sleep
                nextTick = System.nanoTime();
            }
        }
    }
}
