package org.jyre;

import org.zeromq.api.Backgroundable;
import org.zeromq.api.Context;
import org.zeromq.api.LoopAdapter;
import org.zeromq.api.Message;
import org.zeromq.api.Pollable;
import org.zeromq.api.Reactor;
import org.zeromq.api.Socket;
import org.zeromq.api.SocketType;
import org.zeromq.jzmq.UdpSocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class ZreInterfaceAgent implements Backgroundable, ZreConstants {
    public static final Message.Frame ENTER = new Message.Frame("ENTER");
    public static final Message.Frame JOIN = new Message.Frame("JOIN");
    public static final Message.Frame WHISPER = new Message.Frame("WHISPER");
    public static final Message.Frame SHOUT = new Message.Frame("SHOUT");
    public static final Message.Frame LEAVE = new Message.Frame("LEAVE");
    public static final Message.Frame EXIT = new Message.Frame("EXIT");

    private Context context;
    private Socket pipe;
    private ZreSocket inbox;
    private Reactor reactor;
    private UdpBeacon beacon;
    private UdpSocket udp;
    private ZreLogger logger;

    /**
     * Our endpoint.
     */
    private String endpoint;

    /**
     * Our port.
     */
    private int port;

    /**
     * Our change counter (e.g. status).
     */
    private int status;

    /**
     * Known peers, by identity.
     */
    private Map<String, ZrePeer> peers = new HashMap<>();

    /**
     * Groups that our peers are in, by group name.
     */
    private Map<String, ZreGroup> peerGroups = new HashMap<>();

    /**
     * Groups that we are in, by name.
     */
    private Map<String, ZreGroup> ownGroups = new HashMap<>();

    /**
     * Our header values.
     */
    private Map<String, String> headers = new HashMap<>();

    @Override
    public void run(Context context, Socket socket) {
        this.context = context;
        this.pipe = socket;

        // Bind to dummy address first, then random port
        Socket inbox = context.buildSocket(SocketType.ROUTER).bind(String.format("inproc://inbox-%s", toString()));
        this.inbox = new ZreSocket(inbox);
        this.port = inbox.getZMQSocket().bindToRandomPort("tcp://*", 0xc000, 0xffff);
        if (port < 0) {
            throw new IllegalStateException("Failed to bind to random port");
        }

        // Create a UdpSocket for beacon broadcast
        this.beacon = new UdpBeacon(UUID.randomUUID(), port);
        try {
            this.udp = new UdpSocket(PING_PORT);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to initialize DatagramChannel for UDP beacon:", ex);
        }

        this.endpoint = String.format("tcp://%s:%d", udp.getHost(), port);
        this.logger = new ZreLogger(context.buildSocket(SocketType.PUB).connect(endpoint), beacon.getIdentity());

        // Create a Reactor for pipe, inbox, and beacon sockets
        this.reactor = context.buildReactor()
            .withInPollable(pipe, new PipeHandler())
            .withInPollable(inbox, new InboxHandler())
            .withInPollable(udp.getChannel(), new BeaconListener())
            .withTimerRepeating(PING_INTERVAL, new PingListener())
            .build();

        // Start the reactor
        reactor.start();
    }

    @Override
    public void onClose() {
        for (ZrePeer peer : peers.values()) {
            peer.disconnect();
        }

        reactor.stop();
        pipe.close();
        inbox.close();
        udp.close();
        logger.close();
    }

    private ZrePeer getZrePeer(String identity, int port) {
        ZrePeer peer = peers.get(identity);
        if (peer == null) {
            peer = new ZrePeer(context, identity);

            // Check for other peers on this endpoint
            for (ZrePeer other : peers.values()) {
                if (other.getEndpoint().equals(endpoint)) {
                    other.disconnect();
                }
            }

            peers.put(identity, peer);
            peer.connect(beacon.getIdentity(), String.format("tcp://%s:%d", udp.getFrom(), port));

            // Handshake discovery by sending HELLO as first message
            HelloMessage hello = new HelloMessage()
                .withIpAddress(udp.getHost())
                .withMailbox(this.port)
                .withGroups(new ArrayList<>(ownGroups.keySet()))
                .withStatus(status)
                .withHeaders(headers);
            peer.send(hello);

            logger.info(ZreLogger.Event.ENTER, peer.getIdentity(), "Peer %s connected to %s", peer.getIdentity(), beacon.getIdentity());
            pipe.send(new Message(ENTER).addString(peer.getIdentity()));
        }

        return peer;
    }

    private ZreGroup getZreGroup(String name) {
        ZreGroup group = peerGroups.get(name);
        if (group == null) {
            group = new ZreGroup(name);
            peerGroups.put(name, group);
        }

        return group;
    }

    private class PipeHandler extends LoopAdapter {
        @Override
        protected void execute(Reactor reactor, Socket socket) {
            Message message = pipe.receiveMessage();
            if (message == null) {
                return; // Interrupted
            }

            String command = message.popString();
            switch (command) {
                case "WHISPER":
                    onWhisper(message);
                    break;
                case "SHOUT":
                    onShout(message);
                    break;
                case "JOIN":
                    onJoin(message);
                    break;
                case "LEAVE":
                    onLeave(message);
                    break;
                case "SET":
                    onSet(message);
                    break;
                case "PUBLISH":
                    onPublish(message);
                    break;
            }
        }

        private void onWhisper(Message message) {
            // Get peer to send message to
            String identity = message.popString();
            ZrePeer peer = peers.get(identity);

            // Send frame on out to peer's mailbox, drop message
            // if peer doesn't exist (may have been destroyed)
            if (peer != null) {
                WhisperMessage whisper = new WhisperMessage().withContent(message.popFrame());
                peer.send(whisper);
            }
        }

        private void onShout(Message message) {
            // Get group to send message to
            String name = message.popString();
            ZreGroup group = peerGroups.get(name);

            // Send frame on out to group's mailbox, drop message
            // if group doesn't exist (may have been destroyed)
            if (group != null) {
                ShoutMessage shout = new ShoutMessage().withContent(message.popFrame());
                group.send(shout);
            }
        }

        private void onJoin(Message message) {
            // Get group to send message to
            String name = message.popString();

            // Only send if we're not already in group
            if (!ownGroups.containsKey(name)) {
                ownGroups.put(name, new ZreGroup(name));

                JoinMessage join = new JoinMessage().withGroup(name).withStatus(incrementStatus());
                for (ZrePeer peer : peers.values()) {
                    peer.send(join);
                }

                logger.info(ZreLogger.Event.JOIN, null, "Peer %s joined group %s", endpoint, name);
            }
        }

        private void onLeave(Message message) {
            // Get group to send message to
            String name = message.popString();

            // Only send if we are actually in group
            if (ownGroups.containsKey(name)) {
                ownGroups.remove(name);

                LeaveMessage leave = new LeaveMessage().withGroup(name).withStatus(incrementStatus());
                for (ZrePeer peer : peers.values()) {
                    peer.send(leave);
                }

                logger.info(ZreLogger.Event.LEAVE, null, "Peer %s left group %s", endpoint, name);
            }
        }

        private void onSet(Message message) {
            headers.put(message.popString(), message.popString());
        }

        private void onPublish(Message message) {
            // TODO: Support FileMQ
        }

        private int incrementStatus() {
            if (++status > ZreConstants.UBYTE_MAX) {
                status = 0;
            }

            return status;
        }
    }

    private class InboxHandler extends LoopAdapter {
        @Override
        protected void execute(Reactor reactor, Socket socket) {
            ZreSocket.MessageType messageType = inbox.receive();
            if (messageType == null) {
                return; // Interrupted
            }

            String identity = inbox.getAddress().getString();
            ZrePeer peer = peers.get(identity);
            if (messageType == ZreSocket.MessageType.HELLO) {
                // On HELLO we may create the peer if it's unknown
                // On other commands the peer must already exist
                peer = getZrePeer(identity, inbox.getHello().getMailbox());
                peer.onReady();
            }

            // Ignore command if peer isn't ready
            if (peer == null || !peer.isReady()) {
                return;
            }

            // Activity from peer resets peer timers
            peer.onPing();

            // Now process each command
            switch (messageType) {
                case HELLO:
                    onHello(peer);
                    break;
                case WHISPER:
                    onWhisper(peer);
                    break;
                case SHOUT:
                    onShout(peer);
                    break;
                case PING:
                    onPing(peer);
                    break;
                case JOIN:
                    onJoin(peer);
                    break;
                case LEAVE:
                    onLeave(peer);
                    break;
            }
        }

        private void onHello(ZrePeer peer) {
            HelloMessage hello = inbox.getHello();
            checkSequence(peer, hello.getSequence());

            // Join peer to listed groups
            for (String name : hello.getGroups()) {
                ZreGroup group = getZreGroup(name);
                peer.join(group);

                // Now tell the caller about the peer joined a group
                pipe.send(new Message(JOIN).addString(peer.getIdentity()).addString(name));
            }

            // Hello command holds latest status of peer
            peer.setStatus(hello.getStatus());

            // Store peer headers for future reference
            peer.setHeaders(hello.getHeaders());
        }

        private void onWhisper(ZrePeer peer) {
            WhisperMessage whisper = inbox.getWhisper();
            checkSequence(peer, whisper.getSequence());

            pipe.send(new Message(WHISPER).addString(peer.getIdentity()).addFrame(whisper.getContent()));
        }

        private void onShout(ZrePeer peer) {
            ShoutMessage shout = inbox.getShout();
            checkSequence(peer, shout.getSequence());

            pipe.send(new Message(SHOUT).addString(peer.getIdentity()).addString(shout.getGroup()).addFrame(shout.getContent()));
        }

        private void onPing(ZrePeer peer) {
            PingMessage ping = inbox.getPing();
            checkSequence(peer, ping.getSequence());

            peer.send(new PingOkMessage());
        }

        private void onJoin(ZrePeer peer) {
            JoinMessage join = inbox.getJoin();
            checkSequence(peer, join.getSequence());

            String name = join.getGroup();
            ZreGroup group = getZreGroup(name);
            peer.join(group);
            assert (join.getStatus() == peer.getStatus());

            // Now tell the caller about the peer joined a group
            pipe.send(new Message(JOIN).addString(peer.getIdentity()).addString(name));
        }

        private void onLeave(ZrePeer peer) {
            LeaveMessage leave = inbox.getLeave();
            checkSequence(peer, leave.getSequence());

            String name = leave.getGroup();
            ZreGroup group = getZreGroup(name);
            peer.leave(group);
            assert (leave.getStatus() == peer.getStatus());

            // Now tell the caller about the peer joined a group
            pipe.send(new Message(LEAVE).addString(peer.getIdentity()).addString(name));
        }

        private boolean checkSequence(ZrePeer peer, int sequence) {
            boolean isValid = peer.isValidSequence(sequence);
            if (!isValid) {
                logger.error(ZreLogger.Event.OTHER, peer.getIdentity(), "Peer %s lost messages from %s", beacon.getIdentity(), peer.getIdentity());
            }

            return isValid;
        }
    }

    private class BeaconListener extends LoopAdapter {
        private ByteBuffer buffer = ByteBuffer.allocate(UdpBeacon.BEACON_SIZE);

        @Override
        protected void execute(Reactor reactor, SelectableChannel channel) {
            try {
                int size = udp.receive(buffer);

                // Ignore invalid beacons
                UdpBeacon message = new UdpBeacon(buffer);
                if (size == UdpBeacon.BEACON_SIZE
                        && message.getProtocol().equals(UdpBeacon.BEACON_PROTOCOL)
                        && message.getVersion() == UdpBeacon.BEACON_VERSION
                        && !message.getUuid().equals(beacon.getUuid())) {
                    ZrePeer peer = getZrePeer(message.getIdentity(), message.getPort());
                    peer.onPing();
                }
            } catch (IOException ex) {
                System.err.println("E: Unable to receive UDP beacon");
                ex.printStackTrace();
            } finally {
                buffer.clear();
            }
        }
    }

    private class PingListener extends LoopAdapter {
        @Override
        public void execute(Reactor reactor, Pollable pollable) {
            sendUdpBeacon();
            pingPeers();
        }

        private void sendUdpBeacon() {
            try {
                udp.send(beacon.getBuffer());
            } catch (IOException ex) {
                System.err.println("E: Unable to send UDP beacon");
                ex.printStackTrace();
            }
        }

        private void pingPeers() {
            List<ZrePeer> listOfPeers = new ArrayList<>(peers.values());
            for (ZrePeer peer : listOfPeers) {
                String identity = peer.getIdentity();
                peer.onWake();

                if (peer.isExpired()) {
                    peer.disconnect();

                    logger.info(ZreLogger.Event.EXIT, peer.getIdentity(), "Peer %s disconnected", peer.getIdentity());
                    pipe.send(new Message(EXIT).addString(identity));
                    peers.remove(identity);
                    for (ZreGroup group : peerGroups.values()) {
                        peer.leave(group);
                    }
                } else if (peer.isEvasive()) {
                    peer.send(new PingMessage());
                }
            }
        }
    }
}
