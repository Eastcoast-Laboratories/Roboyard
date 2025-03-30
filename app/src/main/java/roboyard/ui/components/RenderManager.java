package roboyard.ui.components;
import android.graphics.Canvas;
import android.content.Context;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;

import java.util.Random;

import timber.log.Timber;

/**
 * Manages rendering operations such as drawing shapes, images, and text on a canvas.
 */
public class RenderManager {
    private Canvas target, mainTarget;
    private final Paint brush;
    private final Resources resources;
    private final SparseArray<Drawable> resourceMap;
    private final Random random;
    private Context context; // For accessibility features

    /**
     * Constructor for the RenderManager class.
     * @param resources The resources of the project.
     */
    public RenderManager(Resources resources){
        this.target = null;
        this.brush = new Paint();
        this.brush.setAntiAlias(true);
        this.brush.setColor(Color.WHITE);
        this.resources = resources;
        this.resourceMap = new SparseArray<>();
        this.random = new Random();
    }

    /**
     * Sets the Android context to be used for accessibility features
     * @param context The Android context
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * Gets the Android context for accessibility features
     * @return The Android context or null if not set
     */
    public Context getContext() {
        return this.context;
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
        if (image != 0) {
            try {
                this.resourceMap.append(image, this.resources.getDrawable(image));
            } catch (Exception e) {
                Timber.e("Error loading image resource %d: %s", image, e.getMessage());
            }
        }
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
     * Get the current text size
     * @return The current text size
     */
    public int getTextSize() {
        return (int) this.brush.getTextSize();
    }

    public Rect drawLinkText(int x, int y, String text, int color, int textSize) {
        Rect bounds = new Rect();
        brush.setColor(color);
        brush.setTextSize(textSize);
        brush.getTextBounds(text, 0, text.length(), bounds);
        target.drawText(text, x, y + bounds.height() - textSize, brush);

        return new Rect(x, y - textSize, x + bounds.width(), y + bounds.height());
    }

    /**
     * Saves the current canvas state.
     */
    public void save() {
        if (target != null) {
            target.save();
        }
    }

    /**
     * Restores the previously saved canvas state.
     */
    public void restore() {
        if (target != null) {
            target.restore();
        }
    }

    /**
     * Set the stroke width for drawing
     * @param width Width of the stroke in pixels
     */
    public void setStrokeWidth(float width) {
        brush.setStrokeWidth(width);
        brush.setStyle(Paint.Style.STROKE);
    }

    /**
     * Translates the canvas by the specified amount.
     * @param dx The amount to translate in x direction
     * @param dy The amount to translate in y direction
     */
    public void translate(float dx, float dy) {
        this.target.translate(dx, dy);
    }

    /**
     * Draws a rectangle on the canvas.
     * @param left The left coordinate
     * @param top The top coordinate
     * @param right The right coordinate
     * @param bottom The bottom coordinate
     */
    public void drawRect(float left, float top, float right, float bottom) {
        this.target.drawRect(left, top, right, bottom, this.brush);
    }

    /**
     * Draws a filled rectangle on the canvas.
     * @param left The left coordinate of the rectangle.
     * @param top The top coordinate of the rectangle.
     * @param right The right coordinate of the rectangle.
     * @param bottom The bottom coordinate of the rectangle.
     */
    public void fillRect(float left, float top, float right, float bottom) {
        if (target != null) {
            Paint.Style oldStyle = brush.getStyle();
            brush.setStyle(Paint.Style.FILL);
            target.drawRect(left, top, right, bottom, brush);
            brush.setStyle(oldStyle);
        }
    }

    /**
     * Measures the width of text with current text settings.
     * @param text The text to measure
     * @return The width of the text in pixels
     */
    public float measureText(String text) {
        return this.brush.measureText(text);
    }

    /**
     * Draws a drawable directly to the canvas at the specified coordinates.
     * @param left Left coordinate
     * @param top Top coordinate
     * @param right Right coordinate
     * @param bottom Bottom coordinate
     * @param drawable The drawable to draw
     */
    public void drawDrawable(int left, int top, int right, int bottom, Drawable drawable) {
        if (drawable != null) {
            drawable.setBounds(left, top, right, bottom);
            drawable.draw(target);
        }
    }

    /**
     * Gets a drawable from the resource map.
     * @param resourceId The resource ID of the drawable.
     * @return The drawable, or null if not found.
     */
    public Drawable getDrawable(int resourceId) {
        return this.resourceMap.get(resourceId);
    }

    /**
     * Draws a bitmap directly to the canvas at the specified coordinates.
     * @param bitmap The bitmap to draw
     * @param left Left coordinate
     * @param top Top coordinate
     * @param right Right coordinate
     * @param bottom Bottom coordinate
     */
    public void drawBitmap(Bitmap bitmap, int left, int top, int right, int bottom) {
        if (bitmap != null) {
            Rect destRect = new Rect(left, top, right, bottom);
            this.target.drawBitmap(bitmap, null, destRect, this.brush);
        }
    }

    /**
     * Draws a rounded rectangle directly to the canvas at the specified coordinates.
     * @param x Left coordinate
     * @param y Top coordinate
     * @param i Right coordinate
     * @param i1 Bottom coordinate
     * @param cornerRadius The corner radius
     */
    public void drawRoundRect(int x, int y, int i, int i1, int cornerRadius) {
        this.target.drawRoundRect(x, y, i, i1, cornerRadius, cornerRadius, this.brush);
    }
}
