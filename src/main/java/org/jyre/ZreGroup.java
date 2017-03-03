package org.jyre;

import org.jyre.protocol.HelloMessage;
import org.jyre.protocol.JoinMessage;
import org.jyre.protocol.LeaveMessage;
import org.jyre.protocol.PingMessage;
import org.jyre.protocol.PingOkMessage;
import org.jyre.protocol.ShoutMessage;
import org.jyre.protocol.WhisperMessage;

import java.util.HashMap;

public class ZreGroup {
    private final String name;
    private final HashMap<String, ZrePeer> peers;

    public ZreGroup(String name) {
        this.name = name;
        this.peers = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public HashMap<String, ZrePeer> getPeers() {
        return peers;
    }

    public void send(HelloMessage message) {
        for (ZrePeer peer : peers.values()) {
            peer.send(message);
        }
    }

    public void send(JoinMessage message) {
        for (ZrePeer peer : peers.values()) {
            peer.send(message);
        }
    }

    public void send(LeaveMessage message) {
        for (ZrePeer peer : peers.values()) {
            peer.send(message);
        }
    }

    public void send(PingMessage message) {
        for (ZrePeer peer : peers.values()) {
            peer.send(message);
        }
    }

    public void send(PingOkMessage message) {
        for (ZrePeer peer : peers.values()) {
            peer.send(message);
        }
    }

    public void send(ShoutMessage message) {
        for (ZrePeer peer : peers.values()) {
            peer.send(message);
        }
    }

    public void send(WhisperMessage message) {
        for (ZrePeer peer : peers.values()) {
            peer.send(message);
        }
    }
}
