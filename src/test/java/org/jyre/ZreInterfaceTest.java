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
                Message incoming = inf.receive();

                if (incoming == null)       // Interrupted
                    break;

                //  If new peer, say hello to it and wait for it to answer us
                String event = incoming.popString();
                switch (event) {
                    case "ENTER": {
                        String identity = incoming.popString();
                        System.out.printf("I: [%s] peer entered\n", identity);
                        break;
                    }
                    case "WHISPER": {
                        String peer = incoming.popString();
                        String msg = incoming.popString();

                        if (msg.equals("HELLO")) {
                            Message outgoing = new Message();
                            outgoing.addString(peer);
                            outgoing.addString("WORLD");
                            inf.whisper(outgoing);
                        }

                        if (msg.equals("QUIT")) {
                            inf.close();
                            return;
                        }
                        break;
                    }
                    case "SHOUT": {
                        String identity = incoming.popString();
                        incoming.popString();
                        String msg = incoming.popString();

                        if (msg.equals("HELLO")) {
                            Message outgoing = new Message();
                            outgoing.addString(identity);
                            outgoing.addString("WORLD");
                            inf.whisper(outgoing);
                        }

                        if (msg.equals("QUIT")) {
                            inf.close();
                            return;
                        }
                        break;
                    }
                    case "JOIN": {
                        incoming.popString();
                        String group = incoming.popString();

                        inf.join(group);
                        break;
                    }
                    case "LEAVE": {
                        incoming.popString();
                        String group = incoming.popString();

                        inf.leave(group);
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

        Message incoming = inf.receive();

        String event = incoming.popString();
        assertThat(event, is("ENTER"));
        String peer = incoming.popString();

        Message outgoing = new Message();
        outgoing.addString(peer);
        outgoing.addString("HELLO");
        inf.whisper(outgoing);

        incoming = inf.receive();
        event = incoming.popString();
        assertThat(event, is("WHISPER"));
        assertThat(incoming.popString(), is(peer));
        assertThat(incoming.popString(), is("WORLD"));

        outgoing = new Message();
        outgoing.addString(peer);
        outgoing.addString("QUIT");
        inf.whisper(outgoing);

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

        Message incoming = inf.receive();

        String event = incoming.popString();
        assertThat(event, is("ENTER"));
        String peer = incoming.popString();

        incoming = inf.receive();
        event = incoming.popString();
        assertThat(event, is("JOIN"));

        Message outgoing = new Message();
        outgoing.addString(group);
        outgoing.addString("HELLO");
        inf.shout(outgoing);

        incoming = inf.receive();
        event = incoming.popString();
        assertThat(event, is("WHISPER"));
        assertThat(incoming.popString(), is(peer));
        assertThat(incoming.popString(), is("WORLD"));

        inf.leave(group);

        incoming = inf.receive();
        event = incoming.popString();
        assertThat(event, is("LEAVE"));

        outgoing = new Message();
        outgoing.addString(peer);
        outgoing.addString("QUIT");
        inf.whisper(outgoing);

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

        assertThat(inf.receive().popString(), is("ENTER"));
        assertThat(inf.receive().popString(), is("ENTER"));

        inf.join(group);

        assertThat(inf.receive().popString(), is("JOIN"));
        assertThat(inf.receive().popString(), is("JOIN"));

        Message outgoing = new Message();
        outgoing.addString(group);
        outgoing.addString("HELLO");
        inf.shout(outgoing);

        assertThat(inf.receive().popString(), is("WHISPER"));
        assertThat(inf.receive().popString(), is("WHISPER"));

        outgoing = new Message();
        outgoing.addString(group);
        outgoing.addString("QUIT");
        inf.shout(outgoing);

        ping.join();
        ping2.join();
        inf.close();
    }

    @Test
    public void testExit() throws Exception {
        ZrePing ping = new ZrePing();
        ping.start();

        ZreInterface inf = new ZreInterface();
        inf.start();

        Message incoming = inf.receive();

        String event = incoming.popString();
        assertThat(event, is("ENTER"));
        String peer = incoming.popString();

        Message outgoing = new Message();
        outgoing.addString(peer);
        outgoing.addString("QUIT");
        inf.whisper(outgoing);

        outgoing = new Message();
        outgoing.addString(peer);
        outgoing.addString("QUIT");
        inf.whisper(outgoing);

        ping.join();

        // will take PEER_EVASIVE milliseconds
        incoming = inf.receive();

        event = incoming.popString();
        assertThat(event, is("EVASIVE"));
        assertThat(incoming.popString(), is(peer));

        // will take PEER_EXPIRED milliseconds
        incoming = inf.receive();

        event = incoming.popString();
        assertThat(event, is("EXIT"));
        assertThat(incoming.popString(), is(peer));

        inf.close();
    }
}
