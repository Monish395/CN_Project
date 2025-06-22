import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 5000; //port number
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>()); //list to handle multiple clients
    private static String secretWord;
    private static char[] guessedWord;
    private static int remainingChances = 6;
    private static final Random random = new Random();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT, 10, InetAddress.getByName("0.0.0.0"))) { //server socket
            System.out.println("Server is running on port " + PORT);

            //waiting for clients to join
            while (clients.size() < 2) { // Accept only 2 players
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
            }

            // Wait until both players have entered their names
            while (clients.stream().anyMatch(c -> !c.isNameSet())) {
                try {
                    Thread.sleep(100); // Polling wait
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            String playagain;
            do {
                remainingChances = 6;
                //choosing chooser by random
                int ChooserIndex = random.nextInt(2);
                ClientHandler Guesser = clients.get((ChooserIndex + 1) % 2);
                ClientHandler Chooser = clients.get(ChooserIndex);

                //get secret word and clue from chooser
                Chooser.sendMessage("Enter the secret word:");
                secretWord = getGameInput(Chooser, Guesser);
                Chooser.sendMessage("Enter Clue for the word:");
                String wordClue = getGameInput(Chooser, Guesser);

                //guessed word - blanks initially
                guessedWord = new char[secretWord.length()];
                Arrays.fill(guessedWord, '_');

                broadcast(Chooser.getname() + " has entered the secret word. " + Guesser.getname() + " starts guessing.");
                Guesser.sendMessage("It's your turn to guess.");

                // Game loop
                ArrayList<Character> guessedLetters = new ArrayList<>(); //list of guessed letters
                while (remainingChances > 0 && new String(guessedWord).contains("_")) {
                    broadcast("Clue: " + wordClue);
                    Guesser.sendMessage("Current word: " + String.valueOf(guessedWord));
                    Chooser.sendMessage("Word: " + secretWord); // chooser sees actual word

                    Chooser.sendMessage(Guesser.getname() + " is guessing...");
                    Guesser.sendMessage("Enter a letter:");

                    // Poll for input from all clients; only return when current player's non-chat input is received.
                    String input = getGameInput(Guesser, null);

                    if (input.length() != 1) {
                        Guesser.sendMessage("Invalid guess! Enter a single letter.");
                        continue;
                    }
                    char guessedLetter = input.charAt(0);
                    Chooser.sendMessage("Guessed letter: " + guessedLetter);
                    Guesser.sendMessage("You guessed: " + guessedLetter);

                    if (guessedLetters.contains(guessedLetter)) {
                        broadcast("The letter " + guessedLetter + " has already been guessed");
                        continue;
                    } else {
                        guessedLetters.add(guessedLetter);
                        if (!updateWord(guessedLetter)) {
                            remainingChances--;
                            broadcast("Incorrect guess! Guessed Letter: " + guessedLetter + "  Remaining chances: " + remainingChances);
                        } else {
                            broadcast("Correct guess! Guessed Letter: " + guessedLetter + " Updated word: " + String.valueOf(guessedWord));
                        }
                    }
                    //Word has been guessed, Round ends
                    if (!new String(guessedWord).contains("_")) {
                        Guesser.sendMessage("Current word: " + String.valueOf(guessedWord));
                        Chooser.sendMessage("Word: " + secretWord);
                        Guesser.sendMessage("Congratulations! You won the game!!!");
                        Chooser.sendMessage("You lose, " + Guesser.getname() + " won the game");
                        break;
                    }
                }
                //Chances over, Round ends
                if (remainingChances == 0) {
                    Guesser.sendMessage("Game over! The word was: " + secretWord);
                    Chooser.sendMessage("Congratulations! " + Guesser.getname() + " is out of guesses, You win!!!");
                }

                //Play again with customized messages to each player.
                broadcast("Do you wanna play another game?");
                String playchoice1 = getGameInput(clients.get(0), null);
                String playchoice2 = getGameInput(clients.get(1), null);
                if (playchoice1.equalsIgnoreCase("yes") && playchoice2.equalsIgnoreCase("yes")) {
                    playagain = "yes";
                    broadcast("Starting New Game...");
                } else if (playchoice1.equalsIgnoreCase("yes") && !playchoice2.equalsIgnoreCase("yes")) {
                    playagain = "no";
                    clients.get(0).sendMessage(clients.get(1).getname() + " doesn't want to play, disconnecting from server...");
                    clients.get(1).sendMessage("Disconnecting from server...");
                } else if (playchoice2.equalsIgnoreCase("yes") && !playchoice1.equalsIgnoreCase("yes")) {
                    playagain = "no";
                    clients.get(1).sendMessage(clients.get(0).getname() + " doesn't want to play, disconnecting from server...");
                    clients.get(0).sendMessage("Disconnecting from server...");
                } else {
                    playagain = "no";
                    broadcast("Disconnecting from server...");
                }
            } while (playagain.equalsIgnoreCase("yes"));

            //Close connection
            for (ClientHandler client : clients) {
                client.closeConnection();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Method to get GameInput from clients - ignoring chat messages
    private static String getGameInput(ClientHandler current, ClientHandler other) throws IOException {
        while (true) {
            boolean inputProcessed = false;
            for (ClientHandler client : clients) {
                if (client.isInputReady()) {
                    String msg = client.readLineNonBlocking();
                    if (msg == null) continue;
                    // If it's a chat message ( starts with @chat: ) process it immediately.
                    if (msg.startsWith("@chat:")) {
                        String chatMsg = msg.substring(6).trim();
                        broadcast("@chat:"+" ["+client.getname()+"] "+ chatMsg);
                    } else {
                        // If the message is from the client we expect, return it.
                        if (client == current) {
                            return msg;
                        } else {
                            // If the message comes from another client, treat it as chat.
                            broadcast("@chat:"+" ["+client.getname()+"] "+ msg);
                        }
                    }
                    inputProcessed = true;
                }
            }
            if (!inputProcessed) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) { }
            }
        }
    }

    //method to update the guessed word on each guess and return true if guessed word is present in the secret word.
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

    //broadcast - send message to all the clients
    private static void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
}

//Class to handle each client
class ClientHandler extends Thread {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String name;
    private boolean nameSet = false;

    public synchronized boolean isNameSet() {
        return nameSet;
    }

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper method to check if input is available non-blocking.
    public boolean isInputReady() {
        try {
            return in.ready();
        } catch (IOException e) {
            return false;
        }
    }

    // Helper method to read a line non-blocking.
    public String readLineNonBlocking() {
        try {
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void run() {
        try {
            out.println("Enter your name:");
            name = in.readLine();
            System.out.println("Player joined: " + name);
            out.println("Welcome, " + name + "!");
            synchronized (this) {
                nameSet = true;
            }
        } catch (IOException e) {
            System.out.println("Client disconnected.");
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String getname() {
        return name;
    }

    public void closeConnection() throws IOException {
        socket.close();
    }
}