package roboyard.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ToggleButton;

/**
 * A ToggleButton that ignores setVisibility(GONE) calls.
 * Used in the alternative layout so the eye-toggle stays visible
 * even when the fragment tries to hide it.
 */
public class LiiveModeToggleButtonAlt extends ToggleButton {

    public LiiveModeToggleButtonAlt(Context context) {
        super(context);
    }

    public LiiveModeToggleButtonAlt(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LiiveModeToggleButtonAlt(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setVisibility(int visibility) {
        if (visibility == GONE) {
            return; // stay visible in alt layout
        }
        super.setVisibility(visibility);
    }
}
