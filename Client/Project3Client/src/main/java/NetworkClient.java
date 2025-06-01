import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class NetworkClient extends Thread {
    private String host = "127.0.0.1";
    private int port = 5555;
    
    private Socket socketClient;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
    private String username;
    private boolean isLoggedIn;
    private int playerNumber; // 1 or 2
    private GameBoard gameBoard;
    private boolean inGame;
    private String opponent;
    
    private Consumer<Message> messageHandler;
    
    public NetworkClient(Consumer<Message> messageHandler) {
        this.messageHandler = messageHandler;
    }
    
    @Override
    public void run() {
        try {
            socketClient = new Socket(host, port);
            out = new ObjectOutputStream(socketClient.getOutputStream());
            in = new ObjectInputStream(socketClient.getInputStream());
            socketClient.setTcpNoDelay(true);
            
            // Handle incoming messages
            while(true) {
                try {
                    // Read message from server
                    Message message = (Message) in.readObject();
                    
                    // Update game state if included in message
                    if (message.getType() == Message.MessageType.GAME_STATE ||
                        message.getType() == Message.MessageType.GAME_STARTED) {
                        if (message.getData() != null) {
                            // Handle BoardState objects
                            if (message.getData() instanceof BoardState) {
                                BoardState boardState = (BoardState) message.getData();
                                System.out.println("Received BoardState object");
                                
                                // If we don't have a game board yet, create one
                                if (this.gameBoard == null) {
                                    this.gameBoard = new GameBoard();
                                }
                                
                                // Update the game board with the board state data
                                this.gameBoard.updateFromBoardState(boardState);
                                System.out.println("Updated game board from BoardState");
                            } 
                            // Handle GameBoard objects (for backward compatibility)
                            else if (message.getData() instanceof GameBoard) {
                                this.gameBoard = (GameBoard) message.getData();
                                System.out.println("Updated game board from GameBoard message");
                            }
                        }
                    }
                    
                    // Special handling for GAME_STARTED message
                    if (message.getType() == Message.MessageType.GAME_STARTED) {
                        // Set game-in-progress state
                        this.inGame = true;
                        
                        // Get player number from message data
                        if (message.getData() instanceof Integer) {
                            this.playerNumber = (Integer) message.getData();
                        }
                        
                        // Extract opponent name from the content
                        try {
                            String content = message.getContent();
                            
                            // Parse out opponent name from message content
                            if (content.contains("against")) {
                                int startIndex = content.indexOf("against") + 8;
                                int endIndex = content.indexOf(".", startIndex);
                                if (endIndex == -1) endIndex = content.length();
                                this.opponent = content.substring(startIndex, endIndex).trim();
                            }
                            
                            // IMPORTANT: Create a new game board if one doesn't exist yet
                            if (this.gameBoard == null) {
                                this.gameBoard = new GameBoard(); // Player 1 always starts first
                                System.out.println("Created new game board with player 1 starting");
                            }
                        } catch (Exception e) {
                            System.out.println("Error parsing game start info: " + e.getMessage());
                        }
                    }
                    
                    // Process message with handler
                    messageHandler.accept(message);
                }
                catch(Exception e) {
                    System.out.println("Error reading from server: " + e.getMessage());
                    e.printStackTrace();
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Client connection error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (socketClient != null && !socketClient.isClosed()) {
                    socketClient.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Send a message to the server
    private void sendMessage(Message message) {
        try {
            if (socketClient == null || socketClient.isClosed()) {
                System.out.println("Cannot send message - socket is closed or null");
                return;
            }
            
            if (out == null) {
                System.out.println("Cannot send message - output stream is null");
                return;
            }
            
            out.writeObject(message);
            out.flush();
            System.out.println("Sent message of type: " + message.getType());
        } catch (IOException e) {
            System.out.println("Error sending message to server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Login with username
    public void login(String username) {
        this.username = username;
        Message loginMsg = new Message(Message.MessageType.LOGIN, username, username);
        sendMessage(loginMsg);
    }
    
    // Request to be matched with another player
    public void requestGame() {
        Message requestMsg = new Message(Message.MessageType.GAME_REQUEST, username, "Request game");
        sendMessage(requestMsg);
    }
    
    // Make a move at the specified column
    public void makeMove(int column) {
        System.out.println("makeMove called for column " + column);
        
        // If game board doesn't exist, create one
        if (gameBoard == null && inGame) {
            gameBoard = new GameBoard();
            System.out.println("Created a new game board");
        }
        
        // Only send move if we're in a game and it's our turn
        if (inGame && gameBoard != null && gameBoard.getCurrentPlayer() == playerNumber) {
            System.out.println("SENDING MOVE: Column " + column);
            System.out.println("Current player: " + gameBoard.getCurrentPlayer());
            System.out.println("Your player number: " + playerNumber);
            
            // Create and send the move message with column in both content and data
            // Some server implementations might check content, others might check data
            Message moveMsg = new Message(Message.MessageType.GAME_MOVE, username, Integer.toString(column), column);
            sendMessage(moveMsg);
        } else {
            System.out.println("⚠️ MOVE REJECTED ⚠️");
            System.out.println("inGame: " + inGame);
            if (gameBoard != null) {
                System.out.println("Current player's turn: " + gameBoard.getCurrentPlayer());
                System.out.println("Your player number: " + playerNumber);
                System.out.println("Is your turn: " + (gameBoard.getCurrentPlayer() == playerNumber));
            } else {
                System.out.println("Game board is null");
            }
        }
    }
    
    // Request to play again after a game ends
    public void playAgain() {
        if (!inGame) {
            System.out.println("Starting a new game");
            inGame = true;
        }
        
        Message playAgainMsg = new Message(Message.MessageType.PLAY_AGAIN, username, "Play again");
        sendMessage(playAgainMsg);
        
        // If we have a game board, reset it for the new game
        if (gameBoard != null) {
            gameBoard.resetGame();
            System.out.println("Reset game board for new game");
        }
    }
    
    // Quit the current game
    public void quitGame() {
        Message quitMsg = new Message(Message.MessageType.QUIT, username, "Quit game");
        sendMessage(quitMsg);
        inGame = false;
        gameBoard = null;
    }
    
    // Send a chat message
    public void sendChatMessage(String content) {
        Message chatMsg = new Message(Message.MessageType.CHAT, username, content);
        sendMessage(chatMsg);
    }
    
    // Utility method to create a new game board
    public void createNewBoard() {
        this.gameBoard = new GameBoard();
        System.out.println("Created new game board");
    }
    
    // Getters
    public String getUsername() { return username; }
    public int getPlayerNumber() { return playerNumber; }
    public GameBoard getGameBoard() { return gameBoard; }
    public String getOpponent() { return opponent; }
    
    // Setters
    public void setPlayerNumber(int playerNumber) { this.playerNumber = playerNumber; }
    public void setInGame(boolean inGame) { this.inGame = inGame; }
    public void setOpponent(String opponent) { this.opponent = opponent; }
}