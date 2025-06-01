import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.control.ListView;

public class Server {
    private final int port = 5555;
    private ArrayList<ClientThread> clients;
    private HashMap<String, ClientThread> userClientMap;
    private HashMap<String, String> playerPairings;  // Maps player to their opponent
    private HashMap<String, GameBoard> gameBoards;   // Game boards for each pairing
    private HashMap<String, String> waitingPlayers;  // Players waiting for a match
    private TheServer server;
    private Consumer<String> serverLog;
    
    // Constructor with logging capability
    public Server(Consumer<String> logger) {
        this.serverLog = logger;
        clients = new ArrayList<>();
        userClientMap = new HashMap<>();
        playerPairings = new HashMap<>();
        gameBoards = new HashMap<>();
        waitingPlayers = new HashMap<>();
        server = new TheServer();
        server.start();
    }
    
    // Constructor for use without a GUI
    public Server() {
        this(s -> System.out.println(s));
    }

    // Logs server activity
    private void log(String message) {
        serverLog.accept(message);
    }
    
    private class TheServer extends Thread {
        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                log("Server started on port: " + port);
                
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    ClientThread client = new ClientThread(clientSocket);
                    clients.add(client);
                    client.start();
                    log("Client connected. Total clients: " + clients.size());
                }
            } catch (Exception e) {
                log("Error starting server: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private class ClientThread extends Thread {
        private Socket connection;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String username;
        private boolean loggedIn = false;

        ClientThread(Socket socket) {
            this.connection = socket;
            this.username = "Anonymous";
        }
        
        // Send message to this client
        public void sendMessage(Message message) {
            try {
                // Make sure output is properly flushed
                out.writeObject(message);
                out.flush();
                out.reset(); // Reset the cache to avoid reference problems
                log("Message sent to " + username + ": " + message.getType());
            } catch (IOException e) {
                log("Error sending message to client " + username + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Process login request
        private void handleLogin(Message message) {
            String requestedUsername = message.getContent();
            
            // Check if username is already taken
            if (userClientMap.containsKey(requestedUsername)) {
                sendMessage(new Message(Message.MessageType.LOGIN_FAILED, "SERVER", 
                        "Username already taken. Please choose another."));
            } else {
                // Username is available, register the client
                username = requestedUsername;
                userClientMap.put(username, this);
                loggedIn = true;
                
                sendMessage(new Message(Message.MessageType.LOGIN_SUCCESS, "SERVER", 
                        "Login successful. Welcome, " + username + "!"));
                
                log("User logged in: " + username);
                
                // Broadcast new user to all clients
                broadcastMessage(new Message(Message.MessageType.CHAT, "SERVER", 
                        username + " has joined the game!"), null);
            }
        }
        
        // Handle game request
        private void handleGameRequest(Message message) {
            if (!loggedIn) {
                sendMessage(new Message(Message.MessageType.CHAT, "SERVER", 
                        "You must login first!"));
                return;
            }
            
            // If player is already in a game, ignore the request
            if (playerPairings.containsKey(username)) {
                sendMessage(new Message(Message.MessageType.CHAT, "SERVER", 
                        "You are already in a game!"));
                return;
            }
            
            // If player is waiting, remove from waiting list (cancel request)
            if (waitingPlayers.containsValue(username)) {
                waitingPlayers.remove(username);
                sendMessage(new Message(Message.MessageType.CHAT, "SERVER", 
                        "Game request canceled."));
                return;
            }
            
            // Check if there are other players waiting
            if (!waitingPlayers.isEmpty()) {
                // Get first waiting player
                String opponent = waitingPlayers.keySet().iterator().next();
                waitingPlayers.remove(opponent);
                
                // Create game pairing
                playerPairings.put(username, opponent);
                playerPairings.put(opponent, username);
                
                // Create new game board
                GameBoard gameBoard = new GameBoard();
                gameBoards.put(username, gameBoard);
                gameBoards.put(opponent, gameBoard);
                
                // Notify both players
                ClientThread opponentClient = userClientMap.get(opponent);
                
                // First player to request is player 1, second player is player 2
                sendMessage(new Message(Message.MessageType.GAME_STARTED, "SERVER", 
                        "Game started against " + opponent + ". You are Player 1 (Red).", 1));
                opponentClient.sendMessage(new Message(Message.MessageType.GAME_STARTED, "SERVER", 
                        "Game started against " + username + ". You are Player 2 (Yellow).", 2));
                
                log("Player 1: " + username + ", Player 2: " + opponent);
                
                // Send initial game state
                sendGameState(username);
                sendGameState(opponent);
                
                log("Game started between " + username + " and " + opponent);
            } else {
                // No other players waiting, add to waiting list
                waitingPlayers.put(username, username);
                sendMessage(new Message(Message.MessageType.CHAT, "SERVER", 
                        "Waiting for an opponent..."));
                log(username + " is waiting for a game");
            }
        }
        
        // Handle game move
        private void handleGameMove(Message message) {
            log("Received game move from " + username);
            
            if (!loggedIn || !playerPairings.containsKey(username)) {
                log("Move rejected: User not logged in or not in a game");
                return;
            }
            
            String opponent = playerPairings.get(username);
            GameBoard gameBoard = gameBoards.get(username);
            
            log("Game state - Current player: " + gameBoard.getCurrentPlayer() + 
                ", Player: " + username + ", Opponent: " + opponent);
            
            // Extract move (column)
            Object moveData = message.getData();
            log("Move data type: " + (moveData != null ? moveData.getClass().getName() : "null"));
            log("Move data: " + moveData);
            
            Integer column = null;
            try {
                if (moveData instanceof Integer) {
                    column = (Integer) moveData;
                } else if (moveData instanceof String) {
                    column = Integer.parseInt((String) moveData);
                }
            } catch (Exception e) {
                log("Error parsing column: " + e.getMessage());
            }
            
            log("Parsed column: " + column);
            
            if (column == null || column < 0 || column >= GameBoard.getCols()) {
                log("Invalid column: " + column);
                return;
            }
            
            // Make the move
            boolean moveSuccess = gameBoard.makeMove(column);
            log("Move success: " + moveSuccess);
            
            if (moveSuccess) {
                // Get board state after the move
                log("Board state after move:");
                int[][] board = gameBoard.getBoard();
                for (int r = 0; r < GameBoard.getRows(); r++) {
                    StringBuilder rowStr = new StringBuilder();
                    for (int c = 0; c < GameBoard.getCols(); c++) {
                        rowStr.append(board[r][c]).append(" ");
                    }
                    log("Row " + r + ": " + rowStr.toString());
                }
                
                // Create a simple BoardState object instead of sending the whole GameBoard
                int[][] boardCopy = new int[GameBoard.getRows()][GameBoard.getCols()];
                int[][] originalBoard = gameBoard.getBoard();
                
                // Make a deep copy of the board
                for (int r = 0; r < GameBoard.getRows(); r++) {
                    for (int c = 0; c < GameBoard.getCols(); c++) {
                        boardCopy[r][c] = originalBoard[r][c];
                    }
                }
                
                // Create a BoardState object with the current game state
                BoardState boardState = new BoardState(
                    boardCopy,
                    gameBoard.getCurrentPlayer(),
                    gameBoard.getWinner(),
                    gameBoard.isGameOver()
                );
                
                // Get clients for both players
                ClientThread player1 = userClientMap.get(username);
                ClientThread player2 = userClientMap.get(opponent);
                
                if (player1 != null && player2 != null) {
                    // Send direct game state updates to both players
                    log("Sending game state to player 1: " + username);
                    
                    try {
                        Message stateMsg = new Message(Message.MessageType.GAME_STATE, "SERVER", 
                            "Game state updated", boardState);
                        player1.sendMessage(stateMsg);
                        player2.sendMessage(stateMsg);
                        
                        log("Game state sent to both players. Current player: " + boardState.getCurrentPlayer());
                    } catch (Exception e) {
                        log("Error sending game state: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    log("ERROR: One or both players not found in userClientMap!");
                    if (player1 == null) log("Player 1 (" + username + ") is null");
                    if (player2 == null) log("Player 2 (" + opponent + ") is null");
                }
                
                // Check if game is over
                if (gameBoard.isGameOver()) {
                    handleGameOver(username, opponent, gameBoard);
                }
            }
        }
        
        // Send game state to a player
        private void sendGameState(String player) {
            ClientThread playerClient = userClientMap.get(player);
            GameBoard gameBoard = gameBoards.get(player);
            
            if (playerClient != null && gameBoard != null) {
                try {
                    log("Sending game state to " + player + ": Current player=" + gameBoard.getCurrentPlayer());
                    
                    // Log the current board state
                    int[][] board = gameBoard.getBoard();
                    for (int r = 0; r < GameBoard.getRows(); r++) {
                        StringBuilder row = new StringBuilder();
                        for (int c = 0; c < GameBoard.getCols(); c++) {
                            row.append(board[r][c]).append(" ");
                        }
                        log("Board row " + r + ": " + row.toString());
                    }
                    
                    // Create a deep copy of the board data
                    int[][] boardCopy = new int[GameBoard.getRows()][GameBoard.getCols()];
                    for (int r = 0; r < GameBoard.getRows(); r++) {
                        for (int c = 0; c < GameBoard.getCols(); c++) {
                            boardCopy[r][c] = board[r][c];
                        }
                    }
                    
                    // Create a BoardState with the current state
                    BoardState boardState = new BoardState(
                        boardCopy,
                        gameBoard.getCurrentPlayer(),
                        gameBoard.getWinner(),
                        gameBoard.isGameOver()
                    );
                    
                    // Send the BoardState object
                    playerClient.sendMessage(new Message(Message.MessageType.GAME_STATE, "SERVER", 
                            "Game state updated", boardState));
                    
                    log("Game state sent successfully to " + player);
                } catch (Exception e) {
                    log("Error sending game state: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                log("Cannot send game state - playerClient or gameBoard is null");
            }
        }
        
        // Handle game over
        private void handleGameOver(String player1, String player2, GameBoard gameBoard) {
            ClientThread client1 = userClientMap.get(player1);
            ClientThread client2 = userClientMap.get(player2);
            
            String resultMessage;
            if (gameBoard.getWinner() == 1) {
                resultMessage = player1 + " wins!";
            } else if (gameBoard.getWinner() == 2) {
                resultMessage = player2 + " wins!";
            } else {
                resultMessage = "Game ended in a draw!";
            }
            
            // Send game over message to both players
            Message gameOverMsg = new Message(Message.MessageType.GAME_OVER, "SERVER", resultMessage, gameBoard.getWinner());
            client1.sendMessage(gameOverMsg);
            client2.sendMessage(gameOverMsg);
            
            log("Game over: " + resultMessage);
        }
        
        // Handle play again request
        private void handlePlayAgain(Message message) {
            if (!loggedIn || !playerPairings.containsKey(username)) {
                return;
            }
            
            String opponent = playerPairings.get(username);
            GameBoard gameBoard = gameBoards.get(username);
            
            // Reset the game board
            gameBoard.resetGame();
            
            // Notify both players
            ClientThread opponentClient = userClientMap.get(opponent);
            sendMessage(new Message(Message.MessageType.CHAT, "SERVER", "New game started!"));
            opponentClient.sendMessage(new Message(Message.MessageType.CHAT, "SERVER", "New game started!"));
            
            // Send initial game state
            sendGameState(username);
            sendGameState(opponent);
            
            log("New game started between " + username + " and " + opponent);
        }
        
        // Handle quit request
        private void handleQuit(Message message) {
            if (loggedIn && playerPairings.containsKey(username)) {
                String opponent = playerPairings.get(username);
                ClientThread opponentClient = userClientMap.get(opponent);
                
                // Notify opponent
                if (opponentClient != null) {
                    opponentClient.sendMessage(new Message(Message.MessageType.CHAT, "SERVER", 
                            username + " has left the game."));
                    
                    // Remove game pairings and boards
                    playerPairings.remove(opponent);
                    gameBoards.remove(opponent);
                }
                
                playerPairings.remove(username);
                gameBoards.remove(username);
                
                log(username + " has quit their game");
            }
        }
        
        // Handle chat message
        private void handleChat(Message message) {
            if (!loggedIn) {
                return;
            }
            
            // If player is in a game, send chat to both the player and their opponent
            if (playerPairings.containsKey(username)) {
                String opponent = playerPairings.get(username);
                ClientThread opponentClient = userClientMap.get(opponent);
                
                // Send to sender (self) too so they can see their own messages
                sendMessage(message);
                
                // Send to opponent
                if (opponentClient != null) {
                    opponentClient.sendMessage(message);
                }
            } else {
                // Otherwise, broadcast to all including self
                broadcastMessage(message, null);
            }
            
            log("Chat: " + message.toString());
        }

        @Override
        public void run() {
            try {
                connection.setTcpNoDelay(true);
                out = new ObjectOutputStream(connection.getOutputStream());
                in = new ObjectInputStream(connection.getInputStream());
                
                sendMessage(new Message(Message.MessageType.CHAT, "SERVER", 
                        "Welcome! Please log in with a unique username."));
                
                while (true) {
                    try {
                        Message message = (Message) in.readObject();
                        
                        switch (message.getType()) {
                            case LOGIN:
                                handleLogin(message);
                                break;
                            case CHAT:
                                handleChat(message);
                                break;
                            case GAME_REQUEST:
                                handleGameRequest(message);
                                break;
                            case GAME_MOVE:
                                handleGameMove(message);
                                break;
                            case PLAY_AGAIN:
                                handlePlayAgain(message);
                                break;
                            case QUIT:
                                handleQuit(message);
                                break;
                            default:
                                break;
                        }
                    } catch (ClassNotFoundException e) {
                        log("Unknown object received from client: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                // Handle client disconnection
                log("Client disconnected: " + username);
                
                if (loggedIn) {
                    // Clean up resources
                    userClientMap.remove(username);
                    
                    // If in a game, notify opponent
                    handleQuit(new Message(Message.MessageType.QUIT, username, "quit"));
                    
                    // If waiting for a game, remove from waiting list
                    waitingPlayers.remove(username);
                    
                    // Notify all clients
                    broadcastMessage(new Message(Message.MessageType.CHAT, "SERVER", 
                            username + " has disconnected."), null);
                }
                
                clients.remove(this);
            }
        }
    }
    
    // Broadcast message to all clients except the sender
    private void broadcastMessage(Message message, ClientThread exclude) {
        for (ClientThread client : clients) {
            if (client != exclude) {
                client.sendMessage(message);
            }
        }
    }
}