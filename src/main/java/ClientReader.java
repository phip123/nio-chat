import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ClientReader implements Runnable {



    public static void main(String[] args) {
        ClientReader client = new ClientReader();

        new Thread(client).start();
    }

    @Override
    public void run() {
        try {
            PrintWriter writer = new PrintWriter(System.out);
            Selector selector = Selector.open();
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(9999));
            socketChannel.configureBlocking(false);
            int interestSet = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
            socketChannel.register(selector, interestSet);
            boolean sendName = true;
            while (true) {
                int readyChannels = selector.selectNow();
                if (readyChannels > 0) {
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                    while (keyIterator.hasNext()) {

                        SelectionKey key = keyIterator.next();
                        SocketChannel socket = (SocketChannel) key.channel();

                        if (key.isWritable() && sendName) {
                            System.out.println("sending name");
                            ByteBuffer buf = ByteBuffer.allocate(Constants.MESSAGE_LENGTH);
                            buf.clear();
                            buf.put("chatroom\n".getBytes());

                            buf.flip();

                            while(buf.hasRemaining()) {
                                socket.write(buf);
                            }


                            sendName = false;
                        }

                        if (key.isReadable()) {
                            SocketChannel channel = (SocketChannel) key.channel();
                            ByteBuffer buf = ByteBuffer.allocate(Constants.MESSAGE_LENGTH);
                            String msg = readFromSocketChannel(channel, buf);
                            buf.clear();
                            if (msg != null && msg.length() > 0) {
                                System.out.print(msg);
                            }
                        }

                        keyIterator.remove();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
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
}
