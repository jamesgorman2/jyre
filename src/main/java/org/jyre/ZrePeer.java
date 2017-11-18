package org.jyre;

import org.jyre.protocol.HelloMessage;
import org.jyre.protocol.JoinMessage;
import org.jyre.protocol.LeaveMessage;
import org.jyre.protocol.PingMessage;
import org.jyre.protocol.PingOkMessage;
import org.jyre.protocol.ShoutMessage;
import org.jyre.protocol.WhisperMessage;
import org.jyre.protocol.ZreCodec;
import org.jyre.protocol.ZreSocket;
import org.zeromq.api.Context;
import org.zeromq.api.Message;
import org.zeromq.api.Socket;
import org.zeromq.api.SocketType;

import java.util.List;
import java.util.Map;

class ZrePeer {
    /**
     * Valid peer states.
     */
    public enum State {
        DISCONNECTED, CONNECTED, READY, EVASIVE, EXPIRING, EXPIRED
    }

    private Context context;
    private ZreSocket socket;
    private String identity;
    private String endpoint;
    private String name;
    private State state = State.DISCONNECTED;
    private int status;
    private long evasiveAt;
    private long expiredAt;
    private int sentSequence;
    private int recvSequence;
    private List<String> groups;
    private Map<String, String> headers;

    public ZrePeer(Context context, String identity) {
        this.context = context;
        this.identity = identity;
        this.name = identity;
    }

    public void send(HelloMessage message) {
        if (state == State.DISCONNECTED) return;
        assert socket != null;

        boolean result = socket.send(message.withSequence(incrementSentSequence()));
        checkSendResult(result, ZreCodec.MessageType.HELLO);
    }

    public void send(JoinMessage message) {
        if (state == State.DISCONNECTED) return;
        assert socket != null;

        boolean result = socket.send(message.withSequence(incrementSentSequence()));
        checkSendResult(result, ZreCodec.MessageType.JOIN);
    }

    public void send(LeaveMessage message) {
        if (state == State.DISCONNECTED) return;
        assert socket != null;

        boolean result = socket.send(message.withSequence(incrementSentSequence()));
        checkSendResult(result, ZreCodec.MessageType.LEAVE);
    }

    public void send(PingMessage message) {
        if (state == State.DISCONNECTED) return;
        assert socket != null;

        boolean result = socket.send(message.withSequence(incrementSentSequence()));
        checkSendResult(result, ZreCodec.MessageType.PING);
    }

    public void send(PingOkMessage message) {
        if (state == State.DISCONNECTED) return;
        assert socket != null;

        boolean result = socket.send(message.withSequence(incrementSentSequence()));
        checkSendResult(result, ZreCodec.MessageType.PING_OK);
    }

    public void send(ShoutMessage message) {
        if (state == State.DISCONNECTED) return;
        assert socket != null;

        boolean result = socket.send(message.withSequence(incrementSentSequence()));
        checkSendResult(result, ZreCodec.MessageType.SHOUT);
    }

    public void send(WhisperMessage message) {
        if (state == State.DISCONNECTED) return;
        assert socket != null;

        boolean result = socket.send(message.withSequence(incrementSentSequence()));
        checkSendResult(result, ZreCodec.MessageType.WHISPER);
    }

    /**
     * Connect to peer's mailbox with a given reply-to address.
     *
     * @param replyTo The address of the local peer
     * @param endpoint The endpoint of the remote peer
     */
    public void connect(String replyTo, String endpoint) {
        assert state == State.DISCONNECTED;

        Socket socket = context.buildSocket(SocketType.DEALER)
            .withIdentity(replyTo.getBytes(Message.CHARSET))
            .withSendHighWatermark(ZreConstants.PEER_HWM)
            .withSendTimeout(0)
            .connect(endpoint);
        this.socket = new ZreSocket(socket);
        this.state = State.CONNECTED;
        this.endpoint = endpoint;
    }

