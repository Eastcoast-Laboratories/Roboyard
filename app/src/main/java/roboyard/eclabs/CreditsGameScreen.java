package roboyard.eclabs;

import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the Credits game screen.
 * Created by Pierre on 04/02/2015.
 */
public class CreditsGameScreen extends GameScreen {
    private List<GameButtonLink> links = new ArrayList<>();
    public CreditsGameScreen(GameManager gameManager) {
        super(gameManager);
    }

    int hs2; // Half the screen height

    /**
     * Creates game objects for the Credits screen.
     */
    @Override
    public void create() {
        int ws2 = this.gameManager.getScreenWidth() / 2;
        hs2 = this.gameManager.getScreenHeight() / 2;
        // Add back button
        this.instances.add(new GameButtonGoto(7 * ws2 / 4 - 128, 9 * hs2 / 5 - 128, 128, 128, R.drawable.bt_back_up, R.drawable.bt_back_down, 0));
    }

    /**
     * Loads assets for rendering.
     *
     * @param renderManager The render manager
     */
    @Override
    public void load(RenderManager renderManager) {
        super.load(renderManager);
    }

    /**
     * Renders the Credits screen.
     *
     * @param renderManager The render manager
     */
    @Override
    public void draw(RenderManager renderManager) {
        // Get app version information
        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;

        // Set background color
        //renderManager.setColor(Color.BLUE);
        renderManager.setColor(Color.parseColor("#B0CC99")); // Light green background
        renderManager.paintScreen();

        // Set text color and size
        renderManager.setColor(Color.BLACK);
        int ts = hs2 / 10; // Text size

        // Draw credits text
        renderManager.setTextSize((int) (1.2 * ts));
        renderManager.drawText(10, 2 * ts, "Created by");
        renderManager.setTextSize((int) (0.7 * ts));
        renderManager.drawText(10, 3 * ts, "Alain Caillaud");
        renderManager.drawText(10, 4 * ts, "Pierre Michel");
        renderManager.drawText(10, 5 * ts, "Ruben Barkow-Kuder");

        renderManager.setTextSize((int) (1.2 * ts));
        renderManager.drawText(10, 7 * ts, "Based on");
        renderManager.setTextSize((int) (0.7 * ts));
        renderManager.drawText(10, 8 * ts, "Ricochet Robots(r)");

        renderManager.setTextSize((int) (1.2 * ts));
        renderManager.drawText(10, 10 * ts, "Imprint/privacy policy");
        renderManager.setTextSize((int) (0.7 * ts));
        links.clear();
        drawClickableLink(renderManager, 10, 11 * ts, "https://eclabs.de/datenschutz.html");

        renderManager.setColor(Color.BLACK);
        renderManager.setTextSize((int) (1.2 * ts));
        renderManager.drawText(10, 13 * ts, "Open Source");

        drawClickableLink(renderManager, 10, 14 * ts, "https://git.io/fjs5H");

        renderManager.setColor(Color.BLACK);
        renderManager.drawText(10, 17 * ts, "Version: " + versionName + " (Build " + versionCode + ")");

        super.draw(renderManager);
    }

    /**
     * Draws a clickable link.
     *
     * @param renderManager The render manager
     * @param x             The x-coordinate of the link
     * @param y             The y-coordinate of the link
     * @param url           The URL of the link
     */
    private void drawClickableLink(RenderManager renderManager, int x, int y, String url) {
        Rect rect = renderManager.drawLinkText(x, y, url, Color.BLUE, (int) (0.7 * (hs2 / 10)));
        links.add(new GameButtonLink(rect.left, rect.top, rect.right, rect.bottom, url));


//        renderManager.drawClickableText(x, y, url, Color.BLUE, (int) (0.7 * (hs2 / 10)), new RenderManager.ClickListener() {
//            @Override
//            public void onClick() {
//                openLink(url);
//            }
//
//            @Override
//            public void setClickableBounds(float left, float top, float right, float bottom) {
//                // No need to assign values here
//                // These bounds are set internally by the RenderManager
//            }
//
//            @Override
//            public boolean isInsideClickableBounds(float x, float y) {
//                // Check if the touch coordinates (x, y) are inside the clickable bounds
//                // Not implemented in this snippet, but you would check the bounds here
//                return true; // Placeholder return value
//            }
//
//            @Override
//            public boolean isClickable() {
//                // Determine if the text is clickable
//                // Not implemented in this snippet, but you would handle the clickable state here
//                return true; // Placeholder return value
//            }
//        });
    }

    /**
     * Opens a link in a web browser.
     *
     * @param url The URL to open
     */
    private void openLink(String url) {
        // Create an intent to open a web browser with the specified URL
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        // Start the activity (open the web browser)
        gameManager.getActivity().startActivity(browserIntent);
    }

    /**
     * Updates the Credits screen.
     *
     * @param gameManager The game manager
     */
    @Override
    public void update(GameManager gameManager) {
        super.update(gameManager);
        InputManager im = gameManager.getInputManager();
        // Handle back button press to return to the main menu
        if (im.backOccurred()) {
            gameManager.setGameScreen(0); // Set the main menu screen
        } else if(im.eventHasOccurred()) {
            for (GameButtonLink link : links) {
                boolean linkTouched = (im.getTouchX() >= link.getX() && im.getTouchX() <= link.getW()) && (im.getTouchY() >= link.getY() && im.getTouchY() <= link.getH());
                if(linkTouched) {
                    openLink(link.getUrl());
                }
            }
        }
    }

    /**
     * Cleans up resources used by the Credits screen.
     */
    @Override
    public void destroy() {
        super.destroy();
    }
}
