package com.test.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

import java.io.*;
import java.net.Socket;

// Enum to track user input stage
enum InputStage {
    NAME, SECRET, CLUE, GUESS, NONE
}

public class GameScreen implements Screen {
    private HangmanClient game;

    // Core rendering tools
    private Stage stage;
    private Skin skin;
    private SpriteBatch batch;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;

    // UI components
    private TextField inputField;
    private Label wordLabel, clueLabel, promptLabel, statusLabel, resultLabel;
    private Label nameLabel, roleLabel;
    private TextButton submitButton;

    // Chat UI
    private TextArea chatArea;
    private TextField chatInput;
    private TextButton sendButton;

    // Networking
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final String serverAddress = "127.0.0.1";
    private final int port = 5000;

    // Game state
    private InputStage currentStage = InputStage.NONE;
    private boolean isChooser = false;
    private boolean gameOver = false;
    private boolean playerLost = false;
    private String playerName = "";

    // Animation variables
    private int wrongGuessCount = 0;
    private float headAlpha = 0f;
    private boolean fadingHead = false;
    private float shakeDuration = 0f;
    private float shakeIntensity = 5f;

    // Placeholder container for hangman drawing area
    Container<Label> hangmanSpace = new Container<>();

    public GameScreen(HangmanClient game) {
        this.game = game;

        // Initialize rendering and UI
        batch = new SpriteBatch();
        font = new BitmapFont();
        shapeRenderer = new ShapeRenderer();

        stage = new Stage();
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        Gdx.input.setInputProcessor(stage);

        // Initialize UI labels and fields
        nameLabel = new Label("", skin);
        roleLabel = new Label("", skin);
        promptLabel = new Label("", skin);
        statusLabel = new Label("", skin);
        resultLabel = new Label("", skin);
        wordLabel = new Label("", skin);
        clueLabel = new Label("", skin);
        inputField = new TextField("", skin);
        submitButton = new TextButton("Submit", skin);


        // Configure hangmanSpace container.
        // Reserve space for hangman drawing
        // Here we add a dummy label to reserve layout space,
        // and set a preferred size which will be used in drawing.
        hangmanSpace.setActor(new Label(" ", skin));
        hangmanSpace.prefWidth(120);
        hangmanSpace.prefHeight(180);

        // ===== Layout Structure =====
        // Root layout: split screen horizontally into 2 columns.
        // Root table divides into game area and chat/drawing area
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Left column: Game UI table.
        // Add the game components (name, prompt, input, etc.)
        Table gameTable = new Table().left().center();
        gameTable.add(new Label("HANGMAN",skin)).colspan(2).padBottom(20).row();
        gameTable.add(new Label("Game Area",skin)).colspan(2).pad(30).row();
        gameTable.add(nameLabel).colspan(2).pad(10).left().row();
        gameTable.add(roleLabel).colspan(2).pad(10).left().row();
        gameTable.add(promptLabel).colspan(2).pad(10).left().row();
        gameTable.add(wordLabel).colspan(2).pad(10).left().row();
        gameTable.add(clueLabel).colspan(2).pad(10).left().row();
        gameTable.add(inputField).width(100).pad(10);
        gameTable.add(submitButton).width(100).pad(10).row();
        gameTable.add(statusLabel).colspan(2).pad(10).left().row();
        gameTable.add(resultLabel).colspan(2).pad(10).left().row();

        // Right column (Drawing + Chat)
        // Right column: A vertical table that holds:
        // Top: Hangman drawing area.
        // Bottom: Chat UI table.
        Table rightColumn = new Table().top().pad(10);
        // Add hangmanSpace container first (for drawing area).
        rightColumn.add(new Label("Hangman Figure", skin)).padTop(10).padBottom(10).row();
        rightColumn.add(hangmanSpace).width(hangmanSpace.getPrefWidth())
            .height(hangmanSpace.getPrefHeight())
            .row();

        // Chat UI setup
        // Create Chat UI table.
        Table chatTable = new Table().top().right();
        chatArea = new TextArea("", skin);
        chatArea.setDisabled(true);
        chatArea.setPrefRows(8);
        chatArea.setColor(Color.DARK_GRAY);
        ScrollPane chatScroll = new ScrollPane(chatArea, skin);
        chatScroll.setFadeScrollBars(false);
        chatInput = new TextField("", skin);
        sendButton = new TextButton("Send", skin);

        chatTable.add(new Label("Chat", skin)).colspan(2).pad(5).row();
        chatTable.add(chatScroll).colspan(2).width(200).height(200).pad(5).row();
        chatTable.add(chatInput).width(150).pad(5);
        chatTable.add(sendButton).width(50).pad(5).row();

        // Add chat table to the right column.
        rightColumn.add(chatTable).expandY().top();

        // Assemble root layout
        // Arrange left and right columns in the root table.
        // Adjust padding and expand as needed.
        root.add(gameTable).expand().fillY().pad(10);
        root.add(rightColumn).expand().fillY().pad(10);

        // Submit button handler
        submitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String input = inputField.getText().trim();
                inputField.setText("");

                if (input.isEmpty()) return;

                switch (currentStage) {
                    case NAME:
                    case SECRET:
                    case CLUE:
                        sendInput(input);
                        break;
                    case GUESS:
                        if (input.length() == 1 && Character.isLetter(input.charAt(0))) {
                            sendInput(input);
                        } else {
                            statusLabel.setText("Please enter a single letter.");
                        }
                        break;
                    case NONE:
                        statusLabel.setText("Wait for your turn...");
                        break;
                }
            }
        });

        // Chat send button
        sendButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sendChat();
            }
        });

        // Pressing Enter in chat input triggers send
        chatInput.setTextFieldListener((field, c) -> {
            if (c == '\r' || c == '\n') {
                sendChat();
            }
        });

        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket(serverAddress, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            new Thread(this::listenToServer).start();
        } catch (IOException e) {
            statusLabel.setText("Could not connect to server");
        }
    }

    private void listenToServer() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    final String finalMsg = msg;
                    Gdx.app.postRunnable(() -> handleServerMessage(finalMsg));
                }
            } catch (IOException e) {
                Gdx.app.postRunnable(() -> statusLabel.setText("Disconnected from server"));
            }
        }).start();
    }

    // Parses and reacts to server messages
    private void handleServerMessage(String msg) {
        if (msg == null || msg.isEmpty()) return;

        if (msg.startsWith("@chat:")) {
            String chatMsg = msg.substring(6); // remove prefix
            chatArea.appendText(chatMsg + "\n");
            return;
        }

        //Debug message
        // System.out.println("GameOver: " + gameOver + " | PlayerLost: " + playerLost + " | WrongGuessCount: " + wrongGuessCount);
        String msgLower = msg.toLowerCase();

        // [The rest of your server message handling remains unchanged...]
        if (msg.startsWith("Enter your name")) {
            promptLabel.setText("Enter your name:");
            resultLabel.setText("");
            resultLabel.setColor(1, 1, 1, 1);
            wordLabel.setText("");
            clueLabel.setText("");
            isChooser = false;
            currentStage = InputStage.NAME;
        } else if (msg.startsWith("Welcome,")) {
            playerName = msg.substring("Welcome,".length()).replace("!", "").trim();
            nameLabel.setText("Name: " + playerName);
            promptLabel.setText("Waiting for the other player...");
            resultLabel.setText("");
        } else if (msg.startsWith("Enter the secret word")) {
            promptLabel.setText("Enter the secret word:");
            isChooser = true;
            currentStage = InputStage.SECRET;
            roleLabel.setText("Role: Chooser");
        } else if (msg.startsWith("Enter Clue")) {
            promptLabel.setText("Enter a clue:");
            currentStage = InputStage.CLUE;
        } else if (msg.startsWith("Clue:")) {
            clueLabel.setText(msg);
        } else if (msg.startsWith("Current word:")) {
            String wordContent = msg.substring("Current word:".length()).trim();
            wordLabel.setText(isChooser ? "Word: " + wordContent : msg);
        } else if (msg.startsWith("Word:")) {
            statusLabel.setText("");
            wordLabel.setText(msg);
        } else if (msg.startsWith("It's your turn to guess.")) {
            resultLabel.setText("You are the guesser. Get ready!");
            resultLabel.setColor(1, 1, 1, 1);
            if (!isChooser) {
                roleLabel.setText("Role: Guesser");
            }
        } else if (msg.startsWith("Enter a letter:")) {
            promptLabel.setText("Enter a letter:");
            currentStage = InputStage.GUESS;
        } else if (msg.startsWith("Correct guess")) {
            resultLabel.setText(msg);
            resultLabel.setColor(0, 1, 0, 1);
        } else if (msg.startsWith("Incorrect guess")) {
            resultLabel.setText(msg);
            resultLabel.setColor(1, 0, 0, 1);
            wrongGuessCount++;

            if (wrongGuessCount == 1) {
                headAlpha = 0f;
                fadingHead = true;
            }
            shakeDuration = 0.3f; // shake for 0.3 seconds
        } else if (msg.endsWith("is guessing...")) {
            if (isChooser) {
                promptLabel.setText(msg);
                currentStage = InputStage.NONE;
            }
        } else if (msg.contains("already been guessed")) {
            resultLabel.setText(msg);
            resultLabel.setColor(1, 0.5f, 0, 1);
        } else if (msgLower.contains("you won") || msgLower.contains("you lose") || msgLower.contains("game over")) {
            resultLabel.setText(msg);
            resultLabel.setColor(1, 1, 0, 1);
            promptLabel.setText("");
            currentStage = InputStage.NONE;
            gameOver = true;
            if (isChooser) {
                playerLost = false;
            } else {
                playerLost = msgLower.contains("game over");
            }
        } else if (msg.startsWith("Do you wanna play another game?")) {
            promptLabel.setText("Do you want to play again? (yes/no)");
            currentStage = InputStage.NAME;
            resultLabel.setColor(1, 1, 1, 1);
        } else if (msg.startsWith("Starting New Game")) {
            resultLabel.setText("Starting a new game...");
            resultLabel.setColor(0.8f, 0.8f, 1, 1);
            promptLabel.setText("");
            wordLabel.setText("");
            clueLabel.setText("");
            isChooser = false;
            wrongGuessCount = 0;
            gameOver = false;
            playerLost = false;
        } else if (msg.toLowerCase().contains("disconnecting from server")) {
            resultLabel.setText(msg);
            promptLabel.setText("");
            wordLabel.setText("");
            clueLabel.setText("");
            statusLabel.setText("");
            nameLabel.setText("");
            roleLabel.setText("");
            currentStage = InputStage.NONE;
        } else if (msgLower.contains("wait for your turn")) {
            if (currentStage != InputStage.NONE) {
                resultLabel.setText(msg);
                resultLabel.setColor(1, 1, 1, 1);
            }
        } else {
            resultLabel.setText(msg);
            resultLabel.setColor(1, 1, 1, 1);
        }
    }

    private void sendInput(String input) {
        if (out != null && !input.trim().isEmpty()) {
            out.println(input.trim());
        }
    }

    private void sendChat() {
        String msg = chatInput.getText().trim();
        if (!msg.isEmpty()) {
            out.println("@chat:" + msg); // Special prefix for server
            chatInput.setText("");
        }
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // Fade-in head
        if (fadingHead) {
            headAlpha += delta * 2;
            if (headAlpha >= 1f) {
                headAlpha = 1f;
                fadingHead = false;
            }
        }

        // Shake logic
        float shakeX = 0, shakeY = 0;
        if (shakeDuration > 0) {
            shakeDuration -= delta;
            shakeX = (float)(Math.random() * 2 - 1) * shakeIntensity;
            shakeY = (float)(Math.random() * 2 - 1) * shakeIntensity;
        }

        // Update stage with possible shake effect
        batch.begin();
        stage.getViewport().getCamera().position.set(
            stage.getViewport().getWorldWidth() / 2f + shakeX,
            stage.getViewport().getWorldHeight() / 2f + shakeY,
            0
        );
        stage.getViewport().getCamera().update();
        stage.act(delta);
        stage.draw();
        batch.end();

        // ======== HANGMAN DRAWING ==========
        // Retrieve the stage coordinates from hangmanSpace.
        Vector2 hangmanPos = hangmanSpace.localToStageCoordinates(new Vector2(0, hangmanSpace.getHeight()));
        float baseX = hangmanPos.x;          // Left edge of the container
        float baseY = hangmanPos.y;          // Top edge of the container

        // Use a margin inside the container.
        float margin = 10f;
        float drawX = baseX + margin;
        float drawY = baseY - margin;

        // Set a scale factor to magnify the drawing.
        float scale = 1.75f;  // Increase this value for a larger drawing.

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        // Draw the gallows, scaling the lengths.
        // Vertical post:
        shapeRenderer.line(drawX, drawY, drawX, drawY - 100 * scale);
        // Horizontal beam:
        shapeRenderer.line(drawX, drawY, drawX + 50 * scale, drawY);
        // Rope:
        shapeRenderer.line(drawX + 50 * scale, drawY, drawX + 50 * scale, drawY - 20 * scale);

        // Draw the hangman parts based on wrongGuessCount.
        if (wrongGuessCount >= 1) {
            shapeRenderer.setColor(1, 1, 1, headAlpha);
            // Head as circle with scaled radius:
            shapeRenderer.circle(drawX + 50 * scale, drawY - 30 * scale, 10 * scale);
            shapeRenderer.setColor(Color.WHITE);
        }
        if (wrongGuessCount >= 2) {
            // Body line:
            shapeRenderer.line(drawX + 50 * scale, drawY - 40 * scale, drawX + 50 * scale, drawY - 70 * scale);
        }
        if (wrongGuessCount >= 3) {
            // Left arm:
            shapeRenderer.line(drawX + 50 * scale, drawY - 50 * scale, drawX + 40 * scale, drawY - 60 * scale);
        }
        if (wrongGuessCount >= 4) {
            // Right arm:
            shapeRenderer.line(drawX + 50 * scale, drawY - 50 * scale, drawX + 60 * scale, drawY - 60 * scale);
        }
        if (wrongGuessCount >= 5) {
            // Left leg:
            shapeRenderer.line(drawX + 50 * scale, drawY - 70 * scale, drawX + 40 * scale, drawY - 80 * scale);
        }
        if (wrongGuessCount >= 6) {
            // Right leg:
            shapeRenderer.line(drawX + 50 * scale, drawY - 70 * scale, drawX + 60 * scale, drawY - 80 * scale);
        }

        // Game over eye Xs
        if (gameOver && playerLost && wrongGuessCount >= 1) {
            shapeRenderer.setColor(Color.RED);
            float cx = drawX + 50 * scale;
            float cy = drawY - 30 * scale;
            float eyeOffsetX = 3 * scale, eyeOffsetY = 3 * scale;
            // Left eye 'X'
            shapeRenderer.line(cx - eyeOffsetX - 2 * scale, cy + eyeOffsetY + 2 * scale, cx - eyeOffsetX + 2 * scale, cy + eyeOffsetY - 2 * scale);
            shapeRenderer.line(cx - eyeOffsetX - 2 * scale, cy + eyeOffsetY - 2 * scale, cx - eyeOffsetX + 2 * scale, cy + eyeOffsetY + 2 * scale);
            // Right eye 'X'
            shapeRenderer.line(cx + eyeOffsetX - 2 * scale, cy + eyeOffsetY + 2 * scale, cx + eyeOffsetX + 2 * scale, cy + eyeOffsetY - 2 * scale);
            shapeRenderer.line(cx + eyeOffsetX - 2 * scale, cy + eyeOffsetY - 2 * scale, cx + eyeOffsetX + 2 * scale, cy + eyeOffsetY + 2 * scale);
            shapeRenderer.setColor(Color.WHITE);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    
    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        batch.dispose();
        font.dispose();
        shapeRenderer.dispose();
        try { if (socket != null) socket.close(); } catch(IOException e) { e.printStackTrace(); }
    }
}
