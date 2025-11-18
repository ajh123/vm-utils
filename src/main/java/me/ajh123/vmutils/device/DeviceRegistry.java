package me.ajh123.vmutils.device;

import li.cil.sedna.api.device.Device;
import li.cil.sedna.api.device.PhysicalMemory;
import li.cil.sedna.riscv.R5Board;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DeviceRegistry {
    private final Map<String, DeviceType<?>> factories = new HashMap<>();

    public <T extends Device> DeviceType<T> register(String type, DeviceType<T> factory) {
        factories.put(type, factory);
        return factory;
    }

    public Optional<DeviceType<?>> get(String type) {
        return Optional.ofNullable(factories.get(type));
    }

    @FunctionalInterface
    public interface DeviceFactory<T extends Device> {
        T create(Map<String, Object> options, R5Board board, PhysicalMemory memory) throws IOException;
    }

    public static class DeviceType<T extends Device> {
        private final DeviceFactory<T> factory;
        private T device;

        /** Require a factory for creation. */
        public DeviceType(DeviceFactory<T> factory) {
            if (factory == null) {
                throw new IllegalArgumentException("DeviceFactory cannot be null");
            }
            this.factory = factory;
        }

        /** Create and store the device internally. */
        public void attach(Map<String, Object> options, R5Board board, PhysicalMemory memory) throws IOException {
            this.device = factory.create(options, board, memory);
        }

        /** Get the previously created device. */
        public Optional<T> getDevice() {
            return Optional.ofNullable(device);
        }
    }
}

