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
package org.jyre.protocol;

import org.zeromq.ZMQ;
import org.zeromq.api.Message;
import org.zeromq.api.Message.Frame;
import org.zeromq.api.MessageFlag;
import org.zeromq.api.Socket;

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
public class ZreLogSocket implements ZreLogCodec.Constants, java.io.Closeable {
    //  Structure of our class
    private Socket socket;               //  Internal socket handle
    private ZreLogCodec codec;           //  Serialization codec
    private Frame address;               //  Address of peer if any

    /**
     * Create a new ZreLogSocket.
     * 
     * @param socket The internal socket
     */
    public ZreLogSocket(Socket socket) {
        assert socket != null;
        this.socket = socket;
        this.codec = new ZreLogCodec();
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
     * Get the internal socket.
     *
     * @return The internal socket
     */
    public ZreLogCodec getCodec() {
        return codec;
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
     *
     * @return The MessageType of the received message
     */
    public ZreLogCodec.MessageType receive() {
        return receive(MessageFlag.NONE);
    }

    /**
     * Receive a message on the socket.
     *
     * @param flag Flag controlling behavior of the receive operation
     * @return The MessageType of the received message, or null if no message received
     */
    public ZreLogCodec.MessageType receive(MessageFlag flag) {
        //  Read valid message frame from socket; we loop over any
        //  garbage data we might receive from badly-connected peers
        ZreLogCodec.MessageType type;
        Message frames;
        do {
            frames = socket.receiveMessage(flag);

            //  If we're reading from a ROUTER socket, get address
            if (socket.getZMQSocket().getType() == ZMQ.ROUTER) {
                this.address = frames.popFrame();
            }

            //  Get and check protocol signature
            type = codec.deserialize(frames);
        } while (type == null && flag == MessageFlag.NONE);          //  Protocol assertion, drop message if malformed or invalid

        return type;
    }

    /**
     * Get a LOG message from the socket.
     *
     * @return The LogMessage last received on this socket
     */
    public LogMessage getLog() {
        return codec.log;
    }

    /**
     * Send the LOG to the socket in one step.
     *
     * @param message The message to send
     * @return true if the message was sent, false otherwise
     */
    public boolean send(LogMessage message) {
        Message frames = codec.serialize(message);

        //  If we're sending to a ROUTER, we add the address first
        if (socket.getZMQSocket().getType() == ZMQ.ROUTER) {
            assert address != null;
            frames.pushFrame(address);
        }

        return socket.send(frames);
    }
}

