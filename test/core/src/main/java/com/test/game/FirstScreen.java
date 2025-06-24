package com.test.game;

// LibGDX imports for UI and stage management
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * FirstScreen is the initial screen shown when the game starts.
 * It presents a single "Start Game" button that transitions to the main GameScreen.
 * Simple and clean screen: Only contains one button to begin the game.
 */
public class FirstScreen implements Screen {
    private HangmanClient game; // Reference to the main game client
    private Stage stage;        // Stage for managing UI actors
    private Skin skin;          // UI skin for styling buttons and widgets

    /**
     * Constructor initializes UI components and sets up the Start button.
     */
    public FirstScreen(HangmanClient game) {
        this.game = game;

        // Create stage to hold UI elements
        stage = new Stage();

        // Load skin from internal asset file (ensure uiskin.json is in the assets folder)
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // Set the stage to handle input events
        Gdx.input.setInputProcessor(stage);

        // Create a table layout to organize UI elements
        Table table = new Table();
        table.setFillParent(true); // Make table occupy entire screen
        stage.addActor(table);     // Add table to the stage

        // Create a "Start Game" button
        TextButton startButton = new TextButton("Start Game", skin);
        table.add(startButton).pad(10); // Add button to table with padding

        // Define what happens when the button is clicked
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Switch to the main game screen when clicked
                game.setScreen(new GameScreen(game));
            }
        });
    }

    // Called when this screen becomes the current screen
    @Override
    public void show() {}

    // Called every frame to update and render UI
    @Override
    public void render(float delta) {
        stage.act(delta); // Update stage actions
        stage.draw();     // Draw stage contents
    }

    // Called when the window is resized
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}   // Not used for now
    @Override public void resume() {}  // Not used for now
    @Override public void hide() {}    // Not used for now

    // Dispose of assets when screen is no longer used
    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
