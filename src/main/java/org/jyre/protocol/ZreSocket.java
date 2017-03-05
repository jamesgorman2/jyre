/* ============================================================================
 * ZreSocket.java
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

import org.zeromq.ZMQ;
import org.zeromq.api.Message;
import org.zeromq.api.Message.Frame;
import org.zeromq.api.Socket;

import java.io.Closeable;

/**
 * ZreSocket class.
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
public class ZreSocket extends ZreCodec implements Closeable {
    //  Structure of our class
    private Socket socket;        //  Internal socket handle
    private Frame address;        //  Address of peer if any

    /**
     * Create a new ZreSocket.
     * 
     * @param socket The internal socket
     */
    public ZreSocket(Socket socket) {
        assert socket != null;
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
     * Destroy the ZreSocket.
     */
    @Override
    public void close() {
        socket.close();
    }

    /**
     * Receive a message on the socket.
     *
     * @return The MessageType of the received message
     */
    public MessageType receive() {
        //  Read valid message frame from socket; we loop over any
        //  garbage data we might receive from badly-connected peers
        MessageType type;
        Message frames;
        do {
            frames = socket.receiveMessage();

            //  If we're reading from a ROUTER socket, get address
            if (socket.getZMQSocket().getType() == ZMQ.ROUTER) {
                this.address = frames.popFrame();
            }

            //  Get and check protocol signature
            type = deserialize(frames);
        } while (type == null);          //  Protocol assertion, drop message if malformed or invalid

        return type;
    }

    /**
     * Send the HELLO to the socket in one step.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public boolean send(HelloMessage message) {
        Message frames = serialize(message);

        //  If we're sending to a ROUTER, we add the address first
        if (socket.getZMQSocket().getType() == ZMQ.ROUTER) {
            assert address != null;
            frames.pushFrame(address);
        }

        return socket.send(frames);
    }

    /**
     * Send the WHISPER to the socket in one step.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public boolean send(WhisperMessage message) {
        Message frames = serialize(message);

        //  If we're sending to a ROUTER, we add the address first
        if (socket.getZMQSocket().getType() == ZMQ.ROUTER) {
            assert address != null;
            frames.pushFrame(address);
        }

        return socket.send(frames);
    }

    /**
     * Send the SHOUT to the socket in one step.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public boolean send(ShoutMessage message) {
        Message frames = serialize(message);

        //  If we're sending to a ROUTER, we add the address first
        if (socket.getZMQSocket().getType() == ZMQ.ROUTER) {
            assert address != null;
            frames.pushFrame(address);
        }

        return socket.send(frames);
    }

    /**
     * Send the JOIN to the socket in one step.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public boolean send(JoinMessage message) {
        Message frames = serialize(message);

        //  If we're sending to a ROUTER, we add the address first
        if (socket.getZMQSocket().getType() == ZMQ.ROUTER) {
            assert address != null;
            frames.pushFrame(address);
        }

        return socket.send(frames);
    }

    /**
     * Send the LEAVE to the socket in one step.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public boolean send(LeaveMessage message) {
        Message frames = serialize(message);

        //  If we're sending to a ROUTER, we add the address first
        if (socket.getZMQSocket().getType() == ZMQ.ROUTER) {
            assert address != null;
            frames.pushFrame(address);
        }

        return socket.send(frames);
    }

    /**
     * Send the PING to the socket in one step.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public boolean send(PingMessage message) {
        Message frames = serialize(message);

        //  If we're sending to a ROUTER, we add the address first
        if (socket.getZMQSocket().getType() == ZMQ.ROUTER) {
            assert address != null;
            frames.pushFrame(address);
        }

        return socket.send(frames);
    }

    /**
     * Send the PING_OK to the socket in one step.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public boolean send(PingOkMessage message) {
        Message frames = serialize(message);

        //  If we're sending to a ROUTER, we add the address first
        if (socket.getZMQSocket().getType() == ZMQ.ROUTER) {
            assert address != null;
            frames.pushFrame(address);
        }

        return socket.send(frames);
    }
}

