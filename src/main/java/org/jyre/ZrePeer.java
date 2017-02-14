package org.jyre;

import org.zeromq.api.Context;
import org.zeromq.api.Message;
import org.zeromq.api.Socket;
import org.zeromq.api.SocketType;

import java.util.Map;

class ZrePeer {
    private Context context;
    private ZreSocket socket;
    private String identity;
    private String endpoint;
    private State state = State.DISCONNECTED;
    private int status;
    private long evasiveAt;
    private long expiredAt;
    private int sentSequence;
    private int recvSequence;
    private Map<String, String> headers;

    public ZrePeer(Context context, String identity) {
        this.context = context;
        this.identity = identity;
    }

    public void send(HelloMessage message) {
        if (!isConnected()) return;
        if (!socket.send(message.withSequence(incrementSentSequence()))) disconnect();
    }

    public void send(JoinMessage message) {
        if (!isConnected()) return;
        if (!socket.send(message.withSequence(incrementSentSequence()))) disconnect();
    }

    public void send(LeaveMessage message) {
        if (!isConnected()) return;
        if (!socket.send(message.withSequence(incrementSentSequence()))) disconnect();
    }

    public void send(PingMessage message) {
        if (!isConnected()) return;
        if (!socket.send(message.withSequence(incrementSentSequence()))) disconnect();
    }

    public void send(PingOkMessage message) {
        if (!isConnected()) return;
        if (!socket.send(message.withSequence(incrementSentSequence()))) disconnect();
    }

    public void send(ShoutMessage message) {
        if (!isConnected()) return;
        if (!socket.send(message.withSequence(incrementSentSequence()))) disconnect();
    }

    public void send(WhisperMessage message) {
        if (!isConnected()) return;
        if (!socket.send(message.withSequence(incrementSentSequence()))) disconnect();
    }

    /**
     * Connect to peer's mailbox with a given reply-to address.
     *
     * @param replyTo The address of the local peer
     * @param endpoint The endpoint of the remote peer
     */
    public void connect(String replyTo, String endpoint) {
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
        try {
            socket.close();
        } finally {
            this.state = State.DISCONNECTED;
            this.socket = null;
            this.endpoint = null;
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

    private int incrementSentSequence() {
        if (++sentSequence > ZreConstants.USHORT_MAX) {
            sentSequence = 0;
        }

        return sentSequence;
    }

    public boolean isValidSequence(int sequence) {
        if (++recvSequence > ZreConstants.USHORT_MAX) {
            recvSequence = 0;
        }

        boolean isValid = recvSequence == sequence;
        if (!isValid) {
            // rollback increment
            if (--recvSequence < 0) {
                recvSequence = ZreConstants.USHORT_MAX;
            }
        }

        return isValid;
    }

    /**
     * Update evasive and expired status in response to receiving a PING.
     */
    public void onPing() {
        long now = System.currentTimeMillis();
        evasiveAt = now + ZreConstants.PEER_EVASIVE;
        expiredAt = now + ZreConstants.PEER_EXPIRED;
        state = State.READY;
    }

    /**
     * Check evasive and expired status and update state accordingly.
     */
    public void onWake() {
        long now = System.currentTimeMillis();
        if (now >= expiredAt) {
            state = State.EXPIRED;
        } else if (state != State.EXPIRING) {
            if (now >= evasiveAt) {
                state = State.EVASIVE;
            }
        }
    }

    /**
     * Set state to READY.
     */
    public void onReady() {
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

    public String getHeader(String key, String defaultValue) {
        String header = headers.get(key);
        if (header == null) {
            header = defaultValue;
        }

        return header;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * Check if peer is in a CONNECTED state.
     *
     * @return true if peer's state is CONNECTED or greater, false otherwise
     */
    public boolean isConnected() {
        return state.ordinal() >= State.CONNECTED.ordinal();
    }

    /**
     * Check if peer is in a READY state.
     *
     * @return true if peer's state is READY or greater, false otherwise
     */
    public boolean isReady() {
        return state.ordinal() >= State.READY.ordinal();
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
        boolean evasive = state == State.EVASIVE;
        if (evasive) {
            state = State.EXPIRING;
        }

        return evasive;
    }

    /**
     * Check if peer is in an EXPIRING state.
     *
     * @return true if peer's state is EXPIRING, false otherwise
     */
    public boolean isExpiring() {
        return state == State.EXPIRING;
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
     * Valid peer states.
     */
    public enum State {
        DISCONNECTED, CONNECTED, READY, EVASIVE, EXPIRING, EXPIRED
    }
}
