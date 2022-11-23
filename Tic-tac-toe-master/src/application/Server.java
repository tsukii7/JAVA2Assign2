package application;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Server {

    public static final int SBAP_PORT = 8888;

    private static ServerSocket server;

    public static ArrayList<User> waitingUsers = new ArrayList<>();
    private static ArrayList<Thread> waitingThreads = new ArrayList<>();
    private static HashMap<String, User> userMap = new HashMap<>();
    private static int userCnt = 0;
    private static int gameCnt = 0;
    private static String filePath =
            "C:\\Users\\Ksco\\OneDrive\\文档\\CourseFile\\CS209_JAVA2\\lab"
                    + "\\Tic-tac-toe-master\\src\\application\\database";

    public static void main(String[] args) throws IOException {
        try {
            server = new ServerSocket(SBAP_PORT);
        } catch (IOException e) {
            System.out.println("Failed to initialize server: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Waiting for users to connect...");
        Socket socket;

        while (true) {
            socket = server.accept();
            userCnt++;
            System.out.println("\nUser " + userCnt + " connected.");
            System.out.println("User name:" + socket.getInetAddress().getHostName() + "\tUser ip:"
                    + socket.getInetAddress().getHostAddress());
            User user = verify(socket);
            if (user == null) {
                continue;
            }
            userMap.put(user.username, user);
            System.out.println("User login successfully!");
            if (!checkDisconnect(user)) {
                System.out.println("Add 1 waiting user.");
                waitingUsers.add(user);
                System.out.println(printWaitingUsers());
                notifyUpdate();
                System.out.println("Waiting for other users to connect...");
                waitingThreads.add(new Thread(new WaitingThread(
                        waitingUsers.get(waitingUsers.size() - 1))));
                waitingThreads.get(waitingThreads.size() - 1).start();
            }
        }
    }

    public static boolean checkDisconnect(User user) {
        try {
            File file = new File(filePath);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            String[] splited;
            while ((line = br.readLine()) != null) {
                splited = line.split(" ");
                if (user.username.equals(splited[0]) && "unfinished".equals(splited[5])) {
                    User opponent = userMap.get(splited[6]);

//                    String[] str = splited[8].split(",");
                    boolean isFirst = "yes".equals(splited[7]);
                    if (isFirst) {
                        new Thread(new Session(user, opponent, opponent, ++gameCnt, splited[8])).start();

                    } else {
                        new Thread(new Session(opponent, user, opponent, ++gameCnt, splited[8])).start();
                    }
                    br.close();
                    return true;
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void saveAccount(String username, String password) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath, true)));
            out.write(username + " " + password + " 0 0 0 finished null null 0\r\n");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean verifyAccount(String username, String password) {
        try {
            File file = new File(filePath);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            String[] splited;
            while ((line = br.readLine()) != null) {
                splited = line.split(" ");
                if (username.equals(splited[0]) && password.equals(splited[1])) {
                    br.close();
                    return true;
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static User verify(Socket socket) throws IOException {
        try {
            Scanner fromUser = new Scanner(socket.getInputStream());
            PrintWriter toUser = new PrintWriter(socket.getOutputStream(), true);
            toUser.println("VERIFY");
            while (true) {
                String instruction = fromUser.nextLine();
                if ("REGISTER".equals(instruction)) {
                    String account = fromUser.nextLine();
                    String password = fromUser.nextLine();
                    saveAccount(account, password);
                    toUser.println("SUCCESS");
                    return new User(account, userCnt, socket);
                } else if ("LOGIN".equals(instruction)) {
                    String account = fromUser.nextLine();
                    String password = fromUser.nextLine();
                    if (verifyAccount(account, password)) {
                        toUser.println("VERIFIED");
                        return new User(account, userCnt, socket);
                    } else {
                        toUser.println("UNVERIFIED");
                    }
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("User " + userCnt + " disconnected.");
        }
        return null;
    }


    public static String printWaitingUsers() {
        StringBuilder sb = new StringBuilder();
        sb.append("Waiting users list: [");
        for (int i = 0; i < waitingUsers.size(); i++) {
            sb.append("User " + waitingUsers.get(i).username + "(" + waitingUsers.get(i).id + ") ");
            if (i < waitingUsers.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("].");
        return sb.toString();
    }

    public static void notifyUpdate() {
        for (User waitingUser : waitingUsers) {
            try {
                new PrintWriter(waitingUser.socket.getOutputStream(), true).println(
                        "UPDATE");
                new PrintWriter(waitingUser.socket.getOutputStream(), true).println(
                        printWaitingUsers());
            } catch (IOException e) {
                System.out.println("Can not get output stream.");
            }
        }
    }


    public static class User {
        String username;
        int id;
        Socket socket;

        public User(String username, int id, Socket socket) {
            this.id = id;
            this.username = username;
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
                toUser.println("NAME");
                toUser.println(user.id);
                String msg = new Scanner(user.socket.getInputStream()).nextLine();
                if ("CHOSEN".equals(msg)) {
                    return;
                }
                int id = Integer.parseInt(msg);
                System.out.println(
                        "Get entered opponent ID: " + msg + " from User " + user.username + ".");
                User opponent = null;
                for (User waitingUser : waitingUsers) {
                    if (waitingUser.id == id) {
                        opponent = waitingUser;
                    }
                }
                gameCnt++;
                waitingUsers.remove(opponent);
                waitingUsers.remove(user);
                notifyUpdate();
                System.out.println(printWaitingUsers());
                if (opponent != null) {
                    new Thread(new Session(opponent, user, null, gameCnt, "")).start();
                } else {
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
