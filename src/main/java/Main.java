import org.apache.commons.logging.LogFactory;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class Main {



    public static void main(String[] args) {
        BlockingQueue<SelectionKey> keys = new LinkedBlockingQueue<>();
        Server server = new Server();
        new Thread(server).start();
    }
}
