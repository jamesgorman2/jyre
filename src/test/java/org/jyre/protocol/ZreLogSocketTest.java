package org.jyre.protocol;

import static org.junit.Assert.*;

import org.junit.*;
import org.jyre.protocol.LogMessage;
import org.jyre.protocol.ZreLogSocket;
import org.zeromq.api.*;
import org.zeromq.jzmq.*;

/**
 * Test ZreLogSocket.
 */
public class ZreLogSocketTest {
    private Context context;
    private Socket dealer;
    private Socket router;
    
    @Before
    public void setUp() {
        context = new ManagedContext();
        dealer = context.buildSocket(SocketType.DEALER)
            .bind("inproc://selftest");
        router = context.buildSocket(SocketType.ROUTER)
            .connect("inproc://selftest");
    }

    @Test
    public void testLog() {
        ZreLogSocket out = new ZreLogSocket(dealer);
        ZreLogSocket in = new ZreLogSocket(router);
        
        LogMessage message = new LogMessage();
        message.setLevel(123);
        message.setEvent(123);
        message.setNode(123);
        message.setPeer(123);
        message.setTime(456L);
        message.setData("Life is short but Now lasts for ever");
        
        assertTrue(out.send(message));
        assertEquals(ZreLogSocket.MessageType.LOG, in.receive());
        message = in.getLog();
        assertEquals(message.getLevel(), Integer.valueOf(123));
        assertEquals(message.getEvent(), Integer.valueOf(123));
        assertEquals(message.getNode(), Integer.valueOf(123));
        assertEquals(message.getPeer(), Integer.valueOf(123));
        assertEquals(message.getTime(), Long.valueOf(456));
        assertEquals(message.getData(), "Life is short but Now lasts for ever");
        
        out.close();
        in.close();
    }
}