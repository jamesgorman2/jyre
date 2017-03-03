/* ============================================================================
 * PingMessage.java
 * 
 * Generated codec class for PingMessage
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
 * PingMessage class.
 */
public class PingMessage {
    public static final ZreSocket.MessageType MESSAGE_TYPE = ZreSocket.MessageType.PING;

    protected Integer version;
    protected Integer sequence;

    /**
     * Get the sequence field.
     * 
     * @return The sequence field
     */
    public Integer getSequence() {
        return sequence;
    }

    /**
     * Set the sequence field.
     * 
     * @param sequence The sequence field
     */
    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    /**
     * Set the sequence field.
     *
     * @param sequence The sequence field
     * @return The PingMessage, for method chaining
     */
    public PingMessage withSequence(Integer sequence) {
        this.sequence = sequence;
        return this;
    }

    /**
     * Serialize the PING message.
     *
     * @return The serialized message
     */
    public Message toMessage() {
        //  Serialize message into the frame
        FrameBuilder builder = new FrameBuilder();
        builder.putShort((short) (0xAAA0 | 1));
        builder.putByte((byte) 6);       //  Message ID

        builder.putByte((byte) 2);
        builder.putShort((short) (int) sequence);

        //  Create multi-frame message
        Message frames = new Message();

        //  Now add the data frame
        frames.addFrame(builder.build());

        return frames;
    }

    /**
     * Create a new PING message.
     *
     * @param frames The message frames
     * @return The deserialized message
     */
    public static PingMessage fromMessage(Message frames) {
        PingMessage message = new PingMessage();
        Frame needle = frames.popFrame();
        message.version = (0xff) & needle.getByte();
        if (message.version != 2) {
            throw new IllegalArgumentException();
        }
        message.sequence = (0xffff) & needle.getShort();

        return message;
    }
}
