import java.nio.channels.SocketChannel;
import java.util.Objects;

public class ClientSession {

    private final SocketChannel socketChannel;
    private String userName;

    public ClientSession(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientSession session = (ClientSession) o;
        return Objects.equals(userName, session.userName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(userName);
    }

}
