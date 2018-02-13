import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class ClientWriterBroker implements Runnable {

    private final BlockingQueue<String> queue;
    private final static int MESSAGE_LENGTH = 256;

    public ClientWriterBroker(BlockingQueue<String> queue) {
        this.queue = queue;
    }


    @Override
    public void run() {
        try {
            Selector selector = Selector.open();
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(9999));
            socketChannel.configureBlocking(false);
            int interestSet = SelectionKey.OP_WRITE;
            socketChannel.register(selector, interestSet);

            while (true) {
                String write = queue.take();
                while (write != null) {
                    write += "\n";
                    int readyChannels = selector.selectNow();
                    if (readyChannels > 0) {
                        Set<SelectionKey> selectedKeys = selector.selectedKeys();
                        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                        while (keyIterator.hasNext()) {

                            SelectionKey key = keyIterator.next();
                            SocketChannel socket = (SocketChannel) key.channel();
                            if (key.isWritable()) {
                                ByteBuffer buf = ByteBuffer.allocate(MESSAGE_LENGTH);
                                buf.clear();
                                buf.put(write.getBytes());

                                buf.flip();

                                while(buf.hasRemaining()) {
                                    socket.write(buf);
                                }


                            }

                            keyIterator.remove();
                        }
                        write = null;
                    }
                }
            }



        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
