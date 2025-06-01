import java.io.Serializable;

public class BoardState implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int[][] board; // The board grid: 0=empty, 1=player1, 2=player2
    private int currentPlayer; // Whose turn is it: 1 or 2
    private int winner; // 0=no winner yet, 1=player1, 2=player2, 3=draw
    private boolean gameOver;
    
    public BoardState(int[][] board, int currentPlayer, int winner, boolean gameOver) {
        this.board = board;
        this.currentPlayer = currentPlayer;
        this.winner = winner;
        this.gameOver = gameOver;
    }
    
    public int[][] getBoard() {
        return board;
    }
    
    public int getCurrentPlayer() {
        return currentPlayer;
    }
    
    public int getWinner() {
        return winner;
    }
    
    public boolean isGameOver() {
        return gameOver;
    }
}