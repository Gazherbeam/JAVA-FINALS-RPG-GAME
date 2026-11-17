package main;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Small background-music player for the game.
 * Uses Java's built-in audio support, so music files should be WAV format.
 */
public class Music {

    // =====================================================================
    // MUSIC DIRECTORY: PUT YOUR MUSIC FILES IN res/Soundtrack/
    // Example: res/Soundtrack/title_theme.wav
    // The res folder must be configured as a resources/classpath root.
    // =====================================================================
    private static final String MUSIC_DIRECTORY = "/Soundtrack/";

    private Clip currentClip;
    private boolean enabled = true;
    private float volume = 0.5f; // Range: 0.0 (silent) to 1.0 (full volume)

    /** Loads a WAV file from res/Soundtrack/ and loops it until another track is played or stopped. */
    public synchronized void play(String fileName) {
        stop();
        if (fileName == null || fileName.trim().isEmpty()) {
            System.err.println("Music: no filename was supplied.");
            return;
        }

        String resourcePath = MUSIC_DIRECTORY + fileName;
        try (InputStream raw = getClass().getResourceAsStream(resourcePath)) {
            if (raw == null) {
                System.err.println("Music: file not found: " + resourcePath);
                return;
            }
            try (AudioInputStream audio = AudioSystem.getAudioInputStream(
                    new BufferedInputStream(raw))) {
                currentClip = AudioSystem.getClip();
                currentClip.open(audio);
                applyVolume();
                if (enabled) currentClip.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } catch (Exception ex) {
            System.err.println("Music: could not play " + resourcePath + " (use a supported WAV file).");
            ex.printStackTrace();
            stop();
        }
    }

    /** Enables or mutes music. The current track resumes from the beginning when re-enabled. */
    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (currentClip == null || !currentClip.isOpen()) return;
        if (enabled) {
            if (!currentClip.isRunning()) {
                currentClip.setFramePosition(0);
                currentClip.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } else {
            currentClip.stop();
        }
    }

    /** Sets music volume from 0.0 to 1.0. */
    public synchronized void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        applyVolume();
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    /** Stops and releases the currently loaded track. */
    public synchronized void stop() {
        if (currentClip != null) {
            currentClip.stop();
            currentClip.close();
            currentClip = null;
        }
    }

    private void applyVolume() {
        if (currentClip == null || !currentClip.isOpen()) return;
        if (currentClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) currentClip.getControl(FloatControl.Type.MASTER_GAIN);
            if (volume <= 0.0001f) {
                gain.setValue(gain.getMinimum());
            } else {
                float decibels = (float) (20.0 * Math.log10(volume));
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
            }
        }
    }
}
