package examples;

import org.jyre.ZreInterface;
import org.zeromq.api.Message;

import java.io.Console;

public class Chat extends Thread {
    private String name;

    public Chat(String name) {
        this.name = name;
    }

    @Override
    public void run() {
        Moderator moderator = new Moderator();
        moderator.start();
    }

    private class Moderator extends Thread {
        private ZreInterface zre;

        @Override
        public void run() {
            zre = new ZreInterface();
            if (name != null) {
                zre.setName(name);
            }
            zre.start();

            while (true) {
                Message message = zre.receive();
                String command = message.popString();
                switch (command) {
                    case "ENTER":
                        onEnter(message);
                        break;
                    case "EXIT":
                        onExit(message);
                        break;
                    case "JOIN":
                        onJoin(message);
                        break;
                    case "LEAVE":
                        onLeave(message);
                        break;
                    case "WHISPER":
                        onWhisper(message);
                        break;
                    case "SHOUT":
                        onShout(message);
                        break;
                    case "EVASIVE":
                        break;
                }
            }
        }

        private void onLeave(Message message) {
            String peer = message.popString();
            String group = message.popString();
            System.out.printf("[INFO] Peer %s left %s\n", peer, group);
        }

        private void onJoin(Message message) {
            String peer = message.popString();
            String group = message.popString();
            System.out.printf("[INFO] Peer %s joined %s\n", peer, group);
        }

        private void onShout(Message message) {
            String peer = message.popString();
            String group = message.popString();
            String content = message.popString();
            System.out.printf("[%s] (%s) %s\n", zre.getPeerName(peer), group, content);
        }

        private void onWhisper(Message message) {
            String peer = message.popString();
            String content = message.popString();
            System.out.printf("[%s] %s\n", zre.getPeerName(peer), content);

            readLine(peer);
        }

        private void readLine(String peer) {
            Console console = System.console();
            String line = console.readLine("> ");
            zre.whisper(new Message(peer).addString(line));
        }

        private void onEnter(Message message) {
            String peer = message.popString();
            System.out.printf("[INFO] Peer %s entered\n", peer);

            readLine(peer);
        }

        private void onExit(Message message) {
            String peer = message.popString();
            System.out.printf("[INFO] Peer %s left\n", peer);
        }
    }

    public static void main(String[] args) {
        String name = null;
        if (args.length > 0) {
            name = args[0];
        }

        new Chat(name).start();
    }
}
