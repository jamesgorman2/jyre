package examples;

import org.jyre.ZreInterface;
import org.zeromq.api.LoopAdapter;
import org.zeromq.api.Message;
import org.zeromq.api.Pollable;
import org.zeromq.api.Reactor;
import org.zeromq.api.Socket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Chat extends Thread {
    private String name;

    private ZreInterface zre;
    private Deque<String> list = new ConcurrentLinkedDeque<>();
    private Reactor reactor;

    public Chat(String name) {
        this.name = name;
    }

    @Override
    public void run() {
        zre = new ZreInterface();
        if (name != null) {
            zre.setName(name);
        }
        zre.start();
        zre.join("home");
        reactor = zre.getContext().buildReactor()
            .withInPollable(zre.getSocket(), new ZyreHandler())
            .withTimerRepeating(500, new ChatHandler())
            .build();
        reactor.start();

        while (true) {
            String line = readLine();
            if (line != null) {
                if (line.startsWith("/")) {
                    String command = line.substring(1, line.contains(" ") ? line.indexOf(" ") : line.length());
                    switch (command) {
                        case "join":
                            zre.join(line.substring(line.indexOf(" ") + 1));
                            break;
                        case "leave":
                            zre.leave(line.substring(line.indexOf(" ") + 1));
                            break;
                        case "exit":
                        case "quit":
                            exit();
                            return;
                    }
                } else {
                    list.add(line);
                }
            } else {
                exit();
                return;
            }
        }
    }

    private void exit() {
        zre.close();
        reactor.stop();
    }

    private String readLine() {
        try {
            return new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        String name = null;
        if (args.length > 0) {
            name = args[0];
        }

        new Chat(name).start();
    }

    private class ChatHandler extends LoopAdapter {
        @Override
        public void execute(Reactor reactor, Pollable pollable) {
            List<String> groups = zre.getOwnGroups();
            while (!list.isEmpty()) {
                String message = list.pop();
                for (String group : groups) {
                    zre.shout(new Message(group).addString(message));
                }
            }
        }
    }

    private class ZyreHandler extends LoopAdapter {
        @Override
        protected void execute(Reactor reactor, Socket socket) {
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

        private void onEnter(Message message) {
            String peer = message.popString();
            System.out.printf("[INFO] %s entered\n", zre.getPeerName(peer));
        }

        private void onExit(Message message) {
            String peer = message.popString();
            System.out.printf("[INFO] %s left\n", zre.getPeerName(peer));
        }

        private void onJoin(Message message) {
            String peer = message.popString();
            String group = message.popString();
            System.out.printf("[INFO] %s joined %s\n", zre.getPeerName(peer), group);
        }

        private void onLeave(Message message) {
            String peer = message.popString();
            String group = message.popString();
            System.out.printf("[INFO] %s left %s\n", zre.getPeerName(peer), group);
        }

        private void onWhisper(Message message) {
            String peer = message.popString();
            String content = message.popString();
            System.out.printf("[%s] %s\n", zre.getPeerName(peer), content);
        }

        private void onShout(Message message) {
            String peer = message.popString();
            String group = message.popString();
            String content = message.popString();
            System.out.printf("[%s:%s] %s\n", group, zre.getPeerName(peer), content);
        }
    }
}
