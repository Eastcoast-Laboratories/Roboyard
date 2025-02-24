package roboyard.eclabs;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.graphics.Color;
// import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the Credits game screen.
 * Created by Pierre on 04/02/2015.
 */
public class CreditsGameScreen extends GameScreen {
    private float scrollY = 0;
    private float lastTouchY = 0;
    private boolean isDragging = false;
    private ArrayList<ClickableLink> links = new ArrayList<>();
    private List<GameButtonLink> gameButtonLinks = new ArrayList<>();
    private Context mContext;

    public CreditsGameScreen(GameManager gameManager, Context context) {
        super(gameManager);
        mContext = context;
    }

    int hs2; // Half the screen height
    float ts; // Text size
    float ts_large; // Large text size

    /**
     * Creates game objects for the Credits screen.
     */
    @Override
    public void create() {
        int ws2 = this.gameManager.getScreenWidth() / 2;
        hs2 = this.gameManager.getScreenHeight() / 2;
        ts = hs2 / 10; // Text size
        ts_large = ts * 0.9f; // Large text size
        
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
        int versionCode = getVersionCode(mContext);
        String versionName = getVersionName(mContext);

        // Clear background
        renderManager.setColor(Color.parseColor("#B0CC99")); // Light green background
        renderManager.paintScreen();

        // Apply scroll transformation
        renderManager.save();
        renderManager.translate(0, scrollY);

        float pos = 1;
        renderManager.setColor(Color.BLACK);
        renderManager.setTextSize((int) (ts_large));
        renderManager.drawText(10, (int) (pos++ * ts), "How to Play");
        renderManager.setTextSize((int) (0.5 * ts));
        renderManager.drawText(10, (int) (pos * ts), "• Swipe to move robots");
        pos+=0.7;
        renderManager.drawText(10, (int) (pos * ts), "• Robots move until they hit a wall or");
        pos+=0.7;
        renderManager.drawText(10, (int) (pos * ts), "  another robot");
        pos+=0.7;
        renderManager.drawText(10, (int) (pos * ts), "• Complete levels to earn stars");
        pos+=0.7;
        renderManager.drawText(10, (int) (pos * ts), "• Unlock new levels by earning stars");

        pos++;
        pos++;
        renderManager.setTextSize((int) (ts_large));
        renderManager.drawText(10, (int) (pos++ * ts), "Based on");
        renderManager.setTextSize((int) (0.7 * ts));
        renderManager.drawText(10, (int) (pos++ * ts), "Ricochet Robots(r)");

        pos++;
        renderManager.setTextSize((int) (ts_large));
        renderManager.drawText(10, (int) (pos++ * ts), "Imprint/privacy policy");
        renderManager.setTextSize((int) (0.7 * ts));
        links.clear();
        drawClickableLink(renderManager, 10, pos++ * ts, "https://eclabs.de/ds.html");

        pos++;
        renderManager.setColor(Color.BLACK);
        renderManager.setTextSize((int) (ts_large));
        renderManager.drawText(10, (int) (pos++ * ts), "Open Source");
        renderManager.setTextSize((int) (0.7 * ts));
        drawClickableLink(renderManager, 10, pos++ * ts, "https://git.io/fjs5H");
        renderManager.setColor(Color.BLACK);
        renderManager.setTextSize((int) (0.7 * ts));
        renderManager.drawText(10, (int) (pos++ * ts), "Version: " + versionName + " (Build " + versionCode + ")");

        pos++;
        renderManager.setTextSize((int) (ts_large));
        renderManager.drawText(10, (int) (pos++ * ts), "Contact Us");
        renderManager.setTextSize((int) (0.7 * ts));
        drawClickableLink(renderManager, 10, pos++ * ts, "https://eclabs.de/contact");

        pos++;
        // Draw credits text
        renderManager.setColor(Color.BLACK);
        renderManager.setTextSize((int) (ts_large));
        renderManager.drawText(10, (int) (pos++ * ts), "Created by");
        renderManager.setTextSize((int) (0.7 * ts));
        renderManager.drawText(10, (int) (pos++ * ts), "Alain Caillaud");
        renderManager.drawText(10, (int) (pos++ * ts), "Pierre Michel");
        renderManager.drawText(10, (int) (pos++ * ts), "Ruben Barkow-Kuder");

        renderManager.restore();
        super.draw(renderManager);
    }

    // Method to retrieve version code using PackageManager
    private int getVersionCode(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            String packageName = context.getPackageName();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return -1; // Error occurred, return -1 or handle it accordingly
        }
    }

    // Method to retrieve version name using PackageManager
    private String getVersionName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            String packageName = context.getPackageName();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return ""; // Error occurred, return empty string or handle it accordingly
        }
    }

    private void drawClickableLink(RenderManager renderManager, float x, float y, String url) {
        renderManager.setColor(Color.BLUE);
        renderManager.drawText((int)x, (int)y, url);
        links.add(new ClickableLink(x, y - 30, x + renderManager.measureText(url), y, url));
    }

    private void openLink(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        mContext.startActivity(intent);
    }

    /**
     * Updates the Credits screen.
     *
     * @param gameManager The game manager
     */
    @Override
    public void update(GameManager gameManager) {
        super.update(gameManager);
        
        InputManager inputManager = gameManager.getInputManager();
        if (inputManager.eventHasOccurred()) {
            float x = inputManager.getTouchX();
            float y = inputManager.getTouchY();
            
            if (inputManager.downOccurred()) {
                lastTouchY = y;
                isDragging = true;
            } else if (inputManager.moveOccurred() && isDragging) {
                float deltaY = y - lastTouchY;
                scrollY += deltaY;
                
                // Limit scrolling
                float maxScroll = 30 * ts; // Adjust based on content height
                scrollY = Math.max(Math.min(scrollY, 0), -maxScroll);
                
                lastTouchY = y;
            } else if (inputManager.upOccurred()) {
                isDragging = false;
                
                // Handle link clicks only if we haven't scrolled much
                if (Math.abs(y - lastTouchY) < 10) {
                    for (ClickableLink link : links) {
                        if (link.contains(x, y - scrollY)) {  // Adjust click position by scroll
                            openLink(link.getUrl());
                            break;
                        }
                    }
                }
            }
        }
        
        InputManager im = gameManager.getInputManager();
        // Handle back button press to return to the main menu
        if (im.backOccurred()) {
            gameManager.setGameScreen(0); // Set the main menu screen
        } else if(im.eventHasOccurred()) {
            for (GameButtonLink link : gameButtonLinks) {
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

class ClickableLink {
    private float x, y, w, h;
    private String url;

    public ClickableLink(float x, float y, float w, float h, String url) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.url = url;
    }

    public boolean contains(float x, float y) {
        return x >= this.x && x <= this.w && y >= this.y && y <= this.h;
    }

    public String getUrl() {
        return url;
    }
}
