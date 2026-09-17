package roboyard.logic.audio

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.SampleBuffer
import roboyard.logic.core.Preferences

/**
 * Desktop implementation of SoundManager.
 *
 * Mirrors Android roboyard.ui.util.SoundManager semantics:
 * - one-shot sound effects, a new sound is skipped while another is playing
 * - robot-specific collision sounds robot_{a}_hits_robot_{t} with generic fallback
 * - "lose" maps to hit_wall, "none" plays nothing
 * - effect volume from Preferences.soundEffectsVolume (0-100)
 * - looping background music at Preferences.backgroundSoundVolume (0-100)
 *
 * MP3 files are decoded to PCM once via JLayer and cached, then played through
 * javax.sound.sampled clips so per-play volume control is cheap.
 */
class DesktopSoundManager : SoundManager {

    private data class DecodedSound(val pcm: ByteArray, val format: AudioFormat)

    private val decodedCache = ConcurrentHashMap<String, DecodedSound?>()

    @Volatile
    private var currentClip: Clip? = null

    @Volatile
    private var backgroundClip: Clip? = null

    override fun playSound(soundId: String) {
        playSound(soundId, -1, -1)
    }

    override fun playSound(soundId: String, attackerRobotId: Int, targetRobotId: Int) {
        if (!isSoundEnabled()) return
        val resourceName = resolveSoundName(soundId, attackerRobotId, targetRobotId) ?: return
        val decoded = load(resourceName) ?: run {
            System.err.println("[SOUND] Missing sound resource: $resourceName")
            return
        }

        // Android semantics: skip while another effect is still playing
        val playing = currentClip
        if (playing != null && playing.isRunning) return

        try {
            val clip = AudioSystem.getClip()
            clip.open(decoded.format, decoded.pcm, 0, decoded.pcm.size)
            setClipVolume(clip, Preferences.soundEffectsVolume)
            currentClip = clip
            clip.start()
        } catch (e: Exception) {
            System.err.println("[SOUND] Failed to play $resourceName: ${e.message}")
        }
    }

    private fun resolveSoundName(soundId: String, attackerRobotId: Int, targetRobotId: Int): String? {
        if (soundId == "hit_robot" &&
            attackerRobotId in 0..4 && targetRobotId in 0..4
        ) {
            val specific = "robot_${attackerRobotId}_hits_robot_${targetRobotId}"
            return if (resourceExists(specific)) specific else "robot_hit_robot"
        }
        return when (soundId) {
            "move" -> "robot_move"
            "hit_wall", "lose" -> "robot_hit_wall"
            "hit_robot" -> "robot_hit_robot"
            "win" -> "robot_win"
            "none" -> null
            else -> {
                System.err.println("[SOUND] Unknown sound type: $soundId")
                null
            }
        }
    }

    private fun resourceExists(name: String): Boolean =
        javaClass.getResource("/sounds/$name.mp3") != null

    private fun load(name: String): DecodedSound? {
        if (decodedCache.containsKey(name)) return decodedCache[name]
        val decoded = decodeMp3(name)
        decodedCache[name] = decoded
        return decoded
    }

    private fun decodeMp3(name: String): DecodedSound? {
        val input = javaClass.getResourceAsStream("/sounds/$name.mp3") ?: return null
        return try {
            val bitstream = Bitstream(input.buffered())
            val decoder = Decoder()
            val out = ByteArrayOutputStream()
            var format: AudioFormat? = null
            while (true) {
                val header = bitstream.readFrame() ?: break
                val buffer = decoder.decodeFrame(header, bitstream) as SampleBuffer
                if (format == null) {
                    format = AudioFormat(
                        decoder.outputFrequency.toFloat(),
                        16,
                        decoder.outputChannels,
                        true,
                        false // little-endian PCM
                    )
                }
                val samples = buffer.buffer
                for (i in 0 until buffer.bufferLength) {
                    val s = samples[i].toInt()
                    out.write(s and 0xFF)
                    out.write((s shr 8) and 0xFF)
                }
                bitstream.closeFrame()
            }
            bitstream.close()
            format?.let { DecodedSound(out.toByteArray(), it) }
        } catch (e: Exception) {
            System.err.println("[SOUND] Failed to decode $name: ${e.message}")
            null
        }
    }

    private fun setClipVolume(clip: Clip, volumePercent: Int) {
        val vol = volumePercent.coerceIn(0, 100) / 100f
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val dB = if (vol <= 0f) gain.minimum
            else (20f * Math.log10(vol.toDouble())).toFloat().coerceIn(gain.minimum, gain.maximum)
            gain.value = dB
        }
    }

    override fun stopAll() {
        currentClip?.let {
            if (it.isRunning) it.stop()
            it.close()
        }
        currentClip = null
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
        backgroundClip?.let { setClipVolume(it, v) }
    }

    override fun pauseBackground() {
        backgroundClip?.let { if (it.isRunning) it.stop() }
    }

    override fun resumeBackground() {
        ensureBackgroundStarted()
        backgroundClip?.let { if (!it.isRunning) it.start() }
    }

    private fun ensureBackgroundStarted() {
        if (backgroundClip != null) return
        val decoded = load("singing_bowls_in_a_forest") ?: run {
            System.err.println("[SOUND] Missing background music resource")
            return
        }
        try {
            val clip = AudioSystem.getClip()
            clip.open(decoded.format, decoded.pcm, 0, decoded.pcm.size)
            setClipVolume(clip, Preferences.backgroundSoundVolume)
            clip.loop(Clip.LOOP_CONTINUOUSLY)
            backgroundClip = clip
        } catch (e: Exception) {
            System.err.println("[SOUND] Failed to start background music: ${e.message}")
        }
    }

    private fun stopBackground() {
        backgroundClip?.let {
            if (it.isRunning) it.stop()
            it.close()
        }
        backgroundClip = null
    }

    override fun isSoundEnabled(): Boolean = Preferences.soundEnabled

    override fun setSoundEnabled(enabled: Boolean) {
        Preferences.soundEnabled = enabled
        if (!enabled) stopAll()
    }
}

// Android parity: roboyard.ui.util.SoundManager is a singleton — a new
// instance per call cannot pause/resume/change the volume of the clip
// that is actually playing.
private val instance by lazy { DesktopSoundManager() }

actual fun getSoundManager(): SoundManager {
    return instance
}
