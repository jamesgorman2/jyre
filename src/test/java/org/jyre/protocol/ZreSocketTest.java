package org.jyre.protocol;

import static org.junit.Assert.*;

import org.junit.*;
import org.zeromq.api.*;
import org.zeromq.api.Message.Frame;
import org.zeromq.jzmq.*;

/**
 * Test ZreSocket.
 */
public class ZreSocketTest {
    private Socket dealer;
    private Socket router;
    
    @Before
    public void setUp() {
        Context context = new ManagedContext();
        dealer = context.buildSocket(SocketType.DEALER)
            .bind("inproc://selftest");
        router = context.buildSocket(SocketType.ROUTER)
            .connect("inproc://selftest");
    }

    @Test
    public void testHello() {
        ZreSocket out = new ZreSocket(dealer);
        ZreSocket in = new ZreSocket(router);
        
        HelloMessage message = new HelloMessage();
        message.setSequence(123);
        message.setEndpoint("Life is short but Now lasts for ever");
        message.addGroup("Name: Brutus");
        message.addGroup("Age: 43");
        message.setStatus(123);
        message.setName("Life is short but Now lasts for ever");
        message.putHeader("Name", "Brutus");
        message.putHeader("Age", 43);
        
        assertTrue(out.send(message));
        assertEquals(ZreCodec.MessageType.HELLO, in.receive());
        message = in.getHello();
        assertEquals(message.getSequence(), Integer.valueOf(123));
        assertEquals(message.getEndpoint(), "Life is short but Now lasts for ever");
        assertEquals(message.getGroups().size(), 2);
        assertEquals(message.getGroups().get(0), "Name: Brutus");
        assertEquals(message.getGroups().get(1), "Age: 43");
        assertEquals(message.getStatus(), Integer.valueOf(123));
        assertEquals(message.getName(), "Life is short but Now lasts for ever");
        assertEquals(message.getHeaders().size(), 2);
        assertEquals(message.getHeader("Name", "?"), "Brutus");
        assertEquals(message.getHeader("Age", 0), 43);
        
        out.close();
        in.close();
    }

    @Test
    public void testWhisper() {
        ZreSocket out = new ZreSocket(dealer);
        ZreSocket in = new ZreSocket(router);
        
        WhisperMessage message = new WhisperMessage();
        message.setSequence(123);
        message.setContent(Frame.of("Captcha Diem"));
        
        assertTrue(out.send(message));
        assertEquals(ZreCodec.MessageType.WHISPER, in.receive());
        message = in.getWhisper();
        assertEquals(message.getSequence(), Integer.valueOf(123));
        assertEquals("Captcha Diem", message.getContent().toString());
        
        out.close();
        in.close();
    }

    @Test
    public void testShout() {
        ZreSocket out = new ZreSocket(dealer);
        ZreSocket in = new ZreSocket(router);
        
        ShoutMessage message = new ShoutMessage();
        message.setSequence(123);
        message.setGroup("Life is short but Now lasts for ever");
        message.setContent(Frame.of("Captcha Diem"));
        
        assertTrue(out.send(message));
        assertEquals(ZreCodec.MessageType.SHOUT, in.receive());
        message = in.getShout();
        assertEquals(message.getSequence(), Integer.valueOf(123));
        assertEquals(message.getGroup(), "Life is short but Now lasts for ever");
        assertEquals("Captcha Diem", message.getContent().toString());
        
        out.close();
        in.close();
    }

    @Test
    public void testJoin() {
        ZreSocket out = new ZreSocket(dealer);
        ZreSocket in = new ZreSocket(router);
        
        JoinMessage message = new JoinMessage();
        message.setSequence(123);
        message.setGroup("Life is short but Now lasts for ever");
        message.setStatus(123);
        
        assertTrue(out.send(message));
        assertEquals(ZreCodec.MessageType.JOIN, in.receive());
        message = in.getJoin();
        assertEquals(message.getSequence(), Integer.valueOf(123));
        assertEquals(message.getGroup(), "Life is short but Now lasts for ever");
        assertEquals(message.getStatus(), Integer.valueOf(123));
        
        out.close();
        in.close();
    }

    @Test
    public void testLeave() {
        ZreSocket out = new ZreSocket(dealer);
        ZreSocket in = new ZreSocket(router);
        
        LeaveMessage message = new LeaveMessage();
        message.setSequence(123);
        message.setGroup("Life is short but Now lasts for ever");
        message.setStatus(123);
        
        assertTrue(out.send(message));
        assertEquals(ZreCodec.MessageType.LEAVE, in.receive());
        message = in.getLeave();
        assertEquals(message.getSequence(), Integer.valueOf(123));
        assertEquals(message.getGroup(), "Life is short but Now lasts for ever");
        assertEquals(message.getStatus(), Integer.valueOf(123));
        
        out.close();
        in.close();
    }

    @Test
    public void testPing() {
        ZreSocket out = new ZreSocket(dealer);
        ZreSocket in = new ZreSocket(router);
        
        PingMessage message = new PingMessage();
        message.setSequence(123);
        
        assertTrue(out.send(message));
        assertEquals(ZreCodec.MessageType.PING, in.receive());
        message = in.getPing();
        assertEquals(message.getSequence(), Integer.valueOf(123));
        
        out.close();
        in.close();
    }

    @Test
    public void testPingOk() {
        ZreSocket out = new ZreSocket(dealer);
        ZreSocket in = new ZreSocket(router);
        
        PingOkMessage message = new PingOkMessage();
        message.setSequence(123);
        
        assertTrue(out.send(message));
        assertEquals(ZreCodec.MessageType.PING_OK, in.receive());
        message = in.getPingOk();
        assertEquals(message.getSequence(), Integer.valueOf(123));
        
        out.close();
        in.close();
    }
}