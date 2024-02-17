package roboyard.eclabs;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.MotionEvent;

import java.util.Random;

/**
 * Manages rendering operations such as drawing shapes, images, and text on a canvas.
 * Created by Pierre on 21/01/2015.
 */
public class RenderManager {
    private Canvas target, mainTarget;
    private final Paint brush;
    private final Resources resources;
    private final SparseArray<Drawable> resourceMap;
    private final Random random;

    /**
     * Constructor for the RenderManager class.
     * @param resources The resources of the project.
     */
    public RenderManager(Resources resources){
        this.target = null;
        this.brush = new Paint();
        this.brush.setColor(Color.WHITE);
        this.resources = resources;
        this.resourceMap = new SparseArray<>();
        this.random = new Random();
    }

    /**
     * Sets the main target canvas for rendering.
     * @param target The main canvas to target.
     */
    public void setMainTarget(Canvas target){
        this.mainTarget = target;
        this.target = target;
    }

    /**
     * Sets the target canvas for rendering.
     * @param target The canvas to target.
     */
    public void setTarget(Canvas target){
        this.target = target;
    }

    /**
     * Resets the target canvas to the main canvas.
     */
    public void resetTarget(){
        this.target = this.mainTarget;
    }

    /**
     * Changes the color used for rendering.
     * @param color The new color.
     */
    public void setColor(int color){
        this.brush.setColor(color);
    }

    /**
     * Fills the entire target canvas with the default color.
     */
    public void paintScreen(){
        this.target.drawColor(this.brush.getColor());
    }

    /**
     * Draws a circle on the target canvas.
     * @param x The x-coordinate of the center of the circle.
     * @param y The y-coordinate of the center of the circle.
     * @param radius The radius of the circle.
     */
    public void drawCircle(float x, float y, int radius){
        this.target.drawCircle(x, y, radius, this.brush);
    }

    /**
     * Draws a rectangle on the target canvas.
     * @param x1 The x-coordinate of the top-left corner of the rectangle.
     * @param y1 The y-coordinate of the top-left corner of the rectangle.
     * @param x2 The x-coordinate of the bottom-right corner of the rectangle.
     * @param y2 The y-coordinate of the bottom-right corner of the rectangle.
     */
    public void drawRect(float x1, float y1, float x2, float y2){
        this.target.drawRect(x1, y1, x2, y2, this.brush);
    }

    /**
     * Draws an image on the target canvas.
     * @param x1 The x-coordinate of the top-left corner of the image.
     * @param y1 The y-coordinate of the top-left corner of the image.
     * @param x2 The x-coordinate of the bottom-right corner of the image.
     * @param y2 The y-coordinate of the bottom-right corner of the image.
     * @param image The index of the image to draw.
     */
    public void drawImage(int x1, int y1, int x2, int y2, int image){
        Drawable d = this.resourceMap.get(image);
        if (d == null) {
            return;
        }
        d.setBounds(x1, y1, x2, y2);
        d.draw(this.target);
    }

    /**
     * Loads an image into memory.
     * @param image The index of the image to load.
     */
    public void loadImage(int image){
        this.resourceMap.append(image, this.resources.getDrawable(image));
    }

    /**
     * Draws a bitmap on the canvas.
     * @param bmp The bitmap to draw.
     * @param x The x-coordinate of the top-left corner of the bitmap.
     * @param y The y-coordinate of the top-left corner of the bitmap.
     */
    public void drawBitmap(Bitmap bmp, float x, float y){
        this.target.drawBitmap(bmp, x, y, null);
    }

    /**
     * Loads a bitmap into memory.
     * @param bmp The bitmap to load.
     * @return The ID of the loaded bitmap.
     */
    public int loadBitmap(Bitmap bmp){
        int id = this.random.nextInt();
        while(this.resourceMap.indexOfKey(id) >= 0){
            id = this.random.nextInt();
        }
        this.resourceMap.append(id, new BitmapDrawable(this.resources, bmp));
        return id;
    }

    /**
     * Unloads a bitmap from memory.
     * @param id The ID of the bitmap to unload.
     */
    public void unloadBitmap(int id){
        if(this.resourceMap.indexOfKey(id) >= 0){
            this.resourceMap.delete(id);
        }
    }

    /**
     * Retrieves the resources associated with the RenderManager.
     * @return The resources.
     */
    public Resources getResources()
    {
        return resources;
    }

    /**
     * Writes text at a specified position.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param str The text to write.
     */
    public void drawText(int x, int y, String str){
        this.target.drawText(str, x, y, this.brush);
    }

    /** TODO: still crashes
     * second possibility to call the same function name
     * Writes text at a specified position with a custom font.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param str The text to write.
     * @param font The custom font to use.
     * @param ctx The application context.
    public void drawText(int x, int y, String str, String font, Context ctx){
        Typeface oldfont=this.brush.getTypeface();
        Typeface.createFromAsset(ctx.getAssets(), "fonts/" + font + ".ttf");
        this.brush.setTypeface(Typeface.create(font,oldfont.getStyle()));
        this.target.drawText(str, x, y, this.brush);
        this.brush.setTypeface(oldfont);
    }
    */

    /**
     * Sets the text size.
     * @param s The text size.
     */
    public void setTextSize(int s){
        this.brush.setTextSize(s);
    }

    /**
     * Draws clickable text on the canvas
     * @param x x-coordinate
     * @param y y-coordinate
     * @param text Text to display
     * @param color Color of the text
     * @param textSize Size of the text
     * @param clickListener Listener for the click event
     */
    public void drawClickableText(int x, int y, String text, int color, int textSize, ClickListener clickListener) {
        Rect bounds = new Rect();
        brush.setColor(color);
        brush.setTextSize(textSize);
        brush.getTextBounds(text, 0, text.length(), bounds);
        target.drawText(text, x, y + bounds.height() - textSize, brush);
        clickListener.setClickableBounds(x, y, x + bounds.width(), y + bounds.height());
    }

    public Rect drawLinkText(int x, int y, String text, int color, int textSize) {
        Rect bounds = new Rect();
        brush.setColor(color);
        brush.setTextSize(textSize);
        brush.getTextBounds(text, 0, text.length(), bounds);
        target.drawText(text, x, y + bounds.height() - textSize, brush);

//        clickListener.setClickableBounds(x, y, x + bounds.width(), y + bounds.height());
        return new Rect(x, y - textSize, x + bounds.width(), y + bounds.height());
    }

    /**
     * Handles touch events for clickable text.
     * @param event The MotionEvent.
     * @param clickListener The ClickListener for the text.
     */
    public void handleTouchEvent(MotionEvent event, ClickListener clickListener) {
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_UP:
                if (clickListener != null && clickListener.isClickable() && clickListener.isInsideClickableBounds(x, y)) {
                    clickListener.onClick();
                }
                break;
        }
    }

    /**
     * Interface for click events on clickable text.
     */
    public interface ClickListener {
        void onClick();
        void setClickableBounds(float left, float top, float right, float bottom);
        boolean isInsideClickableBounds(float x, float y);
        boolean isClickable();
    }
}
