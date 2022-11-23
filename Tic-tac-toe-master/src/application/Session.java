package application;

import application.Server.User;

import java.io.*;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Session implements Runnable {
    private final Scanner fromUser1;
    private final PrintWriter toUser1;
    private final Scanner fromUser2;
    private final PrintWriter toUser2;
    private int[][] chessBoard = new int[3][3];
    private String board;
    private int totalStep = 0;

    public int player1 = 1;
    public int player2 = -1;
    private User user1;
    private User user2;
    private User waitingUser;
    private final int gameId;
    private static String filePath =
            "C:\\Users\\Ksco\\OneDrive\\文档\\CourseFile\\CS209_JAVA2\\lab"
                    + "\\Tic-tac-toe-master\\src\\application\\database";
//    private static String user1Name;
//    private static String user2Name;

    public Session(User userFirst, User userSecond, User waitingUser, int gameId, String board) throws IOException {
        this.board = board;
        this.user1 = userFirst;
        this.user2 = userSecond;
        this.waitingUser = waitingUser;
//        user1Name = userFirst.username;
//        user2Name = userSecond.username;
//        Socket user1 = userFirst.socket;
//        Socket user2 = userSecond.socket;
        fromUser1 = new Scanner(user1.socket.getInputStream());
        toUser1 = new PrintWriter(user1.socket.getOutputStream(), true);
        fromUser2 = new Scanner(user2.socket.getInputStream());
        toUser2 = new PrintWriter(user2.socket.getOutputStream(), true);
//        this.userId = userId;
        this.gameId = gameId;
    }

    @Override
    public void run() {
        int player = player1;
        if (waitingUser == null) {
            toUser1.println(gameId);
            toUser2.println(gameId);
            toUser1.println(player1);
            toUser2.println(player2);
            System.out.println("\n******The " + gameId + "_th game of User " + user1.id
                    + " and User " + user2.id + " start!******");
        } else {
            if (waitingUser == user1) {
                toUser2.println("RECONNECT");
                toUser2.println(player2);
                toUser2.println(board);
                player = player2;
            } else {
                toUser1.println("RECONNECT");
                toUser1.println(player1);
                toUser1.println(board);
                player = player1;
            }
            String[] strs = board.split(",");
            int cnt = 0;
            for (int i = 0; i < chessBoard.length; i++) {
                for (int j = 0; j < chessBoard[i].length; j++) {
                    chessBoard[i][j] = Integer.parseInt(strs[cnt++]);
                }
            }
        }
        try {
            while (receive(player)) {
                player = -player;

            }
        } catch (NoSuchElementException e) {
//            (player == player1 ? toUser2 : toUser1).println("OPPONENT_EXIT");
            saveGameState(player);
            System.out.println("Game abort! User"
                    + (player == player1 ? user1.id : user2.id) + " disconnected.");
        }
    }

    public void saveGameState(int player) {
        try {
            File file = new File(filePath);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            String[] splited;
            StringBuilder sb = new StringBuilder();
            String name = (player == player1 ? user1.username : user2.username);
            while ((line = br.readLine()) != null) {
                splited = line.split(" ");
                if (name.equals(splited[0])) {
                    for (int i = 0; i < 5; i++) {
                        sb.append(splited[i] + " ");
                    }
                    sb.append("unfinished ");
                    sb.append((player == player1 ? user2.username : user1.username) + " ");
                    sb.append((player == player1 ? "yes" : "no") + " ");
                    StringBuilder board = new StringBuilder();
                    for (int i = 0; i < chessBoard.length; i++) {
                        for (int j = 0; j < chessBoard[i].length; j++) {
                            board.append(chessBoard[i][j]).append(",");
                        }
                    }
                    sb.append(board);
                    System.out.println("Saved game state!");
                } else {
                    sb.append(line);
                }
                sb.append("\n");
            }
            br.close();
            // write sb to file
            String str = sb.toString();
            FileWriter writer;
            writer = new FileWriter(filePath);
            writer.write("");
            writer.write(str);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
                        + (player == player1 ? user1.id : user2.id) + " exited.");
                saveGameState(player);
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
        System.out.println("User" + (player == player1 ? user1.id : user2.id)
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
            System.out.println("User" + user1.id + " win!");
        } else if (end < 0) {
            toUser2.println("WIN");
            toUser1.println("LOSE");
            System.out.println("User" + user2.id + " win!");
        } else if (totalStep == 9) {
            toUser1.println("DRAW");
            toUser2.println("DRAW");
            System.out.println("Draw!");
        } else {
            return false;
        }
        updateDatabase(end);
        return true;

    }

    public boolean updateDatabase(int result) {
        try {
            File file = new File(filePath);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            String[] splited;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                splited = line.split(" ");
                if (user1.username.equals(splited[0])) {
                    int win = Integer.parseInt(splited[2]);
                    int lose = Integer.parseInt(splited[3]);
                    int draw = Integer.parseInt(splited[4]);
                    if (result > 0) {
                        sb.append(splited[0] + " " + splited[1] + " "
                                + (win + 1) + " " + lose + " " + draw);
                    } else if (result < 0) {
                        sb.append(splited[0] + " " + splited[1] + " "
                                + win + " " + (lose + 1) + " " + draw);
                    } else {
                        sb.append(splited[0] + " " + splited[1] + " "
                                + win + " " + lose + " " + (draw + 1));
                    }
                } else if (user2.username.equals(splited[0])) {
                    int win = Integer.parseInt(splited[2]);
                    int lose = Integer.parseInt(splited[3]);
                    int draw = Integer.parseInt(splited[4]);
                    if (result < 0) {
                        sb.append(splited[0] + " " + splited[1] + " "
                                + (win + 1) + " " + lose + " " + draw);
                    } else if (result > 0) {
                        sb.append(splited[0] + " " + splited[1] + " "
                                + win + " " + (lose + 1) + " " + draw);
                    } else {
                        sb.append(splited[0] + " " + splited[1] + " "
                                + win + " " + lose + " " + (draw + 1));
                    }
                } else {
                    sb.append(line);
                }
                sb.append("\n");
            }
            br.close();
            // write sb to file
            String str = sb.toString();
            FileWriter writer;
            writer = new FileWriter(filePath);
            writer.write("");
            writer.write(str);
            writer.flush();
            writer.close();
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
