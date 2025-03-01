package roboyard.eclabs;

import android.graphics.Paint;
import android.graphics.Rect;
import java.util.ArrayList;
import java.util.List;

public class GameDropdown implements IGameObject {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final List<String> options;
    private final List<Runnable> actions;
    private boolean isOpen;
    private int selectedIndex;
    private final Paint paint;
    private final int backgroundColor = 0xFF404040;
    private final int textColor = 0xFFFFFFFF;
    private final int highlightColor = 0xFF606060;

    public GameDropdown(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.options = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.isOpen = false;
        this.selectedIndex = 0;
        
        this.paint = new Paint();
        this.paint.setTextSize(height * 0.6f);
        this.paint.setColor(textColor);
        this.paint.setAntiAlias(true);
    }

    public void addOption(String option, Runnable action) {
        options.add(option);
        actions.add(action);
    }
    
    /**
     * Sets the selected index of the dropdown
     * @param index The index to select
     */
    public void setSelectedIndex(int index) {
        if (index >= 0 && index < options.size()) {
            selectedIndex = index;
        }
    }

    @Override
    public void create() {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void load(RenderManager renderManager) {
    }

    @Override
    public void update(GameManager gameManager) {
        InputManager input = gameManager.getInputManager();
        if (input.eventHasOccurred() && input.downOccurred()) {
            float touchX = input.getTouchX();
            float touchY = input.getTouchY();

            // Check if click is in dropdown area
            if (touchX >= x && touchX <= x + width) {
                if (!isOpen) {
                    // Check header click
                    if (touchY >= y && touchY <= y + height) {
                        isOpen = true;
                        input.resetEvents();
                        return;
                    }
                } else {
                    // Check option clicks
                    int totalHeight = (options.size() + 1) * height;
                    if (touchY >= y && touchY <= y + totalHeight) {
                        int clickedIndex = (int)((touchY - y) / height - 1);
                        if (clickedIndex >= 0 && clickedIndex < options.size()) {
                            selectedIndex = clickedIndex;
                            actions.get(clickedIndex).run();
                        }
                        isOpen = false;
                        input.resetEvents();
                        return;
                    } else {
                        isOpen = false;
                    }
                }
            } else if (isOpen) {
                isOpen = false;
            }
        }
    }

    @Override
    public void draw(RenderManager renderManager) {
        // Draw dropdown header background
        renderManager.setColor(backgroundColor);
        renderManager.drawRect(x, y, x + width, y + height);
        
        // Draw dropdown header text
        String selectedText = options.get(selectedIndex);
        float textWidth = paint.measureText(selectedText);
        float textX = x + (width - textWidth) / 2;
        float textY = y + height * 0.7f;
        renderManager.setColor(textColor);
        renderManager.drawText((int)textX, (int)textY, selectedText);
        
        // Draw dropdown arrow
        String arrow = isOpen ? "▲" : "▼";
        float arrowWidth = paint.measureText(arrow);
        renderManager.drawText((int)(x + width - arrowWidth - 10), (int)textY, arrow);

        // Draw options if open
        if (isOpen) {
            for (int i = 0; i < options.size(); i++) {
                int optionY = y + (i + 1) * height;
                renderManager.setColor(i == selectedIndex ? highlightColor : backgroundColor);
                renderManager.drawRect(x, optionY, x + width, optionY + height);
                
                String optionText = options.get(i);
                float optionWidth = paint.measureText(optionText);
                float optionX = x + (width - optionWidth) / 2;
                float optionTextY = optionY + height * 0.7f;
                renderManager.setColor(textColor);
                renderManager.drawText((int)optionX, (int)optionTextY, optionText);
            }
        }
    }
}
