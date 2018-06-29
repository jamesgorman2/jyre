/* ============================================================================
 * ZreCodec.java
 *
 * Generated codec class for ZreSocket
 * ----------------------------------------------------------------------------
 * Copyright (c) 1991-2012 iMatix Corporation -- http://www.imatix.com     
 * Copyright other contributors as noted in the AUTHORS file.              
 *                                                                         
 * This file is part of Zyre, an open-source framework for proximity-based 
 * peer-to-peer applications -- See http://zyre.org.                       
 *                                                                         
 * This is free software; you can redistribute it and/or modify it under   
 * the terms of the GNU Lesser General Public License as published by the  
 * Free Software Foundation; either version 3 of the License, or (at your  
 * option) any later version.                                              
 *                                                                         
 * This software is distributed in the hope that it will be useful, but    
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTA-   
 * BILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General  
 * Public License for more details.                                        
 *                                                                         
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.      
 * ============================================================================
 */
package org.jyre.protocol;

import org.zeromq.api.Message;
import org.zeromq.api.Message.Frame;
import org.zeromq.api.Message.FrameBuilder;

/**
 * ZreCodec class.
 *
 * The specification for this class is as follows:
 * <pre class="text">
 *  HELLO - Greet a peer so it can connect back to us
 *    version                      number 1
 *    sequence                     number 2
 *    endpoint                     string
 *    groups                       strings
 *    status                       number 1
 *    name                         string
 *    headers                      hash
 *  WHISPER - Send a multi-part message to a peer
 *    version                      number 1
 *    sequence                     number 2
 *    content                      frame
 *  SHOUT - Send a multi-part message to a group
 *    version                      number 1
 *    sequence                     number 2
 *    group                        string
 *    content                      frame
 *  JOIN - Join a group
 *    version                      number 1
 *    sequence                     number 2
 *    group                        string
 *    status                       number 1
 *  LEAVE - Leave a group
 *    version                      number 1
 *    sequence                     number 2
 *    group                        string
 *    status                       number 1
 *  PING - Ping a peer that has gone silent
 *    version                      number 1
 *    sequence                     number 2
 *  PING_OK - Reply to a peer's ping
 *    version                      number 1
 *    sequence                     number 2
 * </pre>
 *
 * @author sriesenberg
 */
public class ZreCodec {
    //  Protocol constants
    public interface Constants {
    }

    //  Enumeration of message types
    public enum MessageType {
        HELLO,
        WHISPER,
        SHOUT,
        JOIN,
        LEAVE,
        PING,
        PING_OK
    }

    protected HelloMessage hello;
    protected WhisperMessage whisper;
    protected ShoutMessage shout;
    protected JoinMessage join;
    protected LeaveMessage leave;
    protected PingMessage ping;
    protected PingOkMessage pingOk;

