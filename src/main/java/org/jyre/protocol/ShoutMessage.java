/* ============================================================================
 * ShoutMessage.java
 * 
 * Generated codec class for ShoutMessage
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
 * ShoutMessage class.
 */
public class ShoutMessage {
    public static final ZreSocket.MessageType MESSAGE_TYPE = ZreSocket.MessageType.SHOUT;

    protected Integer version;
    protected Integer sequence;
    protected String group;
    protected Frame content = Message.EMPTY_FRAME;

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
     * @return The ShoutMessage, for method chaining
     */
    public ShoutMessage withSequence(Integer sequence) {
        this.sequence = sequence;
        return this;
    }

    /**
     * Get the group field.
     * 
     * @return The group field
     */
    public String getGroup() {
        return group;
    }

    /**
     * Set the group field.
     * 
     * @param group The group field
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Set the group field.
     *
     * @param group The group field
     * @return The ShoutMessage, for method chaining
     */
    public ShoutMessage withGroup(String group) {
        this.group = group;
        return this;
    }

    /**
     * Get the content field.
     * 
     * @return The content field
     */
    public Frame getContent() {
        return content;
    }

    /**
     * Set the content field, and take ownership of supplied frame.
     *
     * @param frame The new content frame
     */
    public void setContent(Frame frame) {
        this.content = frame;
    }

    /**
     * Set the content field, and take ownership of supplied frame.
     *
     * @param frame The new content frame
     * @return The ShoutMessage, for method chaining
     */
    public ShoutMessage withContent(Frame frame) {
        this.content = frame;
        return this;
    }

    /**
     * Serialize the SHOUT message.
     *
     * @return The serialized message
     */
    public Message toMessage() {
        //  Serialize message into the frame
        FrameBuilder builder = new FrameBuilder();
        builder.putShort((short) (0xAAA0 | 1));
        builder.putByte((byte) 3);       //  Message ID

        builder.putByte((byte) 2);
        builder.putShort((short) (int) sequence);
        if (group != null) {
            builder.putString(group);
        } else {
            builder.putString("");       //  Empty string
        }

        //  Create multi-frame message
        Message frames = new Message();

        //  Now add the data frame
        frames.addFrame(builder.build());

        //  Now add any frame fields, in order
        frames.addFrame(content);

        return frames;
    }

    /**
     * Create a new SHOUT message.
     *
     * @param frames The message frames
     * @return The deserialized message
     */
    public static ShoutMessage fromMessage(Message frames) {
        ShoutMessage message = new ShoutMessage();
        Frame needle = frames.popFrame();
        message.version = (0xff) & needle.getByte();
        if (message.version != 2) {
            throw new IllegalArgumentException();
        }
        message.sequence = (0xffff) & needle.getShort();
        message.group = needle.getChars();
        //  Get next frame, leave current untouched
        if (!frames.isEmpty()) {
            message.content = frames.popFrame();
        } else {
            throw new IllegalArgumentException("Invalid message: missing frame: content");
        }

        return message;
    }
}
