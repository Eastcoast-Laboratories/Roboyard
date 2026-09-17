package roboyard.logic.audio

import android.content.Context

/**
 * Android implementation of SoundManager.
 * Simplified version that stores sound settings in preferences.
 * Full SoundService integration will be done when MainActivity is replaced.
 */
class AndroidSoundManager(private val context: Context) : SoundManager {
    
    private var soundEnabled = true
    private var backgroundVolume = 50
    
    override fun playSound(soundId: String) {
        // Placeholder for sound playback
        // Will be integrated with SoundService when MainActivity is replaced
    }
    
    override fun stopAll() {
        // Placeholder for stopping all sounds
    }
    
    override fun setBackgroundVolume(volume: Int) {
        backgroundVolume = volume.coerceIn(0, 100)
    }
    
    override fun pauseBackground() {
        // Placeholder for pausing background music
    }
    
    override fun resumeBackground() {
        // Placeholder for resuming background music
    }
    
    override fun isSoundEnabled(): Boolean {
        return soundEnabled
    }
    
    override fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }
}

private var registeredSoundManager: SoundManager? = null

/** Called once by the app module's Application class (mirrors initPlatformStorage). */
fun initSoundManager(soundManager: SoundManager) {
    registeredSoundManager = soundManager
}

actual fun getSoundManager(): SoundManager {
    return registeredSoundManager
        ?: throw IllegalStateException(
            "SoundManager not initialized. Call initSoundManager() from the Application class."
        )
}
