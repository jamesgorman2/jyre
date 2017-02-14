/* ============================================================================
 * HelloMessage.java
 * 
 * Generated codec class for HelloMessage
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
 * HelloMessage class.
 */
public class HelloMessage {
    public static final ZreSocket.MessageType MESSAGE_TYPE = ZreSocket.MessageType.HELLO;

    protected Integer sequence;
    protected String ipAddress;
    protected Integer mailbox;
    protected List<String> groups;
    protected Integer status;
    protected Map<String, String> headers;

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
     * @return The HelloMessage, for method chaining
     */
    public HelloMessage withSequence(Integer sequence) {
        this.sequence = sequence;
        return this;
    }

    /**
     * Get the ipAddress field.
     * 
     * @return The ipAddress field
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Set the ipAddress field.
     * 
     * @param ipAddress The ipAddress field
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Set the ipAddress field.
     *
     * @param ipAddress The ipAddress field
     * @return The HelloMessage, for method chaining
     */
    public HelloMessage withIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    /**
     * Get the mailbox field.
     * 
     * @return The mailbox field
     */
    public Integer getMailbox() {
        return mailbox;
    }

    /**
     * Set the mailbox field.
     * 
     * @param mailbox The mailbox field
     */
    public void setMailbox(Integer mailbox) {
        this.mailbox = mailbox;
    }

    /**
     * Set the mailbox field.
     *
     * @param mailbox The mailbox field
     * @return The HelloMessage, for method chaining
     */
    public HelloMessage withMailbox(Integer mailbox) {
        this.mailbox = mailbox;
        return this;
    }

    /**
     * Get the list of groups strings.
     * 
     * @return The groups strings
     */
    public List<String> getGroups() {
        if (groups == null) {
            groups = new ArrayList<>();
        }
        return groups;
    }

    /**
     * Append a value to the groups field.
     *
     * @param value The value
     */
    public void addGroup(String value) {
        getGroups().add(value);
    }

    /**
     * Append a value to the groups field.
     *
     * @param value The value
     * @return The HelloMessage, for method chaining
     */
    public HelloMessage withGroup(String value) {
        getGroups().add(value);
        return this;
    }

    /**
     * Set the list of groups strings.
     * 
     * @param groups The groups collection
     */
    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    /**
     * Set the list of groups strings.
     *
     * @param groups The groups collection
     * @return The HelloMessage, for method chaining
     */
    public HelloMessage withGroups(List<String> groups) {
        this.groups = groups;
        return this;
    }

    /**
     * Get the status field.
     * 
     * @return The status field
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * Set the status field.
     * 
     * @param status The status field
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * Set the status field.
     *
     * @param status The status field
     * @return The HelloMessage, for method chaining
     */
    public HelloMessage withStatus(Integer status) {
        this.status = status;
        return this;
    }

    /**
     * Get the the headers dictionary.
     * 
     * @return The headers dictionary
     */
    public Map<String, String> getHeaders() {
        if (headers == null) {
            headers = new HashMap<>();
        }
        return headers;
    }

    /**
     * Get a value in the headers dictionary as a string.
     * 
     * @param key The dictionary key
     * @param defaultValue The default value if the key does not exist
     */
    public String getHeader(String key, String defaultValue) {
        String value = defaultValue;
        if (headers != null && headers.containsKey(key)) {
            value = headers.get(key);
        }
        return value;
    }

    /**
     * Get a value in the headers dictionary as a long.
     * 
     * @param key The dictionary key
     * @param defaultValue The default value if the key does not exist
     */
    public long getHeader(String key, long defaultValue) {
        long value = defaultValue;
        if (headers != null && headers.containsKey(key)) {
            value = Long.parseLong(headers.get(key));
        }
        return value;
    }

    /**
     * Get a value in the headers dictionary as a long.
     *
     * @param key The dictionary key
     * @param defaultValue The default value if the key does not exist
     */
    public int getHeader(String key, int defaultValue) {
        int value = defaultValue;
        if (headers != null && headers.containsKey(key)) {
            value = Integer.parseInt(headers.get(key));
        }
        return value;
    }

    /**
     * Set a value in the headers dictionary.
     *
     * @param key The dictionary key
     * @param value The value
     */
    public void putHeader(String key, String value) {
        getHeaders().put(key, value);
    }

    /**
     * Set a value in the headers dictionary.
     *
     * @param key The dictionary key
     * @param value The value
     * @return The HelloMessage, for method chaining
     */
    public HelloMessage withHeader(String key, String value) {
        getHeaders().put(key, value);
        return this;
    }

    /**
     * Set a value in the headers dictionary.
     * 
     * @param key The dictionary key
     * @param value The value
     */
    public void putHeader(String key, int value) {
        getHeaders().put(key, String.valueOf(value));
    }

    /**
     * Set a value in the headers dictionary.
     *
     * @param key The dictionary key
     * @param value The value
     * @return The HelloMessage, for method chaining
     */
    public HelloMessage withHeader(String key, int value) {
        getHeaders().put(key, String.valueOf(value));
        return this;
    }

    /**
     * Set a value in the headers dictionary.
     * 
     * @param key The dictionary key
     * @param value The value
     */
    public void putHeader(String key, long value) {
        getHeaders().put(key, String.valueOf(value));
    }

    /**
     * Set a value in the headers dictionary.
     *
     * @param key The dictionary key
     * @param value The value
     * @return The HelloMessage, for method chaining
     */
    public HelloMessage withHeader(String key, long value) {
        getHeaders().put(key, String.valueOf(value));
        return this;
    }

    /**
     * Set the headers dictionary.
     * 
     * @param headers The new headers dictionary
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * Set the headers dictionary.
     *
     * @param headers The new headers dictionary
     * @return The HelloMessage, for method chaining
     */
    public HelloMessage withHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }
}
