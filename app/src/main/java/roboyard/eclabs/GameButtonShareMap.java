package roboyard.eclabs;

import android.content.Intent;
import android.net.Uri;
import android.app.Activity;
import android.widget.Toast;

import timber.log.Timber;

/**
 * Button to share a map via URL
 */
public class GameButtonShareMap extends GameButton {
    private final String mapPath;
    private final int parentButtonX;
    private final int parentButtonY;
    private final int parentButtonSize;
    private ScreenLayout layout;

    public GameButtonShareMap(int x, int y, int width, int height, int upImage, int downImage, String mapPath,
                            int parentButtonX, int parentButtonY, int parentButtonSize) {
        super(x, y, width, height, upImage, downImage);
        this.mapPath = mapPath;
        this.parentButtonX = parentButtonX;
        this.parentButtonY = parentButtonY;
        this.parentButtonSize = parentButtonSize;
    }

    @Override
    public void create() {
        super.create();
    }

    @Override
    public void update(GameManager gameManager) {
        if (layout == null) {
            // Initialize screen layout on first update when we have gameManager
            layout = new ScreenLayout(
                gameManager.getScreenWidth(),
                gameManager.getScreenHeight(),
                gameManager.getActivity().getResources().getDisplayMetrics().density
            );
        }
        // Position the share button at the bottom left of the parent button
        this.x = parentButtonX - layout.x(15);
        this.y = parentButtonY + parentButtonSize - layout.y(33);

        // Let parent class handle click detection
        super.update(gameManager);
    }

    @Override
    public void onClick(GameManager gameManager) {
        Activity activity = gameManager.getActivity();
        String saveData = FileReadWrite.readPrivateData(activity, mapPath);
        if (saveData != null && !saveData.isEmpty()) {
            try {
                String shareUrl = "https://roboyard.z11.de/share_map?data=" + Uri.encode(saveData);
                Timber.d("Sharebutton: Opening browser to share map " + shareUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(shareUrl));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            } catch (Exception e) {
                Timber.e(e, "Failed to open browser");
                Toast.makeText(activity, "Could not open browser", Toast.LENGTH_SHORT).show();
            }
        } else {
            Timber.d("No save data found for " + mapPath);
        }
    }
}
