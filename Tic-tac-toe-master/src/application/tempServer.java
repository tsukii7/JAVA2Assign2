package application;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class tempServer {

    public final static int SBAP_PORT = 8888;

    private static ServerSocket server;
    private static ArrayList<User> waitingUsers = new ArrayList<User>();

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
        User user = null;
        while (true) {
            if (!hasUser1) {
                user1 = server.accept();
                System.out.println("User " + userCnt + " connected.");
                System.out.println("User name：" + user1.getInetAddress().getHostName() + "\tUser ip:"
                        + user1.getInetAddress().getHostAddress());
                waitingUsers.add(new User(++userCnt, user1));
                System.out.println("Waiting for another user to connect as a opponent...");
                System.out.println("Add 1 waiting user.");
                printWaitingUsers();
                new Thread(new CheckAlive(waitingUsers.get(waitingUsers.size() - 1))).start();
                new PrintWriter(user1.getOutputStream(), true).println("WAIT");
            }
            user2 = server.accept();
            waitingUsers.add(new User(++userCnt, user2));
            System.out.println("Add 1 waiting user.");
            printWaitingUsers();
            new Thread(new CheckAlive(waitingUsers.get(waitingUsers.size() - 1))).start();
            userCnt++;
            try {
                new PrintWriter(user1.getOutputStream(), true).println("CHECK_ALIVE");
                new PrintWriter(user2.getOutputStream(), true).println("CHECK_ALIVE");
//                if ("ALIVE".equals(new Scanner(user1.getInputStream()).nextLine())
//                        && "ALIVE".equals(new Scanner(user2.getInputStream()).nextLine())) {
                System.out.println("User " + userCnt + " connected.");
                System.out.println("User name：" + user2.getInetAddress().getHostName() + "\tUser ip:"
                        + user2.getInetAddress().getHostAddress());
                gameCnt++;
                new Thread(new Session(user1, user2, userCnt, gameCnt)).start();
                hasUser1 = false;
//                }
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

    public static void printWaitingUsers() {
        System.out.print("Waiting users list: [");
        for (int i = 0; i < waitingUsers.size(); i++) {
            System.out.print("User " + waitingUsers.get(i).id);
        }
        System.out.println("].");
    }


    private static class User {
        int id;
        Socket socket;

        public User(int id, Socket socket) {
            this.id = id;
            this.socket = socket;
        }
    }


    private static class CheckAlive implements Runnable {
        User user = null;

        public CheckAlive(User user) {
            this.user = user;
        }

        @Override
        public void run() {
            try {
                new Scanner(user.socket.getInputStream()).nextLine();
            } catch (NoSuchElementException e) {
                waitingUsers.remove(user);
                printWaitingUsers();
            } catch (IOException e) {
                System.out.println("Can not get socket input stream.");
            }

        }
    }
}
