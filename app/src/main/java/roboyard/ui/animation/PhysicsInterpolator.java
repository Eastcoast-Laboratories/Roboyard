package roboyard.ui.animation;

import android.animation.TimeInterpolator;

/**
 * Custom interpolator that simulates physics-based movement including
 * acceleration, constant speed, deceleration, and spring-back overshoot.
 */
public class PhysicsInterpolator implements TimeInterpolator {
    // Animation parameters
    private final RobotAnimationManager.AnimationParameters params;
    
    /**
     * Create a new physics interpolator
     * @param params Animation parameters to use
     */
    public PhysicsInterpolator(RobotAnimationManager.AnimationParameters params) {
        this.params = params;
    }
    
    @Override
    public float getInterpolation(float input) {
        // Calculate phase fractions
        float accelerationFraction = params.accelerationDuration / params.totalDuration;
        float decelerationStartFraction = 1.0f - (params.decelerationDuration / params.totalDuration);
        float springBackStartFraction = 1.0f;
        
        // Acceleration phase (ease-in quadratic)
        if (input < accelerationFraction) {
            return (input / accelerationFraction) * (input / accelerationFraction);
        }
        // Constant speed phase (linear)
        else if (input < decelerationStartFraction) {
            float normalizedInput = (input - accelerationFraction) / 
                                   (decelerationStartFraction - accelerationFraction);
            return accelerationFraction * accelerationFraction + 
                   (1.0f - accelerationFraction * accelerationFraction - 
                    (1.0f - decelerationStartFraction) * (1.0f - decelerationStartFraction)) * 
                   normalizedInput;
        }
        // Deceleration phase (ease-out quadratic)
        else if (input <= springBackStartFraction) {
            float normalizedInput = (input - decelerationStartFraction) / 
                                   (springBackStartFraction - decelerationStartFraction);
            return decelerationStartFraction + 
                   (1.0f - decelerationStartFraction) * 
                   (1.0f - (1.0f - normalizedInput) * (1.0f - normalizedInput));
        }
        // Overshoot and spring-back phase (damped sine wave)
        else {
            float overshootInput = (input - springBackStartFraction) / 
                                  (params.springBackDuration / params.totalDuration);
            
            // Damped sine wave for spring effect
            return 1.0f + params.overshootPercentage * 
                   (float)Math.sin(overshootInput * Math.PI) * 
                   (float)Math.exp(-4 * overshootInput);
        }
    }
}
