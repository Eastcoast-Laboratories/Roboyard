package roboyard.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ToggleButton;

/**
 * A ToggleButton that ignores setVisibility(GONE) calls.
 * Used in the alternative layout so the live-move-toggle stays visible
 * even when the fragment tries to hide it.
 */
public class LiveModeToggleButtonAlt extends ToggleButton {

    public LiveModeToggleButtonAlt(Context context) {
        super(context);
    }

    public LiveModeToggleButtonAlt(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LiveModeToggleButtonAlt(Context context, AttributeSet attrs, int defStyleAttr) {
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
