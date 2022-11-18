package application;

import javafx.application.Platform;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Server {

    public final static int SBAP_PORT = 8888;

    private static ServerSocket server;

    public static void main(String[] args) throws IOException {
        try {
            server = new ServerSocket(SBAP_PORT);
        } catch (IOException e) {
            System.out.println("Failed to initialize server: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Waiting for users to connect...");
        boolean hasUser1 = false;
        Socket user1 = null, user2;
        int userCnt = 0;
        int gameCnt = 0;
        while (true) {
            if (!hasUser1) {
                user1 = server.accept();
//                new Scanner(user1.getInputStream()).nextLine();
                userCnt++;
                System.out.println("User " + userCnt + " connected.");
                System.out.println("User name：" + user1.getInetAddress().getHostName() + "\tUser ip:"
                        + user1.getInetAddress().getHostAddress());
                System.out.println("Waiting for another user to connect as a opponent...");
                new PrintWriter(user1.getOutputStream(), true).println("WAIT");
            }
            user2 = server.accept();
//            new Scanner(user2.getInputStream()).nextLine();
            userCnt++;
            try {
                new PrintWriter(user1.getOutputStream(), true).println("CHECK_ALIVE");
                new PrintWriter(user2.getOutputStream(), true).println("CHECK_ALIVE");
                if ("ALIVE".equals(new Scanner(user1.getInputStream()).nextLine())
                        && "ALIVE".equals(new Scanner(user2.getInputStream()).nextLine())) {
                    System.out.println("User " + userCnt + " connected.");
                    System.out.println("User name：" + user2.getInetAddress().getHostName() + "\tUser ip:"
                            + user2.getInetAddress().getHostAddress());
                    gameCnt++;
                    new Thread(new Session(user1, user2, userCnt, gameCnt)).start();
                    hasUser1 = false;
                }
            } catch (NoSuchElementException e) {
                System.out.println("User " + (userCnt - 1) + " disconnected.");
                user1 = user2;
                hasUser1 = true;
                System.out.println("User " + userCnt + " connected.");
                System.out.println("User name：" + user2.getInetAddress().getHostName() + "\tUser ip:"
                        + user2.getInetAddress().getHostAddress());
                System.out.println("Waiting for another user to connect as a opponent...");
                new PrintWriter(user1.getOutputStream(), true).println("WAIT");
            }


        }
    }


}
