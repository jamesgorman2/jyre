/* ============================================================================
 * ZreLogCodec.java
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
package org.jyre.protocol;

import org.zeromq.api.Message;
import org.zeromq.api.Message.Frame;
import org.zeromq.api.Message.FrameBuilder;

/**
 * ZreLogCodec class.
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
public class ZreLogCodec {
    //  Protocol constants
    public interface Constants {
        int VERSION           = 1;
        int LEVEL_ERROR       = 1;
        int LEVEL_WARNING     = 2;
        int LEVEL_INFO        = 3;
        int EVENT_JOIN        = 1;
        int EVENT_LEAVE       = 2;
        int EVENT_ENTER       = 3;
        int EVENT_EXIT        = 4;
        int EVENT_OTHER       = 5;
    }

    //  Enumeration of message types
    public enum MessageType {
        LOG
    }

    protected LogMessage log;

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
            if (signature != (0xaaa0 | 2)) {
                return null;             //  Invalid signature
            }

            //  Get message id, which is first byte in frame
            int id = (0xff) & needle.getByte();
            type = MessageType.values()[id-1];
            switch (type) {
                case LOG: {
                    LogMessage message = this.log = new LogMessage();
                    message.level = (0xff) & needle.getByte();
                    message.event = (0xff) & needle.getByte();
                    message.node = (0xffff) & needle.getShort();
                    message.peer = (0xffff) & needle.getShort();
                    message.time = needle.getLong();
                    message.data = needle.getString();
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
     * Get a LOG message from the socket.
     *
     * @return The LogMessage last received on this socket
     */
    public LogMessage getLog() {
        return log;
    }

    /**
     * Send the LOG to the socket in one step.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public Message serialize(LogMessage message) {
        //  Now serialize message into the frame
        FrameBuilder builder = new FrameBuilder();
        builder.putShort((short) (0xaaa0 | 2));
        builder.putByte((byte) 1);       //  Message ID

        builder.putByte((byte) (int) message.level);
        builder.putByte((byte) (int) message.event);
        builder.putShort((short) (int) message.node);
        builder.putShort((short) (int) message.peer);
        builder.putLong(message.time);
        if (message.data != null) {
            builder.putString(message.data);
        } else {
            builder.putString("");       //  Empty string
        }

        //  Create multi-frame message
        Message frames = new Message();

        //  Now add the data frame
        frames.addFrame(builder.build());

        return frames;
    }
}

