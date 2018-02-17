import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Server implements Runnable {

    private final static Log LOG = LogFactory.getLog(Server.class);
    private ServerSocketChannel serverSocketChannel = null;
    private final Map<ClientSession, List<String>> toSend;
    private final Map<ClientSession, List<String>> alreadySent;

    public Server() {
        alreadySent = new HashMap<>();
        toSend = new HashMap<>();
    }

    @Override
    public void run() {
        startServerSocket();
    }

    private void startServerSocket() {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(9999));
            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            int interestSet = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
            while (true) {
                int readyChannels = selector.select();
                if (readyChannels > 0) {
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                    while (keyIterator.hasNext()) {

                        SelectionKey key = keyIterator.next();

                        if (key.isAcceptable()) {
                            acceptSocket(selector, interestSet);
                        } else if (key.isReadable()) {
                            ClientSession session = (ClientSession) key.attachment();
                            SocketChannel socketChannel = session.getSocketChannel();

                            // a channel is ready for reading
                            if (session.getUserName() != null) {
                                ByteBuffer buf = ByteBuffer.allocate(Constants.MESSAGE_LENGTH);
                                String msg = readFromSocketChannel(socketChannel, buf).replace("\n","");
                                buf.clear();
                                if (msg != null && msg.length() > 0) {
                                    msg += " " + Date.from(Instant.now());
                                    msg = session.getUserName() + ": " + msg;
                                    this.toSend.putIfAbsent(session, new LinkedList<>());
                                    List<String> msgs = this.toSend.get(session);
                                    msgs.add(msg);
                                    LOG.info("GOT message: '" + msg + "'");
                                    this.toSend.put(session, msgs);
                                }
                            } else {
                                ByteBuffer buf = ByteBuffer.allocate(48);
                                String name = readFromSocketChannel(socketChannel, buf);
                                name = name.replace("\n","");
                                buf.clear();
                                if (name != null && name.length() > 0) {
                                    LOG.info("GOT username: '" + name + "'");
                                    session.setUserName(name);
                                }
                            }
                        } else if (key.isWritable()) {
                            // a channel is ready for writing
                            ClientSession session = (ClientSession) key.attachment();
                            SocketChannel socketChannel = session.getSocketChannel();

                            if (session.getUserName() != null) {
                                writeToUser(session);
                            }
                        }

                        keyIterator.remove();
                    }
                }
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        } finally {
            try {
                if (serverSocketChannel != null) {
                    serverSocketChannel.close();
                }
            } catch (IOException e) {
                LOG.error("Error shutdown serverSocketChannel\n" + e.getMessage());
            }
        }


    }

    private void writeToUser(ClientSession session) throws IOException {
        SocketChannel socket = session.getSocketChannel();
        alreadySent.putIfAbsent(session, new LinkedList<>());
        List<String> alreadySent = this.alreadySent.get(session);
        for (Map.Entry<ClientSession, List<String>> entry : this.toSend.entrySet()) {
            if (!entry.getKey().equals(session)) {
                List<String> list = entry.getValue();
                ByteBuffer buf = ByteBuffer.allocate(Constants.MESSAGE_LENGTH);

                for (String s : list) {
                    if (!alreadySent.contains(s)) {
                        alreadySent.add(s);
                        LOG.info("WRITING msg to user '" + session.getUserName() + "': " + s);
                        s += "\n";
                        buf.put(s.getBytes());

                        buf.flip();

                        while(buf.hasRemaining()) {
                            socket.write(buf);
                        }
                    }
                }
                buf.clear();
            }

        }

    }

    private String readFromSocketChannel(SocketChannel channel, ByteBuffer buffer) throws IOException {
        int bytesRead = channel.read(buffer); //read into buffer.
        List<Character> chars = new LinkedList<>();
        while (bytesRead != -1) {

            buffer.flip();  //make buffer ready for read

            while (buffer.hasRemaining()) {
                chars.add((char) buffer.get()); // read 1 byte at a time
                String s = chars.stream().map(Object::toString).collect(Collectors.joining());
                if (s.contains("\n")) {
                    buffer.clear();
                    return s;
                }
            }

            buffer.clear(); //make buffer ready for writing
            bytesRead = channel.read(buffer);
        }
        return chars.stream().map(Object::toString).collect(Collectors.joining());
    }

    private void sendMessages(List<String> messages) {

    }

    private List<String> readMessages() {
        return new LinkedList<>();
    }

    private void acceptSocket(Selector selector, int interestSet) throws IOException {
        SocketChannel socketChannel =
                serverSocketChannel.accept();

        if (socketChannel != null) {
            socketChannel.configureBlocking(false);
            LOG.info("Accpted socket");
            socketChannel.register(selector, interestSet, new ClientSession(socketChannel));
        }
    }
}
