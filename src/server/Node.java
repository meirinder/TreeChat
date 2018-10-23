package server;
import java.net.InetAddress;

public class Node {
    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    private InetAddress address;
    private Integer port;

    public Node(InetAddress address, Integer port) {
        this.address = address;
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Node && ((Node)obj).address.equals(this.address) && ((Node)obj).port.equals(this.port);
    }
}