    /**
     * Disconnect from peer's mailbox. No more messages will be sent to peer
     * until connected again.
     */
    public void disconnect() {
        if (socket != null) {
            try {
                socket.close();
            } finally {
                this.state = State.DISCONNECTED;
                this.socket = null;
                this.endpoint = null;
            }
        }
    }

    /**
     * Join a group.
     *
     * @param group The group
     */
    public void join(ZreGroup group) {
        group.getPeers().put(identity, this);
        onUpdate();
    }

    /**
     * Leave a group.
     *
     * @param group The group
     */
    public void leave(ZreGroup group) {
        group.getPeers().remove(identity);
        onUpdate();
    }

    public boolean isValidSequence(int sequence) {
        int newSequence = recvSequence + 1;
        if (newSequence > ZreConstants.USHORT_MAX) {
            newSequence = 0;
        }

        boolean isValid = newSequence == sequence;
        if (isValid) {
            recvSequence = newSequence;
        }

        return isValid;
    }

    /**
     * Update evasive and expired status in response to receiving a PING.
     *
     * @param evasiveTimeout Amount of time before peer is considered evasive, in milliseconds
     * @param expiredTimeout Amount of time before peer is considered expired, in milliseconds
     */
    public void onPing(int evasiveTimeout, int expiredTimeout) {
        assert state == State.READY
            || state == State.EVASIVE
            || state == State.EXPIRING;
        long now = System.currentTimeMillis();
        evasiveAt = now + evasiveTimeout;
        expiredAt = now + expiredTimeout;
    }

    /**
     * Check evasive and expired status and update state accordingly.
     */
    public void onWake() {
        long now = System.currentTimeMillis();
        if (state == State.CONNECTED || state == State.READY) {
            if (now >= evasiveAt) {
                state = State.EVASIVE;
            }
        } else if (state == State.EVASIVE) {
            state = State.EXPIRING;
        } else if (state == State.EXPIRING) {
            if (now >= expiredAt) {
                state = State.EXPIRED;
            }
        }
    }

    /**
     * Set state to READY.
     */
    public void onReady() {
        assert state == State.CONNECTED
            || state == State.READY
            || state == State.EVASIVE
            || state == State.EXPIRING;
        state = State.READY;
    }

    /**
     * Increment status change counter in response to JOIN or EXIT events.
     */
    public void onUpdate() {
        if (++status > ZreConstants.UBYTE_MAX) {
            status = 0;
        }
    }

    public String getIdentity() {
        return identity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getEvasiveAt() {
        return evasiveAt;
    }

    public long getExpiredAt() {
        return expiredAt;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public String getHeader(String key, String defaultValue) {
        String header = headers.get(key);
        if (header == null) {
            header = defaultValue;
        }

        return header;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * Check if peer is in a READY state.
     *
     * @return true if peer's state is READY, false otherwise
     */
    public boolean isReady() {
        return state == State.READY;
    }

    /**
     * Check if peer is in an EVASIVE state.
     * <p>
     * Note: Can only be evasive once, then becomes expiring. This approximates
     * a state machine transition so we only send one PING via TCP before simply
     * waiting until expired status.
     *
     * @return true if peer's state is EVASIVE, false otherwise
     */
    public boolean isEvasive() {
        return state == State.EVASIVE;
    }

    /**
     * Check if peer is in an EXPIRED state.
     *
     * @return true if peer's state is EXPIRED, false otherwise
     */
    public boolean isExpired() {
        return state == State.EXPIRED;
    }

    /**
     * Get peer state.
     *
     * @return The peer's state
     */
    public State getState() {
        return state;
    }

    private int incrementSentSequence() {
        if (++sentSequence > ZreConstants.USHORT_MAX) {
            sentSequence = 0;
        }

        return sentSequence;
    }

    private void checkSendResult(boolean result, ZreCodec.MessageType messageType) {
        if (!result) {
            System.err.printf("E: Failed to send %s message\n", messageType);
            disconnect();
        }
    }
}
