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
import org.zeromq.jzmq.reactor.ReactorBuilder;

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
    public static final Message.Frame EVASIVE = new Message.Frame("EVASIVE");

    private Context context;
    private Socket pipe;
    private Socket inbox;
    private Socket outbox;
    private ZreSocket zre;
    private ReactorBuilder reactor;
    private UdpBeacon beacon;
    private UdpSocket udp;
    private ZreLogger logger;
    private PipeHandler pipeHandler;
    private InboxHandler inboxHandler;
    private BeaconHandler beaconHandler;
    private PingHandler pingHandler;

    /**
     * Our endpoint.
     */
    private String endpoint;

    /**
     * Our port.
     */
    private int port;

    /**
     * Our identity.
     */
    private String identity;

    /**
     * Our name.
     */
    private String name;

    /**
     * Our change counter (e.g. status).
     */
    private int status;

    /**
     * Flag to specify whether verbose output is enabled or not.
     */
    private boolean verbose;

    /**
     * Amount of time before peer is considered evasive, in milliseconds.
     */
    private int evasiveTimeout = ZreConstants.PEER_EVASIVE;

    /**
     * Amount of time before peer is considered expired, in milliseconds.
     */
    private int expiredTimeout = ZreConstants.PEER_EXPIRED;


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

        // Bind to dummy address first, then random port
        this.inbox = context.buildSocket(SocketType.ROUTER).bind(String.format("inproc://inbox-%s", toString()));
        this.port = inbox.getZMQSocket().bindToRandomPort("tcp://*", 0xc000, 0xffff);
        try {
            this.udp = new UdpSocket(PING_PORT);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to initialize DatagramChannel for UDP beacon:", ex);
        }
        this.pipe = socket;
        this.outbox = context.buildSocket(SocketType.PAIR).connect("inproc://inbox");
        this.zre = new ZreSocket(inbox);
        this.beacon = new UdpBeacon(UUID.randomUUID(), port);
        this.identity = beacon.getIdentity();
        this.name = identity;
        this.endpoint = String.format("tcp://%s:%d", udp.getHost(), port);
        this.logger = new ZreLogger(context.buildSocket(SocketType.PUB).connect(endpoint), identity);
        this.pipeHandler = new PipeHandler();
        this.inboxHandler = new InboxHandler();
        this.beaconHandler = new BeaconHandler();
        this.pingHandler = new PingHandler();

        // Create a Reactor for pipe, inbox, and beacon sockets
        this.reactor = context.buildReactor()
            .withInPollable(pipe, pipeHandler);

        // Start the reactor
        reactor.start();
    }

    @Override
    public void onClose() {
        stop();
    }

    public void start() {
        reactor.withInPollable(inbox, inboxHandler);
        reactor.withInPollable(udp.getChannel(), beaconHandler);
        reactor.withTimerRepeating(ZreConstants.PING_INTERVAL, pingHandler);
    }

    private void stop() {
        for (ZrePeer peer : peers.values()) {
            peer.disconnect();
        }

        reactor.build().stop();
        logger.close();
        zre.close();
        outbox.close();
        pipe.close();
        udp.close();
    }

    private ZrePeer getZrePeer(String identity, String endpoint) {
        ZrePeer peer = peers.get(identity);
        if (peer == null) {
            peer = new ZrePeer(context, identity);

            // Check for other peers on this endpoint
            for (ZrePeer other : peers.values()) {
                if (other.getEndpoint().equals(this.endpoint)) {
                    other.disconnect();
                }
            }

            peers.put(identity, peer);
            peer.connect(this.identity, endpoint);

            // Handshake discovery by sending HELLO as first message
            HelloMessage hello = new HelloMessage()
                .withEndpoint(this.endpoint)
                .withName(this.name)
                .withGroups(new ArrayList<>(ownGroups.keySet()))
                .withStatus(this.status)
                .withHeaders(headers);
            peer.send(hello);

            logger.info(ZreLogger.Event.ENTER, peer.getIdentity(), "Peer %s connected to %s", identity, this.identity);
        }

        return peer;
    }

    private void removeZrePeer(ZrePeer peer) {
        peer.disconnect();
        peers.remove(peer.getIdentity());

        for (ZreGroup group : peerGroups.values()) {
            peer.leave(group);
        }

        logger.info(ZreLogger.Event.EXIT, peer.getIdentity(), "Peer %s disconnected from %s", peer.getIdentity(), this.identity);
        outbox.send(new Message(EXIT).addString(peer.getIdentity()).addString(peer.getName()));
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
                case "UUID":
                    onUuid();
                    break;
                case "NAME":
                    onName();
                    break;
                case "SET NAME":
                    onSetName(message);
                    break;
                case "SET HEADER":
                    onSetHeader(message);
                    break;
                case "SET VERBOSE":
                    onSetVerbose();
                    break;
                case "SET PORT":
                    onSetPort(message);
                    break;
                case "SET EVASIVE TIMEOUT":
                    onSetEvasiveTimeout(message);
                    break;
                case "SET EXPIRED TIMEOUT":
                    onSetExpiredTimeout(message);
                    break;
                case "SET INTERVAL":
                    onSetInterval(message);
                    break;
                case "SET ENDPOINT":
                    onSetEndpoint(message);
                    break;
                case "PEERS":
                    onPeers();
                    break;
                case "GROUP PEERS":
                    onGroupPeers(message);
                    break;
                case "PEER NAME":
                    onPeerName(message);
                    break;
                case "PEER ENDPOINT":
                    onPeerEndpoint(message);
                    break;
                case "PEER HEADER":
                    onPeerHeader(message);
                    break;
                case "PEER HEADERS":
                    onPeerHeaders(message);
                    break;
                case "PEER GROUPS":
                    onPeerGroups(message);
                    break;
                case "OWN GROUPS":
                    onOwnGroups();
                    break;
                case "START":
                    start();
                    break;
                case "STOP":
                    stop();
                    break;
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
                case "PUBLISH":
                    onPublish(message);
                    break;
            }
        }

        private void onUuid() {
            pipe.send(new Message(identity));
        }

        private void onName() {
            pipe.send(new Message(name));
        }

        private void onSetName(Message message) {
            name = message.popString();
        }

        private void onSetHeader(Message message) {
            headers.put(message.popString(), message.popString());
        }

        private void onSetVerbose() {
            verbose = true;
        }

        private void onSetPort(Message message) {
            int port = message.popInt();
            reactor.build().cancel(beaconHandler);
            udp.close();
            try {
                udp = new UdpSocket(port);
            } catch (IOException ex) {
                System.err.println("E: Unable to initialize DatagramChannel for UDP beacon");
                if (verbose) {
                    ex.printStackTrace();
                }
            }
            reactor.withInPollable(udp.getChannel(), beaconHandler);
        }

        private void onSetEvasiveTimeout(Message message) {
            evasiveTimeout = message.popInt();
        }

        private void onSetExpiredTimeout(Message message) {
            expiredTimeout = message.popInt();
        }

        private void onSetInterval(Message message) {
            int interval = message.popInt();
            reactor.build().cancel(pingHandler);
            reactor.withTimerRepeating(interval, pingHandler);
        }

        private void onSetEndpoint(Message message) {
            zre.getSocket().getZMQSocket().unbind(endpoint);
            endpoint = message.popString();
            zre.getSocket().getZMQSocket().bind(endpoint);
        }

        private void onPeers() {
            pipe.send(new Message().addStrings(new ArrayList<>(peers.keySet())));
        }

        private void onGroupPeers(Message message) {
            String name = message.popString();
            ZreGroup group = peerGroups.get(name);
            if (group != null) {
                pipe.send(new Message().addStrings(new ArrayList<>(group.getPeers().keySet())));
            } else {
                pipe.send(new Message());
            }
        }

        private void onPeerName(Message message) {
            String identity = message.popString();
            ZrePeer peer = peers.get(identity);
            if (peer != null) {
                pipe.send(new Message(peer.getName()));
            } else {
                pipe.send(new Message());
            }
        }

        private void onPeerEndpoint(Message message) {
            String identity = message.popString();
            ZrePeer peer = peers.get(identity);
            if (peer != null) {
                pipe.send(new Message(peer.getEndpoint()));
            } else {
                pipe.send(new Message());
            }
        }

        private void onPeerHeader(Message message) {
            String identity = message.popString();
            String key = message.popString();
            ZrePeer peer = peers.get(identity);
            if (peer != null) {
                pipe.send(new Message(peer.getHeader(key, "")));
            } else {
                pipe.send(new Message());
            }
        }

        private void onPeerHeaders(Message message) {
            String identity = message.popString();
            ZrePeer peer = peers.get(identity);
            if (peer != null) {
                pipe.send(new Message().addMap(peer.getHeaders()));
            } else {
                pipe.send(new Message());
            }
        }

        private void onPeerGroups(Message message) {
            String identity = message.popString();
            ZrePeer peer = peers.get(identity);
            if (peer != null) {
                pipe.send(new Message().addStrings(peer.getGroups()));
            } else {
                pipe.send(new Message());
            }
        }

        private void onOwnGroups() {
            pipe.send(new Message().addStrings(new ArrayList<>(ownGroups.keySet())));
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
                ShoutMessage shout = new ShoutMessage().withGroup(name).withContent(message.popFrame());
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

                logger.info(ZreLogger.Event.JOIN, null, "Peer %s joined group %s", identity, name);
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

                logger.info(ZreLogger.Event.LEAVE, null, "Peer %s left group %s", identity, name);
            }
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
            ZreSocket.MessageType messageType = zre.receive();
            if (messageType == null) {
                return; // Interrupted
            }

            String identity = zre.getAddress().getString();
            ZrePeer peer = peers.get(identity);
            if (messageType == ZreSocket.MessageType.HELLO) {
                // On HELLO we may create the peer if it's unknown
                // On other commands the peer must already exist
                peer = getZrePeer(identity, zre.getHello().getEndpoint());
                peer.onReady();
            }

            // Ignore command if peer isn't ready
            if (peer == null || !peer.isReady()) {
                return;
            }

            // Activity from peer resets peer timers
            peer.onPing(evasiveTimeout, expiredTimeout);

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
            HelloMessage hello = zre.getHello();
            if (!checkSequence(peer, hello.getSequence())) {
                return;
            }

            peer.setName(hello.getName());
            peer.setGroups(hello.getGroups());
            peer.setHeaders(hello.getHeaders());

            // Tell the caller about the new peer
            outbox.send(new Message(ENTER).addString(peer.getIdentity()).addString(peer.getName()));

            // Join peer to listed groups
            for (String name : hello.getGroups()) {
                ZreGroup group = getZreGroup(name);
                peer.join(group);

                // Now tell the caller about the peers group
                outbox.send(new Message(JOIN).addString(peer.getIdentity()).addString(name));
            }

            // Hello command holds latest status of peer
            peer.setStatus(hello.getStatus());

            // Store peer headers for future reference
            peer.setHeaders(hello.getHeaders());
        }

        private void onWhisper(ZrePeer peer) {
            WhisperMessage whisper = zre.getWhisper();
            if (!checkSequence(peer, whisper.getSequence())) {
                return;
            }

            outbox.send(new Message(WHISPER).addString(peer.getIdentity()).addFrame(whisper.getContent()));
        }

        private void onShout(ZrePeer peer) {
            ShoutMessage shout = zre.getShout();
            if (!checkSequence(peer, shout.getSequence())) {
                return;
            }

            outbox.send(new Message(SHOUT).addString(peer.getIdentity()).addString(shout.getGroup()).addFrame(shout.getContent()));
        }

        private void onPing(ZrePeer peer) {
            PingMessage ping = zre.getPing();
            checkSequence(peer, ping.getSequence());

            peer.send(new PingOkMessage());
        }

        private void onJoin(ZrePeer peer) {
            JoinMessage join = zre.getJoin();
            if (!checkSequence(peer, join.getSequence())) {
                return;
            }

            String name = join.getGroup();
            ZreGroup group = getZreGroup(name);
            peer.join(group);
            assert (join.getStatus() == peer.getStatus());

            // Now tell the caller about the peer joined a group
            outbox.send(new Message(JOIN).addString(peer.getIdentity()).addString(name));
        }

        private void onLeave(ZrePeer peer) {
            LeaveMessage leave = zre.getLeave();
            if (!checkSequence(peer, leave.getSequence())) {
                return;
            }

            String name = leave.getGroup();
            ZreGroup group = getZreGroup(name);
            peer.leave(group);
            assert (leave.getStatus() == peer.getStatus());

            // Now tell the caller about the peer joined a group
            outbox.send(new Message(LEAVE).addString(peer.getIdentity()).addString(name));
        }

        private boolean checkSequence(ZrePeer peer, int sequence) {
            boolean isValid = peer.isValidSequence(sequence);
            if (!isValid) {
                logger.error(ZreLogger.Event.OTHER, peer.getIdentity(), "Peer %s lost messages from %s", identity, peer.getIdentity());
                removeZrePeer(peer);
            }

            return isValid;
        }
    }

    private class BeaconHandler extends LoopAdapter {
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
                    ZrePeer peer = getZrePeer(message.getIdentity(), String.format("tcp://%s:%d", udp.getFrom(), message.getPort()));
                    if (message.getPort() > 0) {
                        peer.onPing(evasiveTimeout, expiredTimeout);
                    } else {
                        removeZrePeer(peer);
                    }
                }
            } catch (IOException ex) {
                System.err.println("E: Unable to receive UDP beacon");
                if (verbose) {
                    ex.printStackTrace();
                }
            } finally {
                buffer.clear();
            }
        }
    }

    private class PingHandler extends LoopAdapter {
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
                if (verbose) {
                    ex.printStackTrace();
                }
            }
        }

        private void pingPeers() {
            List<ZrePeer> listOfPeers = new ArrayList<>(peers.values());
            for (ZrePeer peer : listOfPeers) {
                String identity = peer.getIdentity();
                peer.onWake();

                if (peer.isExpired()) {
                    removeZrePeer(peer);
                } else if (peer.isEvasive()) {
                    peer.send(new PingMessage());

                    logger.info(ZreLogger.Event.OTHER, identity, "Peer %s is being evasive", identity);
                    outbox.send(new Message(EVASIVE).addString(identity));
                }
            }
        }
    }
}