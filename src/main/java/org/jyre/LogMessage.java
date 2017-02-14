/* ============================================================================
 * LogMessage.java
 * 
 * Generated codec class for LogMessage
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

import java.util.*;

import org.zeromq.api.*;
import org.zeromq.api.Message.Frame;

/**
 * LogMessage class.
 */
public class LogMessage {
    public static final ZreLogSocket.MessageType MESSAGE_TYPE = ZreLogSocket.MessageType.LOG;

    protected Integer level;
    protected Integer event;
    protected Integer node;
    protected Integer peer;
    protected Long time;
    protected String data;

    /**
     * Get the level field.
     * 
     * @return The level field
     */
    public Integer getLevel() {
        return level;
    }

    /**
     * Set the level field.
     * 
     * @param level The level field
     */
    public void setLevel(Integer level) {
        this.level = level;
    }

    /**
     * Set the level field.
     *
     * @param level The level field
     * @return The LogMessage, for method chaining
     */
    public LogMessage withLevel(Integer level) {
        this.level = level;
        return this;
    }

    /**
     * Get the event field.
     * 
     * @return The event field
     */
    public Integer getEvent() {
        return event;
    }

    /**
     * Set the event field.
     * 
     * @param event The event field
     */
    public void setEvent(Integer event) {
        this.event = event;
    }

    /**
     * Set the event field.
     *
     * @param event The event field
     * @return The LogMessage, for method chaining
     */
    public LogMessage withEvent(Integer event) {
        this.event = event;
        return this;
    }

    /**
     * Get the node field.
     * 
     * @return The node field
     */
    public Integer getNode() {
        return node;
    }

    /**
     * Set the node field.
     * 
     * @param node The node field
     */
    public void setNode(Integer node) {
        this.node = node;
    }

    /**
     * Set the node field.
     *
     * @param node The node field
     * @return The LogMessage, for method chaining
     */
    public LogMessage withNode(Integer node) {
        this.node = node;
        return this;
    }

    /**
     * Get the peer field.
     * 
     * @return The peer field
     */
    public Integer getPeer() {
        return peer;
    }

    /**
     * Set the peer field.
     * 
     * @param peer The peer field
     */
    public void setPeer(Integer peer) {
        this.peer = peer;
    }

    /**
     * Set the peer field.
     *
     * @param peer The peer field
     * @return The LogMessage, for method chaining
     */
    public LogMessage withPeer(Integer peer) {
        this.peer = peer;
        return this;
    }

    /**
     * Get the time field.
     * 
     * @return The time field
     */
    public Long getTime() {
        return time;
    }

    /**
     * Set the time field.
     * 
     * @param time The time field
     */
    public void setTime(Long time) {
        this.time = time;
    }

    /**
     * Set the time field.
     *
     * @param time The time field
     * @return The LogMessage, for method chaining
     */
    public LogMessage withTime(Long time) {
        this.time = time;
        return this;
    }

    /**
     * Get the data field.
     * 
     * @return The data field
     */
    public String getData() {
        return data;
    }

    /**
     * Set the data field.
     * 
     * @param data The data field
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * Set the data field.
     *
     * @param data The data field
     * @return The LogMessage, for method chaining
     */
    public LogMessage withData(String data) {
        this.data = data;
        return this;
    }
}

