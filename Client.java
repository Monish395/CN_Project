import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        String serverAddress = "127.0.0.1";
        int port = 5000;

        try (Socket socket = new Socket(serverAddress, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to server.");
            System.out.println(in.readLine()); // Ask for name
            out.println(userInput.readLine()); // Send name

            // Start a thread to listen for server messages
            new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        System.out.println("\n" + response);
                        System.out.print("> ");
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            }).start();

            // Send user input to server
            String message;
            while (true) {
                System.out.print("> ");
                message = userInput.readLine();
                out.println(message);

                if ("exit".equalsIgnoreCase(message)) {
                    System.out.println("Disconnected from server.");
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
