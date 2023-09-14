import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<ClientUser> clients = new CopyOnWriteArraySet<>();

    public static void main(String[] args) {
        System.out.println("Chat Server is running...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientUser{
        private PrintWriter printWriter;
        private String userName;

        public ClientUser(PrintWriter printWriter, String userName) {
            this.printWriter = printWriter;
            this.userName = userName;
        }
        public PrintWriter getPrintWriter() {
            return printWriter;
        }
        public String getUserName() {
            return userName;
        }
        
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Kullanıcıdan bir isim iste ve ekleyin
                out.println("Lütfen kullanici adinizi girin:");
                username = in.readLine();
                broadcast(username + " sohbete katildi.");
                clients.add(new ClientUser(out, username));

                // Çevrimiçi kullanıcı listesini gönder
                sendOnlineUsers();

                // Kullanıcıdan gelen mesajları alıp diğer kullanıcılara iletecek bir döngü oluşturun
                String message;
                while ((message = in.readLine()) != null) {
                    broadcast(username + ": " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (username != null) {
                    clients.forEach(obj->{
                        if(obj.getPrintWriter().equals(out)){
                            clients.remove(obj);
                        }
                    });
                    // clients.remove(out);
                    broadcast(username + " sohbetten ayrildi.");
                    sendOnlineUsers();
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void broadcast(String message) {
            for (ClientUser client : clients) {
                client.printWriter.println(message);
            }
        }

        private void sendOnlineUsers() {
            StringBuilder onlineUsers = new StringBuilder("Çevrimiçi Kullanicilar: ");
            for (ClientUser client : clients) {
                onlineUsers.append(client.getPrintWriter().equals(out) ? "[" + username + "] " : client.getUserName() + " ");
            }
            for (ClientUser client : clients) {
                client.getPrintWriter().println(onlineUsers.toString());
            }
        }
    }
}
