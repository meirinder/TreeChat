package server;
import java.io.*;
import java.net.DatagramPacket;

public class DatagramWrapper {

    private Message message;

    public Node getNode() {
        return node;
    }

    private Node node;
    private ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
    private ObjectOutputStream oos;

    private int timesSent = 0;

    public Message getMessage() {
        return message;
    }

    public DatagramWrapper(Message message, Node node) {
        try {
            oos = new ObjectOutputStream(baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.message = message;
        this.node = node;
    }

    public DatagramWrapper(DatagramPacket packet) throws IOException, ClassNotFoundException {
        try {
            oos = new ObjectOutputStream(baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.node = new Node(packet.getAddress(), packet.getPort());
        this.message = deserializeObject(packet.getData());
    }

    private synchronized byte[] serializeObject(Object object) throws IOException {
        oos.writeObject(object);
        return baos.toByteArray();
    }

    private synchronized <T> T deserializeObject(byte[] rawData) throws IOException, ClassNotFoundException {
        return (T) new ObjectInputStream(new ByteArrayInputStream(rawData)).readObject();
    }

    public DatagramPacket convertToDatagramPacket() throws IOException {
        byte[] toSend = serializeObject(message);
        return new DatagramPacket(toSend, toSend.length, node.getAddress(), node.getPort());
    }

    public int getTimesSent() {
        return timesSent;
    }

    public void increaseTimesSent() {
        timesSent++;
    }
}
