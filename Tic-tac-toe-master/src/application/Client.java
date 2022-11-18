package application;

import application.controller.Controller;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Client extends Application {
    private static Socket socket;

    private static Scanner fromServer;
    private static PrintWriter toServer;
    public static int player; // 先手

    private static Controller controller;

    public static boolean connect() {
        try {
            socket = new Socket("127.0.0.1", Server.SBAP_PORT);
            System.out.println("Connect to server successfully, local port:" + socket.getLocalPort());

            InputStream instream = socket.getInputStream();
            OutputStream outstream = socket.getOutputStream();

            fromServer = new Scanner(instream);
            toServer = new PrintWriter(outstream, true);

            return true;

        } catch (Exception e) {
            System.out.println("Falied to connect to the server！");
            closeConnection(socket);
            return false;
        }
    }

    public static void closeConnection(Closeable... connections) {
        for (Closeable connect : connections) {
            if (connect != null) {
                try {
                    connect.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();

            fxmlLoader.setLocation(getClass().getClassLoader().getResource("mainUI.fxml"));
            Pane root = fxmlLoader.load();
            primaryStage.setTitle("Tic Tac Toe");
            primaryStage.setScene(new Scene(root));
            primaryStage.setResizable(false);

            controller = fxmlLoader.getController();
            controller.setUser(player);
            primaryStage.setOnCloseRequest(e -> {
                toServer.println("Close");
                System.exit(0);
            });
            primaryStage.show();
            new Thread(new Step()).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Step implements Runnable {
        public Step() {

        }

        @Override
        public void run() {
            String instruction;
            try {
                while (true) {
                    instruction = fromServer.nextLine();
                    switch (instruction) {
                        case "WIN":
                            System.out.println("You win!");
                            controller.myTurn = false;
                            return;
                        case "LOSE":
                            System.out.println("You lose!");
                            controller.myTurn = false;
                            return;
                        case "DRAW":
                            System.out.println("Draw!");
                            controller.myTurn = false;
                            return;
                        case "OPPONENT_EXIT":
                            System.out.println("Game abort! Opponent exited.");
                            System.exit(1);
                        case "GO":
                            System.out.println("You go!");
                            try {
//                            controller.go();
                                controller.myTurn = true;
                                while (true) {
                                    if (!controller.myTurn) {
                                        Platform.runLater(() -> controller.refreshBoard(controller.pos[0], controller.pos[1], player));
                                        toServer.println("MOVE");
                                        toServer.println(controller.pos[0] + " " + controller.pos[1]);
                                        System.out.println("You choosed (" + controller.pos[0] + ", " + controller.pos[1] + ").");
                                        break;
                                    }
                                    Thread.sleep(100);
                                }
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        case "OPPONENT":
                            System.out.println("Opponent go!");
                            String[] pos = fromServer.nextLine().split(" ");
                            int x = Integer.parseInt(pos[0]);
                            int y = Integer.parseInt(pos[1]);
                            controller.myTurn = false;
                            Platform.runLater(() -> controller.refreshBoard(x, y, -player));
                            break;
                    }

                }
            } catch (NoSuchElementException e) {
                System.out.println("Server disconnected, client exited.");
                System.exit(1);
            }
        }
    }

    public static void main(String[] args) {
        if (!connect()) {
            return;
        }
        String instruction;
        try {
            while (true) {
                instruction = fromServer.nextLine();
                if ("CHECK_ALIVE".equals(instruction)) {
                    toServer.println("ALIVE");
                } else if ("WAIT".equals(instruction)) {
                    System.out.println("Waiting for the opponent to connect...");
                } else {
                    System.out.println("The " + instruction + "_th game start!");
                    player = fromServer.nextInt();
                    System.out.println("player:" + player);
                    launch(args);
                    break;
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("Server disconnected, client exited.");
            System.exit(1);
        }
    }

}