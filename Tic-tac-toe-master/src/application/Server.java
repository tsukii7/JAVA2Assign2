package application;

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

    public static ArrayList<User> waitingUsers = new ArrayList<>();
    private static ArrayList<Thread> waitingThreads = new ArrayList<>();
   private static int userCnt = 0;
   private static int gameCnt = 0;

    public static void main(String[] args) throws IOException {
        try {
            server = new ServerSocket(SBAP_PORT);
        } catch (IOException e) {
            System.out.println("Failed to initialize server: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Waiting for users to connect...");
        Socket  socket;

        while (true) {
            socket = server.accept();
            userCnt++;
            System.out.println("\nUser " + userCnt + " connected.");
            System.out.println("User name：" + socket.getInetAddress().getHostName() + "\tUser ip:"
                    + socket.getInetAddress().getHostAddress());
            System.out.println("Add 1 waiting user.");
            waitingUsers.add(new User(userCnt, socket));
            System.out.println(printWaitingUsers());
            notifyUpdate();
            System.out.println("Waiting for other users to connect...");
//            printWaitingUsers();
            waitingThreads.add(new Thread(new WaitingThread(waitingUsers.get(waitingUsers.size() - 1))));
            waitingThreads.get(waitingThreads.size() - 1).start();
        }
    }

    public static String printWaitingUsers() {
        StringBuilder sb = new StringBuilder();
        sb.append("Waiting users list: [");
        for (int i = 0; i < waitingUsers.size(); i++) {
            sb.append("User " + waitingUsers.get(i).id);
            if (i < waitingUsers.size() - 1)
                sb.append(", ");
        }
        sb.append("].");
        return sb.toString();
    }

    public static void notifyUpdate() {
        for (int i = 0; i < waitingUsers.size(); i++) {
            try {
                new PrintWriter(waitingUsers.get(i).socket.getOutputStream(), true).println("UPDATE");
                new PrintWriter(waitingUsers.get(i).socket.getOutputStream(), true).println(printWaitingUsers());
            } catch (IOException e) {
                System.out.println("Can not get output stream.");
            }
        }
    }


    private static class User {
        int id;
        Socket socket;

        public User(int id, Socket socket) {
            this.id = id;
            this.socket = socket;
        }
    }


    private static class WaitingThread implements Runnable {
        User user;
        PrintWriter toUser;

        public WaitingThread(User user) throws IOException {
            this.user = user;
            this.toUser = new PrintWriter(user.socket.getOutputStream(), true);
        }


        @Override
        public void run() {
            try {
                toUser.println("ID");
                toUser.println(user.id);
                String msg = new Scanner(user.socket.getInputStream()).nextLine();
                if ("CHOSEN".equals(msg)){
                    return;
                }
                int id = Integer.parseInt(msg);
                System.out.println("Get entered opponent ID: " + id+" from User "+user.id+".");
                User opponent = null;
                for (int i = 0; i < waitingUsers.size(); i++) {
                    if (waitingUsers.get(i).id == id)
                        opponent = waitingUsers.get(i);
                }
                gameCnt++;
                waitingUsers.remove(opponent);
                waitingUsers.remove(user);
                notifyUpdate();
                System.out.println(printWaitingUsers());
                if (opponent != null) {
                    new Thread(new Session(opponent.socket, user.socket, userCnt, gameCnt)).start();
                }else {
                    System.out.println("User entered an invalid opponent ID.");
                }
            } catch (NoSuchElementException e) {
                System.out.println("\nOne user disconnected.");
                waitingUsers.remove(user);
                System.out.println(printWaitingUsers());
                notifyUpdate();
            } catch (IOException e) {
                System.out.println("Can not get socket input stream.");
            }
        }
    }
}
