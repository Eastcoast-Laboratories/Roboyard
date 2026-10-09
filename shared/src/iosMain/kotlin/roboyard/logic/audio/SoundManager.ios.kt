package roboyard.logic.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSBundle
import platform.Foundation.NSLog
import platform.Foundation.NSURL
import roboyard.logic.core.Preferences

/**
 * iOS implementation of SoundManager via AVAudioPlayer.
 *
 * Same semantics as the desktop/Android versions: one-shot effects are
 * skipped while another is playing, "preview" bypasses the enabled gate,
 * background music loops and follows backgroundSoundVolume.
 * Sound files are expected at sounds/<name>.mp3 inside the app bundle.
 */
@OptIn(ExperimentalForeignApi::class)
class IosSoundManager : SoundManager {

    private var currentPlayer: AVAudioPlayer? = null
    private var backgroundPlayer: AVAudioPlayer? = null

    override fun playSound(soundId: String) {
        playSound(soundId, -1, -1)
    }

    override fun playSound(soundId: String, attackerRobotId: Int, targetRobotId: Int) {
        // "preview" bypasses the enabled gate — the settings volume preview
        // must stay audible while game sounds are off (Android parity)
        if (soundId != "preview" && !isSoundEnabled()) return
        val name = resolveSoundName(soundId, attackerRobotId, targetRobotId) ?: return
        val url = soundUrl(name) ?: run {
            NSLog("[SOUND] Missing sound resource: $name")
            return
        }

        // Android semantics: skip while another effect is still playing
        if (currentPlayer?.playing == true) return

        val player = AVAudioPlayer(contentsOfURL = url, error = null) ?: run {
            NSLog("[SOUND] Failed to create player for $name")
            return
        }
        player.volume = Preferences.soundEffectsVolume.coerceIn(0, 100) / 100f
        player.play()
        currentPlayer = player
    }

    private fun resolveSoundName(soundId: String, attackerRobotId: Int, targetRobotId: Int): String? {
        if (soundId == "hit_robot" &&
            attackerRobotId in 0..4 && targetRobotId in 0..4
        ) {
            val specific = "robot_${attackerRobotId}_hits_robot_${targetRobotId}"
            return if (soundUrl(specific) != null) specific else "robot_hit_robot"
        }
        return when (soundId) {
            "move" -> "robot_move"
            "hit_wall", "lose" -> "robot_hit_wall"
            "hit_robot" -> "robot_hit_robot"
            "win" -> "robot_win"
            "preview" -> "robot_1_hit_wall"
            "none" -> null
            else -> {
                NSLog("[SOUND] Unknown sound type: $soundId")
                null
            }
        }
    }

    private fun soundUrl(name: String): NSURL? {
        val bundle = NSBundle.mainBundle
        val path = bundle.pathForResource(name, "mp3", "sounds")
            ?: bundle.pathForResource(name, "mp3", "compose-resources/sounds")
            ?: bundle.pathForResource(name, "mp3", null)
        return path?.let { NSURL.fileURLWithPath(it) }
    }

    override fun stopAll() {
        currentPlayer?.stop()
        currentPlayer = null
        stopBackground()
    }

    override fun setBackgroundVolume(volume: Int) {
        val v = volume.coerceIn(0, 100)
        // Android parity: updateBackgroundSoundService stops the service at 0
        if (v <= 0) {
            stopBackground()
            return
        }
        ensureBackgroundStarted()
        backgroundPlayer?.volume = v / 100f
    }

    override fun pauseBackground() {
        backgroundPlayer?.pause()
    }

    override fun resumeBackground() {
        ensureBackgroundStarted()
        backgroundPlayer?.play()
    }

    private fun ensureBackgroundStarted() {
        if (backgroundPlayer != null) return
        val url = soundUrl("singing_bowls_in_a_forest") ?: run {
            NSLog("[SOUND] Missing background music resource")
            return
        }
        val player = AVAudioPlayer(contentsOfURL = url, error = null) ?: return
        player.numberOfLoops = -1 // loop forever
        player.volume = Preferences.backgroundSoundVolume.coerceIn(0, 100) / 100f
        player.play()
        backgroundPlayer = player
    }

    private fun stopBackground() {
        backgroundPlayer?.stop()
        backgroundPlayer = null
    }

    override fun isSoundEnabled(): Boolean = Preferences.soundEnabled

    override fun setSoundEnabled(enabled: Boolean) {
        Preferences.soundEnabled = enabled
        if (!enabled) stopAll()
    }
}

// Android parity: SoundManager is a singleton — a new instance per call
// cannot pause/resume/change the volume of the player that is running.
private val instance by lazy { IosSoundManager() }

actual fun getSoundManager(): SoundManager {
    return instance
}
