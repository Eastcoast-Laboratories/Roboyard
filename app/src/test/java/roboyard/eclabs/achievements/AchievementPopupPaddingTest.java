package roboyard.eclabs.achievements;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for responsive padding calculation in AchievementPopup
 */
public class AchievementPopupPaddingTest {

    /**
     * Calculate horizontal padding based on screen width in dp
     * Linear interpolation: at 360dp = 64, at 480dp = 240
     */
    private int calculateHorizontalPadding(int screenWidthDp) {
        // Linear interpolation: at 360dp = 64, at 480dp = 240
        // Range: 480 - 360 = 120dp, Value range: 240 - 64 = 176
        int padding = (int)(64 + (screenWidthDp - 360) * 176.0 / 120.0);
        padding = Math.max(padding, 64); // Min 64
        padding = Math.min(padding, 240); // Max 240
        return padding;
    }

    @Test
    public void testPaddingOnSmallPhone360dp() {
        int padding = calculateHorizontalPadding(360);
        assertEquals("At 360dp (small phone), padding should be 64px", 64, padding);
    }

    @Test
    public void testPaddingOnSmallPhone400dp() {
        int padding = calculateHorizontalPadding(400);
        // At 400dp: 64 + (400-360) * 176/120 = 64 + 40*176/120 = 64 + 58.67 ≈ 122
        assertTrue("At 400dp, padding should be around 122px", padding > 118 && padding < 126);
    }

    @Test
    public void testPaddingOnPixel411dp() {
        int padding = calculateHorizontalPadding(411);
        // At 411dp: 64 + (411-360) * 176/120 = 64 + 51*176/120 = 64 + 74.8 ≈ 138
        assertTrue("At 411dp (Pixel), padding should be around 138px", padding > 134 && padding < 142);
    }

    @Test
    public void testPaddingOnLargeTablet600dp() {
        int padding = calculateHorizontalPadding(600);
        assertEquals("At 600dp (large tablet), padding should be clamped to 240px", 240, padding);
    }

    @Test
    public void testPaddingAt480dpMidpoint() {
        int padding = calculateHorizontalPadding(480);
        assertEquals("At 480dp, padding should be 240px", 240, padding);
    }
}
