import sun.awt.geom.AreaOp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientWriter implements Runnable {

    private final BlockingQueue<String> queue;

    public ClientWriter(BlockingQueue<String> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String read = reader.readLine();
                this.queue.add(read);
            }
        } catch (IOException e) {
            System.err.println("io error");
        }
    }

    public static void main(String[] args) {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        ClientWriter writer = new ClientWriter(queue);
        ClientWriterBroker writerBroker = new ClientWriterBroker(queue);
        new Thread(writer).start();
        new Thread(writerBroker).start();
    }
}
