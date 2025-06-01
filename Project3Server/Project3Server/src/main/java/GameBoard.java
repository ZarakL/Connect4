import java.io.Serializable;

public class GameBoard implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Use transient for static fields to avoid serialization issues
    private static final transient int ROWS = 6;
    private static final transient int COLS = 7;
    
    // Make sure all fields are serializable
    private int[][] board;  // 0: empty, 1: player 1, 2: player 2
    private int currentPlayer;
    private boolean gameOver;
    private int winner;  // 0: no winner yet, 1: player 1, 2: player 2, 3: draw
    
    public GameBoard() {
        board = new int[ROWS][COLS];
        currentPlayer = 1;  // Player 1 goes first
        gameOver = false;
        winner = 0;
    }
    
    public boolean makeMove(int column) {
        if (column < 0 || column >= COLS || gameOver) {
            return false;
        }
        
        // Find the lowest empty row in the selected column
        // In Connect 4, row 0 is the top, and row 5 is the bottom
        // So we scan from the bottom (ROWS-1) to the top (0)
        // This represents the "dropping" of the token to the bottom
        int row = -1;
        for (int r = ROWS - 1; r >= 0; r--) {
            if (board[r][column] == 0) {
                row = r;
                break;
            }
        }
        
        // If column is full, move is invalid
        if (row == -1) {
            return false;
        }
        
        // Place the piece
        board[row][column] = currentPlayer;
        
        // Check for win
        if (checkWin(row, column)) {
            gameOver = true;
            winner = currentPlayer;
            return true;
        }
        
        // Check for draw
        if (isBoardFull()) {
            gameOver = true;
            winner = 3;  // Draw
            return true;
        }
        
        // Switch player
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        return true;
    }
    
    private boolean isBoardFull() {
        for (int c = 0; c < COLS; c++) {
            if (board[0][c] == 0) {
                return false;
            }
        }
        return true;
    }
    
    private boolean checkWin(int row, int col) {
        int player = board[row][col];
        
        // Check horizontal
        int count = 0;
        for (int c = 0; c < COLS; c++) {
            if (board[row][c] == player) {
                count++;
                if (count >= 4) return true;
            } else {
                count = 0;
            }
        }
        
        // Check vertical
        count = 0;
        for (int r = 0; r < ROWS; r++) {
            if (board[r][col] == player) {
                count++;
                if (count >= 4) return true;
            } else {
                count = 0;
            }
        }
        
        // Check diagonal (down-right)
        for (int r = 0; r < ROWS - 3; r++) {
            for (int c = 0; c < COLS - 3; c++) {
                if (board[r][c] == player && 
                    board[r+1][c+1] == player && 
                    board[r+2][c+2] == player && 
                    board[r+3][c+3] == player) {
                    return true;
                }
            }
        }
        
        // Check diagonal (up-right)
        for (int r = 3; r < ROWS; r++) {
            for (int c = 0; c < COLS - 3; c++) {
                if (board[r][c] == player && 
                    board[r-1][c+1] == player && 
                    board[r-2][c+2] == player && 
                    board[r-3][c+3] == player) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public int[][] getBoard() {
        return board;
    }
    
    public int getCurrentPlayer() {
        return currentPlayer;
    }
    
    public boolean isGameOver() {
        return gameOver;
    }
    
    public int getWinner() {
        return winner;
    }
    
    public void resetGame() {
        board = new int[ROWS][COLS];
        currentPlayer = 1;
        gameOver = false;
        winner = 0;
    }
    
    public static int getRows() {
        return ROWS;
    }
    
    public static int getCols() {
        return COLS;
    }
}