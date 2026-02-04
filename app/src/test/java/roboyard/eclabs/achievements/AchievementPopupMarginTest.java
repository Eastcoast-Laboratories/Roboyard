package roboyard.eclabs.achievements;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for topMargin interpolation calculation in AchievementPopup
 */
public class AchievementPopupMarginTest {

    /**
     * Calculate topMargin based on the smaller screen side in dp.
     * This ensures consistent positioning across portrait/landscape.
     * Requirements:
     * - At 360dp (small phone): topMargin = -60
     * - At 411dp (Pixel): topMargin = +44
     */
    private int calculateTopMargin(int screenWidthDp, int screenHeightDp) {
        int minSideDp = Math.min(screenWidthDp, screenHeightDp);
        // Linear interpolation: at 360dp = -60, at 411dp = +44
        // Range: 411 - 360 = 51dp, Value range: 44 - (-60) = 104
        return (int)(-60 + (minSideDp - 360) * 104.0 / 51.0);
    }

    @Test
    public void testTopMarginAt360dp() {
        int margin = calculateTopMargin(360, 800);
        assertEquals("At 360dp, topMargin should be -60", -60, margin);
    }

    @Test
    public void testTopMarginAt411dp() {
        int margin = calculateTopMargin(411, 900);
        assertEquals("At 411dp (Pixel), topMargin should be +44", 44, margin);
    }

    @Test
    public void testTopMarginAt385dp() {
        // Midpoint between 360 and 411
        int margin = calculateTopMargin(385, 900);
        // -60 + 25 * (104/51) ~= -60 + 50.98 ~= -9
        assertTrue("At 385dp, topMargin should be around -9", margin > -15 && margin < -3);
    }

    @Test
    public void testPortraitAndLandscapeUseSameMinSideDp() {
        int portrait = calculateTopMargin(411, 891);
        int landscape = calculateTopMargin(891, 411);
        assertEquals("Portrait and landscape should yield same topMargin when minSideDp is equal", portrait, landscape);
    }
}
