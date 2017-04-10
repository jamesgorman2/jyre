package org.jyre;

import org.zeromq.api.Message;

public class ZreEvent {
    private ZreEventType eventType;
    private String peer;
    private String name;
    private String group;
    private Message content;

    public ZreEvent(ZreEventType eventType) {
        this.eventType = eventType;
    }

    public ZreEventType getEventType() {
        return eventType;
    }

    public void setEventType(ZreEventType eventType) {
        this.eventType = eventType;
    }

    public String getPeer() {
        return peer;
    }

    public void setPeer(String peer) {
        this.peer = peer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Message getContent() {
        return content;
    }

    public void setContent(Message content) {
        this.content = content;
    }

    public static ZreEvent newZreEvent(Message message) {
        ZreEventType eventType = ZreEventType.valueOf(message.popString());
        ZreEvent event = new ZreEvent(eventType);
        event.setPeer(message.popString());
        event.setName(message.popString());
        switch (eventType) {
            case JOIN:
            case SHOUT:
            case LEAVE:
                event.setGroup(message.popString());
                break;
        }
        switch (eventType) {
            case WHISPER:
            case SHOUT:
                event.setContent(message);
                break;
        }

        return event;
    }
}
