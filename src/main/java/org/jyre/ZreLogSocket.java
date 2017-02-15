/* ============================================================================
 * ZreLogSocket.java
 * 
 * Generated codec class for ZreLogSocket
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
package org.jyre;

import org.zeromq.ZMQ;
import org.zeromq.api.Message;
import org.zeromq.api.Message.Frame;
import org.zeromq.api.Message.FrameBuilder;
import org.zeromq.api.Socket;

import java.io.Closeable;

/**
 * ZreLogSocket class.
 * 
 * The specification for this class is as follows:
 * <pre class="text">
 *  LOG - Log an event
 *    level                        number 1
 *    event                        number 1
 *    node                         number 2
 *    peer                         number 2
 *    time                         number 8
 *    data                         string
 * </pre>
 * 
 * @author sriesenberg
 */
public class ZreLogSocket implements Closeable {
    //  Protocol constants
    public static final int VERSION           = 1;
    public static final int LEVEL_ERROR       = 1;
    public static final int LEVEL_WARNING     = 2;
    public static final int LEVEL_INFO        = 3;
    public static final int EVENT_JOIN        = 1;
    public static final int EVENT_LEAVE       = 2;
    public static final int EVENT_ENTER       = 3;
    public static final int EVENT_EXIT        = 4;
    public static final int EVENT_OTHER       = 5;

    //  Enumeration of message types
    public enum MessageType {
        LOG
    }

    //  Structure of our class
    private Socket socket;        //  Internal socket handle
    private Frame address;        //  Address of peer if any

    private LogMessage log;

    /**
     * Create a new ZreLogSocket.
     * 
     * @param socket The internal socket
     */
    public ZreLogSocket(Socket socket) {
        assert (socket != null);
        this.socket = socket;
    }

    /**
     * Get the message address.
     * 
     * @return The message address frame
     */
    public Frame getAddress() {
        return address;
    }

    /**
     * Set the message address.
     * 
     * @param address The new message address
     */
    public void setAddress(Frame address) {
        this.address = address;
    }

    /**
     * Get the internal socket.
     *
     * @return The internal socket
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Destroy the ZreLogSocket.
     */
    @Override
    public void close() {
        socket.close();
    }

    /**
     * Receive a message on the socket.
     */
    public MessageType receive() {
        int id = 0;
        Message frames;
        Frame needle;
        MessageType type;
        try {
            //  Read valid message frame from socket; we loop over any
            //  garbage data we might receive from badly-connected peers
            while (true) {
                frames = socket.receiveMessage();

                //  If we're reading from a ROUTER socket, get address
                if (socket.getZMQSocket().getType() == ZMQ.ROUTER) {
                    this.address = frames.popFrame();
                }

                //  Read and parse command in frame
                needle = frames.popFrame();

                //  Get and check protocol signature
                int signature = (0xffff) & needle.getShort();
                if (signature == (0xAAA0 | 2))
                    break;                //  Valid signature

                //  Protocol assertion, drop message
            }

            //  Get message id, which is first byte in frame
            id = (0xff) & needle.getByte();
            type = MessageType.values()[id-1];
            switch (type) {
                case LOG: {
                    LogMessage message = this.log = new LogMessage();
                    message.level = (0xff) & needle.getByte();
                    message.event = (0xff) & needle.getByte();
                    message.node = (0xffff) & needle.getShort();
                    message.peer = (0xffff) & needle.getShort();
                    message.time = needle.getLong();
                    message.data = needle.getChars();
                    break;
                }
                default:
                    throw new IllegalArgumentException("Invalid message: unrecognized type: " + type);
            }

            return type;
        } catch (Exception ex) {
            //  Error returns
            System.err.printf("E: Malformed message: %s\n", id);
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Get a LOG message from the socket.
     */
    public LogMessage getLog() {
        return log;
    }

    /**
     * Send the LOG to the socket in one step.
     */
    public boolean send(LogMessage message) {
        //  Now serialize message into the frame
        FrameBuilder builder = new FrameBuilder();
        builder.putShort((short) (0xAAA0 | 2));
        builder.putByte((byte) 1);       //  Message ID

        builder.putByte((byte) (int) message.level);
        builder.putByte((byte) (int) message.event);
        builder.putShort((short) (int) message.node);
        builder.putShort((short) (int) message.peer);
        builder.putLong(message.time);
        if (message.data != null) {
            builder.putString(message.data);
        } else {
            builder.putString("");        //  Empty string
        }

        //  Create multi-frame message
        Message frames = new Message();

        //  If we're sending to a ROUTER, we add the address first
        if (socket.getZMQSocket().getType() == ZMQ.ROUTER) {
            assert (address != null);
            frames.addFrame(address);
        }

        //  Now add the data frame
        frames.addFrame(builder.build());

        return socket.send(frames);
    }
}

