package model;

public class Message {
    private String payload;
    private int attempts;

    public Message(String payload) {
        this.payload = payload;
        this.attempts = 0;
    }

    public String getMessage() {
        return payload;
    }

    public int getAttempts() {
        return attempts;
    }

    public void increaseAttempts() {
        attempts++;
    }
}
