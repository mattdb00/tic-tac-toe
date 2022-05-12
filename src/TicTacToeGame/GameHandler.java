package TicTacToeGame;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * The GameHandler class (an observable) handles the sessions and game logic that is sent through the server.
 */
public class GameHandler implements Runnable {

    private Socket player1;
    private Socket player2;

    private ObjectInputStream fromPlayer1;
    private ObjectOutputStream toPlayer1;
    private ObjectInputStream fromPlayer2;
    private ObjectOutputStream toPlayer2;

    private int[][] board;
    GameBoard gameBoard;

    /**
     * Creates a new game handler with two player sockets and their output streams.
     * @param player1 The first player's socket.
     * @param toPlayer1 The first player's output stream.
     * @param player2 The second player's socket.
     * @param toPlayer2 The second player's output stream.
     * @throws IOException Occurs when not connected to the Internet.
     */
    public GameHandler(Socket player1, ObjectOutputStream toPlayer1, Socket player2, ObjectOutputStream toPlayer2) throws IOException {

        this.player1 = player1;
        this.toPlayer1 = toPlayer1;

        this.player2 = player2;
        this.toPlayer2 = toPlayer2;
    }

    @Override
    public void run() {

        try {
            fromPlayer1 = new ObjectInputStream(player1.getInputStream());
            fromPlayer2 = new ObjectInputStream(player2.getInputStream());

            toPlayer1.writeObject(new SessionData());   // Write something to player 1 to notify them that there are now two players.

            // Infinitely loop here, reading SessionData objects from both players and passing them to the other player.
            while(true) {

                SessionData dataToReceive = (SessionData) fromPlayer1.readObject();

                if(dataToReceive instanceof SessionData) {
                    System.out.println("Received data from player1, sending it to player 2...");

                    if(dataToReceive.isRunning()) {
                        dataToReceive = verifyMove(dataToReceive);
                        sendSessionData(toPlayer2, dataToReceive);
                    } else {
                        sendSessionData(toPlayer2, dataToReceive);
                    }
                    dataToReceive.debugSessionData();
                }

                SessionData dataToReceive2 = (SessionData) fromPlayer2.readObject();

                if(dataToReceive2 instanceof SessionData) {
                    System.out.println("Received data from player2, sending it to player 1...");

                    if(dataToReceive2.isRunning()) {
                        dataToReceive2 = verifyMove(dataToReceive2);
                        sendSessionData(toPlayer1, dataToReceive2);
                    } else {
                        sendSessionData(toPlayer1, dataToReceive2);
                    }
                    
                    dataToReceive2.debugSessionData();
                }

            }

        } catch (Exception e) {
            System.out.println("An error occured within the GameHandler!");
            e.printStackTrace();
        }
        
    }

    /**
     * Sends given session data to the given output stream.
     * @param destination
     * @param dataToSend
     * @throws IOException
     */
    private void sendSessionData(ObjectOutputStream destination, SessionData dataToSend) throws IOException {
        destination.writeObject(dataToSend);
        destination.flush();
    }

    /**
     * Given a SessionData object, determines if the sending player made a winning or stalemate move.
     * @param dataToVerify The SessionData object to verify.
     * @return The same SessionData object if no winning or stalemate move was made, otherwise a new SessionData object indicating winner.
     */
    private SessionData verifyMove(SessionData dataToVerify) {

        SessionData dataToReturn = dataToVerify;            // Assign returning object to the object in parameter.
        PlayerObject player = dataToVerify.getSender();     // Get player to check.

        // If a session data object is passed with -1 X and Y positions.
        if(dataToReturn.getXPos() == -1 && dataToReturn.getYPos() == -1) {
            return dataToReturn;
        }

        board = new int[3][3];
        int [][] tempBoard = dataToVerify.getSenderBoardState();

        for(int m = 0; m < 3; m++) {
            for(int a = 0; a < 3; a++) {
                board[m][a] = tempBoard[m][a];
            }
        }

        // Check for a vertical win (if X1, X2, X3 are the same)
        for(int col = 0; col < 3; col++) {
            if(board[0][col] == player.getID() && board[1][col] == player.getID() && board[2][col] == player.getID()) {
                dataToReturn.setWinner(player);
                return dataToReturn;
            }
        }

        // Check for horizontal win (if 1X, 2X, 3X are the same)
        for(int row = 0; row < 3; row++) {
            if(board[row][0] == player.getID() && board[row][1] == player.getID() && board[row][2] == player.getID()) {
                dataToReturn.setWinner(player);
                return dataToReturn;
            }
        }

        // Check for both diagonal orientations (if 11, 22, 33 OR 31, 22, 13 are the same)
        if(board[0][0] == player.getID() && board[1][1] == player.getID() && board[2][2] == player.getID()) {
            dataToReturn.setWinner(player);
            return dataToReturn;
        }

        if(board[0][2] == player.getID() && board[1][1] == player.getID() && board[2][0] == player.getID()) {
            dataToReturn.setWinner(player);
            return dataToReturn;
        }

        // Check if the entire board is filled. If so, return a stalemate.
        for(int row = 0; row < 3; row++) {
            for(int col = 0; col < 3; col++) {

                // If there is a zero at any position, then it is unfilled. Return!
                if(board[row][col] == 0) {
                    return dataToReturn;
                }
            }
        }
        
        dataToReturn.setStalemate();
        return dataToReturn;    // Return a stalemate otherwise.
    }
}
