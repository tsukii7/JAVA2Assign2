package application;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Session implements Runnable {
    private final Scanner fromUser1;
    private final PrintWriter toUser1;
    private final Scanner fromUser2;
    private final PrintWriter toUser2;
    private final int[][] chessBoard = new int[3][3];
    private int totalStep = 0;

    public int player1 = 1;
    public int player2 = -1;
    private final int userId;
    private final int gameId;

    public Session(Socket user1, Socket user2, int userId, int gameId) throws IOException {
        fromUser1 = new Scanner(user1.getInputStream());
        toUser1 = new PrintWriter(user1.getOutputStream(), true);
        fromUser2 = new Scanner(user2.getInputStream());
        toUser2 = new PrintWriter(user2.getOutputStream(), true);
        this.userId = userId;
        this.gameId = gameId;
    }

    @Override
    public void run() {
        toUser1.println(gameId);
        toUser2.println(gameId);
        toUser1.println(player1);
        toUser2.println(player2);
        System.out.println("\n******The " + gameId + "_th game of User " + (userId - 1)
                + " and User " + userId + " start!******");
        int player = player1;
        try {
            while (receive(player)) {
                player = -player;
            }
        } catch (NoSuchElementException e) {
            (player == player1 ? toUser2 : toUser1).println("OPPONENT_EXIT");
            System.out.println("Game abort! User"
                    + (player == player1 ? userId - 1 : userId) + " disconnected.");
        }
    }

    public boolean receive(int player) {
        String instruction;
        Scanner fromUser = (player == player1 ? fromUser1 : fromUser2);
        PrintWriter toUser = player == player1 ? toUser1 : toUser2;
        PrintWriter toOpponent = player == player1 ? toUser2 : toUser1;
        toUser.println("GO");
        instruction = fromUser.nextLine();
        if ("CHOSEN".equals(instruction)) {
            instruction = fromUser.nextLine();
        }
        switch (instruction) {
            case "MOVE" -> {
                String pos = fromUser.nextLine();
                toOpponent.println("OPPONENT");
                toOpponent.println(pos);
                if (step(pos, player)) {
                    return false;
                }
            }
            case "Close" -> {
                System.out.println("Game abort! User"
                        + (player == player1 ? userId - 1 : userId) + " exited.");
                return false;
            }
            default -> System.out.println("Invalid instruction.");
        }
        return true;
    }

    public boolean step(String pos, int player) {
        int x = Integer.parseInt(pos.charAt(0) + "");
        int y = Integer.parseInt(pos.charAt(2) + "");
        chessBoard[x][y] = player;
        System.out.println("User" + (player == player1 ? userId - 1 : userId)
                + " chose (" + x + ", " + y + ")");
        ++totalStep;
        return checkResult();
    }

    public boolean checkResult() {
        int end = 0;
        for (int i = 0; i < chessBoard.length; i++) {
            int line = chessBoard[i][0] + chessBoard[i][1] + chessBoard[i][2];
            int col = chessBoard[0][i] + chessBoard[1][i] + chessBoard[2][i];
            if (line == 3 || col == 3) {
                end = 1;
            } else if (line == -3 || col == -3) {
                end = -1;
            }
        }
        int cross = chessBoard[0][0] + chessBoard[1][1] + chessBoard[2][2];
        if (cross == 3) {
            end = 1;
        } else if (cross == -3) {
            end = -1;
        }
        cross = chessBoard[2][0] + chessBoard[1][1] + chessBoard[0][2];
        if (cross == 3) {
            end = 1;
        } else if (cross == -3) {
            end = -1;
        }
        if (end > 0) {
            toUser1.println("WIN");
            toUser2.println("LOSE");
            System.out.println("User" + (userId - 1) + " win!");
        } else if (end < 0) {
            toUser2.println("WIN");
            toUser1.println("LOSE");
            System.out.println("User" + userId + " win!");
        } else if (totalStep == 9) {
            toUser1.println("DRAW");
            toUser2.println("DRAW");
            System.out.println("Draw!");
        } else {
            return false;
        }
        return true;

    }
}
