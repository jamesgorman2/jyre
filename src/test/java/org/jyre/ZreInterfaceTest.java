package org.jyre;

import org.junit.Test;
import org.zeromq.api.Message;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ZreInterfaceTest {
    private static class ZrePing extends Thread {
        @Override
        public void run() {
            ZreInterface inf = new ZreInterface();
            inf.start();

            while (true) {
                ZreEvent event = inf.receive();

                if (event == null)       // Interrupted
                    break;

                //  If new peer, say hello to it and wait for it to answer us
                switch (event.getEventType()) {
                    case ENTER: {
                        System.out.printf("I: [%s] peer entered\n", event.getPeer());
                        break;
                    }
                    case WHISPER: {
                        String msg = event.getContent().popString();
                        if (msg.equals("HELLO")) {
                            Message outgoing = new Message("WORLD");
                            inf.whisper(event.getPeer(), outgoing);
                        }

                        if (msg.equals("QUIT")) {
                            inf.close();
                            return;
                        }
                        break;
                    }
                    case SHOUT: {
                        String msg = event.getContent().popString();

                        if (msg.equals("HELLO")) {
                            Message outgoing = new Message("WORLD");
                            inf.whisper(event.getPeer(), outgoing);
                        }

                        if (msg.equals("QUIT")) {
                            inf.close();
                            return;
                        }
                        break;
                    }
                    case JOIN: {
                        inf.join(event.getGroup());
                        break;
                    }
                    case LEAVE: {
                        inf.leave(event.getGroup());
                        break;
                    }
                }
            }
        }
    }

    @Test
    public void testInterfaceWhisper() throws Exception {
        ZrePing ping = new ZrePing();
        ping.start();

        ZreInterface inf = new ZreInterface();
        inf.start();

        ZreEvent event = inf.receive();
        String peer = event.getPeer();

        assertThat(event.getEventType(), is(ZreEventType.ENTER));

        Message outgoing = new Message("HELLO");
        inf.whisper(event.getPeer(), outgoing);

        event = inf.receive();
        assertThat(event.getEventType(), is(ZreEventType.WHISPER));
        assertThat(event.getPeer(), is(peer));
        assertThat(event.getContent().popString(), is("WORLD"));

        outgoing = new Message("QUIT");
        inf.whisper(peer, outgoing);

        ping.join();
        inf.close();
    }

    @Test
    public void testInterfaceGroup() throws Exception {
        String group = "TEST";

        ZrePing ping = new ZrePing();
        ping.start();

        ZreInterface inf = new ZreInterface();
        inf.start();
        inf.join(group);

        ZreEvent event = inf.receive();

        assertThat(event.getEventType(), is(ZreEventType.ENTER));
        String peer = event.getPeer();

        event = inf.receive();
        assertThat(event.getEventType(), is(ZreEventType.JOIN));

        Message outgoing = new Message("HELLO");
        inf.shout(group, outgoing);

        event = inf.receive();
        assertThat(event.getEventType(), is(ZreEventType.WHISPER));
        assertThat(event.getPeer(), is(peer));
        assertThat(event.getContent().popString(), is("WORLD"));

        inf.leave(group);

        event = inf.receive();
        assertThat(event.getEventType(), is(ZreEventType.LEAVE));

        outgoing = new Message("QUIT");
        inf.whisper(peer, outgoing);

        ping.join();
        inf.close();
    }

    @Test
    public void testInterfaceShout() throws Exception {
        String group = "TEST";

        ZrePing ping = new ZrePing();
        ping.start();

        ZrePing ping2 = new ZrePing();
        ping2.start();

        ZreInterface inf = new ZreInterface();
        inf.start();

        assertThat(inf.receive().getEventType(), is(ZreEventType.ENTER));
        assertThat(inf.receive().getEventType(), is(ZreEventType.ENTER));

        inf.join(group);

        assertThat(inf.receive().getEventType(), is(ZreEventType.JOIN));
        assertThat(inf.receive().getEventType(), is(ZreEventType.JOIN));

        Message outgoing = new Message("HELLO");
        inf.shout(group, outgoing);

        assertThat(inf.receive().getEventType(), is(ZreEventType.WHISPER));
        assertThat(inf.receive().getEventType(), is(ZreEventType.WHISPER));

        outgoing = new Message("QUIT");
        inf.shout(group, outgoing);

        ping.join();
        ping2.join();
        inf.close();
    }

    @Test
    public void testExit() throws Exception {
        ZrePing ping = new ZrePing();
        ping.start();

        ZreInterface inf = new ZreInterface();
        inf.setEvasiveTimeout(1);
        inf.setExpiredTimeout(1);
        inf.start();

        ZreEvent event = inf.receive();

        assertThat(event.getEventType(), is(ZreEventType.ENTER));
        String peer = event.getPeer();

        Message outgoing = new Message("QUIT");
        inf.whisper(peer, outgoing);

        outgoing = new Message("QUIT");
        inf.whisper(peer, outgoing);

        ping.join();

        // will take PEER_EVASIVE milliseconds
        event = inf.receive();

        assertThat(event.getEventType(), is(ZreEventType.EVASIVE));
        assertThat(event.getPeer(), is(peer));

        // will take PEER_EXPIRED milliseconds
        event = inf.receive();

        assertThat(event.getEventType(), is(ZreEventType.EXIT));
        assertThat(event.getPeer(), is(peer));

        inf.close();
    }
}
