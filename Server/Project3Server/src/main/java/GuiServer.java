import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class GuiServer extends Application {
    
    private Server server;
    private ListView<String> serverLog;
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        serverLog = new ListView<>();
        
        // Create server with logger that adds to ListView
        server = new Server(message -> {
            Platform.runLater(() -> {
                serverLog.getItems().add(message);
                // Auto-scroll to bottom
                serverLog.scrollTo(serverLog.getItems().size() - 1);
            });
        });
        
        // Set up the layout
        BorderPane root = new BorderPane();
        
        Text headerText = new Text("Connect Four Server Log");
        headerText.setFont(new Font(20));
        
        root.setPadding(new Insets(10));
        root.setTop(headerText);
        
        ScrollPane scrollPane = new ScrollPane(serverLog);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        root.setCenter(scrollPane);
        
        // Set up the scene
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Connect Four Server");
        primaryStage.show();
    }
}