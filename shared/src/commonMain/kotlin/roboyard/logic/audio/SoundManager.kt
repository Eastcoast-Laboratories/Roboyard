package roboyard.logic.audio

/**
 * Interface for managing sound effects and background music in a platform-agnostic way.
 */
interface SoundManager {
    /**
     * Play a sound effect.
     * @param soundId The ID of the sound to play
     */
    fun playSound(soundId: String)
    
    /**
     * Stop all sounds.
     */
    fun stopAll()
    
    /**
     * Set the volume for background music.
     * @param volume Volume level (0-100)
     */
    fun setBackgroundVolume(volume: Int)
    
    /**
     * Pause background music.
     */
    fun pauseBackground()
    
    /**
     * Resume background music.
     */
    fun resumeBackground()
    
    /**
     * Check if sound is enabled.
     */
    fun isSoundEnabled(): Boolean
    
    /**
     * Enable or disable sound.
     * @param enabled Whether sound should be enabled
     */
    fun setSoundEnabled(enabled: Boolean)
}

/**
 * Factory function to get the platform-specific sound manager.
 */
expect fun getSoundManager(): SoundManager
