import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ClientMain extends Application {
    
    private NetworkClient client; // Network client that handles communication with server
    private Stage primaryStage;
    
    // Home screen components
    private Scene homeScene;
    
    // Login scene components
    private Scene loginScene;
    private TextField usernameField;
    
    // Main game scene components
    private Scene gameScene;
    private BorderPane gameRoot;
    private Text statusText;
    private GridPane boardGrid;
    private ListView<String> chatLog;
    private TextField chatField;
    private Button findGameBtn;
    private Button quitGameBtn;
    private Button playAgainBtn;
    
    // Game board display - represents BoardView in class diagram
    private Circle[][] boardCircles;
    
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        // Initialize client with message handler
        client = new NetworkClient(message -> Platform.runLater(() -> handleMessage(message)));
        
        // Create all scenes
        createHomeScene();
        createLoginScene();
        createGameScene();
        
        // Start with home scene
        primaryStage.setScene(homeScene);
        primaryStage.setTitle("Connect Four");
        primaryStage.show();
        
        // Start client thread
        client.start();
    }
    
    private void createHomeScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #336699;");
        
        VBox homeBox = new VBox(20);
        homeBox.setAlignment(Pos.CENTER);
        
        Text titleText = new Text("CONNECT FOUR");
        titleText.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleText.setFill(Color.WHITE);
        
        Button playButton = new Button("PLAY");
        playButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        playButton.setPrefWidth(200);
        playButton.setPrefHeight(50);
        playButton.setOnAction(e -> primaryStage.setScene(loginScene));
        
        Button howToPlayButton = new Button("HOW TO PLAY");
        howToPlayButton.setFont(Font.font("Arial", 16));
        howToPlayButton.setPrefWidth(200);
        howToPlayButton.setPrefHeight(40);
        howToPlayButton.setOnAction(e -> showInstructions());
        
        Button quitButton = new Button("QUIT");
        quitButton.setFont(Font.font("Arial", 16));
        quitButton.setPrefWidth(200);
        quitButton.setPrefHeight(40);
        quitButton.setOnAction(e -> {
            Alert confirmQuit = new Alert(Alert.AlertType.CONFIRMATION);
            confirmQuit.setTitle("Quit Application");
            confirmQuit.setHeaderText("Are you sure you want to quit?");
            confirmQuit.setContentText("This will close the Connect Four application.");
            
            confirmQuit.showAndWait().ifPresent(response -> {
                if (response == javafx.scene.control.ButtonType.OK) {
                    Platform.exit();
                }
            });
        });
        
        homeBox.getChildren().addAll(titleText, playButton, howToPlayButton, quitButton);
        root.setCenter(homeBox);
        
        homeScene = new Scene(root, 500, 400);
    }
    
    private void showInstructions() {
        Stage instructionsStage = new Stage();
        instructionsStage.initModality(Modality.APPLICATION_MODAL);
        instructionsStage.initOwner(primaryStage);
        instructionsStage.setTitle("How to Play Connect Four");
        
        BorderPane instructionsPane = new BorderPane();
        instructionsPane.setPadding(new Insets(20));
        
        TextArea instructionsText = new TextArea(
            "Connect Four Rules:\n\n" +
            "1. Connect Four is a two-player board game.\n\n" +
            "2. Players take turns dropping their colored discs into a vertical grid.\n\n" +
            "3. The discs fall to the lowest available space in the column.\n\n" +
            "4. The first player to form a horizontal, vertical, or diagonal line of four of their discs wins.\n\n" +
            "5. If the grid fills up without a winner, the game ends in a draw.\n\n" +
            "How to Play in this Application:\n\n" +
            "1. Enter your username on the login screen.\n\n" +
            "2. Click 'Find Game' to be matched with another player.\n\n" +
            "3. Click on a column to drop your disc there when it's your turn.\n\n" +
            "4. Use the chat panel to communicate with your opponent.\n\n" +
            "5. After a game ends, you can choose to play again or quit."
        );
        
        instructionsText.setEditable(false);
        instructionsText.setWrapText(true);
        instructionsText.setPrefHeight(400);
        instructionsText.setPrefWidth(500);
        
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> instructionsStage.close());
        
        VBox layout = new VBox(20);
        layout.getChildren().addAll(instructionsText, closeButton);
        layout.setAlignment(Pos.CENTER);
        
        instructionsPane.setCenter(layout);
        
        Scene instructionsScene = new Scene(instructionsPane, 550, 500);
        instructionsStage.setScene(instructionsScene);
        instructionsStage.showAndWait();
    }
    
    private void createLoginScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #336699;");
        
        VBox loginBox = new VBox(15);
        loginBox.setAlignment(Pos.CENTER);
        
        Text titleText = new Text("Enter Your Name");
        titleText.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        titleText.setFill(Color.WHITE);
        
        Text instructionText = new Text("Please enter a unique username to begin");
        instructionText.setFont(Font.font("Arial", 16));
        instructionText.setFill(Color.WHITE);
        
        usernameField = new TextField();
        usernameField.setPromptText("Your Name");
        usernameField.setMaxWidth(250);
        usernameField.setPrefHeight(40);
        usernameField.setFont(Font.font("Arial", 14));
        
        Button loginBtn = new Button("START GAME");
        loginBtn.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        loginBtn.setPrefWidth(200);
        loginBtn.setPrefHeight(40);
        loginBtn.setOnAction(e -> {
            if (!usernameField.getText().trim().isEmpty()) {
                client.login(usernameField.getText().trim());
            }
        });
        
        Button backBtn = new Button("BACK");
        backBtn.setFont(Font.font("Arial", 14));
        backBtn.setPrefWidth(120);
        backBtn.setPrefHeight(30);
        backBtn.setOnAction(e -> primaryStage.setScene(homeScene));
        
        loginBox.getChildren().addAll(titleText, instructionText, usernameField, loginBtn, backBtn);
        root.setCenter(loginBox);
        
        loginScene = new Scene(root, 500, 400);
    }
    
    private void createGameScene() {
        gameRoot = new BorderPane();
        gameRoot.setPadding(new Insets(20));
        gameRoot.setStyle("-fx-background-color: #2A3950;");
        
        // Top status area
        HBox topBox = new HBox();
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(0, 0, 15, 0));
        
        statusText = new Text("Welcome to Connect Four");
        statusText.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        statusText.setFill(Color.WHITE);
        topBox.getChildren().add(statusText);
        gameRoot.setTop(topBox);
        
        // Center game board
        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.CENTER);
        
        boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);
        boardGrid.setHgap(8);
        boardGrid.setVgap(8);
        boardGrid.setPadding(new Insets(15));
        
        // Initialize board circles
        boardCircles = new Circle[GameBoard.getRows()][GameBoard.getCols()];
        
        // Create the visual board
        for (int r = 0; r < GameBoard.getRows(); r++) {
            for (int c = 0; c < GameBoard.getCols(); c++) {
                Circle circle = new Circle(28); // Larger circle for visibility
                circle.setFill(Color.WHITE);
                circle.setStroke(Color.BLACK);
                circle.setStrokeWidth(2); // Make the border more visible
                boardCircles[r][c] = circle;
                
                // In Connect4, row 0 is the top row and row 5 is the bottom row
                // We want to display correctly with pieces dropping from top to bottom
                boardGrid.add(circle, c, r);
                
                // Add a mouse click handler for each circle - another way to make a move
                final int column = c;
                circle.setOnMouseClicked(e -> {
                    System.out.println("=== CLICK EVENT ===");
                    System.out.println("Circle clicked in column " + column);
                    System.out.println("Player number: " + client.getPlayerNumber());
                    if (client.getGameBoard() != null) {
                        System.out.println("Current player: " + client.getGameBoard().getCurrentPlayer());
                        System.out.println("Is it your turn: " + (client.getGameBoard().getCurrentPlayer() == client.getPlayerNumber()));
                    } else {
                        System.out.println("Game board is null");
                    }
                    client.makeMove(column);
                });
            }
        }
        
        // Make the grid more visually appealing with a gradient blue background
        boardGrid.setStyle("-fx-background-color: linear-gradient(to bottom, #3366A3, #1E4D8C); " +
                         "-fx-padding: 15; -fx-hgap: 10; -fx-vgap: 10; " +
                         "-fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 10, 0, 0, 5);");
        
        centerBox.getChildren().add(boardGrid);
        gameRoot.setCenter(centerBox);
        
        // Right chat area
        VBox chatBox = new VBox(10);
        chatBox.setPadding(new Insets(5, 5, 5, 15));
        chatBox.setStyle("-fx-background-color: #1C2C44; -fx-background-radius: 10;");
        
        Label chatLabel = new Label("CHAT");
        chatLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        chatLabel.setTextFill(Color.WHITE);
        
        chatLog = new ListView<>();
        chatLog.setPrefHeight(300);
        chatLog.setPrefWidth(220);
        chatLog.setStyle("-fx-background-color: #F0F0F0; -fx-background-radius: 5;");
        
        chatField = new TextField();
        chatField.setPromptText("Type a message...");
        chatField.setPrefHeight(30);
        chatField.setOnAction(e -> {
            if (!chatField.getText().trim().isEmpty()) {
                client.sendChatMessage(chatField.getText().trim());
                chatField.clear();
            }
        });
        
        chatBox.getChildren().addAll(chatLabel, chatLog, chatField);
        gameRoot.setRight(chatBox);
        
        // Bottom buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));
        
        findGameBtn = new Button("FIND GAME");
        findGameBtn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        findGameBtn.setPrefWidth(140);
        findGameBtn.setPrefHeight(35);
        findGameBtn.setOnAction(e -> client.requestGame());
        
        playAgainBtn = new Button("PLAY AGAIN");
        playAgainBtn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        playAgainBtn.setPrefWidth(140);
        playAgainBtn.setPrefHeight(35);
        playAgainBtn.setOnAction(e -> {
            client.playAgain();
            playAgainBtn.setDisable(true); // Disable button immediately after clicking
        });
        playAgainBtn.setDisable(true);
        
        quitGameBtn = new Button("QUIT GAME");
        quitGameBtn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        quitGameBtn.setPrefWidth(140);
        quitGameBtn.setPrefHeight(35);
        quitGameBtn.setOnAction(e -> {
            // Show confirmation dialog before quitting
            Alert confirmQuit = new Alert(Alert.AlertType.CONFIRMATION);
            confirmQuit.setTitle("Quit Game");
            confirmQuit.setHeaderText("Are you sure you want to quit?");
            confirmQuit.setContentText("This will close the application and end your current game.");
            
            confirmQuit.showAndWait().ifPresent(response -> {
                if (response == javafx.scene.control.ButtonType.OK) {
                    // First quit the game
                    client.quitGame();
                    // Then close the application
                    Platform.exit();
                }
            });
        });
        quitGameBtn.setDisable(true);
        
        Button homeBtn = new Button("HOME");
        homeBtn.setFont(Font.font("Arial", 14));
        homeBtn.setPrefWidth(120);
        homeBtn.setPrefHeight(35);
        homeBtn.setOnAction(e -> {
            // If a game is in progress (quitGameBtn is enabled), show confirmation
            if (!quitGameBtn.isDisabled()) {
                Alert confirmHome = new Alert(Alert.AlertType.CONFIRMATION);
                confirmHome.setTitle("Return to Home");
                confirmHome.setHeaderText("Are you sure you want to return to the home screen?");
                confirmHome.setContentText("This will end your current game.");
                
                confirmHome.showAndWait().ifPresent(response -> {
                    if (response == javafx.scene.control.ButtonType.OK) {
                        // Quit the current game before going back to home
                        client.quitGame();
                        primaryStage.setScene(homeScene);
                    }
                });
            } else {
                // No game in progress, just go home
                primaryStage.setScene(homeScene);
            }
        });
        
        buttonBox.getChildren().addAll(findGameBtn, playAgainBtn, quitGameBtn, homeBtn);
        gameRoot.setBottom(buttonBox);
        
        gameScene = new Scene(gameRoot, 700, 600);
    }
    
    // Handle incoming messages from server - acts as MessageHandler
    private void handleMessage(Message message) {
        System.out.println("Handling message of type: " + message.getType());
        
        switch (message.getType()) {
            case LOGIN_SUCCESS:
                // Switch to game scene
                primaryStage.setScene(gameScene);
                statusText.setText("Logged in as: " + client.getUsername());
                break;
                
            case LOGIN_FAILED:
                // Show error dialog
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Login Error");
                alert.setHeaderText("Login Failed");
                alert.setContentText(message.getContent());
                alert.showAndWait();
                break;
                
            case CHAT:
                // Add message to chat log
                chatLog.getItems().add(message.toString());
                // Auto-scroll to bottom
                chatLog.scrollTo(chatLog.getItems().size() - 1);
                break;
                
            case GAME_STARTED:
                // Update UI for game start
                System.out.println("Game started message received. Opponent: " + client.getOpponent() + 
                                  ", Player Number: " + client.getPlayerNumber());
                
                findGameBtn.setDisable(true);
                quitGameBtn.setDisable(false);
                playAgainBtn.setDisable(true);
                
                // Set color and turn indicator based on player number
                if (client.getPlayerNumber() == 1) {
                    statusText.setText("YOUR TURN - You go first (RED)");
                    statusText.setFill(Color.RED);
                    System.out.println("You are player 1 (RED) - Your turn first");
                } else {
                    statusText.setText("Waiting for opponent's move (You are YELLOW)");
                    statusText.setFill(Color.YELLOW);
                    System.out.println("You are player 2 (YELLOW) - Opponent goes first");
                }
                break;
                
            case GAME_STATE:
                System.out.println("Received GAME_STATE message in GUI");
                
                try {
                    // Update board display
                    if (client.getGameBoard() != null) {
                        System.out.println("Client has valid gameboard, updating display");
                        updateBoardDisplay();
                        
                        // Update status text with current turn
                        if (!client.getGameBoard().isGameOver()) {
                            if (client.getGameBoard().getCurrentPlayer() == client.getPlayerNumber()) {
                                statusText.setText("YOUR TURN - Click a column to place your piece");
                                statusText.setFill(client.getPlayerNumber() == 1 ? Color.RED : Color.YELLOW);
                            } else {
                                statusText.setText("Waiting for opponent's move...");
                                statusText.setFill(Color.WHITE);
                            }
                            // Reset play again button when game is in progress
                            playAgainBtn.setDisable(true);
                        }
                    } else {
                        System.out.println("ERROR: Game board is null after GAME_STATE message");
                    }
                } catch (Exception e) {
                    System.out.println("Error updating board display: " + e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case GAME_OVER:
                // Update board one last time
                updateBoardDisplay();
                
                // Update status text with game result
                int winner = (Integer) message.getData();
                if (winner == client.getPlayerNumber()) {
                    statusText.setText("YOU WON! 🎉");
                    statusText.setFill(client.getPlayerNumber() == 1 ? Color.RED : Color.YELLOW);
                } else if (winner == 3) {
                    statusText.setText("GAME ENDED IN A DRAW!");
                    statusText.setFill(Color.WHITE);
                } else {
                    statusText.setText("You lost. Better luck next time!");
                    statusText.setFill(Color.GRAY);
                }
                
                // Enable/disable buttons
                playAgainBtn.setDisable(false);
                break;
                
            default:
                break;
        }
    }
    
    // Update the visual board based on game state - acts as BoardView
    private void updateBoardDisplay() {
        System.out.println("updateBoardDisplay called");
        
        if (client.getGameBoard() == null) {
            System.out.println("Game board is null, cannot update display");
            return;
        }
        
        int[][] board = client.getGameBoard().getBoard();
        System.out.println("Board state:");
        
        for (int r = 0; r < GameBoard.getRows(); r++) {
            StringBuilder rowStr = new StringBuilder();
            for (int c = 0; c < GameBoard.getCols(); c++) {
                rowStr.append(board[r][c]).append(" ");
            }
            System.out.println(rowStr.toString());
        }
        
        // Update the visual representation
        Platform.runLater(() -> {
            try {
                // First clear the board
                for (int r = 0; r < GameBoard.getRows(); r++) {
                    for (int c = 0; c < GameBoard.getCols(); c++) {
                        boardCircles[r][c].setFill(Color.WHITE);
                    }
                }
                
                // Then update with pieces
                for (int r = 0; r < GameBoard.getRows(); r++) {
                    for (int c = 0; c < GameBoard.getCols(); c++) {
                        // The board array and boardCircles array have the same orientation
                        // (row 0 is top row, row 5 is bottom row)
                        Circle circle = boardCircles[r][c];
                        
                        if (board[r][c] == 1) {
                            // Always show player 1's pieces as RED
                            System.out.println("Setting circle at [" + r + "][" + c + "] to RED (Player 1)");
                            circle.setFill(Color.RED);
                        } else if (board[r][c] == 2) {
                            // Always show player 2's pieces as YELLOW
                            System.out.println("Setting circle at [" + r + "][" + c + "] to YELLOW (Player 2)");
                            circle.setFill(Color.YELLOW);
                        }
                    }
                }
                
                // Turn indicator is now handled in the GAME_STATE message handler
                // so we don't need to update it here to avoid conflicts
                
                System.out.println("Board display updated successfully");
            } catch (Exception e) {
                System.out.println("Error updating board UI: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}