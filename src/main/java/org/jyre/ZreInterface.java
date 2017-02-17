package org.jyre;

import org.zeromq.ContextFactory;
import org.zeromq.api.Context;
import org.zeromq.api.Message;
import org.zeromq.api.Socket;
import org.zeromq.api.SocketType;

import java.util.List;
import java.util.Map;

public class ZreInterface {
    // Commands sent to background agent
    private static final Message.Frame UUID                = new Message.Frame("UUID");
    private static final Message.Frame NAME                = new Message.Frame("NAME");
    private static final Message.Frame SET_NAME            = new Message.Frame("SET NAME");
    private static final Message.Frame SET_HEADER          = new Message.Frame("SET HEADER");
    private static final Message.Frame SET_VERBOSE         = new Message.Frame("SET VERBOSE");
    private static final Message.Frame SET_PORT            = new Message.Frame("SET PORT");
    private static final Message.Frame SET_EVASIVE_TIMEOUT = new Message.Frame("SET EVASIVE TIMEOUT");
    private static final Message.Frame SET_EXPIRED_TIMEOUT = new Message.Frame("SET EXPIRED TIMEOUT");
    private static final Message.Frame SET_INTERVAL        = new Message.Frame("SET INTERVAL");
    private static final Message.Frame SET_ENDPOINT        = new Message.Frame("SET ENDPOINT");
    private static final Message.Frame PEERS               = new Message.Frame("PEERS");
    private static final Message.Frame GROUP_PEERS         = new Message.Frame("GROUP PEERS");
    private static final Message.Frame PEER_NAME           = new Message.Frame("PEER NAME");
    private static final Message.Frame PEER_ENDPOINT       = new Message.Frame("PEER ENDPOINT");
    private static final Message.Frame PEER_HEADER         = new Message.Frame("PEER HEADER");
    private static final Message.Frame PEER_HEADERS        = new Message.Frame("PEER HEADERS");
    private static final Message.Frame PEER_GROUPS         = new Message.Frame("PEER GROUPS");
    private static final Message.Frame OWN_GROUPS          = new Message.Frame("OWN GROUPS");
    private static final Message.Frame START               = new Message.Frame("START");
    private static final Message.Frame STOP                = new Message.Frame("STOP");
    private static final Message.Frame JOIN                = new Message.Frame("JOIN");
    private static final Message.Frame LEAVE               = new Message.Frame("LEAVE");
    private static final Message.Frame WHISPER             = new Message.Frame("WHISPER");
    private static final Message.Frame SHOUT               = new Message.Frame("SHOUT");
    private static final Message.Frame PUBLISH             = new Message.Frame("PUBLISH");

    private Context context;
    private Socket pipe;
    private Socket inbox;

    public ZreInterface() {
        this.context = ContextFactory.createContext(1);
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

    public List<String> getGroupPeers(String name) {
        pipe.send(new Message(GROUP_PEERS).addString(name));
        return pipe.receiveMessage().popStrings();
    }

    public String getPeerName(String identity) {
        pipe.send(new Message(PEER_NAME).addString(identity));
        return pipe.receiveMessage().popString();
    }

    public String getPeerEndpoint(String identity) {
        pipe.send(new Message(PEER_ENDPOINT).addString(identity));
        return pipe.receiveMessage().popString();
    }

    public String getPeerHeader(String identity, String key) {
        pipe.send(new Message(PEER_HEADER).addString(identity).addString(key));
        return pipe.receiveMessage().popString();
    }

    public Map<String, String> getPeerHeaders(String identity) {
        pipe.send(new Message(PEER_HEADERS).addString(identity));
        return pipe.receiveMessage().popMap();
    }

    public List<String> getPeerGroups(String identity) {
        pipe.send(new Message(PEER_GROUPS).addString(identity));
        return pipe.receiveMessage().popStrings();
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

    public void join(String group) {
        pipe.send(new Message(JOIN).addString(group));
    }

    public void leave(String group) {
        pipe.send(new Message(LEAVE).addString(group));
    }

    public void whisper(Message message) {
        pipe.send(message.pushFrame(WHISPER));
    }

    public void shout(Message message) {
        pipe.send(message.pushFrame(SHOUT));
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

    public Message receive() {
        return inbox.receiveMessage();
    }

    public void close() {
        context.close();
    }
}
