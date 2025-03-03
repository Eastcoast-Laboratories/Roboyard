package roboyard.eclabs;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class RenderManagerTest {

    @Mock
    private Resources resources;
    
    @Mock
    private Canvas canvas;
    
    @Mock
    private Bitmap bitmap;
    
    private RenderManager renderManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create RenderManager
        renderManager = new RenderManager(resources);
        renderManager.setMainTarget(canvas);
    }

    @Test
    public void testDrawBitmap() {
        // Test the drawBitmap method
        renderManager.drawBitmap(bitmap, 10, 20, 110, 120);
        
        // Verify that the bitmap is drawn on the canvas
        // This is a bit tricky to verify directly with Mockito
        // We would need to capture the Rect argument and verify its values
        verify(canvas).drawBitmap(eq(bitmap), isNull(), any(Rect.class), any(Paint.class));
    }
    
    @Test
    public void testDrawBitmapWithNullBitmap() {
        // Test with null bitmap
        renderManager.drawBitmap(null, 10, 20, 110, 120);
        
        // Verify that nothing is drawn on the canvas
        verify(canvas, never()).drawBitmap(any(Bitmap.class), any(Rect.class), any(Rect.class), any(Paint.class));
    }
    
    @Test
    public void testDrawText() {
        // Test the drawText method
        renderManager.drawText(10, 20, "Test Text");
        
        // Verify that the text is drawn on the canvas
        verify(canvas).drawText(eq("Test Text"), eq(10f), eq(20f), any(Paint.class));
    }
    
    @Test
    public void testDrawRect() {
        // Test the drawRect method
        renderManager.drawRect(10, 20, 110, 120);
        
        // Verify that the rectangle is drawn on the canvas
        verify(canvas).drawRect(eq(10f), eq(20f), eq(110f), eq(120f), any(Paint.class));
    }
    
    @Test
    public void testFillRect() {
        // Test the fillRect method
        renderManager.fillRect(10, 20, 110, 120);
        
        // Verify that the filled rectangle is drawn on the canvas
        verify(canvas).drawRect(eq(10f), eq(20f), eq(110f), eq(120f), any(Paint.class));
    }
}
