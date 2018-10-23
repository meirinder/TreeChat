package server;
import java.io.IOException;
import java.net.*;
import java.util.*;

public class Server implements Runnable {

    private static final int CONFIRMATION_PERIOD = 3000;
    private static final int MAX_TIME_SENT = 3;


    private String name;
    private Integer lossPercentage;
    private Node parent;

    private Random random = new Random();

    private DatagramSocket socket;

    private List<DatagramWrapper> unconfirmedMessages;
    private List<Node> children;

    private Timer timer;

    public Server(String name, Integer selfPort, Integer lossPercentage) throws SocketException {
        this.name = name;
        this.lossPercentage = lossPercentage;

        unconfirmedMessages = new Vector<>();
        socket = new DatagramSocket(selfPort);
        children = new Vector<>();
        timer = new Timer();
    }

    public Server(String name, Integer selfPort, Integer lossPercentage, String parentIp, Integer parentPort)
            throws SocketException, UnknownHostException {
        this(name, selfPort, lossPercentage);
        this.parent = new Node(InetAddress.getByName(parentIp), parentPort);
    }

    @Override
    public void run() {
        try {
            Thread consoleReaderThread = new Thread(new TextMessageSender());
            consoleReaderThread.start();
            timer.schedule(new ResendConfirmationTimerTask(unconfirmedMessages), 1000, CONFIRMATION_PERIOD);
            // if parent was specified, notify it that this node is its child
            if (!isRootNode()) {
                Message helloMsg = new Message(UUID.randomUUID(), "", name, Message.MsgType.REGISTER);
                DatagramWrapper datagramWrapper = new DatagramWrapper(helloMsg, parent);
                unconfirmedMessages.add(datagramWrapper);
                sendMessage(datagramWrapper);
            }

            // receiving packets
            while (true) {
                DatagramPacket receivedPacket = new DatagramPacket(new byte[2048], 0, 2048);
                socket.receive(receivedPacket);
                if (random.nextInt(100) < lossPercentage) {
                    System.out.println("Some packet was lost");
                    continue;
                }
                DatagramWrapper wrapper = new DatagramWrapper(receivedPacket);
                System.out.println("Received message: " + wrapper.getMessage());
                switch (wrapper.getMessage().getType()) {
                    case REGISTER:
                        Node node = new Node(receivedPacket.getAddress(), receivedPacket.getPort());
                        if (children.indexOf(node) == -1) {
                            children.add(node);
                        }
                        sendConfirmation(wrapper);
                        break;
                    case TEXT:
                        if (hasSuchUuid(wrapper.getMessage().getUuid()) == -1) {
                            System.out.println(wrapper.getMessage().getSenderName() + ": " + wrapper.getMessage().getText());
                        }
                        sendConfirmation(wrapper);

                        if (!isRootNode()
                                && ! (parent.getAddress().equals(wrapper.getNode().getAddress())
                                && parent.getPort().equals(wrapper.getNode().getPort()))) {
                            forwardMessage(wrapper.getMessage(), parent);
                        }

                        for (Node child :
                                children) {
                            if (child.getAddress().equals(wrapper.getNode().getAddress()) &&
                                    child.getPort().equals(wrapper.getNode().getPort()))
                                continue;
                            forwardMessage(wrapper.getMessage(), child);
                        }

                        break;
                    case CONFIRMATION:
                        confirmMessageWithUuid(wrapper.getMessage().getUuid());
                        break;
                }
            }
        } catch (IOException |
                ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void forwardMessage(Message message, Node node) throws IOException {
        Message msg = new Message(message);
        msg.setUuid(UUID.randomUUID());
        DatagramWrapper datagramWrapper = new DatagramWrapper(msg, node);
        unconfirmedMessages.add(datagramWrapper);
        sendMessage(datagramWrapper);
    }


    private synchronized void sendMessage(DatagramWrapper datagramWrapper) throws IOException {
        System.out.println("Sending message: " + datagramWrapper.getMessage());
        socket.send(datagramWrapper.convertToDatagramPacket());
    }

    private synchronized void sendMessageToEveryone(Message message) throws IOException {
        for (Node child :
                children) {
            DatagramWrapper wrapper = new DatagramWrapper(message, child);
            unconfirmedMessages.add(wrapper);
            sendMessage(wrapper);
        }
        if (!isRootNode()) {
            DatagramWrapper wrapper = new DatagramWrapper(message, parent);
            unconfirmedMessages.add(wrapper);
            sendMessage(wrapper);
        }
    }

    private boolean isRootNode() {
        return (parent == null);
    }

    private synchronized void confirmMessageWithUuid(UUID uuid) {
        for (int i = 0; i < unconfirmedMessages.size(); i++) {
            if (unconfirmedMessages.get(i).getMessage().getUuid().equals(uuid)) {
                unconfirmedMessages.remove(i);
                return;
            }
        }
    }

    private synchronized int hasSuchUuid(UUID uuid) {
        for (int i = 0; i < unconfirmedMessages.size(); i++) {
            if (unconfirmedMessages.get(i).getMessage().getUuid().equals(uuid)) {
                return i;
            }
        }
        return -1;
    }

    private void sendConfirmation(DatagramWrapper datagramWrapper) throws IOException {
        DatagramWrapper wrapper = new DatagramWrapper(new Message(datagramWrapper.getMessage()), datagramWrapper.getNode());
        wrapper.getMessage().setType(Message.MsgType.CONFIRMATION);
        sendMessage(wrapper);
    }

    private class ResendConfirmationTimerTask extends TimerTask {

        private List<DatagramWrapper> unconfirmedMessages;

        ResendConfirmationTimerTask(List<DatagramWrapper> unconfirmedMessages) {
            this.unconfirmedMessages = unconfirmedMessages;
        }

        @Override
        public void run() {
            for (int i = 0; i < unconfirmedMessages.size(); ++i) {
                DatagramWrapper wrapper = unconfirmedMessages.get(i);
                if (wrapper.getTimesSent() >= MAX_TIME_SENT) {
                    deleteNodeAsInWrapper(wrapper);
                    unconfirmedMessages.remove(wrapper);
                }
                wrapper.increaseTimesSent();
                try {
                    sendMessage(wrapper);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private synchronized void deleteNodeAsInWrapper(DatagramWrapper wrapper) {
            for (int i = 0; i < children.size(); ++i) {
                if (children.get(i).equals(wrapper.getNode())) {
                    children.remove(i);
                    return;
                }
            }
            if (parent.equals(wrapper.getNode()))
                parent = null;
        }
    }

    private class TextMessageSender implements Runnable {

        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String text = scanner.nextLine();
                try {
                    sendMessageToEveryone(new Message(UUID.randomUUID(), text, name, Message.MsgType.TEXT));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
