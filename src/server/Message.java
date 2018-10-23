package server;
import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {

    public MsgType getType() {
        return type;
    }

    public void setType(MsgType type) {
        this.type = type;
    }

    public enum MsgType {
        REGISTER,
        TEXT,
        CONFIRMATION
    }

    private UUID uuid;
    private String text;
    private String senderName;
    private MsgType type;

    public Message(UUID uuid, String text, String senderName, MsgType type) {
        this.uuid = uuid;
        this.text = text;
        this.senderName = senderName;
        this.type = type;
    }

    public Message(Message msg) {
        this.uuid = msg.uuid;
        this.text = msg.text;
        this.senderName = msg.senderName;
        this.type = msg.type;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    @Override
    public String toString() {
        return "UUID: " + uuid.toString() +". Text: " + text + ". Sender: " + senderName + ". Type: " + type.toString();
    }
}
