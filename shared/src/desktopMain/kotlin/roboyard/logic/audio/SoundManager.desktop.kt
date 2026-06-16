package roboyard.logic.audio

/**
 * Desktop implementation of SoundManager.
 * Sound is not implemented on Desktop, so this is a no-op.
 */
class DesktopSoundManager : SoundManager {
    override fun playSound(soundId: String) {
        // No-op on Desktop
    }
    
    override fun stopAll() {
        // No-op on Desktop
    }
    
    override fun setBackgroundVolume(volume: Int) {
        // No-op on Desktop
    }
    
    override fun pauseBackground() {
        // No-op on Desktop
    }
    
    override fun resumeBackground() {
        // No-op on Desktop
    }
    
    override fun isSoundEnabled(): Boolean {
        return false
    }
    
    override fun setSoundEnabled(enabled: Boolean) {
        // No-op on Desktop
    }
}

actual fun getSoundManager(): SoundManager {
    return DesktopSoundManager()
}
