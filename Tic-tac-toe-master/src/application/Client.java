package application;

import application.controller.Controller;
import java.io.*;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;


public class Client extends Application {
    private static Socket socket;

    private static Scanner fromServer;
    private static PrintWriter toServer;
    public static int player;

    private static Controller controller;
    private static int ID;

    public static boolean connect() {
        try {
            socket = new Socket("127.0.0.1", Server.SBAP_PORT);
            System.out.println("Connect to server successfully, local port:"
                    + socket.getLocalPort());

            InputStream inputstream = socket.getInputStream();
            OutputStream outputstream = socket.getOutputStream();

            fromServer = new Scanner(inputstream);
            toServer = new PrintWriter(outputstream, true);

            return true;

        } catch (Exception e) {
            System.out.println("Failed to connect to the server!\n Program exited.");
            closeConnection(socket);
            System.exit(1);
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
                        case "WIN" -> {
                            System.out.println("You win!");
                            controller.myTurn = false;
                            return;
                        }
                        case "LOSE" -> {
                            System.out.println("You lose!");
                            controller.myTurn = false;
                            return;
                        }
                        case "DRAW" -> {
                            System.out.println("Draw!");
                            controller.myTurn = false;
                            return;
                        }
                        case "OPPONENT_EXIT" -> {
                            System.out.println("Opponent exited. Game abort! ");
                            System.exit(1);
                        }
                        case "GO" -> {
                            System.out.println("You go!");
                            try {
                                controller.myTurn = true;
                                while (true) {
                                    if (!controller.myTurn) {
                                        Platform.runLater(() ->
                                                controller.refreshBoard(
                                                        controller.pos[0], controller.pos[1],
                                                        player));
                                        toServer.println("MOVE");
                                        toServer.println(
                                                controller.pos[0] + " " + controller.pos[1]);
                                        System.out.println("You chose ("
                                                + controller.pos[0] + ", "
                                                + controller.pos[1] + ").");
                                        break;
                                    }
                                    Thread.sleep(100);
                                }
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        case "OPPONENT" -> {
                            System.out.println("Opponent go!");
                            String[] pos = fromServer.nextLine().split(" ");
                            int x = Integer.parseInt(pos[0]);
                            int y = Integer.parseInt(pos[1]);
                            controller.myTurn = false;
                            Platform.runLater(() -> controller.refreshBoard(x, y, -player));
                        }
                        default -> System.out.println("Invalid instruction.");
                    }

                }
            } catch (NoSuchElementException e) {
                System.out.println("Server disconnected, client exited.");
                System.exit(1);
            }
        }
    }

    public static void update(String list) {
        System.out.println("\n\n" + list);
        System.out.println("Please choose your opponent...");
        System.out.print("Enter the opponent ID: ");

    }

    private static class ChooseOpponent implements Runnable {
        @Override
        public void run() {
            Scanner in = new Scanner(System.in);
            int id = in.nextInt();
            while (id == Client.ID) {
                System.out.println("You can not choose yourself, please enter again.");
                id = in.nextInt();
            }
            System.out.println("You chose User " + id + ".");
            toServer.println(id);
        }
    }

    public static void verify() {
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println("Please enter 1 to login or 2 to register:");
            String num = in.nextLine();
            if (num.length() > 1) {
                System.out.println("Invalid number");
                continue;
            }
            int ins = Integer.parseInt(num);
            while (true) {
                if (ins == 1) {
                    toServer.println("LOGIN");
                    System.out.println("[Log in]");
                    System.out.print("Please enter the account:");
                    toServer.println(in.nextLine());
                    System.out.print("Please enter the password:");
                    toServer.println(in.nextLine());
                    if ("VERIFIED".equals(fromServer.nextLine())) {
                        System.out.println("Login successfully!");
                        return;
                    } else {
                        System.out.println("Wrong account or password!");
                    }
                    break;
                } else if (ins == 2) {
                    toServer.println("REGISTER");
                    System.out.println("[Register]");
                    System.out.print("Please enter the account:");
                    toServer.println(in.nextLine());
                    System.out.print("Please enter the password:");
                    toServer.println(in.nextLine());
                    if ("SUCCESS".equals(fromServer.nextLine())) {
                        System.out.println("Register successfully! Automatically log in...");
                        return;
                    }
                } else {
                    System.out.println("Invalid input, please try again.");
                    ins = Integer.parseInt(in.nextLine());
                }
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
                if ("NAME".equals(instruction)) {
                    Client.ID = Integer.parseInt(fromServer.nextLine());
                    System.out.println("(Your ID: " + Client.ID + ")");
                    new Thread(new ChooseOpponent()).start();
                } else if ("UPDATE".equals(instruction)) {
                    update(fromServer.nextLine());
                } else if ("VERIFY".equals(instruction)) {
                    verify();
                } else {
                    System.out.println("\n******The " + instruction + "_th game start!******");
                    player = Integer.parseInt(fromServer.nextLine());
                    toServer.println("CHOSEN");
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