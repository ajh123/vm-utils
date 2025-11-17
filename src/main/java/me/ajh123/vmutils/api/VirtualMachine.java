package me.ajh123.vmutils.api;

import li.cil.sedna.api.Sizes;
import li.cil.sedna.api.device.PhysicalMemory;
import li.cil.sedna.api.memory.MemoryAccessException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public abstract class VirtualMachine {
    // cached images (immutable after construction)
    protected final byte[] firmwareBytes;
    protected final byte[] kernelBytes;
    protected final byte[] rootfsBytes;

    public VirtualMachine(final InputStream firmware,
                            final InputStream kernel,
                            final InputStream rootfs
    ) {
        Objects.requireNonNull(firmware, "firmware");
        Objects.requireNonNull(kernel, "kernel");
        Objects.requireNonNull(rootfs, "rootfs");

        try {
            this.firmwareBytes = readAllAndClose(firmware);
            this.kernelBytes = readAllAndClose(kernel);
            this.rootfsBytes = readAllAndClose(rootfs);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read VM images", e);
        }
    }


    /**
     * Prepare the board and load images into RAM. Safe to call before start().
     * Will throw IOException on load failures (including MemoryAccessException).
     */
    public abstract void initialize() throws IOException;

    /**
     * Start VM execution. board.initialize() is invoked here.
     * Call initialize() before start() if not already done.
     */
    public abstract void start() throws IOException;

    /**
     * Stop VM execution gracefully.
     */
    public abstract void stop();

    /**
     * Soft reset the VM: reload images into memory and re-initialize the CPU state.
     */
    public abstract void reset() throws IOException;

    /**
     * Shutdown VM and release any resources if necessary.
     */
    public abstract void shutdown();

    // ---------- Helpers ----------

    protected static byte[] readAllAndClose(final InputStream stream) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(stream)) {
            return bis.readAllBytes();
        }
    }

    /**
     * Load a byte array into the provided PhysicalMemory at the given device-local offset.
     * Performs bounds checks and converts bytes to unsigned longs before calling memory.store.
     *
     * @param memory the PhysicalMemory device
     * @param source the bytes to load
     * @param offset device-local offset (0..memory.getLength()-1)
     * @throws IOException if the image does not fit or a memory access error occurs
     */
    protected void loadProgramBytes(final PhysicalMemory memory,
                                         final byte[] source,
                                         final int offset) throws IOException {
        final int memLen = memory.getLength();
        if (offset < 0) {
            throw new IOException("Negative memory offset: " + offset);
        }
        if ((long) offset + source.length > memLen) {
            throw new IOException(String.format("Image (length=%d) does not fit at offset 0x%X (memory size=%d)",
                    source.length, offset, memLen));
        }

        try {
            for (int i = 0; i < source.length; i++) {
                final long unsigned = source[i] & 0xFFL;
                memory.store(offset + i, unsigned, Sizes.SIZE_8_LOG2);
            }
        } catch (MemoryAccessException mae) {
            throw new IOException("Failed to write to physical memory", mae);
        }
    }
}
