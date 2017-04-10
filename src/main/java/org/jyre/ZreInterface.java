package org.jyre;

import org.zeromq.ContextFactory;
import org.zeromq.api.Context;
import org.zeromq.api.Message;
import org.zeromq.api.Message.Frame;
import org.zeromq.api.Socket;
import org.zeromq.api.SocketType;

import java.util.List;
import java.util.Map;

public class ZreInterface {
    // Commands sent to background agent
    private static final Frame UUID                = Frame.of("UUID");
    private static final Frame NAME                = Frame.of("NAME");
    private static final Frame SET_NAME            = Frame.of("SET NAME");
    private static final Frame SET_HEADER          = Frame.of("SET HEADER");
    private static final Frame SET_VERBOSE         = Frame.of("SET VERBOSE");
    private static final Frame SET_BEACONS_ENABLED = Frame.of("SET BEACONS ENABLED");
    private static final Frame SET_PORT            = Frame.of("SET PORT");
    private static final Frame SET_EVASIVE_TIMEOUT = Frame.of("SET EVASIVE TIMEOUT");
    private static final Frame SET_EXPIRED_TIMEOUT = Frame.of("SET EXPIRED TIMEOUT");
    private static final Frame SET_INTERVAL        = Frame.of("SET INTERVAL");
    private static final Frame SET_ENDPOINT        = Frame.of("SET ENDPOINT");
    private static final Frame PEERS               = Frame.of("PEERS");
    private static final Frame GROUP_PEERS         = Frame.of("GROUP PEERS");
    private static final Frame PEER_NAME           = Frame.of("PEER NAME");
    private static final Frame PEER_ENDPOINT       = Frame.of("PEER ENDPOINT");
    private static final Frame PEER_HEADER         = Frame.of("PEER HEADER");
    private static final Frame PEER_HEADERS        = Frame.of("PEER HEADERS");
    private static final Frame PEER_GROUPS         = Frame.of("PEER GROUPS");
    private static final Frame OWN_GROUPS          = Frame.of("OWN GROUPS");
    private static final Frame START               = Frame.of("START");
    private static final Frame STOP                = Frame.of("STOP");
    private static final Frame JOIN                = Frame.of("JOIN");
    private static final Frame LEAVE               = Frame.of("LEAVE");
    private static final Frame WHISPER             = Frame.of("WHISPER");
    private static final Frame SHOUT               = Frame.of("SHOUT");
    private static final Frame CONNECT             = Frame.of("CONNECT");
    private static final Frame PUBLISH             = Frame.of("PUBLISH");

    private Context context;
    private Socket pipe;
    private Socket inbox;

    public ZreInterface() {
        this(ContextFactory.createContext(1));
    }

    public ZreInterface(Context context) {
        this.context = context;
        this.inbox = context.buildSocket(SocketType.PAIR).bind("inproc://inbox");
        this.pipe = context.fork(new ZreInterfaceAgent());
    }

    public String getUuid() {
        pipe.send(new Message(UUID));
        return pipe.receiveMessage().popString();
    }

    public String getName() {
        pipe.send(new Message(NAME));
        return pipe.receiveMessage().popString();
    }

    public void setName(String name) {
        pipe.send(new Message(SET_NAME).addString(name));
    }

    public void setVerbose() {
        pipe.send(new Message(SET_VERBOSE));
    }

    public void setBeaconsEnabled(boolean enabled) {
        pipe.send(new Message(SET_BEACONS_ENABLED).addString(String.valueOf(enabled)));
    }

    public void setPort(int port) {
        pipe.send(new Message(SET_PORT).addInt(port));
    }

    public void setEvasiveTimeout(int evasiveTimeout) {
        pipe.send(new Message(SET_EVASIVE_TIMEOUT).addInt(evasiveTimeout));
    }

    public void setExpiredTimeout(int expiredTimeout) {
        pipe.send(new Message(SET_EXPIRED_TIMEOUT).addInt(expiredTimeout));
    }

    public void setInterval(int interval) {
        pipe.send(new Message(SET_INTERVAL).addInt(interval));
    }

    public void setEndpoint(String endpoint) {
        pipe.send(new Message(SET_ENDPOINT).addString(endpoint));
    }

    public List<String> getPeers() {
        pipe.send(new Message(PEERS));
        return pipe.receiveMessage().popStrings();
    }

    public List<String> getPeersByGroup(String name) {
        pipe.send(new Message(GROUP_PEERS).addString(name));
        Message message = pipe.receiveMessage();
        return message.isEmpty() ? null : message.popStrings();
    }

    public String getPeerName(String identity) {
        pipe.send(new Message(PEER_NAME).addString(identity));
        Message message = pipe.receiveMessage();
        return message.isEmpty() ? null : message.popString();
    }

    public String getPeerEndpoint(String identity) {
        pipe.send(new Message(PEER_ENDPOINT).addString(identity));
        Message message = pipe.receiveMessage();
        return message.isEmpty() ? null : message.popString();
    }

    public String getPeerHeader(String identity, String key) {
        pipe.send(new Message(PEER_HEADER).addString(identity).addString(key));
        Message message = pipe.receiveMessage();
        return message.isEmpty() ? null : message.popString();
    }

    public Map<String, String> getPeerHeaders(String identity) {
        pipe.send(new Message(PEER_HEADERS).addString(identity));
        Message message = pipe.receiveMessage();
        return message.isEmpty() ? null : message.popMap();
    }

    public List<String> getPeerGroups(String identity) {
        pipe.send(new Message(PEER_GROUPS).addString(identity));
        Message message = pipe.receiveMessage();
        return message.isEmpty() ? null : message.popStrings();
    }

    public List<String> getOwnGroups() {
        pipe.send(new Message(OWN_GROUPS));
        return pipe.receiveMessage().popStrings();
    }

    public void start() {
        pipe.send(new Message(START));
    }

    public void stop() {
        pipe.send(new Message(STOP));
    }

    public boolean connect(String address) {
        pipe.send(new Message(CONNECT).addString(address));
        return pipe.receiveMessage().popString().equals("OK");
    }

    public void join(String group) {
        pipe.send(new Message(JOIN).addString(group));
    }

    public void leave(String group) {
        pipe.send(new Message(LEAVE).addString(group));
    }

    public void whisper(String peer, Message message) {
        pipe.send(message.pushString(peer).pushFrame(WHISPER));
    }

    public void shout(String group, Message message) {
        pipe.send(message.pushString(group).pushFrame(SHOUT));
    }

    public void setHeader(String name, String value) {
        pipe.send(new Message(SET_HEADER).addString(name).addString(value));
    }

    public void publish(String pathName, String virtualSpace) {
        pipe.send(new Message(PUBLISH).addString(pathName).addString(virtualSpace));
    }

    public Context getContext() {
        return context;
    }

    public Socket getSocket() {
        return inbox;
    }

    public ZreEvent receive() {
        Message message = inbox.receiveMessage();
        return message == null ? null : ZreEvent.newZreEvent(message);
    }

    public Message receiveMessage() {
        return inbox.receiveMessage();
    }

    public void close() {
        context.close();
    }
}