    /**
     * Deserialize a message.
     *
     * @return The MessageType of the deserialized message, or null
     */
    public MessageType deserialize(Message frames) {
        MessageType type = null;
        try {
            //  Read and parse command in frame
            Frame needle = frames.popFrame();

            //  Get and check protocol signature
            int signature = (0xffff) & needle.getShort();
            if (signature != (0xaaa0 | 1)) {
                return null;             //  Invalid signature
            }

            //  Get message id, which is first byte in frame
            int id = (0xff) & needle.getByte();
            type = MessageType.values()[id-1];
            switch (type) {
                case HELLO: {
                    HelloMessage message = this.hello = new HelloMessage();
                    message.version = (0xff) & needle.getByte();
                    if (message.version != 2) {
                        throw new IllegalArgumentException();
                    }
                    message.sequence = (0xffff) & needle.getShort();
                    message.endpoint = needle.getString();
                    message.groups = needle.getClobs();
                    message.status = (0xff) & needle.getByte();
                    message.name = needle.getString();
                    message.headers = needle.getMap();
                    break;
                }
                case WHISPER: {
                    WhisperMessage message = this.whisper = new WhisperMessage();
                    message.version = (0xff) & needle.getByte();
                    if (message.version != 2) {
                        throw new IllegalArgumentException();
                    }
                    message.sequence = (0xffff) & needle.getShort();
                    //  Get next frame, leave current untouched
                    if (!frames.isEmpty()) {
                        message.content = frames.popFrame();
                    } else {
                        throw new IllegalArgumentException("Invalid message: missing frame: content");
                    }
                    break;
                }
                case SHOUT: {
                    ShoutMessage message = this.shout = new ShoutMessage();
                    message.version = (0xff) & needle.getByte();
                    if (message.version != 2) {
                        throw new IllegalArgumentException();
                    }
                    message.sequence = (0xffff) & needle.getShort();
                    message.group = needle.getString();
                    //  Get next frame, leave current untouched
                    if (!frames.isEmpty()) {
                        message.content = frames.popFrame();
                    } else {
                        throw new IllegalArgumentException("Invalid message: missing frame: content");
                    }
                    break;
                }
                case JOIN: {
                    JoinMessage message = this.join = new JoinMessage();
                    message.version = (0xff) & needle.getByte();
                    if (message.version != 2) {
                        throw new IllegalArgumentException();
                    }
                    message.sequence = (0xffff) & needle.getShort();
                    message.group = needle.getString();
                    message.status = (0xff) & needle.getByte();
                    break;
                }
                case LEAVE: {
                    LeaveMessage message = this.leave = new LeaveMessage();
                    message.version = (0xff) & needle.getByte();
                    if (message.version != 2) {
                        throw new IllegalArgumentException();
                    }
                    message.sequence = (0xffff) & needle.getShort();
                    message.group = needle.getString();
                    message.status = (0xff) & needle.getByte();
                    break;
                }
                case PING: {
                    PingMessage message = this.ping = new PingMessage();
                    message.version = (0xff) & needle.getByte();
                    if (message.version != 2) {
                        throw new IllegalArgumentException();
                    }
                    message.sequence = (0xffff) & needle.getShort();
                    break;
                }
                case PING_OK: {
                    PingOkMessage message = this.pingOk = new PingOkMessage();
                    message.version = (0xff) & needle.getByte();
                    if (message.version != 2) {
                        throw new IllegalArgumentException();
                    }
                    message.sequence = (0xffff) & needle.getShort();
                    break;
                }
                default:
                    throw new IllegalArgumentException("Invalid message: unrecognized type: " + type);
            }

            return type;
        } catch (Exception ex) {
            //  Error returns
            System.err.printf("E: Malformed message: %s\n", type);
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Get a HELLO message from the socket.
     *
     * @return The HelloMessage last received on this socket
     */
    public HelloMessage getHello() {
        return hello;
    }

    /**
     * Get a WHISPER message from the socket.
     *
     * @return The WhisperMessage last received on this socket
     */
    public WhisperMessage getWhisper() {
        return whisper;
    }

    /**
     * Get a SHOUT message from the socket.
     *
     * @return The ShoutMessage last received on this socket
     */
    public ShoutMessage getShout() {
        return shout;
    }

    /**
     * Get a JOIN message from the socket.
     *
     * @return The JoinMessage last received on this socket
     */
    public JoinMessage getJoin() {
        return join;
    }

    /**
     * Get a LEAVE message from the socket.
     *
     * @return The LeaveMessage last received on this socket
     */
    public LeaveMessage getLeave() {
        return leave;
    }

    /**
     * Get a PING message from the socket.
     *
     * @return The PingMessage last received on this socket
     */
    public PingMessage getPing() {
        return ping;
    }

    /**
     * Get a PING_OK message from the socket.
     *
     * @return The PingOkMessage last received on this socket
     */
    public PingOkMessage getPingOk() {
        return pingOk;
    }

    /**
     * Send the HELLO to the socket in one step.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public Message serialize(HelloMessage message) {
        //  Now serialize message into the frame
        FrameBuilder builder = new FrameBuilder();
        builder.putShort((short) (0xaaa0 | 1));
        builder.putByte((byte) 1);       //  Message ID

        builder.putByte((byte) 2);
        builder.putShort((short) (int) message.sequence);
        if (message.endpoint != null) {
            builder.putString(message.endpoint);
        } else {
            builder.putString("");       //  Empty string
        }
        if (message.groups != null) {
            builder.putClobs(message.groups);
        } else {
            builder.putInt(0);           //  Empty string array
        }
        builder.putByte((byte) (int) message.status);
        if (message.name != null) {
            builder.putString(message.name);
        } else {
            builder.putString("");       //  Empty string
        }
        if (message.headers != null) {
            builder.putMap(message.headers);
        } else {
            builder.putInt(0);           //  Empty hash
        }

        //  Create multi-frame message
        Message frames = new Message();

        //  Now add the data frame
        frames.addFrame(builder.build());

        return frames;
    }

    /**
     * Send the WHISPER to the socket in one step.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public Message serialize(WhisperMessage message) {
        //  Now serialize message into the frame
        FrameBuilder builder = new FrameBuilder();
        builder.putShort((short) (0xaaa0 | 1));
        builder.putByte((byte) 2);       //  Message ID

        builder.putByte((byte) 2);
        builder.putShort((short) (int) message.sequence);

        //  Create multi-frame message
        Message frames = new Message();

        //  Now add the data frame
        frames.addFrame(builder.build());

        //  Now add any frame fields, in order
        frames.addFrame(message.content);

        return frames;
    }

    /**
     * Send the SHOUT to the socket in one step.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public Message serialize(ShoutMessage message) {
        //  Now serialize message into the frame
        FrameBuilder builder = new FrameBuilder();
        builder.putShort((short) (0xaaa0 | 1));
        builder.putByte((byte) 3);       //  Message ID

        builder.putByte((byte) 2);
        builder.putShort((short) (int) message.sequence);
        if (message.group != null) {
            builder.putString(message.group);
        } else {
            builder.putString("");       //  Empty string
        }

        //  Create multi-frame message
        Message frames = new Message();

        //  Now add the data frame
        frames.addFrame(builder.build());

        //  Now add any frame fields, in order
        frames.addFrame(message.content);

        return frames;
    }

    /**
     * Send the JOIN to the socket in one step.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public Message serialize(JoinMessage message) {
        //  Now serialize message into the frame
        FrameBuilder builder = new FrameBuilder();
        builder.putShort((short) (0xaaa0 | 1));
        builder.putByte((byte) 4);       //  Message ID

        builder.putByte((byte) 2);
        builder.putShort((short) (int) message.sequence);
        if (message.group != null) {
            builder.putString(message.group);
        } else {
            builder.putString("");       //  Empty string
        }
        builder.putByte((byte) (int) message.status);

        //  Create multi-frame message
        Message frames = new Message();

        //  Now add the data frame
        frames.addFrame(builder.build());

        return frames;
    }

    /**
     * Send the LEAVE to the socket in one step.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public Message serialize(LeaveMessage message) {
        //  Now serialize message into the frame
        FrameBuilder builder = new FrameBuilder();
        builder.putShort((short) (0xaaa0 | 1));
        builder.putByte((byte) 5);       //  Message ID

        builder.putByte((byte) 2);
        builder.putShort((short) (int) message.sequence);
        if (message.group != null) {
            builder.putString(message.group);
        } else {
            builder.putString("");       //  Empty string
        }
        builder.putByte((byte) (int) message.status);

        //  Create multi-frame message
        Message frames = new Message();

        //  Now add the data frame
        frames.addFrame(builder.build());

        return frames;
    }

    /**
     * Send the PING to the socket in one step.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public Message serialize(PingMessage message) {
        //  Now serialize message into the frame
        FrameBuilder builder = new FrameBuilder();
        builder.putShort((short) (0xaaa0 | 1));
        builder.putByte((byte) 6);       //  Message ID

        builder.putByte((byte) 2);
        builder.putShort((short) (int) message.sequence);

        //  Create multi-frame message
        Message frames = new Message();

        //  Now add the data frame
        frames.addFrame(builder.build());

        return frames;
    }

    /**
     * Send the PING_OK to the socket in one step.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public Message serialize(PingOkMessage message) {
        //  Now serialize message into the frame
        FrameBuilder builder = new FrameBuilder();
        builder.putShort((short) (0xaaa0 | 1));
        builder.putByte((byte) 7);       //  Message ID

        builder.putByte((byte) 2);
        builder.putShort((short) (int) message.sequence);

        //  Create multi-frame message
        Message frames = new Message();

        //  Now add the data frame
        frames.addFrame(builder.build());

        return frames;
    }
}

