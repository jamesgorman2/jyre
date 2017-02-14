package org.jyre;

import org.zeromq.ContextFactory;
import org.zeromq.api.Context;
import org.zeromq.api.Message;
import org.zeromq.api.Socket;

public class ZreInterface {
    // Commands sent to background agent
    private static final Message.Frame JOIN    = new Message.Frame("JOIN");
    private static final Message.Frame LEAVE   = new Message.Frame("LEAVE");
    private static final Message.Frame WHISPER = new Message.Frame("WHISPER");
    private static final Message.Frame SHOUT   = new Message.Frame("SHOUT");
    private static final Message.Frame SET     = new Message.Frame("SET");
    private static final Message.Frame PUBLISH = new Message.Frame("PUBLISH");

    private Context context;
    private Socket pipe;

    public ZreInterface() {
        this.context = ContextFactory.createContext(1);
        this.pipe = context.fork(new ZreInterfaceAgent());
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
        pipe.send(new Message(SET).addString(name).addString(value));
    }

    public void publish(String pathName, String virtualSpace) {
        pipe.send(new Message(PUBLISH).addString(pathName).addString(virtualSpace));
    }

    public Message receive() {
        return pipe.receiveMessage();
    }

    public void close() {
        context.close();
    }
}
