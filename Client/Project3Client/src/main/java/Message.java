import java.io.Serializable;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    public enum MessageType {
        CHAT,           // Regular chat message
        LOGIN,          // Login with username
        LOGIN_SUCCESS,  // Login was successful
        LOGIN_FAILED,   // Login failed (username taken)
        GAME_REQUEST,   // Request to start a game
        GAME_STARTED,   // Game has started with opponent info
        GAME_MOVE,      // A move was made in the game
        GAME_STATE,     // Current state of the game
        GAME_OVER,      // Game is over with result
        PLAY_AGAIN,     // Request to play again
        QUIT            // Quit the game/connection
    }

    private MessageType type;
    private String sender;
    private String content;
    private Object data;  // Can hold game state, move data, etc.

    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.data = null;
    }

    public Message(MessageType type, String sender, String content, Object data) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.data = data;
    }

    public MessageType getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        if (type == MessageType.CHAT) {
            return sender + ": " + content;
        }
        return content;
    }
}