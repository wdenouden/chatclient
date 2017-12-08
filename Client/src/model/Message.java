package model;

public class Message {
    private MessageType type;
    private String payload;

    public Message(MessageType type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public Message(MessageType type) {
        this.type = type;
        this.payload = "";
    }


    public MessageType getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public String getFullMessage() {
        if(payload.isEmpty()) {
            return type.name();
        }else {
            return type.name() + " " + payload;
        }

    }
}
