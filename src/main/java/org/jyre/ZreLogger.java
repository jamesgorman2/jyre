package org.jyre;

import org.jyre.protocol.LogMessage;
import org.jyre.protocol.ZreLogSocket;
import org.zeromq.api.Socket;

public class ZreLogger {
    private final ZreLogSocket socket;
    private final String identity;

    public ZreLogger(Socket socket, String identity) {
        this.socket = new ZreLogSocket(socket);
        this.identity = identity;
    }

    public void info(Event event, String peer, String msg, Object... args) {
        LogMessage message = createLogMessage(Level.INFO, event, peer, msg, args);
        socket.send(message);
    }

    public void warn(Event event, String peer, String msg, Object... args) {
        LogMessage message = createLogMessage(Level.WARNING, event, peer, msg, args);
        socket.send(message);
    }

    public void error(Event event, String peer, String msg, Object... args) {
        LogMessage message = createLogMessage(Level.ERROR, event, peer, msg, args);
        socket.send(message);
    }

    public void close() {
        socket.close();
    }

    private LogMessage createLogMessage(Level level, Event event, String peer, String msg, Object[] args) {
        return new LogMessage()
            .withEvent(event.value())
            .withLevel(level.value())
            .withPeer(peer != null ? peer.hashCode() : 0)
            .withNode(identity.hashCode())
            .withTime(System.currentTimeMillis())
            .withData(String.format(msg, args));
    }

    public enum Level {
        ERROR(ZreLogSocket.LEVEL_ERROR),
        WARNING(ZreLogSocket.LEVEL_WARNING),
        INFO(ZreLogSocket.LEVEL_INFO);

        private int value;

        Level(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public enum Event {
        JOIN(ZreLogSocket.EVENT_JOIN),
        LEAVE(ZreLogSocket.EVENT_LEAVE),
        ENTER(ZreLogSocket.EVENT_ENTER),
        EXIT(ZreLogSocket.EVENT_EXIT),
        OTHER(ZreLogSocket.EVENT_OTHER);

        private int value;

        public int value() {
            return value;
        }

        Event(int value) {
            this.value = value;
        }
    }
}
