package org.jyre;

import java.nio.ByteBuffer;
import java.util.UUID;

class UdpBeacon {
    // Beacon protocol constants
    public static final byte BEACON_SIZE = 22;
    public static final byte BEACON_VERSION = 0x01;
    public static final String BEACON_PROTOCOL = "ZRE";

    private final byte[] protocol;
    private final byte version;
    private final UUID uuid;
    private final String identity;
    private final int port;

    private ByteBuffer buffer;

    public UdpBeacon(ByteBuffer buffer) {
        buffer.rewind();

        this.protocol = new byte[3];
        buffer.get(this.protocol);
        this.version = buffer.get();
        this.uuid = new UUID(buffer.getLong(), buffer.getLong());
        this.port = 0xffff & (int) buffer.getShort();
        this.identity = uuid.toString().replace("-", "").toUpperCase();
        this.buffer = buffer;
    }

    public UdpBeacon(UUID uuid, int port) {
        this.protocol = BEACON_PROTOCOL.getBytes();
        this.version = BEACON_VERSION;
        this.uuid = uuid;
        this.identity = uuid.toString().replace("-", "").toUpperCase();
        this.port = port;
    }

    public String getProtocol() {
        return new String(protocol);
    }

    public byte getVersion() {
        return version;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getIdentity() {
        return identity;
    }

    public int getPort() {
        return port;
    }

    public ByteBuffer getBuffer() {
        if (buffer == null) {
            buffer = ByteBuffer.allocate(BEACON_SIZE);
            buffer.put(protocol);
            buffer.put(version);
            buffer.putLong(uuid.getMostSignificantBits ());
            buffer.putLong(uuid.getLeastSignificantBits ());
            buffer.putShort((short) port);
            buffer.flip();
        }

        buffer.rewind();
        return buffer;
    }
}
