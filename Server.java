import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 5000;
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static String secretWord;
    private static char[] guessedWord;
    private static int remainingChances = 6;
    private static final Random random = new Random();
    private static final int currentPlayerIndex = random.nextInt(2);

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);

            while (clients.size() < 2) { // Accept only 2 players
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
            }

            // Start game when 2 players are connected
            clients.get(currentPlayerIndex).sendMessage("Enter the secret word:");
            secretWord = clients.get(currentPlayerIndex).receiveMessage();
            guessedWord = new char[secretWord.length()];
            Arrays.fill(guessedWord, '_');

            ClientHandler currentPlayer = clients.get((currentPlayerIndex+1)%2); //guesser
            ClientHandler anotherPlayer = clients.get(currentPlayerIndex); //chooser
            broadcast(anotherPlayer.getname()+" has entered the secret word. "+currentPlayer.getname() +" starts guessing.");
            currentPlayer.sendMessage("It's your turn to guess.");

            // Start the game loop
            while (remainingChances > 0 && new String(guessedWord).contains("_")) {
                currentPlayer.sendMessage("Current word: " + String.valueOf(guessedWord));
                currentPlayer.sendMessage("Enter a letter:");

                String input = currentPlayer.receiveMessage();
                if (input.length() != 1) {
                    currentPlayer.sendMessage("Invalid input! Enter a single letter.");
                    continue;
                }

                char guessedLetter = input.charAt(0);
                if (!updateWord(guessedLetter)) {
                    remainingChances--;
                    broadcast("Incorrect guess! Remaining chances: " + remainingChances);
                } else {
                    broadcast("Correct guess! Updated word: " + String.valueOf(guessedWord));
                }

                if (!new String(guessedWord).contains("_")) {
                    broadcast("Congratulations! " + currentPlayer.getname() + " won the game.");
                    break;
                }

//                currentPlayerIndex = (currentPlayerIndex + 1) % 2;
            }

            if (remainingChances == 0) {
                broadcast("Game over! The word was: " + secretWord);
            }

            for (ClientHandler client : clients) {
                client.closeConnection();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean updateWord(char letter) {
        boolean found = false;
        for (int i = 0; i < secretWord.length(); i++) {
            if (Character.toLowerCase(secretWord.charAt(i)) == Character.toLowerCase(letter)) {
                guessedWord[i] = secretWord.charAt(i);
                found = true;
            }
        }
        return found;
    }

    private static void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
}

class ClientHandler extends Thread {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String name;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Enter your name:");
            name = in.readLine();

            out.println("Welcome, " + name + "!");
        } catch (IOException e) {
            System.out.println("Client disconnected.");
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String receiveMessage() throws IOException {
        return in.readLine();
    }

    public String getname() {
        return name;
    }

    public void closeConnection() throws IOException {
        socket.close();
    }
}
